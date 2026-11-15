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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * Only types, variables and expressions get a hover; a hover that only repeats what the editor already shows
 * (keywords, spaces, comments, punctuation, the parts of statements, patterns and declarations) is noise.
 * Hover text is Markdown with token text inside `code spans`. CommonMark renders code spans literally:
 * entity references and backslash escapes are NOT decoded there, so escaping the token text once made a
 * {@code >} read "&amp;gt;" and a {@code (} read "\(".
 */
public class HoverMarkdownTest {
    private static final String   URI     = "inmemory://hover.nl";
    private static final String[] LINES   = {
            "import nelumbo.integers",
            "Person :: Object",
            "Person ::= Piet, Jan",
            "Integer ::= fib(<Integer>), age(<Person>)",
            "Integer n",
            "fib(n) == n if n<=1, fib(n-1)+fib(n-2) if n>1",
            "age(Piet)=42",
            "fib(5) ? [(5)][..]",
            "// a comment" };
    private static NelumboLanguageServer server;

    @BeforeAll
    static void start() {
        server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        server.connect(new RecordingClient());
        server.getWorkspace().getDocumentManager().addDocument(URI, String.join("\n", LINES) + "\n", 1);
    }

    @AfterAll
    static void stop() {
        server.getWorkspace().dispose();
    }

    // the hover on the n-th (from 1) occurrence of text in the given line, null without one
    private static String hover(int line, String text, int occurrence) throws Exception {
        int column = -1;
        for (int i = 0; i < occurrence; i++) {
            column = LINES[line].indexOf(text, column + 1);
        }
        TextDocumentIdentifier doc   = new TextDocumentIdentifier(URI);
        Hover                  hover = server.getTextDocumentService().hover(new HoverParams(doc, new Position(line, column))).get();
        return hover == null ? null : hover.getContents().getRight().getValue();
    }

    private static String hover(int line, String text) throws Exception {
        return hover(line, text, 1);
    }

    @Test
    public void typesShowTheirSupertypesWithoutInternalOnes() throws Exception {
        assertEquals("**type** `Person`\n\nSupertypes: `Object`", hover(1, "Person"), "a type in its own declaration");
        assertEquals("**type** `Object`", hover(1, "Object"), "the internal $Object is not shown");
        assertEquals("**type** `Person`\n\nSupertypes: `Object`", hover(3, "Person"), "a type in a pattern");
    }

    @Test
    public void variablesShowTheirType() throws Exception {
        assertEquals("**variable** `n` : `Integer`", hover(4, "n", 2), "in its declaration (the first n is in Integer)");
        assertEquals("**variable** `n` : `Integer`", hover(5, "n"), "where it is used");
    }

    @Test
    public void expressionsShowTheirFunctorAndType() throws Exception {
        assertEquals("`fib()` : `Integer`", hover(5, "fib", 2), "a parenthesis in a code span is not escaped");
        assertEquals("`+` : `Integer`", hover(5, "+"), "the + itself, not the rule around it");
        assertEquals("`-` : `Integer`", hover(5, "-"));
        assertEquals("`>` : `Boolean`", hover(5, ">"), "a > in a code span is not an entity");
        assertEquals("`1` : `Integer`", hover(5, "1"), "a literal");
        assertEquals("`Piet` : `Person`", hover(6, "Piet"), "a constant");
        assertEquals("`42` : `Integer`", hover(6, "42"));
        assertEquals("`fib()` : `Integer`", hover(7, "fib"), "in a query");
    }

    @Test
    public void noiseHasNoHover() throws Exception {
        assertNull(hover(0, "import"), "keyword");
        assertNull(hover(0, " "), "space");
        assertNull(hover(0, "nelumbo"), "part of an import");
        assertNull(hover(1, "::"), "part of a type declaration");
        assertNull(hover(2, "::="), "part of a functor declaration");
        assertNull(hover(2, "Piet"), "a pattern part");
        assertNull(hover(2, ","), "punctuation");
        assertNull(hover(3, "fib"), "a pattern part");
        assertNull(hover(3, "<"), "a pattern hole");
        assertNull(hover(5, "=="), "part of a rule");
        assertNull(hover(5, "if"), "keyword");
        assertNull(hover(5, "("), "parenthesis");
        assertNull(hover(7, "?"), "part of a query");
        assertNull(hover(7, "["), "part of a query");
        assertNull(hover(7, ".."), "part of a query");
        assertNull(hover(8, "comment"), "comment");
    }

    @Test
    public void codeSpanSurvivesBackticksInTheText() {
        // a run of backticks inside needs a longer fence, padded with spaces
        assertEquals("``a`b``", U.codeSpan("a`b"));
        assertEquals("`` `x ``", U.codeSpan("`x"));
        assertEquals("`<=>`", U.codeSpan("<=>"));
    }
}
