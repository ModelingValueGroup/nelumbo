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

package org.modelingvalue.nelumbo.lsp.workspaceService;

import static java.lang.System.err;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.lsp.CommandType;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.QueryEvaluator;
import org.modelingvalue.nelumbo.lsp.QueryResult;
import org.modelingvalue.nelumbo.lsp.Workspace;
import org.modelingvalue.nelumbo.syntax.Token;

public class WorkspaceExecuteCommandService extends WorkspaceServiceAdapter {

    public WorkspaceExecuteCommandService(Workspace workspace) {
        super(workspace);
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        CommandType command = CommandType.of(params.getCommand());
        //noinspection SwitchStatementWithTooFewBranches
        switch (command) {
            case EXEC_COMMAND -> execute_EXEC_COMMAND(params.getArguments());
            default -> err.println("    execute command: " + params.getCommand() + " not implemented");
        }
        return CompletableFuture.completedFuture(null);
    }

    private void execute_EXEC_COMMAND(List<?> args) {
        if (args.size() < 3) {
            return;
        }
        String   docUri   = asString(args.get(0));
        int      line     = asInt(args.get(1));
        int      pos      = asInt(args.get(2));
        Position position = new Position(line, pos);

        Workspace         ws       = getWorkspace();
        NlDocumentManager dm       = ws.getDocumentManager();
        NlDocument        document = dm.getDocument(docUri);
        if (document == null) {
            showMessage(new MessageParams(MessageType.Error, "Document not found: " + docUri));
            return;
        }

        Map<Query, QueryResult> results = QueryEvaluator.evaluate(ws.getBaseKnowledgeBase(), ws.getEvalDeadlineMs(), document.content(), document.uri());

        QueryResult result = null;
        for (Map.Entry<Query, QueryResult> e : results.entrySet()) {
            Token first = e.getKey().firstToken();
            if (first != null && first.line() == line && first.position() == pos) {
                result = e.getValue();
                break;
            }
        }
        if (result == null) {
            showMessage(new MessageParams(MessageType.Error, "No query found at this position [" + position + "]"));
            return;
        }

        MessageParams message = switch (result.kind()) {
            case RESULT   -> new MessageParams(MessageType.Info, result.inferred());
            case MATCH    -> new MessageParams(MessageType.Info, "✓ " + result.inferred());
            case MISMATCH -> new MessageParams(MessageType.Warning, result.message());
            case ERROR    -> new MessageParams(MessageType.Error, result.inferred());
        };
        showMessage(message);
    }

    private void showMessage(MessageParams message) {
        org.eclipse.lsp4j.services.LanguageClient client = getWorkspace().getClient();
        if (client != null) {
            client.showMessage(message);
        }
    }

    private static String asString(Object o) {
        if (o instanceof JsonElement e) {
            return e.getAsString();
        }
        return o.toString();
    }

    private static int asInt(Object o) {
        if (o instanceof JsonElement e) {
            return e.getAsInt();
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(o.toString());
    }
}
