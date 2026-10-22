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

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.tools.PreferenceText;

public class PreferenceTextTest {
    private Preferences node;

    @BeforeEach
    void createNode() {
        node = Preferences.userRoot().node("org/modelingvalue/nelumbo/test/" + UUID.randomUUID());
    }

    @AfterEach
    void removeNode() throws BackingStoreException {
        node.removeNode();
    }

    @Test
    void roundTripsTextLongerThanOnePreferenceValue() {
        // Preferences.put throws "Value too long" above MAX_VALUE_LENGTH (8 KB); editor content can be longer.
        String text = "x".repeat(3 * Preferences.MAX_VALUE_LENGTH + 5);
        PreferenceText.put(node, "content", text);
        assertEquals(text, PreferenceText.get(node, "content"));
    }

    @Test
    void shorterTextDropsStaleChunks() {
        PreferenceText.put(node, "content", "x".repeat(2 * Preferences.MAX_VALUE_LENGTH));
        PreferenceText.put(node, "content", "short");
        assertEquals("short", PreferenceText.get(node, "content"));
    }

    @Test
    void readsTheSingleValueOfEarlierVersions() {
        node.put("content", "saved before chunking");
        assertEquals("saved before chunking", PreferenceText.get(node, "content"));
    }
}
