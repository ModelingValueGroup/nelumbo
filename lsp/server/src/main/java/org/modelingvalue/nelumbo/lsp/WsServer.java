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
