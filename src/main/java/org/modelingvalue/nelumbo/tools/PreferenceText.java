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

package org.modelingvalue.nelumbo.tools;

import java.util.prefs.Preferences;

/**
 * Text of any length in Preferences, which caps one value at MAX_VALUE_LENGTH chars:
 * the text is split over key, key.1, key.2, ... A text saved as one value reads back as is.
 */
public final class PreferenceText {
    private PreferenceText() {
    }

    public static void put(Preferences preferences, String key, String text) {
        int chunk = 0;
        int start = 0;
        do {
            int end = Math.min(text.length(), start + Preferences.MAX_VALUE_LENGTH);
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) {
                end--; // keep a surrogate pair in one value
            }
            preferences.put(chunkKey(key, chunk++), text.substring(start, end));
            start = end;
        } while (start < text.length());
        while (preferences.get(chunkKey(key, chunk), null) != null) {
            preferences.remove(chunkKey(key, chunk++));
        }
    }

    public static String get(Preferences preferences, String key) {
        StringBuilder text  = new StringBuilder();
        String        value;
        for (int chunk = 0; (value = preferences.get(chunkKey(key, chunk), null)) != null; chunk++) {
            text.append(value);
        }
        return text.toString();
    }

    private static String chunkKey(String key, int chunk) {
        return chunk == 0 ? key : key + "." + chunk;
    }
}
