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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.tools.PreparedQuery.Span;
import org.modelingvalue.nelumbo.tools.QueryDiff;
import org.modelingvalue.nelumbo.tools.QueryDiff.Kind;
import org.modelingvalue.nelumbo.tools.QueryDiff.Match;

public class QueryDiffTest {

    private static Span span(String text, String q) {
        int start = text.indexOf(q);
        return new Span(start, start + q.length());
    }

    private static List<Span> spans(String text, String... queries) {
        List<Span> result = new ArrayList<>();
        for (String q : queries) {
            result.add(span(text, q));
        }
        return result;
    }

    private static Match kept(int i) {
        return new Match(Kind.KEPT, i);
    }

    private static Match moved(int i) {
        return new Match(Kind.MOVED, i);
    }

    private static final Match CHANGED = new Match(Kind.CHANGED, -1);

    private static List<Match> diff(String oldText, String[] oldQ, String newText, String[] newQ) {
        return QueryDiff.of(oldText, spans(oldText, oldQ), newText, spans(newText, newQ));
    }

    @Test
    public void anEditBelowKeepsEverythingAbove() {
        List<Match> m = diff("alpha ?\nbeta ?\n", new String[]{"alpha ?", "beta ?"}, "alpha ?\nbeta ?\n// c\n", new String[]{"alpha ?", "beta ?"});
        assertEquals(List.of(kept(0), kept(1)), m);
    }

    @Test
    public void anEditInsideAQueryChangesOnlyThatQuery() {
        List<Match> m = diff("alpha ?\nbeta ?\ndelta ?\n", new String[]{"alpha ?", "beta ?", "delta ?"}, "alpha ?\nbetax ?\ndelta ?\n", new String[]{"alpha ?", "betax ?", "delta ?"});
        assertEquals(List.of(kept(0), CHANGED, moved(2)), m);
    }

    @Test
    public void anInsertedQueryShiftsTheOnesBelow() {
        List<Match> m = diff("alpha ?\nbeta ?\ndelta ?\n", new String[]{"alpha ?", "beta ?", "delta ?"}, "alpha ?\ngamma ?\nbeta ?\ndelta ?\n", new String[]{"alpha ?", "gamma ?", "beta ?", "delta ?"});
        assertEquals(List.of(kept(0), CHANGED, moved(1), moved(2)), m);
    }

    @Test
    public void aDeletedQueryHasNoMatch() {
        List<Match> m = diff("alpha ?\ngamma ?\nbeta ?\ndelta ?\n", new String[]{"alpha ?", "gamma ?", "beta ?", "delta ?"}, "alpha ?\nbeta ?\ndelta ?\n", new String[]{"alpha ?", "beta ?", "delta ?"});
        assertEquals(List.of(kept(0), moved(2), moved(3)), m);
    }

    @Test
    public void typingRightAfterAQueryChangesIt() {
        List<Match> m = diff("alpha ?\nbeta ?\n", new String[]{"alpha ?", "beta ?"}, "alpha ? [()][]\nbeta ?\n", new String[]{"alpha ? [()][]", "beta ?"});
        assertEquals(List.of(CHANGED, moved(1)), m);
    }

    // two queries made by one transformation share their source tokens: equal spans, each matches its own old query
    @Test
    public void equalSpansMatchAnOldQueryOnce() {
        List<Match> below = diff("alpha ?\n", new String[]{"alpha ?", "alpha ?"}, "alpha ?\n// c\n", new String[]{"alpha ?", "alpha ?"});
        List<Match> above = diff("alpha ?\n", new String[]{"alpha ?", "alpha ?"}, "// c\nalpha ?\n", new String[]{"alpha ?", "alpha ?"});
        List<Match> added = diff("alpha ?\n", new String[]{"alpha ?"}, "alpha ?\n// c\n", new String[]{"alpha ?", "alpha ?"});
        assertEquals(List.of(kept(0), kept(1)), below);
        assertEquals(List.of(moved(0), moved(1)), above);
        assertEquals(List.of(kept(0), CHANGED), added);
    }

    @Test
    public void anEditAboveEverythingMovesAll() {
        List<Match> m = diff("alpha ?\nbeta ?\n", new String[]{"alpha ?", "beta ?"}, "// c\nalpha ?\nbeta ?\n", new String[]{"alpha ?", "beta ?"});
        assertEquals(List.of(moved(0), moved(1)), m);
    }
}
