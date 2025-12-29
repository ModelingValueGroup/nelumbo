//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.modelingvalue.nelumbo.lsp.LspTokenMapping;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentSemanticTokensFullService extends DocumentServiceAdapter {
    public DocumentSemanticTokensFullService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        SemanticTokenMaker maker = new SemanticTokenMaker();
        document.tokens().forEach(maker::add);
        return CompletableFuture.supplyAsync(() -> new SemanticTokens(maker.data()));
    }

    private class SemanticTokenMaker {
        private final List<Integer> data        = new ArrayList<>();
        private       int           prevLineNum = 0;
        private       int           prevCharNum = 0;
        private final boolean       debugging   = workspace().getSetting().debugging();

        public void add(Token token) {
            // Check if token should be treated as VARIABLE (same logic as NelumboEditor)
            TokenType effectiveType = token.variable() != null ? TokenType.VARIABLE : token.type();

            int semTokenTypeNum     = LspTokenMapping.toLspTokenType(effectiveType);
            int semTokenModifierNum = LspTokenMapping.toLspTokenModifier(effectiveType);
            if (semTokenTypeNum != -1) {
                StringBuilder sb           = debugging ? new StringBuilder() : null;
                int           firstLineNum = token.line();
                int           firstCharNum = token.position();

                String[] tokenParts = token.text().split("\n");
                for (int index = 0; index < tokenParts.length; index++) {
                    String tokenPart = tokenParts[index];

                    int lineNum = firstLineNum + index;
                    int charNum = index == 0 ? firstCharNum : 0;

                    int lineNumRel = lineNum - prevLineNum;
                    int charNumRel = lineNumRel == 0 ? charNum - prevCharNum : charNum;

                    data.add(lineNumRel);           // line increment
                    data.add(charNumRel);           // char increment
                    data.add(tokenPart.length());   // tokenPart length
                    data.add(semTokenTypeNum);      // token type
                    data.add(semTokenModifierNum);  // token modifiers

                    if (debugging) {
                        sb.append(String.format("line=%2d  char=%2d  line+=%2d  char+=%2d  len=%2d  type=%2d  txt='%s'%n", lineNum, charNum, lineNumRel, charNumRel, tokenPart.length(), semTokenTypeNum, tokenPart));
                    }

                    prevLineNum = lineNum;
                    prevCharNum = charNum;
                }
                if (debugging) {
                    System.err.print(sb);
                }
            }
        }

        public List<Integer> data() {
            return data;
        }
    }
}
