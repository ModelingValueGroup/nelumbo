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

package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.lsp.CommandType;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentCodeActionService extends DocumentServiceAdapter {
    public DocumentCodeActionService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        String     uri      = params.getTextDocument().getUri();
        NlDocument document = documentManager.getDocument(uri);
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        List<Either<Command, CodeAction>> actions = new ArrayList<>(expectationActions(uri, params));
        Token t = document.tokenAt(params.getRange().getStart());
        if (t != null && t.type() == TokenType.OPERATOR && t.text().equals("?")) {
            actions.add(Either.forLeft(CommandType.EXEC_COMMAND.command()));
        }
        return CompletableFuture.completedFuture(actions.isEmpty() ? null : actions);
    }

    // a query result on the requested lines can be written into its query as the expected result: the edits the
    // inlay hint carries (QueryResultCache), offered here for editors that do not accept hints (IntelliJ, Eclipse)
    private List<Either<Command, CodeAction>> expectationActions(String uri, CodeActionParams params) {
        int                               from    = params.getRange().getStart().getLine();
        int                               to      = params.getRange().getEnd().getLine();
        List<Either<Command, CodeAction>> actions = new ArrayList<>();
        for (InlayHint hint : documentManager.queryResultCache().hints(uri)) {
            int line = hint.getPosition().getLine();
            if (hint.getTextEdits() == null || line < from || line > to) {
                continue;
            }
            boolean    insert = hint.getTextEdits().getFirst().getRange().getStart().equals(hint.getPosition());
            CodeAction action = new CodeAction(insert ? "Insert the result as expected result" : "Replace the expected result by the result");
            action.setKind(CodeActionKind.QuickFix);
            action.setEdit(new WorkspaceEdit(Map.of(uri, hint.getTextEdits())));
            actions.add(Either.forRight(action));
        }
        return actions;
    }
}
