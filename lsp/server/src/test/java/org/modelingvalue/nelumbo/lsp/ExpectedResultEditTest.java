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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * A query result can be written into its query as the expected result: a plain query gets it inserted, a wrong
 * expected result gets replaced, a matching one needs nothing. The inlay hint carries the edit (Monaco applies it on
 * a double-click), and the same edit is a quick-fix code action for the editors that do not accept hints.
 */
public class ExpectedResultEditTest {
    private static final String   URI   = "inmemory://expect.nl";
    private static final String[] LINES = {
            "import nelumbo.logic",
            "true ?",
            "false ? [][()]",
            "false ? [()][]" };

    @Test
    public void resultsCanBeWrittenAsTheExpectedResult() throws Exception {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        server.connect(client);
        try {
            server.getWorkspace().getDocumentManager().addDocument(URI, String.join("\n", LINES) + "\n", 1);
            List<InlayHint> hints = LiveQueryResultsTest.awaitResults(server.getWorkspace().getDocumentManager().queryResultCache(), URI, 3, 15);

            assertEquals("true ? [()][]", applied(hints.get(0).getTextEdits()), "a plain query gets the result inserted");
            assertNull(hints.get(1).getTextEdits(), "a matching expected result needs no edit");
            assertEquals("false ? [][()]", applied(hints.get(2).getTextEdits()), "a wrong expected result gets replaced");

            assertEquals(List.of("Insert the result as expected result"), titles(server, 1));
            assertEquals(List.of(), titles(server, 2));
            assertEquals(List.of("Replace the expected result by the result"), titles(server, 3));
            CodeAction replace = action(server, 3);
            assertEquals("false ? [][()]", applied(replace.getEdit().getChanges().get(URI)), "the action does what the hint does");

            // the hover right at the end of a query (where its result shows) is the result's hover
            Hover hover = server.getTextDocumentService().hover(new HoverParams(new TextDocumentIdentifier(URI), hints.get(2).getPosition())).get();
            assertTrue(hover.getContents().getRight().getValue().matches("[0-9,]+ \u03bcs\n\nDouble-click the result to make it the expected result"),
                    hover.getContents().getRight().getValue());
        } finally {
            server.getWorkspace().dispose();
        }
    }

    // the line the (single-line) edits apply to, after applying them
    private static String applied(List<TextEdit> edits) {
        assertEquals(1, edits.size());
        TextEdit edit = edits.getFirst();
        String   line = LINES[edit.getRange().getStart().getLine()];
        return line.substring(0, edit.getRange().getStart().getCharacter()) + edit.getNewText() + line.substring(edit.getRange().getEnd().getCharacter());
    }

    private static List<Either<Command, CodeAction>> actions(NelumboLanguageServer server, int line) throws Exception {
        Range                             range   = new Range(new Position(line, 0), new Position(line, 0));
        CodeActionParams                  params  = new CodeActionParams(new TextDocumentIdentifier(URI), range, new CodeActionContext(List.of()));
        List<Either<Command, CodeAction>> actions = server.getTextDocumentService().codeAction(params).get();
        return actions == null ? List.of() : actions;
    }

    private static List<String> titles(NelumboLanguageServer server, int line) throws Exception {
        return actions(server, line).stream().filter(Either::isRight).map(a -> a.getRight().getTitle()).toList();
    }

    private static CodeAction action(NelumboLanguageServer server, int line) throws Exception {
        return actions(server, line).stream().filter(Either::isRight).map(Either::getRight).findFirst().orElseThrow();
    }
}
