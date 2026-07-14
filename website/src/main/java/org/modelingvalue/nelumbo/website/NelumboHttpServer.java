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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.server.NelumboServer;

import io.javalin.http.staticfiles.Location;

/**
 * The website HTTP server: a {@link NelumboServer} (eval/metadata/health REST endpoints) extended with the public
 * pages, the bundled Monaco frontend under {@code /assets}, and the {@code /lsp} WebSocket.
 */
public final class NelumboHttpServer {

    /** Default per-request inference budget, in milliseconds. */
    public static final long DEFAULT_TIMEOUT_MS       = NelumboServer.DEFAULT_TIMEOUT_MS;
    /** Default cap on concurrent LSP WebSocket sessions. */
    public static final int  DEFAULT_MAX_LSP_SESSIONS = 32;

    private final NelumboServer server;
    private final KnowledgeBase baseKb;
    private final long          timeoutMs;
    private final int           maxLspSessions;

    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles) {
        this(baseKb, loadedFiles, DEFAULT_TIMEOUT_MS);
    }

    /** {@code timeoutMs} is the per-request inference budget; 0 (or less) disables the timeout. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs) {
        this(baseKb, loadedFiles, timeoutMs, DEFAULT_MAX_LSP_SESSIONS);
    }

    /** {@code maxLspSessions} caps the number of concurrent LSP WebSocket sessions. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions) {
        this.server         = new NelumboServer(baseKb, loadedFiles, timeoutMs);
        this.baseKb         = baseKb;
        this.timeoutMs      = timeoutMs;
        this.maxLspSessions = maxLspSessions;
    }

    /** Starts the server on {@code port} (use 0 for an ephemeral port) and returns the actually bound port. */
    public int start(int port) {
        String landing    = loadResource("/public/landing.html");
        String tour       = loadResource("/public/tour.html");
        String playground = loadResource("/public/playground.html");
        String favicon    = loadResource("/public/favicon.svg");
        return server.start(port, config -> {
            // serve the bundled frontend (Monaco js/css + codicon font) from the classpath under /assets
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/assets";
                staticFiles.directory  = "/public/assets";
                staticFiles.location   = Location.CLASSPATH;
                // Jetty has no default mapping for the codicon font
                staticFiles.mimeTypes.add("font/ttf", "ttf");
            });
            // make the 64 KB text-frame limit explicit (Jetty's default is the same size, but do not rely on it)
            config.jetty.modifyWebSocketServletFactory(factory ->
                    factory.setMaxTextMessageSize(LspWebSocket.MAX_MESSAGE_CHARS));
            config.routes.get("/", ctx -> ctx.html(landing));
            config.routes.get("/favicon.svg", ctx -> ctx.contentType("image/svg+xml").result(favicon));
            config.routes.get("/tour.html", ctx -> ctx.html(tour));
            config.routes.get("/playground.html", ctx -> ctx.html(playground));
            config.routes.ws("/lsp", new LspWebSocket(baseKb, timeoutMs, maxLspSessions)::configure);
        });
    }

    public void stop() {
        server.stop();
    }

    private static String loadResource(String path) {
        try (InputStream in = NelumboHttpServer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("resource not found on classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read resource " + path, e);
        }
    }
}
