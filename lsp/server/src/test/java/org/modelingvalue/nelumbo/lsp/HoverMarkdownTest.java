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

import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * Hover text is Markdown and shows token text inside `code spans`. CommonMark renders code spans
 * literally: entity references and backslash escapes are NOT decoded there, so escaping the token text
 * made the hover on the {@code >} of a pattern hole read "&amp;gt;" and on {@code (} read "\(".
 */
public class HoverMarkdownTest {
    private static final String URI     = "inmemory://hover.nl";
    private static final String CONTENT = """
            import nelumbo.integers
            Integer ::= fib(<Integer>)
            Integer n
            """;

    private static String hover(int line, int character) throws Exception {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        server.connect(new RecordingClient());
        try {
            server.getWorkspace().getDocumentManager().addDocument(URI, CONTENT, 1);
            TextDocumentIdentifier doc = new TextDocumentIdentifier(URI);
            return server.getTextDocumentService().hover(new HoverParams(doc, new Position(line, character))).get().getContents().getRight().getValue();
        } finally {
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void patternHoleBracketIsShownVerbatimInItsCodeSpan() throws Exception {
        assertEquals("`>` — Pattern", hover(1, 24));
    }

    @Test
    public void markdownPunctuationIsNotBackslashEscapedInsideACodeSpan() throws Exception {
        String text = hover(1, 15);
        assertTrue(text.startsWith("`(`"), text);
    }

    @Test
    public void codeSpanSurvivesBackticksInTheText() {
        // a run of backticks inside needs a longer fence, padded with spaces
        assertEquals("``a`b``", U.codeSpan("a`b"));
        assertEquals("`` `x ``", U.codeSpan("`x"));
        assertEquals("`<=>`", U.codeSpan("<=>"));
    }
}
