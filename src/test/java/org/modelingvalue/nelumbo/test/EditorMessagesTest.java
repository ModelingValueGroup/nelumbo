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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.tools.EditorMessages;

public class EditorMessagesTest {

    @Test
    void aQueryResultLandsOnItsLine() {
        EditorMessages m = new EditorMessages(3);
        m.setQuery(1, "[()][]", null, false);
        assertEquals("\n[()][]\n", m.text());
    }

    @Test
    void anErrorWinsOverAQueryOnItsLine() {
        EditorMessages m = new EditorMessages(2);
        m.setQuery(1, "[()][]", null, false);
        m.setFixed(1, "Unexpected token", new EditorMessages.Highlight(5, 1, "Unexpected token"));
        assertEquals("\nUnexpected token", m.text());
        assertEquals(1, m.textHighlights().size());
    }

    @Test
    void aMismatchHighlightsItsMessage() {
        EditorMessages m = new EditorMessages(3);
        m.setQuery(0, "abc", null, false);
        m.setQuery(2, "[][()]", new EditorMessages.Highlight(10, 5, "Expected result"), true);
        assertEquals(List.of(new EditorMessages.Highlight(5, 6, "Expected result")), m.messageHighlights());
    }
}
