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

package org.modelingvalue.nelumbo.website;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class BlockedWordsTest {

    private final BlockedWords words = new BlockedWords(List.of("fuck", "kut", "ass", "tit", "lul", "cock", "dick"));

    @Test
    void aLongWordIsBlockedAnywhereInAName() {
        assertTrue(words.inName("fuckyou123"));
        assertTrue(words.inName("Hancock"));
    }

    @Test
    void aShortWordIsBlockedOnlyAsAWholeRunOfLetters() {
        assertTrue(words.inName("Kut-123"));
        assertTrue(words.inName("Dick"));
        assertFalse(words.inName("Hassan"));
        assertFalse(words.inName("Titus"));
        assertFalse(words.inName("Kutlu"));
        assertFalse(words.inName("Lulu"));
        assertFalse(words.inName("Cassandra"));
    }

    @Test
    void aTextIsCheckedOnWholeWords() {
        assertTrue(words.inText("fact kut(A)"));
        assertTrue(words.inText("// FUCK"));
        assertFalse(words.inText("Thing ::= class <Cassandra>"));
        assertFalse(words.inText("// Scunthorpe"));
    }

    @Test
    void theBundledListHasDutchAndEnglish() {
        BlockedWords bundled = BlockedWords.load();
        assertTrue(bundled.inText("fuck"));
        assertTrue(bundled.inText("kut"));
    }
}
