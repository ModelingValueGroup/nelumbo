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
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
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
                lineOf(lines, "i last index [1,2] = -1") + ":1: IndexOutOfBoundsException in lastIndex([1,2],e,-1)");
        EvalResult result = evaluate(source, name, 60_000);
        List<String> found = result.diagnostics().stream().map(d -> d.line() + ":" + d.col() + ": " + d.message()).toList();
        assertEquals(expected.stream().sorted().toList(), found.stream().sorted().toList());
        List<String> unanswered = result.queries().stream().filter(q -> q.result() == null).map(QueryOutcome::query).sorted().toList();
        assertEquals(List.of("at-1in[5,6,7]", "at0in[]", "at3in[5,6,7]", "at3in[5,6,7]=5", "ifirstindex[1,2]=5", "ilastindex[1,2]=-1"),
                unanswered);
        List<QueryOutcome> checked = result.queries().stream().filter(q -> q.expectationMatched() != null).toList();
        assertEquals(7, checked.size(), "queries with an expectation: " + result.queries());
        assertTrue(checked.stream().allMatch(QueryOutcome::expectationMatched), "mismatch: " + checked);
    }

    @RepeatedTest(10)
    public void datetimeTest() {
        testResource("datetimeTest.nl");
    }

}
