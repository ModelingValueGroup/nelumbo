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

package org.modelingvalue.nelumbo.lsp.workspaceService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class QueryExecutionFlowTest {

    private static final String KONINGSDAG = """

            import      nelumbo.integers

            DayOfWeek :: Object
            DayOfWeek ::= Mon, Tue, Wed, Thu, Fri, Sat, Sun

            FactType ::= weekdag van <Integer> april <Integer> is <DayOfWeek>
            Boolean  ::= Koningsdag <Integer> is op <Integer> april

            Integer    y, d
            DayOfWeek  w

            Koningsdag y is op d april <=> d=26 if weekdag van 27 april y is Sun,
                                            d=27 if E[w](weekdag van 27 april y is w & w!=Sun)

            fact weekdag van 27 april 2025 is Sun

            Koningsdag 2025 is op 27 april  ? [][()]
            """;

    /**
     * Mirrors the LSP "Query button" code path: parses and evaluates inside one BASE.run,
     * skipping every Query except the targeted one. The targeted query is a falsifying
     * query whose inference result must be `[][()]` (definitely false, no variables).
     *
     * Regression guard for the bug where the LSP previously parsed in one BASE.run and
     * evaluated in another, leaving user-declared types/patterns unregistered in the
     * inference KB and returning the incomplete `[..][..]` instead.
     */
    @Test
    void targetedFalsifyingQueryProducesDefiniteResult() {
        int[] targetLineIdx = {-1};
        String[] lines = KONINGSDAG.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Koningsdag 2025 is op 27 april")) {
                targetLineIdx[0] = i;
            }
        }
        if (targetLineIdx[0] < 0) {
            fail("test fixture corrupt: target query line not found");
        }

        String[] resultHolder = {null};
        KnowledgeBase.BASE.run(() -> {
            ParserResult parsed = new Parser(new Tokenizer(KONINGSDAG, "test").tokenize()).parseNonThrowing();

            Query target = null;
            for (Node root : parsed.roots()) {
                if (root instanceof Query q) {
                    Token first = q.firstToken();
                    if (first != null && first.line() == targetLineIdx[0]) {
                        target = q;
                        break;
                    }
                }
            }
            assertNotNull(target, "target query not found in parsed AST");
            final Query queryRef = target;

            ParserResult throwing = new ParserResult(null, true);
            try {
                for (Node root : parsed.roots()) {
                    if (root instanceof Evaluatable eval && (!(eval instanceof Query) || eval == queryRef)) {
                        eval.evaluate(KnowledgeBase.CURRENT.get(), throwing);
                    }
                }
            } catch (ParseException e) {
                fail(e);
            }
            resultHolder[0] = queryRef.inferResult() == null ? null : queryRef.inferResult().toString();
        });

        assertEquals("[][()]", resultHolder[0]);
    }
}
