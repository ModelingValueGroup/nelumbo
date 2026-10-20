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

import java.util.List;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.Diagnostic;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;

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
