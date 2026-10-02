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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * LSP requests arrive on lsp4j threads without a {@link KnowledgeBase#CURRENT} context; type checks in
 * {@code Token.colorType()} (semantic tokens) and hover need one (they NPE'd in {@code Type.getAssigned}).
 */
public class LspRequestContextTest {
    private static final String URI     = "inmemory://fib.nl";
    private static final String CONTENT = """
            import nelumbo.integers
            Integer ::= fib(<Integer>)
            Integer n, f
            fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1
            Integer r
            fib(7)=r ?
            """;

    @Test
    public void semanticTokensAndHoverWorkOutsideAKnowledgeBaseContext() throws Exception {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        server.connect(new RecordingClient());
        try {
            server.getWorkspace().getDocumentManager().addDocument(URI, CONTENT, 1);
            TextDocumentIdentifier doc    = new TextDocumentIdentifier(URI);
            SemanticTokens         tokens = server.getTextDocumentService().semanticTokensFull(new SemanticTokensParams(doc)).get();
            assertFalse(tokens.getData().isEmpty(), "semantic tokens for the document");
            Hover hover = server.getTextDocumentService().hover(new HoverParams(doc, new Position(1, 1))).get();
            assertNotNull(hover, "hover on the Integer type reference");
        } finally {
            server.getWorkspace().dispose();
        }
    }
}
