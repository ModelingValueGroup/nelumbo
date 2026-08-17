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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class QueryResultTest {

    @Test
    public void shortLabelIsNotCapped() {
        QueryResult r = QueryResult.result("[()][]");
        assertEquals("[()][]", r.inlineLabel());
        assertEquals("[()][]", r.tooltip());
    }

    @Test
    public void longLabelIsCappedButTooltipIsFull() {
        String      full = "[(f=0),(f=1),(f=2),(f=3),(f=4),(f=5),(f=6),(f=7),(f=8),(f=9)][..]";
        QueryResult r    = QueryResult.result(full);
        assertEquals(60, r.inlineLabel().length(), "inline label is capped at 60 chars");
        assertTrue(r.inlineLabel().endsWith("..."), "capped label ends in an ellipsis");
        assertTrue(full.startsWith(r.inlineLabel().substring(0, 57)), "capped label is a prefix of the full result");
        assertEquals(full, r.tooltip(), "tooltip always carries the full result");
    }

    @Test
    public void matchShowsCheckmarkWithResultTooltip() {
        QueryResult r = QueryResult.match("[()][]");
        assertEquals("✅", r.inlineLabel());
        assertEquals("[()][]", r.tooltip(), "the tooltip reveals the result behind the checkmark");
    }

    @Test
    public void errorTooltipCarriesFullMessage() {
        QueryResult r = QueryResult.error("evaluation exceeded the deadline");
        assertEquals("⚠ evaluation exceeded the deadline", r.inlineLabel());
        assertEquals("⚠ evaluation exceeded the deadline", r.tooltip());
    }
}
