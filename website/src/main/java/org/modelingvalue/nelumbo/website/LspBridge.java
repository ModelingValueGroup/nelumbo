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

package org.modelingvalue.nelumbo.website;

import java.util.List;
import java.util.Map;

import io.javalin.websocket.WsContext;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.lsp.NelumboLanguageServer;

/**
 * One embedded LSP session bridged over a Javalin WebSocket. Speaks the same wire format as
 * vscode-ws-jsonrpc: one plain JSON-RPC message per text frame, no Content-Length framing.
 */
final class LspBridge {
    private final NelumboLanguageServer server;
    private final MessageJsonHandler    jsonHandler;
    private final RemoteEndpoint        remoteEndpoint;

    LspBridge(KnowledgeBase baseKb, long evalDeadlineMs, WsContext ctx) {
        // embedded: an 'exit' request must never kill the host JVM
        server = new NelumboLanguageServer(baseKb, evalDeadlineMs, () -> {
        });
        Map<String, JsonRpcMethod> methods = ServiceEndpoints.getSupportedMethods(LanguageServer.class);
        methods.putAll(ServiceEndpoints.getSupportedMethods(LanguageClient.class));
        jsonHandler    = new MessageJsonHandler(methods);
        remoteEndpoint = new RemoteEndpoint(message -> send(ctx, message), ServiceEndpoints.toEndpoint(List.of(server)));
        jsonHandler.setMethodProvider(remoteEndpoint);
        remoteEndpoint.setJsonHandler(jsonHandler);
        server.connect(ServiceEndpoints.toServiceObject(remoteEndpoint, LanguageClient.class));
    }

    private void send(WsContext ctx, Message message) {
        // The client can close the socket (e.g. navigate away) while the server is still writing a
        // notification/response. Skip closed sessions, and swallow the write-on-closed race so it does
        // not surface as an error stack trace from the LSP endpoint - dropping the message is correct.
        if (!ctx.session.isOpen()) {
            return;
        }
        try {
            ctx.send(jsonHandler.serialize(message));
        } catch (Exception ignored) {
        }
    }

    void onMessage(String json) {
        Message message = jsonHandler.parseMessage(json);
        remoteEndpoint.consume(message);
    }

    void close() {
        server.getWorkspace().dispose();
    }
}
