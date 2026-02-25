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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentCompletionService extends DocumentServiceAdapter {
    public DocumentCompletionService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        Position caretPos   = params.getPosition();
        Token    caretToken = document.tokenAt(caretPos);
        Set<String>          seen  = new HashSet<>();
        List<CompletionItem> items = new ArrayList<>();

        // Primary: parser-state-aware completions
        if (caretToken != null) {
            for (String completion : caretToken.completions()) {
                if (seen.add(completion)) {
                    CompletionItem ci = new CompletionItem(completion);
                    ci.setKind(CompletionItemKind.Keyword);
                    ci.setDetail("keyword");
                    items.add(ci);
                }
            }
        }

        // Supplementary: NAME tokens matching prefix
        if (caretToken != null && caretToken.type() == TokenType.NAME) {
            String prefix = caretToken.text().substring(0, caretPos.getCharacter() - caretToken.position());
            for (Token t : document.tokens()) {
                if (t.type() == TokenType.NAME && t.text().startsWith(prefix) && seen.add(t.text())) {
                    CompletionItem ci = new CompletionItem(t.text());
                    TokenType ct = t.colorType();
                    ci.setKind(switch (ct) {
                        case TYPE -> CompletionItemKind.Class;
                        case VARIABLE -> CompletionItemKind.Variable;
                        case KEYWORD -> CompletionItemKind.Keyword;
                        default -> CompletionItemKind.Text;
                    });
                    ci.setDetail(ct.name().toLowerCase());
                    items.add(ci);
                }
            }
        }

        if (items.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(Either.forLeft(items));
    }
}
