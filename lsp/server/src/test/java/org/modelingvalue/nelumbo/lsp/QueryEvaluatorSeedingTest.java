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

package org.modelingvalue.nelumbo.lsp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class QueryEvaluatorSeedingTest {

    private static final String FIB_SEED = """
            import nelumbo.integers
            Integer ::= fib(<Integer>)
            Integer n, f
            fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1
            """;

    private static KnowledgeBase seeded(String source) {
        return KnowledgeBase.BASE.run(() -> {
            ParserResult result = new Parser(new Tokenizer(source, "seed.nl").tokenize()).parseNonThrowing();
            try {
                result.evaluate();
            } catch (ParseException e) {
                throw new RuntimeException("seed evaluation failed: " + e.getMessage(), e);
            }
        });
    }

    @Test
    public void evaluateUsesTheGivenBaseKb() {
        KnowledgeBase           kb      = seeded(FIB_SEED);
        String                  doc     = "Integer r\nfib(5)=r ?\n";
        Map<Query, QueryResult> results = QueryEvaluator.evaluate(kb, 0, doc, "inmemory://field-1.nl");
        assertEquals(1, results.size());
        QueryResult result = results.values().iterator().next();
        assertEquals(QueryResult.Kind.RESULT, result.kind());
        assertTrue(result.inferred().contains("5"), "expected fib(5) result to mention 5, got: " + result.inferred());
    }

    @Test
    public void withoutSeedingTheQueryDoesNotResolve() {
        String                  doc     = "Integer r\nfib(5)=r ?\n";
        Map<Query, QueryResult> results = QueryEvaluator.evaluate(KnowledgeBase.BASE, 0, doc, "inmemory://field-1.nl");
        boolean resolved = results.values().stream().anyMatch(r -> r.kind() == QueryResult.Kind.RESULT && r.inferred().contains("5"));
        assertFalse(resolved, "fib must be unknown without the seeded KB");
    }

    @Test
    public void deadlineProducesTimeoutMarkerInsteadOfHanging() {
        KnowledgeBase           kb      = seeded(FIB_SEED);
        String                  doc     = "Integer r\nfib(50)=r ?\n";
        Map<Query, QueryResult> results = QueryEvaluator.evaluate(kb, 1, doc, "inmemory://slow.nl");
        boolean timedOut = results.values().stream().anyMatch(r -> r.kind() == QueryResult.Kind.ERROR && r.inferred().toLowerCase().contains("deadline"));
        assertTrue(timedOut || results.isEmpty(), "expected a deadline marker or no results, got: " + results);
    }
}
