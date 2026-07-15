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

package org.modelingvalue.nelumbo.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.modelingvalue.json.Json;
import org.modelingvalue.nelumbo.KnowledgeBase;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A lean HTTP executor for Nelumbo text specs, on the JDK's built-in {@link HttpServer} (no third-party server
 * dependencies). Endpoints: {@code POST /eval}, {@code POST /eval/trace}, {@code GET /metadata}, {@code GET /health}.
 * All request handling is delegated to {@link EvalService}; this class only maps HTTP onto it.
 */
public final class NelumboServer {

    /** Default per-request inference budget, in milliseconds. */
    public static final long DEFAULT_TIMEOUT_MS = EvalService.DEFAULT_TIMEOUT_MS;

    private final EvalService service;

    private HttpServer      server;
    private ExecutorService httpExecutor;

    public NelumboServer(KnowledgeBase baseKb, List<String> loadedFiles) {
        this(baseKb, loadedFiles, DEFAULT_TIMEOUT_MS);
    }

    /** {@code timeoutMs} is the per-request inference budget; 0 (or less) disables the timeout. */
    public NelumboServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs) {
        this.service = new EvalService(baseKb, loadedFiles, timeoutMs);
    }

    /** Starts the server on {@code port} (use 0 for an ephemeral port) and returns the actually bound port. */
    public int start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot bind to port " + port, e);
        }
        httpExecutor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(httpExecutor);
        // one root context with exact-path dispatch: the JDK server's own context matching is
        // prefix-based ("/eval" would also match "/evaluate"), which is not what we want
        server.createContext("/", this::handle);
        server.start();
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (httpExecutor != null) {
            httpExecutor.shutdownNow();
        }
        service.close();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            switch (path) {
            case "/":
                if (requires(exchange, method, "GET")) {
                    respondHtml(exchange, INDEX_HTML);
                }
                break;
            case "/health":
                if (requires(exchange, method, "GET")) {
                    respond(exchange, 200, EvalService.health());
                }
                break;
            case "/metadata":
                if (requires(exchange, method, "GET")) {
                    respond(exchange, 200, service.metadata());
                }
                break;
            case "/eval":
            case "/eval/trace":
                if (requires(exchange, method, "POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                    EvalService.Response response = service.eval(body, contentType, path.endsWith("/trace"));
                    respond(exchange, response.status(), response.body());
                }
                break;
            default:
                respond(exchange, 404, Map.of("error", "not-found"));
            }
        } catch (RuntimeException e) {
            respond(exchange, 500, Map.of("error", "internal", "message", String.valueOf(e.getMessage())));
        } finally {
            exchange.close();
        }
    }

    private boolean requires(HttpExchange exchange, String method, String expected) throws IOException {
        if (expected.equals(method)) {
            return true;
        }
        respond(exchange, 405, Map.of("error", "method-not-allowed"));
        return false;
    }

    private void respond(HttpExchange exchange, int status, Map<String, Object> body) throws IOException {
        byte[] bytes = Json.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void respondHtml(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** A minimal index page so a browser (e.g. via the status window's Open in Browser) sees something useful. */
    private static final String INDEX_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Nelumbo Server</title>
            <style>
              body { max-width: 720px; margin: 40px auto; padding: 0 16px; color: #1b1d23;
                     font: 15px/1.6 system-ui, -apple-system, sans-serif; }
              h1 { font-size: 22px; }
              code, textarea, pre { font: 13px/1.5 ui-monospace, Menlo, Consolas, monospace; }
              table { border-collapse: collapse; }
              td { padding: 2px 12px 2px 0; vertical-align: top; }
              textarea { width: 100%; height: 160px; box-sizing: border-box; }
              pre { background: #f4f4f6; padding: 10px; border-radius: 6px; white-space: pre-wrap; }
              button { font-size: 14px; padding: 4px 14px; }
            </style>
            </head>
            <body>
            <h1>Nelumbo Server</h1>
            <p>A lean executor for Nelumbo documents. Endpoints:</p>
            <table>
              <tr><td><code>POST /eval</code></td><td>evaluate a posted Nelumbo document, returns query results and parse tree as JSON</td></tr>
              <tr><td><code>POST /eval/trace</code></td><td>like /eval, with a (currently stubbed) trace field</td></tr>
              <tr><td><code><a href="/metadata">GET /metadata</a></code></td><td>knowledge base metadata (types, functors, rules, facts)</td></tr>
              <tr><td><code><a href="/health">GET /health</a></code></td><td>liveness check</td></tr>
            </table>
            <h2>Try it</h2>
            <textarea id="src">import nelumbo.integers

            Integer ::= fib(&lt;Integer&gt;)
            Integer n, f

            fib(n)=f &lt;=&gt; f=n if n&lt;=1, f=fib(n-1)+fib(n-2) if n&gt;1

            Integer r
            fib(7)=r ?
            </textarea>
            <p><button onclick="run()">Run</button></p>
            <pre id="out">(press Run to evaluate)</pre>
            <script>
              async function run() {
                const out = document.getElementById('out');
                out.textContent = 'running...';
                try {
                  const response = await fetch('/eval', { method: 'POST',
                    headers: { 'Content-Type': 'text/plain' },
                    body: document.getElementById('src').value });
                  out.textContent = JSON.stringify(await response.json(), null, 2);
                } catch (e) {
                  out.textContent = 'request failed: ' + e;
                }
              }
            </script>
            </body>
            </html>
            """;
}
