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

package org.modelingvalue.nelumbo.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class QueryResultTest {

    @Test
    public void shortLabelIsNotCapped() {
        QueryResult r = QueryResult.result("[()][]", -1);
        assertEquals("[()][]", r.inlineLabel());
        assertEquals("", r.tooltip(), "the label shows the whole result, no time: nothing to add");
    }

    @Test
    public void longLabelIsCappedButTooltipIsFull() {
        String      full = "[(f=0),(f=1),(f=2),(f=3),(f=4),(f=5),(f=6),(f=7),(f=8),(f=9)][..]";
        QueryResult r    = QueryResult.result(full, -1);
        assertEquals(50, r.inlineLabel().length(), "inline label is capped at 50 chars: Monaco draws a longer run as two boxes");
        assertTrue(r.inlineLabel().endsWith("..."), "capped label ends in an ellipsis");
        assertTrue(full.startsWith(r.inlineLabel().substring(0, 47)), "capped label is a prefix of the full result");
        assertEquals(full, r.tooltip(), "the tooltip carries the full result the label cut short");
        assertEquals(full + "  (7 \u03bcs)", QueryResult.result(full, 7).tooltip());
    }

    @Test
    public void theCapKeepsAnEmojiWhole() {
        // the cut after 47 chars falls inside the emoji (chars 47 and 48, a surrogate pair)
        String full  = "x".repeat(46) + "\ud83d\ude00" + "y".repeat(10);
        String label = QueryResult.result(full, -1).inlineLabel();
        assertEquals("x".repeat(46) + "...", label, "no half emoji (it would show as '?')");
    }

    @Test
    public void matchShowsCheckmarkAndOnlyTheTime() {
        QueryResult r = QueryResult.match("[()][]", 1234567);
        assertEquals("✅", r.inlineLabel());
        assertEquals("1,234,567 \u03bcs", r.tooltip(), "the result is the expected result in the query itself");
    }

    @Test
    public void errorTooltipAddsOnlyWhatTheLabelLacks() {
        QueryResult r = QueryResult.error("Infer resulted in nothing", -1);
        assertEquals("\u26a0\ufe0f Infer resulted in nothing", r.inlineLabel());
        assertEquals("", r.tooltip());
        String message = "a message far longer than the sixty characters that fit in a label";
        assertEquals("\u26a0\ufe0f " + message + "  (12 \u03bcs)", QueryResult.error(message, 12).tooltip());
    }

    @Test
    public void timeoutShowsARedCrossAndTheDeadline() {
        assertEquals("❌ evaluation took too long (over 30 s)", QueryResult.timeout(30_000).inlineLabel());
        assertEquals("❌ evaluation took too long (over 300 ms)", QueryResult.timeout(300).inlineLabel());
        assertEquals("", QueryResult.timeout(30_000).tooltip(), "the label says it all");
    }

    @Test
    public void tooltipShowsTheTimeOfTheInferenceTheLabelDoesNot() {
        assertEquals("[()][]", QueryResult.result("[()][]", 1234567).inlineLabel());
        assertEquals("1,234,567 \u03bcs", QueryResult.result("[()][]", 1234567).tooltip());
        assertEquals("Expected result [()][], found [][()]  (12 \u03bcs)", QueryResult.mismatch("[][()]", "Expected result [()][], found [][()]", null, 12).tooltip(),
                "the label shows only what was found");
        assertEquals("12 \u03bcs", QueryResult.error("oops", 12).tooltip());
        assertEquals("", QueryResult.overloaded(2000).timing(), "an overload has no time of its own");
    }

    // the website shows this at the end of the query instead of the tooltip, next to a problem marker there
    @Test
    public void hoverAddsTheTimeAndHowToWriteTheExpectedResult() {
        String accept = "Double-click the result to make it the expected result";
        assertEquals("7 \u03bcs\n\n" + accept, QueryResult.result("[()][]", 7).hover());
        assertEquals("7 \u03bcs", QueryResult.match("[()][]", 7).hover());
        assertEquals("7 \u03bcs\n\n" + accept, QueryResult.mismatch("[][()]", "Expected result [()][], found [][()]", null, 7).hover(),
                "the message of a mismatch is its problem marker");
        assertEquals("", QueryResult.overloaded(2000).hover(), "the message of an overload is its problem marker");
    }

    @Test
    public void overloadedNamesTheBudgetAndCarriesTheCode() {
        QueryResult r = QueryResult.overloaded(2000);
        assertEquals(QueryResult.Kind.OVERLOADED, r.kind());
        assertEquals("\u26a0\ufe0f server busy", r.inlineLabel());
        assertEquals("\u26a0\ufe0f Server busy: evaluation stopped after 2000 ms to protect other users - try again in a moment", r.tooltip());
        assertEquals("Server busy: evaluation stopped after 2000 ms to protect other users - try again in a moment", r.message());
        assertEquals("server-overloaded", QueryResult.OVERLOAD_CODE);
    }

    @Test
    public void calculatingIsOnlyItsLabel() {
        QueryResult r = QueryResult.calculating();
        assertEquals("...calculating", r.inlineLabel());
        assertEquals("", r.tooltip());
        assertEquals("", r.hover());
    }
}
