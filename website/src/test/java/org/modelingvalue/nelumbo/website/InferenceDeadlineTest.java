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

package org.modelingvalue.nelumbo.website;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

/**
 * Verifies the engine-level mechanism the HTTP timeout relies on: an inference whose knowledge base is past its
 * deadline aborts by throwing {@link NelumboTimeoutException}. Deterministic — the deadline is set in the past, so the
 * very first {@code fixpoint} check trips it regardless of timing.
 */
class InferenceDeadlineTest {

    private static final String FIB = """
            import nelumbo.integers

            Integer ::= fib(<Integer>)

            Integer n, f

            fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                          f=fib(n-1)+fib(n-2) if n>1
            """;

    @Test
    void inferencePastDeadlineThrows() {
        KnowledgeBase base = KnowledgeBaseLoader.load(List.of(new NamedSource("fib.nl", FIB)));
        KnowledgeBase request = new KnowledgeBase(base);
        request.setDeadlineNanos(System.nanoTime() - 1); // already expired

        assertThrows(NelumboTimeoutException.class, () -> request.run(() -> {
            ParserResult result = new Parser(new Tokenizer("Integer r\nfib(8)=r ?\n", "<t>").tokenize())
                    .parseNonThrowing();
            try {
                result.evaluate();
            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        }));
    }
}
