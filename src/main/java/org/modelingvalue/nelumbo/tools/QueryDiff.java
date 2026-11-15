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

package org.modelingvalue.nelumbo.tools;

import java.util.ArrayList;
import java.util.List;

import org.modelingvalue.nelumbo.tools.PreparedQuery.Span;

public final class QueryDiff {

    public enum Kind {
        KEPT,
        MOVED,
        CHANGED
    }

    public record Match(Kind kind, int oldIndex) {
    }

    private QueryDiff() {
    }

    // Per new span: the old query it still is (same text, before the edit or shifted past it), else CHANGED. An old
    // query is matched once: the queries made by one transformation share their source tokens, so their spans are equal.
    public static List<Match> of(String oldText, List<Span> oldSpans, String newText, List<Span> newSpans) {
        int oldLength = oldText.length();
        int newLength = newText.length();
        int limit     = Math.min(oldLength, newLength);
        int prefix    = prefix(oldText, newText);
        int suffix    = 0;
        while (suffix < limit - prefix && oldText.charAt(oldLength - 1 - suffix) == newText.charAt(newLength - 1 - suffix)) {
            suffix++;
        }
        int         oldTail = oldLength - suffix;
        int         delta   = newLength - oldLength;
        boolean[]   used    = new boolean[oldSpans.size()];
        List<Match> result  = new ArrayList<>();
        for (Span n : newSpans) {
            String text  = newText.substring(n.start(), n.end());
            Match  match = new Match(Kind.CHANGED, -1);
            for (int i = 0; i < oldSpans.size(); i++) {
                Span o = oldSpans.get(i);
                if (used[i]) {
                    continue;
                }
                if (keepable(o, prefix) && o.equals(n) && oldText.substring(o.start(), o.end()).equals(text)) {
                    match = new Match(Kind.KEPT, i);
                    break;
                }
                if (o.start() >= oldTail && o.start() + delta == n.start() && o.end() + delta == n.end() && oldText.substring(o.start(), o.end()).equals(text)) {
                    match = new Match(Kind.MOVED, i);
                    break;
                }
            }
            if (match.kind() != Kind.CHANGED) {
                used[match.oldIndex()] = true;
            }
            result.add(match);
        }
        return result;
    }

    /** The length of the common prefix of the old and the new text. */
    public static int prefix(String oldText, String newText) {
        int limit  = Math.min(oldText.length(), newText.length());
        int prefix = 0;
        while (prefix < limit && oldText.charAt(prefix) == newText.charAt(prefix)) {
            prefix++;
        }
        return prefix;
    }

    /** Whether an old query can be KEPT by an edit with this common prefix; every other one is invalid. */
    public static boolean keepable(Span old, int prefix) {
        return old.end() <= prefix;
    }
}
