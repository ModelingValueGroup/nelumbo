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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;

public class NelumboEvaluatorTest {

    private static final String FIB = """
            import  nelumbo.integers

            Integer ::= fib(<Integer>)

            Integer n, f

            fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                          f=fib(n-1)+fib(n-2) if n>1

            fib(5)=f    ? [(f=5)][..]
            """;

    @Test
    public void goodFile() {
        EvalResult r = NelumboEvaluator.evaluate(FIB, "good.nl", 0);
        assertTrue(r.ok(), () -> "diagnostics: " + r.diagnostics());
        assertTrue(r.diagnostics().isEmpty());
        assertEquals(1, r.queries().size());
        assertNotNull(r.queries().get(0).result());
        assertEquals(Boolean.TRUE, r.queries().get(0).expectationMatched());
    }

    @Test
    public void parseError() {
        EvalResult r = NelumboEvaluator.evaluate("flurb @@ blarg\n", "bad.nl", 0);
        assertFalse(r.ok());
        assertFalse(r.diagnostics().isEmpty());
        assertTrue(r.diagnostics().get(0).line() >= 1);
        assertTrue(r.diagnostics().get(0).col() >= 1);
        assertNotNull(r.diagnostics().get(0).message());
    }

    @Test
    public void expectationMismatch() {
        String src = FIB.replace("[(f=5)]", "[(f=99)]");
        EvalResult r = NelumboEvaluator.evaluate(src, "mismatch.nl", 0);
        assertFalse(r.ok());
        assertTrue(r.diagnostics().stream().anyMatch(d -> d.message().startsWith("Expected result ")));
        assertEquals(1, r.queries().size());
        assertEquals(Boolean.FALSE, r.queries().get(0).expectationMatched());
    }

    @Test
    public void deadline() {
        String src = """
                import  nelumbo.integers

                Integer ::= loop(<Integer>)

                Integer n, f

                loop(n)=f <=>  loop(n+1)=f | loop(n+2)=f if n>=0

                loop(0)=f ?
                """;
        EvalResult r = NelumboEvaluator.evaluate(src, "loop.nl", 300);
        assertFalse(r.ok());
        assertTrue(r.diagnostics().stream().anyMatch(d -> d.message().contains("deadline")),
                () -> "diagnostics: " + r.diagnostics());
    }

    @Test
    public void missingTrailingNewlineIsTolerated() {
        EvalResult r = NelumboEvaluator.evaluate("import  nelumbo.integers", "nonewline.nl", 0);
        assertTrue(r.ok(), () -> "diagnostics: " + r.diagnostics());
    }

    @Test
    public void declarationsAccumulateAcrossSessionEvaluations() {
        String setup = """
                import  nelumbo.integers

                Integer ::= fib(<Integer>)

                Integer n, f

                fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                              f=fib(n-1)+fib(n-2) if n>1
                """;
        NelumboEvaluator.SessionResult first = NelumboEvaluator.evaluate(org.modelingvalue.nelumbo.KnowledgeBase.BASE, setup, "<setup>", 0);
        assertTrue(first.result().ok(), () -> "diagnostics: " + first.result().diagnostics());
        // the fib declaration from the first evaluation must be visible in the second
        NelumboEvaluator.SessionResult second = NelumboEvaluator.evaluate(first.kb(), "Integer r\nfib(7)=r ?\n", "<query>", 0);
        assertTrue(second.result().ok(), () -> "diagnostics: " + second.result().diagnostics());
        assertEquals(1, second.result().queries().size());
        assertTrue(second.result().queries().get(0).result().contains("r=13"),
                () -> "result: " + second.result().queries().get(0).result());
    }

    @Test
    public void variablesPersistAcrossSessionEvaluationsWhenWorldScoped() {
        org.modelingvalue.nelumbo.KnowledgeBase session = new org.modelingvalue.nelumbo.KnowledgeBase(org.modelingvalue.nelumbo.KnowledgeBase.BASE);
        session.setWorldScopedVariables(true);
        NelumboEvaluator.SessionResult imported = NelumboEvaluator.evaluate(session, "import nelumbo.integers\n", "<1>", 0);
        assertTrue(imported.result().ok(), () -> "diagnostics: " + imported.result().diagnostics());
        NelumboEvaluator.SessionResult declared = NelumboEvaluator.evaluate(imported.kb(), "Integer r\n", "<2>", 0);
        assertTrue(declared.result().ok(), () -> "diagnostics: " + declared.result().diagnostics());
        // r was declared in a previous evaluation and must still be usable here
        NelumboEvaluator.SessionResult queried = NelumboEvaluator.evaluate(declared.kb(), "3*4=r ?\n", "<3>", 0);
        assertTrue(queried.result().ok(), () -> "diagnostics: " + queried.result().diagnostics());
        assertEquals(1, queried.result().queries().size());
        assertTrue(queried.result().queries().get(0).result().contains("r=12"),
                () -> "result: " + queried.result().queries().get(0).result());
        // redeclaring the same variable in a later evaluation must stay harmless
        NelumboEvaluator.SessionResult redeclared = NelumboEvaluator.evaluate(queried.kb(), "Integer r\n3*3=r ?\n", "<4>", 0);
        assertTrue(redeclared.result().ok(), () -> "diagnostics: " + redeclared.result().diagnostics());
        assertTrue(redeclared.result().queries().get(0).result().contains("r=9"),
                () -> "result: " + redeclared.result().queries().get(0).result());
    }
}
