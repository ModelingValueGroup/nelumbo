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

package org.modelingvalue.nelumbo.lsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.websocket.jakarta.WebSocketLauncherBuilder;

public class Main {
    public static final int            DEFAULT_PORT = 44660;
    public static final Logger         LOGGER       = Logger.getLogger("NelumboLanguageServer");
    public static       LanguageClient client;

    public static void main(String[] args) {
        if (args != null && 0 < args.length && args[0].equals("ws")) {
            int port = DEFAULT_PORT;
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1].trim());
                } catch (NumberFormatException e) {
                    Main.LOGGER.warning("Invalid ws-port '" + args[1] + "', falling back to default " + port);
                }
            }
            start(port);
        } else {
            start(System.in, System.out);
        }
    }

    public static void start(InputStream input, OutputStream output) {
        Locale.setDefault(Locale.ENGLISH);
        NelumboLanguageServer    server   = new NelumboLanguageServer();
        Launcher<LanguageClient> launcher = Launcher.createLauncher(server, LanguageClient.class, input, output);
        client = launcher.getRemoteProxy();
        LOGGER.info("Starting Language Server");
        launcher.startListening();
    }

    public static void start(int port) {
        try {
            Locale.setDefault(Locale.ENGLISH);
            // Start an embedded Jakarta WebSocket server and bind our endpoint
            WsEmbedded.bootstrap(port);
            LOGGER.info("Starting Language Server (WebSocket) on ws://localhost:" + port + "/");
            new CountDownLatch(1).await();
        } catch (Exception e) {
            Main.LOGGER.log(Level.SEVERE, "Failed to start WebSocket LSP server", e);
            System.exit(1);
        }
    }

    // ... existing code ...
    // Minimal embedded WS server using Tyrus (Jakarta WebSocket RI)
    static final class WsEmbedded {
        private static org.glassfish.tyrus.server.Server server;

        static void bootstrap(int port) throws Exception {
            if (server != null) {
                return;
            }
            server = new org.glassfish.tyrus.server.Server("localhost", port, "/", null, LspEndpoint.class);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (Exception ignored) {
                }
            }));
        }
    }

    // WebSocket endpoint that bridges the session into LSP4J via WebSocketLauncherBuilder
    @ServerEndpoint(value="/")
    public static class LspEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            try {
                NelumboLanguageServer server = new NelumboLanguageServer();
                // expose our LanguageServer
                // remote proxy type
                Launcher<LanguageClient> launcher = new WebSocketLauncherBuilder<LanguageClient>().setSession(session).setLocalService(server)                  // expose our LanguageServer
                                                                                                  .setRemoteInterface(LanguageClient.class) // remote proxy type
                                                                                                  .create();

                Main.client = launcher.getRemoteProxy();
                launcher.startListening();
                Main.LOGGER.info("WebSocket session opened: " + safeId(session));
            } catch (Exception e) {
                Main.LOGGER.log(Level.SEVERE, "Failed to initialize LSP over WebSocket", e);
                try {
                    session.close();
                } catch (Exception ignore) {
                }
            }
        }

        @OnMessage
        public void onMessage(String ignored) {
            // No-op. Messages are handled by WebSocketMessageHandler installed by the builder.
        }

        @OnClose
        public void onClose(Session session) {
            Main.LOGGER.info("WebSocket session closed: " + safeId(session));
        }

        @OnError
        public void onError(Session session, Throwable thr) {
            Main.LOGGER.log(Level.SEVERE, "WebSocket error on " + safeId(session), thr);
        }

        private static String safeId(Session s) {
            try {
                URI u = s.getRequestURI();
                return (u != null ? u.toString() : s.getId());
            } catch (Throwable t) {
                return s != null ? s.getId() : "unknown";
            }
        }
    }

}
