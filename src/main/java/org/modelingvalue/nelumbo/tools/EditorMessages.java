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

/**
 * The messages pane of one editor version: one message per line, a parse or statement error over a query result on
 * the same line, plus the red highlights in the text and the green ones on the messages.
 */
public final class EditorMessages {

    public record Highlight(int index, int length, String error) {
    }

    private final int             totalLines;
    private final String[]        fixed;
    private final String[]        queries;
    private final String[]        messageErrors;
    private final List<Highlight> textHighlights = new ArrayList<>();

    public EditorMessages(int totalLines) {
        this.totalLines    = totalLines;
        this.fixed         = new String[totalLines];
        this.queries       = new String[totalLines];
        this.messageErrors = new String[totalLines];
    }

    public void setFixed(int line, String message, Highlight textHighlight) {
        if (line >= 0 && line < totalLines) {
            fixed[line] = message;
        }
        if (textHighlight != null) {
            textHighlights.add(textHighlight);
        }
    }

    public void setQuery(int line, String message, Highlight textHighlight, boolean highlightMessage) {
        if (line >= 0 && line < totalLines) {
            queries[line]       = message;
            messageErrors[line] = highlightMessage ? textHighlight.error() : null;
        }
        if (textHighlight != null) {
            textHighlights.add(textHighlight);
        }
    }

    public String text() {
        List<String> lines = new ArrayList<>(totalLines);
        for (int line = 0; line < totalLines; line++) {
            lines.add(line(line));
        }
        return String.join("\n", lines);
    }

    public List<Highlight> textHighlights() {
        return List.copyOf(textHighlights);
    }

    public List<Highlight> messageHighlights() {
        List<Highlight> highlights = new ArrayList<>();
        int             offset     = 0;
        for (int line = 0; line < totalLines; line++) {
            if (fixed[line] == null && messageErrors[line] != null) {
                highlights.add(new Highlight(offset, queries[line].length(), messageErrors[line]));
            }
            offset += line(line).length() + 1;
        }
        return highlights;
    }

    private String line(int line) {
        return fixed[line] != null ? fixed[line] : queries[line] != null ? queries[line] : "";
    }
}
