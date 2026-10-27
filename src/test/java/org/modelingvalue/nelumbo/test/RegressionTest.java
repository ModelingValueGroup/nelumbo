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
import org.junit.jupiter.api.Timeout;
import org.modelingvalue.nelumbo.NelumboConstants;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.Diagnostic;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.QueryOutcome;

// Regression tests promoted from the known-bug suite (KnownBugsTest) once the
// engine fix landed. Each guards a previously-broken behavior; the .nl file's
// query expectation is the assertion (a mismatch throws ParseException and
// fails the test).
public class RegressionTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "true");
        setProp("TRACE_NELUMBO", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @RepeatedTest(10)
    public void repetitionCountLost() {
        testResource("repetition-count-lost.nl");
    }

    @RepeatedTest(10)
    public void stringConcatPrefix() {
        testResource("string-concat-prefix.nl");
    }

    @RepeatedTest(10)
    public void recursiveListConcatClassCast() {
        testResource("recursive-list-concat-classcast.nl");
    }

    @RepeatedTest(10)
    public void nestedListFunctorArgTypeMismatch() {
        testResource("nested-list-functor-arg-type-mismatch.nl");
    }

    @RepeatedTest(10)
    public void setFunctorRhsEqualsNotReduced() {
        testResource("set-functor-rhs-equals-not-reduced.nl");
    }

    @RepeatedTest(10)
    public void emptySetBranchInMapLambda() {
        testResource("empty-set-branch-in-map-lambda.nl");
    }

    @RepeatedTest(10)
    public void neighborQueryChangesResult() {
        testResource("neighbor-query-changes-result.nl");
    }

    // 10 repetitions double as the run-to-run determinism check
    @RepeatedTest(10)
    public void nondeterministicInference() {
        testResource("nondeterministic-inference.nl");
    }

    @RepeatedTest(10)
    public void rulePosOnMappedCollectionListUndecided() {
        testResource("rule-pos-on-mapped-collection-list-undecided.nl");
    }

    // a list element occurring twice has two indexes (was bugs/pos-duplicate-element-inconsistent.nl)
    @RepeatedTest(10)
    public void posDuplicateElementInconsistent() {
        testResource("pos-duplicate-element-inconsistent.nl");
    }

    // a call as the list argument of a rule over the index relation (was bugs/call-arg-unbinds-index-in-pos-rule.nl)
    @RepeatedTest(10)
    public void callArgUnbindsIndexInPosRule() {
        testResource("call-arg-unbinds-index-in-pos-rule.nl");
    }

    @RepeatedTest(10)
    public void inOnFunctionCallUndecided() {
        testResource("in-on-function-call-undecided.nl");
    }

    // the argument of the function is itself a call (user or built-in), also in a rule body
    @RepeatedTest(10)
    public void inOnNestedFunctionCallUndecided() {
        testResource("in-on-nested-function-call-undecided.nl");
    }

    // also covers the list-literal twin [j+1], which did not parse before the fix
    @RepeatedTest(10)
    public void setLiteralArithmeticUnevaluated() {
        testResource("set-literal-arithmetic-unevaluated.nl");
    }

    // 4 and 6 quantifier variables, in rule bodies and in plain queries
    @RepeatedTest(10)
    public void fourVarQuantifierCrash() {
        testResource("four-var-quantifier-crash.nl");
    }

    // Seq<T> must stay a Struct: as an Object its functor is Function-typed and `=` is undecided
    @RepeatedTest(10)
    public void repetitionSeparatorGreedy() {
        testResource("repetition-separator-greedy.nl");
    }

    @RepeatedTest(10)
    public void optionalPresenceLost() {
        testResource("optional-presence-lost.nl");
    }

    @RepeatedTest(10)
    public void alternationOptionIdentity() {
        testResource("alternation-option-identity.nl");
    }

    // was a load-time bug (~80s), so the timeout is the real assertion
    @RepeatedTest(10)
    @Timeout(20)
    public void nestedGenericRuleLoadTime() {
        testResource("nested-generic-rule-load-time.nl");
    }

    @RepeatedTest(10)
    public void unboundPredicateNotResolved() {
        testResource("unbound-predicate-not-resolved.nl");
    }

    // an index error next to a false bound check is irrelevant, in every operand order
    @RepeatedTest(10)
    public void speculativeGuardIndexCrash() {
        testResource("speculative-guard-index-crash.nl");
    }

    // An exception in a built-in operation leaves the query unknown: an error
    // with the exception's message and no result; the other queries still
    // evaluate (was bugs/integers-div-zero.nl, bugs/collections-index-out-of-range.nl
    // and bugs/datetime-multiply-overflow.nl, which crashed the whole evaluation).
    @RepeatedTest(10)
    public void nativeExceptionWithoutResult() throws IOException {
        String name = "native-exception-without-result.nl";
        String source = new String(RegressionTest.class.getResourceAsStream(NelumboConstants.NELUMBO_TESTS + name).readAllBytes());
        List<String> lines = source.lines().toList();
        // an exception without a message (IndexOutOfBoundsException) is named by
        // its class and the built-in call that threw it
        List<String> expected = List.of( //
                lineOf(lines, "6 / 0 = x") + ":1: BigInteger divide by zero", //
                lineOf(lines, "0 * x = 6") + ":1: BigInteger divide by zero", //
                lineOf(lines, "at 7 in [1,2,3] = i") + ":1: IndexOutOfBoundsException in index([1,2,3],e,7)", //
                lineOf(lines, "P1D * 9999999999 = y") + ":1: BigInteger out of int range");
        EvalResult result = NelumboEvaluator.evaluate(source, name, 60_000);
        List<String> found = result.diagnostics().stream().map(d -> d.line() + ":" + d.col() + ": " + d.message()).toList();
        assertEquals(expected.stream().sorted().toList(), found.stream().sorted().toList());
        List<String> unanswered = result.queries().stream().filter(q -> q.result() == null).map(QueryOutcome::query).sorted().toList();
        assertEquals(List.of("0*x=6", "6/0=x", "P1D*9999999999=y", "at7in[1,2,3]=i"), unanswered);
        // the queries after each error still evaluate, to their expected result
        List<QueryOutcome> checked = result.queries().stream().filter(q -> q.expectationMatched() != null).toList();
        assertEquals(4, checked.size(), "queries with an expectation: " + result.queries());
        assertTrue(checked.stream().allMatch(QueryOutcome::expectationMatched), "mismatch: " + checked);
    }

    // An undeclared name in a rule body is not a variable: the rule is not
    // well-formed, the parse error is on the name itself and the query is
    // unknown (was bugs/undeclared-variable-diagnostics.nl, whose error was on
    // a nearby token).
    @RepeatedTest(10)
    public void undeclaredNameInRule() throws IOException {
        String name = "undeclared-name-in-rule.nl";
        String source = new String(RegressionTest.class.getResourceAsStream(NelumboConstants.NELUMBO_TESTS + name).readAllBytes());
        List<String> lines = source.lines().toList();
        int line = lineOf(lines, "f(a,b)=z");
        int col = lines.get(line - 1).indexOf('q') + 1;
        EvalResult result = NelumboEvaluator.evaluate(source, name, 60_000);
        assertEquals(1, result.diagnostics().size(), "diagnostics: " + result.diagnostics());
        Diagnostic error = result.diagnostics().get(0);
        assertTrue(error.message().startsWith("Unexpected token 'q'"), "message: " + error.message());
        assertEquals(List.of(line, col, 1), List.of(error.line(), error.col(), error.length()), "position of " + error);
        assertEquals(1, result.queries().size(), "queries: " + result.queries());
        QueryOutcome query = result.queries().get(0);
        assertEquals("[..][..]", query.result());
        assertEquals(Boolean.TRUE, query.expectationMatched());
    }

    // A NEWLINE after a non-operator ends the statement: an `if` guard on its
    // own line is a parse error there, the alternative before it stays
    // guardless, and f(3) is a conflict (was bugs/guard-on-continuation-line-dropped.nl,
    // which took this for a bug).
    @RepeatedTest(10)
    public void guardOnOwnLine() throws IOException {
        String name = "guard-on-own-line.nl";
        String source = new String(RegressionTest.class.getResourceAsStream(NelumboConstants.NELUMBO_TESTS + name).readAllBytes());
        List<String> lines = source.lines().toList();
        int guardLine = lineOf(lines, "if x>=5");
        int guardCol = lines.get(guardLine - 1).indexOf('x') + 1;
        int conflictLine = lineOf(lines, "f(3)=y");
        EvalResult result = NelumboEvaluator.evaluate(source, name, 60_000);
        assertEquals(2, result.diagnostics().size(), "diagnostics: " + result.diagnostics());
        Diagnostic parse = result.diagnostics().get(0);
        assertTrue(parse.message().startsWith("Unexpected token 'x'"), "message: " + parse.message());
        assertEquals(List.of(guardLine, guardCol), List.of(parse.line(), parse.col()), "position of " + parse);
        Diagnostic conflict = result.diagnostics().get(1);
        assertTrue(conflict.message().startsWith("Inconsistent results"), "message: " + conflict.message());
        assertEquals(List.of(conflictLine, 1), List.of(conflict.line(), conflict.col()), "position of " + conflict);
        List<QueryOutcome> checked = result.queries().stream().filter(q -> q.expectationMatched() != null).toList();
        assertEquals(1, checked.size(), "queries with an expectation: " + result.queries());
        assertEquals(Boolean.TRUE, checked.get(0).expectationMatched(), "f(7): " + checked.get(0));
    }

    // A call evaluated shallowly inside a conjunction already decided false
    // leaves no remembered unknown: the same call later gets its value, and an
    // exception in it is still reported (was bugs/decided-conjunction-hides-later-result.nl).
    @RepeatedTest(10)
    public void decidedConjunctionHidesLaterResult() throws IOException {
        String name = "decided-conjunction-hides-later-result.nl";
        String source = new String(RegressionTest.class.getResourceAsStream(NelumboConstants.NELUMBO_TESTS + name).readAllBytes());
        List<String> lines = source.lines().toList();
        EvalResult result = NelumboEvaluator.evaluate(source, name, 60_000);
        List<String> found = result.diagnostics().stream().map(d -> d.line() + ":" + d.col() + ": " + d.message()).toList();
        assertEquals(List.of(lineOf(lines, "6 / 0 = a") + ":1: BigInteger divide by zero"), found);
        List<QueryOutcome> checked = result.queries().stream().filter(q -> q.expectationMatched() != null).toList();
        assertEquals(7, checked.size(), "queries with an expectation: " + result.queries());
        assertTrue(checked.stream().allMatch(QueryOutcome::expectationMatched), "mismatch: " + checked);
    }

    // Two rules of one relation that disagree are an inconsistency in every rule
    // order (RANDOM_NELUMBO): every query reports it and none has a result (was
    // bugs/inconsistency-missed-by-rule-order.nl, detected in one order only).
    @RepeatedTest(10)
    public void inconsistencyMissedByRuleOrder() throws IOException {
        String name = "inconsistency-missed-by-rule-order.nl";
        String source = new String(RegressionTest.class.getResourceAsStream(NelumboConstants.NELUMBO_TESTS + name).readAllBytes());
        EvalResult result = NelumboEvaluator.evaluate(source, name, 60_000);
        List<String> silent = result.queries().stream().filter(q -> q.result() != null).map(q -> q.query() + " ? " + q.result()).toList();
        assertEquals(List.of(), silent, "queries answered without reporting the inconsistency");
        long inconsistent = result.diagnostics().stream().filter(d -> d.message().startsWith("Inconsistent results")).count();
        assertEquals(32, result.queries().size(), "queries: " + result.queries());
        assertEquals(32, inconsistent, "diagnostics: " + result.diagnostics());
    }

    // A lambda variable ([i](...)) is local to its lambda: using the same
    // variable outside it is an error (was bugs/size-of-lambda-result-closes-falsehoods.nl,
    // where the reused i made the size queries report a closed falsehoods side).
    @RepeatedTest(10)
    public void localVariableUsedOutsideLocalContext() {
        String header = """
                import nelumbo.collections

                Integer i, n

                """;
        List<String> misuses = List.of( //
                "|[1,2,3] where [i](i>1)|=i ? [(i=2)][..]", //
                "|{1,2,3} where [i](i>1)|=i ? [(i=2)][..]", //
                "|[1,2,3] map [i](i+1)|=i ? [(i=3)][..]");
        for (String misuse : misuses) {
            EvalResult result = NelumboEvaluator.evaluate(header + misuse + "\n", "misuse.nl", 60_000);
            List<Diagnostic> local = result.diagnostics().stream() //
                    .filter(d -> d.message().startsWith("Local variable i used")).toList();
            assertTrue(!local.isEmpty(), "no local-variable error for: " + misuse + ", diagnostics: " + result.diagnostics());
            assertTrue(local.stream().allMatch(d -> d.line() == 5), "error on the wrong line: " + local);
        }
        // the same queries with a separate variable outside the lambda are fine
        String correct = header + """
                |[1,2,3] where [i](i>1)|=n ? [(n=2)][..]
                |{1,2,3} where [i](i>1)|=n ? [(n=2)][..]
                |[1,2,3] map [i](i+1)|=n ? [(n=3)][..]
                """;
        EvalResult result = NelumboEvaluator.evaluate(correct, "correct.nl", 60_000);
        assertEquals(List.of(), result.diagnostics());
        assertTrue(result.ok());
    }

}
