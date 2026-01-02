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

import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.lsp4j.jsonrpc.Launcher;

// Use the jakarta websocket builder provided by lsp4j to wire the LSP over an actual websocket Session.
// We host a minimal websocket server using Tyrus.
public final class WsServer {
    private final org.glassfish.tyrus.server.Server server;

    public WsServer(String host, int port) {
        // Map all connections to "/" endpoint
        this.server = new org.glassfish.tyrus.server.Server(host, port, "/", null, LspEndpoint.class);
    }

    public void start(Object languageServer, Class<?> clientClass) throws Exception {
        LspEndpoint.languageServer = languageServer;
        LspEndpoint.clientClass = clientClass;
        server.start();
    }

    public void stop() {
        try {
            server.stop();
        } catch (Throwable ignored) {
        }
    }

    @ServerEndpoint(value = "/")
    public static class LspEndpoint {
        static volatile Object languageServer;
        static volatile Class<?> clientClass;

        @OnOpen
        public void onOpen(Session session) {
            try {
                // Prefer jakarta builder
                Class<?> builderClass = Class.forName("org.eclipse.lsp4j.websocket.jakarta.WebSocketLauncherBuilder");
                Object builder = builderClass.getDeclaredConstructor().newInstance();
                builderClass.getMethod("setSession", Session.class).invoke(builder, session);
                builderClass.getMethod("setLocalService", Object.class).invoke(builder, languageServer);
                builderClass.getMethod("setRemoteInterface", Class.class).invoke(builder, clientClass);
                Object launcherObj = builderClass.getMethod("create").invoke(builder);

                if (launcherObj instanceof Launcher<?> l) {
                    // Capture remote proxy if possible
                    Object proxy = l.getRemoteProxy();
                    if (proxy instanceof org.eclipse.lsp4j.services.LanguageClient lc) {
                        Main.client = lc;
                    }
                    l.startListening();
                } else {
                    // Try to startListening reflectively
                    try {
                        launcherObj.getClass().getMethod("startListening").invoke(launcherObj);
                    } catch (NoSuchMethodException e) {
                        launcherObj.getClass().getMethod("start").invoke(launcherObj);
                    }
                }
            } catch (Exception e) {
                Main.LOGGER.log(java.util.logging.Level.SEVERE, "Failed to initialize WebSocket endpoint", e);
                try {
                    session.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
