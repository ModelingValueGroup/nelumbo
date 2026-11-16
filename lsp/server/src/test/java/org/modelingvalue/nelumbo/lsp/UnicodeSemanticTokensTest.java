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

import java.util.List;

import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * Semantic tokens break a token over lines only at the line breaks of LSP and the editors: a Unicode line separator
 * (U+2028) inside a comment is one character on the comment's line, as the tokenizer counts it.
 */
public class UnicodeSemanticTokensTest {
    private static final String URI = "inmemory://unicode.nl";

    @Test
    public void aLineSeparatorDoesNotSplitAComment() throws Exception {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        server.connect(new RecordingClient());
        try {
            server.getWorkspace().getDocumentManager().addDocument(URI, "// a\u2028b\nimport nelumbo.integers\n", 1);
            List<Integer> data = server.getTextDocumentService().semanticTokensFull(new SemanticTokensParams(new TextDocumentIdentifier(URI))).get().getData();
            assertEquals(List.of(0, 0, 6), data.subList(0, 3), "the comment: line 0, char 0, 6 chars: " + data);
            assertEquals(List.of(1, 0, 6), data.subList(5, 8), "then 'import' on line 1: " + data);
        } finally {
            server.getWorkspace().dispose();
        }
    }
}
