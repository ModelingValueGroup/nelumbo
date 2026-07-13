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

package org.modelingvalue.nelumbo.mcp;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.mcp.Hints.Hint;

public class HintsTest {

    @Test
    public void newlineTokenErrorGetsPrecedenceHint() {
        // The parser uses U.traceable() which renders literal newline as the two-char sequence \n
        Hint h = Hints.hintFor("Unexpected token '\\n', expected '('");
        assertTrue(h.text().contains("#0"));
        assertTrue(h.docRef().contains("precedence"));
    }

    @Test
    public void expectationMismatchGetsExpectedResultHint() {
        Hint h = Hints.hintFor("Expected result [[(f=99)],true,[],true], found [[(f=5)],true,[],true]");
        assertTrue(h.docRef().contains("test-expression"));
        assertTrue(h.text().contains("falsehoods"));
        assertTrue(h.text().contains("open"));
    }

    @Test
    public void otherUnexpectedTokenGetsGrammarHint() {
        Hint h = Hints.hintFor("Unexpected token 'blarg', expected NAME");
        assertTrue(h.docRef().contains("grammar"));
    }

    @Test
    public void unknownMessageGetsNoHint() {
        assertNull(Hints.hintFor("something else entirely"));
    }
}
