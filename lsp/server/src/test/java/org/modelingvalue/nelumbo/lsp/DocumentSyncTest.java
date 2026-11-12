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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * An editor may open a document empty and get its text by a later change (the website's library page does, when the
 * module source arrives after the LSP connection). The empty open must still register the document: it was once
 * dropped, the change then went nowhere, and the page showed no syntax colors.
 */
public class DocumentSyncTest {
    private static final String URI     = "inmemory://field-0.nl";
    private static final String CONTENT = """
            import nelumbo.integers
            Integer ::= fib(<Integer>)
            Integer n
            fib(n) == n if n<=1, fib(n-1)+fib(n-2) if n>1
            """;

    private NelumboLanguageServer server;

    @BeforeEach
    void start() {
        server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        server.connect(new RecordingClient());
    }

    @AfterEach
    void stop() {
        server.getWorkspace().dispose();
    }

    private void open(String text) {
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(URI, "nelumbo", 1, text)));
    }

    private void change(int version, String text) {
        server.getTextDocumentService().didChange(new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(URI, version),
                List.of(new TextDocumentContentChangeEvent(text))));
    }

    private SemanticTokens tokens() throws Exception {
        return server.getTextDocumentService().semanticTokensFull(new SemanticTokensParams(new TextDocumentIdentifier(URI))).get();
    }

    @Test
    void aDocumentOpenedEmptyIsColoredAfterItsTextArrives() throws Exception {
        open("");
        assertNotNull(server.getWorkspace().getDocumentManager().getDocument(URI), "the empty document is registered");
        change(2, CONTENT);
        SemanticTokens tokens = tokens();
        assertNotNull(tokens, "the changed document is known");
        assertFalse(tokens.getData().isEmpty(), "semantic tokens for the text that arrived by the change");
    }

    @Test
    void clearingADocumentClearsItsColors() throws Exception {
        open(CONTENT);
        assertFalse(tokens().getData().isEmpty());
        change(2, "");
        assertTrue(tokens().getData().isEmpty(), "no stale tokens of the text before it was cleared");
    }
}
