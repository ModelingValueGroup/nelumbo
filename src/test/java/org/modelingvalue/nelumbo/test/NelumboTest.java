//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.RepeatedTest;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstants;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.QueryOutcome;

public class NelumboTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @Override
    protected void settings(KnowledgeBase kb) {
        kb.setReverseOrder(env("REVERSE_NELUMBO", false));
        kb.setRandomOrder(env("RANDOM_NELUMBO", true));
        kb.setTrace(env("TRACE_NELUMBO", false));
    }

    @RepeatedTest(10)
    public void initTest() {
        String example = """
                // Init only
                """;
        testString(example, "NelumboTest.initTest");
    }

    @RepeatedTest(10)
    public void langOnly() {
        testResource("langOnly.nl");
    }

    @RepeatedTest(10)
    public void langTest() {
        testResource("langTest.nl");
    }

    @RepeatedTest(10)
    public void logicTest() {
        testResource("logicTest.nl");
    }

    @RepeatedTest(10)
    public void integersTest() {
        testResource("integersTest.nl");
    }

    @RepeatedTest(10)
    public void rationalsTest() {
        testResource("rationalsTest.nl");
    }

    @RepeatedTest(10)
    public void stringsTest() {
        testResource("stringsTest.nl");
    }

    @RepeatedTest(10)
    public void collectionsTest() {
        testResource("collectionsTest.nl");
    }

    // An index outside the list is an error, not false: the query has no result,
    // unless three-valued logic absorbs the error (true | error, false & error).
    @RepeatedTest(10)
    public void collectionsIndexErrorTest() throws IOException {
        String name = "collectionsIndexErrorTest.nl";
        String source = new String(NelumboTest.class.getResourceAsStream(NelumboConstants.NELUMBO_TESTS + name).readAllBytes());
        List<String> lines = source.lines().toList();
        List<String> expected = List.of( //
                lineOf(lines, "at 3 in [5,6,7]") + ":1: IndexOutOfBoundsException in index([5,6,7],e,3)", //
                lineOf(lines, "at -1 in [5,6,7]") + ":1: IndexOutOfBoundsException in index([5,6,7],e,-1)", //
                lineOf(lines, "at 3 in [5,6,7] = 5") + ":1: IndexOutOfBoundsException in index([5,6,7],5,3)", //
                lineOf(lines, "at 0 in []") + ":1: IndexOutOfBoundsException in index([],e,0)", //
                lineOf(lines, "i first index [1,2] = 5") + ":1: IndexOutOfBoundsException in firstIndex([1,2],e,5)", //
                lineOf(lines, "i last index [1,2] = -1") + ":1: IndexOutOfBoundsException in lastIndex([1,2],e,-1)", //
                lineOf(lines, "set 5 at 3 in [1,2,3]") + ":1: IndexOutOfBoundsException in set([1,2,3],3,5,l2)", //
                lineOf(lines, "set 5 at -1 in [1,2,3]") + ":1: IndexOutOfBoundsException in set([1,2,3],-1,5,l2)", //
                lineOf(lines, "set 5 at 0 in []") + ":1: IndexOutOfBoundsException in set([],0,5,l2)");
        EvalResult result = evaluate(source, name, 60_000);
        List<String> found = result.diagnostics().stream().map(d -> d.line() + ":" + d.col() + ": " + d.message()).toList();
        assertEquals(expected.stream().sorted().toList(), found.stream().sorted().toList());
        List<String> unanswered = result.queries().stream().filter(q -> q.result() == null).map(QueryOutcome::query).sorted().toList();
        assertEquals(List.of("at-1in[5,6,7]", "at0in[]", "at3in[5,6,7]", "at3in[5,6,7]=5", "ifirstindex[1,2]=5", "ilastindex[1,2]=-1",
                "set5at-1in[1,2,3]", "set5at0in[]", "set5at3in[1,2,3]"), unanswered);
        List<QueryOutcome> checked = result.queries().stream().filter(q -> q.expectationMatched() != null).toList();
        assertEquals(10, checked.size(), "queries with an expectation: " + result.queries());
        assertTrue(checked.stream().allMatch(QueryOutcome::expectationMatched), "mismatch: " + checked);
    }

    // A compound (!, &, |, ->, <->, a quantifier, true/false) is never looked up as a stored fact, so asserting one
    // would silently add nothing (and fact p(A), !p(A) would hide a contradiction): it is an error.
    @RepeatedTest(10)
    public void compoundFactIsAnError() {
        String header = """
                import nelumbo.logic
                Person :: Object
                Person ::= A, B
                Person x
                Boolean ::= p(<Person>), r(<Person>,<Person>)
                """;
        for (String fact : List.of("!p(A)", "p(A) & p(B)", "p(A) | p(B)", "p(A) -> p(B)", "p(A) <-> p(B)", "E[x](p(x))", "A[x](p(x))", "true", "false")) {
            EvalResult result = evaluate(header + "fact " + fact + "\n", "compoundFact", 10_000);
            List<String> found = result.diagnostics().stream().map(d -> d.line() + ": " + d.message()).toList();
            assertEquals(1, found.size(), fact + ": " + found);
            assertTrue(found.get(0).startsWith("6: Fact ") && found.get(0).contains("cannot be asserted"), fact + ": " + found);
        }
        EvalResult result = evaluate(header + "fact p(A), r(A,B), A=A\np(A) ? [()][]\nr(A,x) ? [(x=B),..][..]\n", "plainFacts", 10_000);
        assertEquals(List.of(), result.diagnostics());
        assertTrue(result.queries().stream().allMatch(QueryOutcome::expectationMatched), "mismatch: " + result.queries());
    }

    // A unique hole (<one T>) holds at most one value for each combination of the OTHER type holes. In a functor with
    // only that type hole there is nothing to be unique for (its facts were never closed, without a message): it is an
    // error at the hole. The functor is still declared, so the statements that use it give no further errors.
    @RepeatedTest(10)
    public void uniqueHoleNeedsAnotherHole() {
        String header = """
                import nelumbo.integers
                Person :: Object
                Person ::= Piet, Jan
                Person p
                Integer i
                """;
        // the last <one T> of each is the hole in error; a token hole (<NAME>) is no type hole
        for (String functor : List.of("Boolean ::= winner(<one Person>)", "Boolean ::= <one Person> wins #30", "Boolean ::= <hidden one Person#30> wins #30",
                "Integer ::= prize(<one Person>)", "Boolean ::= winners <(> <one Person> <)+> #30", "Boolean ::= winner <(> <one Person> <)?> #30",
                "Boolean ::= score of <NAME> is <one Integer> #30", "Boolean ::= age(<Person>,<one Integer>), winner(<one Person>)")) {
            EvalResult result = evaluate(header + functor + "\n", "uniqueHole", 10_000);
            int one = functor.lastIndexOf("one ");
            String type = functor.substring(one + 4).split("[#>]")[0];
            assertEquals(List.of("6:" + (functor.lastIndexOf('<', one) + 1) + ": " + uniqueHoleMessage(type)), diagnostics(result), functor);
        }
        // only the one error: the functor is declared, its fact and queries are evaluated (open: nothing is unique)
        EvalResult single = evaluate(header + "Boolean ::= winner(<one Person>)\nfact winner(Piet)\nwinner(Piet) ? [()][]\nwinner(Jan) ? [..][..]\n",
                "uniqueHoleUsed", 10_000);
        assertEquals(List.of("6:20: " + uniqueHoleMessage("Person")), diagnostics(single));
        assertEquals(2, single.queries().size());
        assertTrue(single.queries().stream().allMatch(QueryOutcome::expectationMatched), "mismatch: " + single.queries());
        // with a second type hole the unique hole is closed per value of the other one, whatever their order
        EvalResult result = evaluate(header + """
                Boolean ::= age(<Person>,<one Integer>), <one Person> is the mother of <Person> #30
                fact age(Piet,7), Jan is the mother of Piet
                age(Piet,i)                ? [(i=7)][..]
                age(Piet,8)                ? [][()]
                age(Jan,i)                 ? [..][..]
                p is the mother of Piet    ? [(p=Jan)][..]
                Piet is the mother of Piet ? [][()]
                """, "uniqueHoles", 10_000);
        assertEquals(List.of(), diagnostics(result));
        assertEquals(5, result.queries().size());
        assertTrue(result.queries().stream().allMatch(QueryOutcome::expectationMatched), "mismatch: " + result.queries());
    }

    private static String uniqueHoleMessage(String type) {
        return "Unique hole <one " + type + "> needs at least 2 type holes in its functor: it has at most one value for each combination of the other holes.";
    }

    private static List<String> diagnostics(EvalResult result) {
        return result.diagnostics().stream().map(d -> d.line() + ":" + d.col() + ": " + d.message()).toList();
    }

    @RepeatedTest(10)
    public void datetimeTest() {
        testResource("datetimeTest.nl");
    }

}
