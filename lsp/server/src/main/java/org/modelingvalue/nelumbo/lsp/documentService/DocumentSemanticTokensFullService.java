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

package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.modelingvalue.nelumbo.lsp.LspTokenMapping;
import org.modelingvalue.nelumbo.lsp.Main;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
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

    private static class SemanticTokenMaker {
        private final List<Integer> data        = new ArrayList<>();
        private int                 prevLineNum = 0;
        private int                 prevCharNum = 0;
        private final boolean       debugging   = Main.debugging();

        public void add(Token token) {
            TokenType colorType = token.colorType();

            int tokenType = LspTokenMapping.toLspTokenType(colorType);
            int tokenMod = LspTokenMapping.toLspTokenModifier(colorType);
            if (tokenType == -1) {
                U.DEBUG("        ---%s: %s", U.renderSpan(token), token);
            } else {
                StringBuilder sb = debugging ? new StringBuilder() : null;
                int firstLineNum = token.line();
                int firstCharPosition = token.position();

                String[] tokenLines = token.text().split("\n");
                for (int index = 0; index < tokenLines.length; index++) {
                    String tokenLine = tokenLines[index];

                    int lineNum = firstLineNum + index;
                    int charNum = index == 0 ? firstCharPosition : 0;

                    int lineIncr = lineNum - prevLineNum;
                    int positionIncr = lineIncr == 0 ? charNum - prevCharNum : charNum;

                    extracted(lineIncr, positionIncr, tokenLine, tokenType, tokenMod);
                    if (debugging) {
                        sb.append(String.format("        [%2d:%2d]  incr=[+%2d:+%2d]  #%d  type/mod=%d/%d  '%s'", lineNum, charNum, lineIncr, positionIncr, tokenLine.length(), tokenType, tokenMod, tokenLine));
                    }

                    prevLineNum = lineNum;
                    prevCharNum = charNum;
                }
                U.DEBUG("%s", sb);
            }
        }

        private void extracted(int lineIncrement, int positionIncrement, String tokenLine, int tokenType, int tokenMod) {
            data.add(lineIncrement);
            data.add(positionIncrement);
            data.add(tokenLine.length());
            data.add(tokenType);
            data.add(tokenMod);
        }

        public List<Integer> data() {
            return data;
        }
    }
}
