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
    private final java.util.concurrent.atomic.AtomicLong requests         = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong errors           = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong handleNanosTotal = new java.util.concurrent.atomic.AtomicLong();
    private volatile long lastHandleNanos;
    private volatile long startMillis;

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
        startMillis = System.currentTimeMillis();
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

    /** The number of HTTP requests handled since start. */
    public long requestCount() {
        return requests.get();
    }

    /** The number of requests answered with a 4xx/5xx status. */
    public long errorCount() {
        return errors.get();
    }

    /** Average request handling time in milliseconds (0 when nothing was handled yet). */
    public double averageHandleMillis() {
        long count = requests.get();
        return count == 0 ? 0 : handleNanosTotal.get() / 1e6 / count;
    }

    /** Handling time of the most recent request in milliseconds. */
    public double lastHandleMillis() {
        return lastHandleNanos / 1e6;
    }

    /** Milliseconds since the server was started. */
    public long uptimeMillis() {
        return startMillis == 0 ? 0 : System.currentTimeMillis() - startMillis;
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        long handleStart = System.nanoTime();
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            switch (path) {
            case "/":
                if (requires(exchange, method, "GET")) {
                    respondHtml(exchange, INDEX_HTML);
                }
                break;
            case "/examples":
                if (requires(exchange, method, "GET")) {
                    respond(exchange, 200, Map.of("examples", exampleNames()));
                }
                break;
            case "/favicon.ico":
                if (requires(exchange, method, "GET")) {
                    if (FAVICON == null) {
                        respond(exchange, 404, Map.of("error", "not-found"));
                    } else {
                        exchange.getResponseHeaders().set("Content-Type", "image/png");
                        exchange.getResponseHeaders().set("Cache-Control", "max-age=86400");
                        exchange.sendResponseHeaders(200, FAVICON.length);
                        try (OutputStream out = exchange.getResponseBody()) {
                            out.write(FAVICON);
                        }
                    }
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
                if (path.startsWith("/examples/")) {
                    if (requires(exchange, method, "GET")) {
                        String name = path.substring("/examples/".length());
                        String source = name.matches("[A-Za-z0-9]+") ? exampleSource(name) : null;
                        if (source == null) {
                            respond(exchange, 404, Map.of("error", "not-found"));
                        } else {
                            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
                            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                            exchange.sendResponseHeaders(200, bytes.length);
                            try (OutputStream out = exchange.getResponseBody()) {
                                out.write(bytes);
                            }
                        }
                    }
                    break;
                }
                respond(exchange, 404, Map.of("error", "not-found"));
            }
        } catch (RuntimeException e) {
            respond(exchange, 500, Map.of("error", "internal", "message", String.valueOf(e.getMessage())));
        } finally {
            long handleNanos = System.nanoTime() - handleStart;
            handleNanosTotal.addAndGet(handleNanos);
            lastHandleNanos = handleNanos;
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
        if (status >= 400) {
            errors.incrementAndGet();
        }
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

    private static final String EXAMPLES_DIR = "org/modelingvalue/nelumbo/examples/";

    /** The bundled example names, enumerated from wherever a known example resource actually lives (jar or dir). */
    public static List<String> exampleNames() {
        java.util.TreeSet<String> names = new java.util.TreeSet<>();
        try {
            java.net.URL known = NelumboServer.class.getResource("/" + EXAMPLES_DIR + "fibonacci.nl");
            if (known == null) {
                return List.of();
            }
            if ("jar".equals(known.getProtocol())) {
                String jarPath = known.getPath().substring("file:".length(), known.getPath().indexOf('!'));
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(java.net.URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                    jar.stream().map(java.util.jar.JarEntry::getName)
                            .filter(n -> n.startsWith(EXAMPLES_DIR) && n.endsWith(".nl"))
                            .forEach(n -> names.add(n.substring(EXAMPLES_DIR.length(), n.length() - 3)));
                }
            } else {
                java.io.File[] files = new java.io.File(known.toURI()).getParentFile().listFiles((d, n) -> n.endsWith(".nl"));
                if (files != null) {
                    for (java.io.File file : files) {
                        names.add(file.getName().substring(0, file.getName().length() - 3));
                    }
                }
            }
        } catch (Exception e) {
            // no enumerable source: the list stays empty
        }
        return new java.util.ArrayList<>(names);
    }

    /** The source of a bundled example, or null when unknown. */
    public static String exampleSource(String name) {
        try (java.io.InputStream in = NelumboServer.class.getResourceAsStream("/" + EXAMPLES_DIR + name + ".nl")) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** The Nelumbo lotus (the shared app icon resource from core) as favicon; null if the resource is missing. */
    private static final byte[] FAVICON = loadFavicon();

    private static byte[] loadFavicon() {
        try (java.io.InputStream in = NelumboServer.class.getResourceAsStream("/org/modelingvalue/nelumbo/tools/nelumbo-icon.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
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
              td:first-child { white-space: nowrap; padding-right: 24px; }
              textarea { width: 100%; height: 160px; box-sizing: border-box; }
              pre { background: #f4f4f6; padding: 10px; border-radius: 6px; white-space: pre-wrap; }
              button { font-size: 14px; padding: 4px 14px; }
              #tree { background: #f4f4f6; padding: 10px; border-radius: 6px;
                      font: 13px/1.5 ui-monospace, Menlo, Consolas, monospace; }
              #tree details { padding-left: 18px; }
              #tree div { padding-left: 18px; }
              #tree summary { cursor: pointer; margin-left: -18px; }
            </style>
            </head>
            <body>
            <h1>Nelumbo Server</h1>
            <p>A lean executor for Nelumbo documents. Endpoints:</p>
            <table>
              <tr><td><code>POST /eval</code></td><td>evaluate a posted Nelumbo document, returns query results and parse tree as JSON</td></tr>
              <tr><td><code>POST /eval/trace</code></td><td>like /eval, with a (currently stubbed) trace field</td></tr>
              <tr><td><code><a href="/metadata">GET /metadata</a></code></td><td>knowledge base metadata (types, functors, rules, facts)</td></tr>
              <tr><td><code><a href="/examples">GET /examples</a></code></td><td>the bundled example names; GET /examples/&lt;name&gt; returns the source</td></tr>
              <tr><td><code><a href="/health">GET /health</a></code></td><td>liveness check</td></tr>
            </table>
            <h2>Try it</h2>
            <p>
              <label><input type="checkbox" id="stdlib"> include stdlib (no import statements needed)</label>
              &nbsp; example:
              <select id="examples" onchange="loadExample()"><option value="">choose...</option></select>
            </p>
            <textarea id="src">import nelumbo.integers

            Integer ::= fib(&lt;Integer&gt;)
            Integer n, f

            fib(n)=f &lt;=&gt; f=n if n&lt;=1, f=fib(n-1)+fib(n-2) if n&gt;1

            Integer r
            fib(7)=r ?
            </textarea>
            <p>
              <button onclick="run('/eval')">/eval</button>
              <button onclick="run('/eval/trace')">/eval/trace</button>
              &nbsp; view as:
              <label><input type="radio" name="view" value="text" checked onchange="showView()"> text</label>
              <label><input type="radio" name="view" value="tree" onchange="showView()"> tree</label>
            </p>
            <pre id="out">(press a button to evaluate)</pre>
            <div id="tree" style="display: none"></div>
            <script>
              function treeNode(label, value) {
                if (value !== null && typeof value === 'object') {
                  const details = document.createElement('details');
                  details.open = true;
                  const summary = document.createElement('summary');
                  summary.textContent = Array.isArray(value) ? label + ' [' + value.length + ']' : label;
                  details.appendChild(summary);
                  const entries = Array.isArray(value) ? value.map((v, i) => ['[' + i + ']', v]) : Object.entries(value);
                  for (const [key, val] of entries) {
                    details.appendChild(treeNode(key, val));
                  }
                  return details;
                }
                const leaf = document.createElement('div');
                leaf.textContent = label + ': ' + value;
                return leaf;
              }
              function showView() {
                const asText = document.querySelector('input[name="view"]:checked').value === 'text';
                document.getElementById('out').style.display = asText ? '' : 'none';
                document.getElementById('tree').style.display = asText ? 'none' : '';
              }
              async function run(path) {
                const out = document.getElementById('out');
                const tree = document.getElementById('tree');
                out.textContent = 'running...';
                tree.replaceChildren();
                const src = document.getElementById('src').value;
                const stdlib = document.getElementById('stdlib').checked;
                try {
                  const result = await (await fetch(path, stdlib
                    ? { method: 'POST', headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ document: src, stdlib: true }) }
                    : { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: src })).json();
                  out.textContent = JSON.stringify(result, null, 2);
                  tree.replaceChildren(treeNode('result', result));
                } catch (e) {
                  out.textContent = 'request failed: ' + e;
                  tree.textContent = out.textContent;
                }
              }
              async function loadExample() {
                const name = document.getElementById('examples').value;
                if (name) {
                  document.getElementById('src').value = await (await fetch('/examples/' + name)).text();
                }
              }
              (async () => {
                const select = document.getElementById('examples');
                for (const name of (await (await fetch('/examples')).json()).examples) {
                  const option = document.createElement('option');
                  option.value = name;
                  option.textContent = name;
                  select.appendChild(option);
                }
              })();
            </script>
            </body>
            </html>
            """;
}
