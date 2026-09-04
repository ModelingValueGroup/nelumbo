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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
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
        Position             caretPos   = params.getPosition();
        Token                caretToken = document.tokenAt(caretPos);
        List<CompletionItem> items      = new ArrayList<>();

        if (caretToken != null) {
            int cursor = offsetIn(caretToken, caretPos);
            for (Token.Completion completion : caretToken.completions(cursor)) {
                //System.err.println("  - " + completion);
                items.add(toCompletionItem(caretToken, completion));
            }
        }
        if (items.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(Either.forLeft(items));
    }

    private static CompletionItem toCompletionItem(Token token, Token.Completion completion) {
        CompletionItem ci = new CompletionItem(completion.text());
        ci.setKind(kindOf(completion.kind()));
        ci.setDetail(completion.kind() == null ? null : completion.kind().name().toLowerCase());
        if (completion.documentation() != null) {
            ci.setDocumentation(completion.documentation());
        }
        Range range = new Range(positionIn(token, completion.replaceStart()),
                                positionIn(token, completion.replaceEnd()));
        ci.setTextEdit(Either.forLeft(new TextEdit(range, completion.text())));
        ci.setInsertTextFormat(InsertTextFormat.PlainText);
        return ci;
    }

    // document Position -> offset in the token text (handles multi-line tokens)
    private static int offsetIn(Token token, Position pos) {
        if (pos.getLine() == token.line()) {
            return pos.getCharacter() - token.position();
        }
        String text   = token.text();
        int    offset = 0;
        for (int line = token.line(); line < pos.getLine(); line++) {
            offset = text.indexOf('\n', offset) + 1;
        }
        return offset + pos.getCharacter();
    }

    // offset in the token text -> document Position (handles multi-line tokens)
    private static Position positionIn(Token token, int offset) {
        String head = token.text().substring(0, offset);
        int    nl   = head.lastIndexOf('\n');
        if (nl < 0) {
            return new Position(token.line(), token.position() + offset);
        }
        int lines = (int) head.chars().filter(c -> c == '\n').count();
        return new Position(token.line() + lines, offset - nl - 1);
    }

    private static CompletionItemKind kindOf(TokenType kind) {
        return switch (kind) {
            case KEYWORD -> CompletionItemKind.Keyword;
            case OPERATOR, META_OPERATOR, SINGLEQUOTE, SEMICOLON, COMMA, LEFT, RIGHT -> CompletionItemKind.Operator;
            case TYPE -> CompletionItemKind.Class;
            case VARIABLE -> CompletionItemKind.Variable;
            case NUMBER, STRING -> CompletionItemKind.Value;
            case null, default -> CompletionItemKind.Text;
        };
    }
}
