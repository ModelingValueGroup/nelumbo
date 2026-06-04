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
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.lsp.CommandType;
import org.modelingvalue.nelumbo.lsp.Main;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.Workspace;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

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

        NlDocumentManager dm       = getWorkspace().getDocumentManager();
        NlDocument        document = dm.getDocument(docUri);
        if (document == null) {
            Main.client.showMessage(new MessageParams(MessageType.Error, "Document not found: " + docUri));
            return;
        }

        KnowledgeBase.BASE.run(() -> {
            KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
            ParserResult  parsed        = new Parser(new Tokenizer(document.content(), document.uri()).tokenize()).parseNonThrowing();

            Query query = null;
            for (Node root : parsed.roots()) {
                if (root instanceof Query q) {
                    Token first = q.firstToken();
                    if (first != null && first.line() == line && first.position() == pos) {
                        query = q;
                        break;
                    }
                }
            }
            if (query == null) {
                Main.client.showMessage(new MessageParams(MessageType.Error, "No query found at this position [" + position + "]"));
                return;
            }
            final Query target = query;

            err.println("####  " + target + "...");
            ParserResult throwing = new ParserResult(null, true);
            for (Node root : parsed.roots()) {
                if (root instanceof Evaluatable eval && (!(eval instanceof Query) || eval == target)) {
                    try {
                        err.println("EVAL  " + eval + "...");
                        eval.evaluate(knowledgeBase, throwing);
                        if (eval == target) {
                            err.println("INFER " + eval + "...");
                            InferResult ir = target.inferResult();
                            err.println("INFER => " + ir + "...");
                            if (ir == null) {
                                Main.client.showMessage(new MessageParams(MessageType.Error, "Infer resulted in nothing"));
                            } else {
                                Main.client.showMessage(new MessageParams(MessageType.Info, ir.toString()));
                            }
                            return;
                        }
                    } catch (ParseException exc) {
                        exc.printStackTrace(err);
                        String shortMsg = exc.getShortMessage();
                        if (eval == target && shortMsg != null && shortMsg.startsWith("Expected result ")) {
                            Main.client.showMessage(new MessageParams(MessageType.Warning, "Result differs: expected " + shortMsg.substring("Expected result ".length())));
                        } else {
                            Main.client.showMessage(new MessageParams(MessageType.Error, "Problem executing " + eval.getClass().getSimpleName() + ": " + exc.getMessage()));
                        }
                        return;
                    }
                }
            }
        });
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
