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

package org.modelingvalue.nelumbo.website;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.lsp.EvalGate;
import org.modelingvalue.nelumbo.server.EvalService;
import org.modelingvalue.nelumbo.server.NelumboServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.router.EndpointNotFound;

/**
 * The website HTTP server: the shared {@link EvalService} REST endpoints (eval/metadata/health) plus the public
 * pages, the documentation under {@code /docs} (see {@link DocsSite}), the bundled Monaco frontend under
 * {@code /assets}, and the {@code /lsp} WebSocket, all served by one
 * Javalin (Jetty) instance — the WebSocket route is why this module does not run on the JDK server like the lean
 * {@code nelumbo-server} executor does.
 */
public final class NelumboHttpServer {

    /** Default per-request inference budget, in milliseconds. */
    public static final long DEFAULT_TIMEOUT_MS       = EvalService.DEFAULT_TIMEOUT_MS;
    /** Default cap on concurrent LSP WebSocket sessions. */
    public static final int  DEFAULT_MAX_LSP_SESSIONS = 32;

    private final EvalService   service;
    private final KnowledgeBase baseKb;
    private final long          timeoutMs;
    private final int           maxLspSessions;
    private final Path          statsDir;
    private final Path          solutionsDir;
    private final ClientAccess  access;
    private final ObjectMapper  statsMapper = new ObjectMapper();

    private Javalin       app;
    private StatsRecorder recorder;
    private StatsStream   stream;

    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles) {
        this(baseKb, loadedFiles, DEFAULT_TIMEOUT_MS);
    }

    /** {@code timeoutMs} is the per-request inference budget; 0 (or less) disables the timeout. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs) {
        this(baseKb, loadedFiles, timeoutMs, DEFAULT_MAX_LSP_SESSIONS);
    }

    /** {@code maxLspSessions} caps the number of concurrent LSP WebSocket sessions. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions) {
        this(baseKb, loadedFiles, timeoutMs, maxLspSessions, null);
    }

    /** {@code statsDir} keeps the per-minute stats history across restarts; null keeps it in memory only. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions, Path statsDir) {
        this(baseKb, loadedFiles, timeoutMs, maxLspSessions, statsDir, null);
    }

    /** {@code solutionsDir} keeps the shared editor solutions ({@code /solutions}) across restarts; null keeps them in memory only. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions, Path statsDir, Path solutionsDir) {
        this(baseKb, loadedFiles, timeoutMs, maxLspSessions, statsDir, solutionsDir, ClientAccess.of(null, null));
    }

    /** {@code access} decides the client address (trusted proxy header) and who is admin. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions, Path statsDir, Path solutionsDir, ClientAccess access) {
        this.service        = new EvalService(baseKb, loadedFiles, timeoutMs);
        this.baseKb         = baseKb;
        this.timeoutMs      = timeoutMs;
        this.maxLspSessions = maxLspSessions;
        this.statsDir       = statsDir;
        this.solutionsDir   = solutionsDir;
        this.access         = access;
    }

    /** Starts the server on {@code port} (use 0 for an ephemeral port) and returns the actually bound port. */
    public int start(int port) {
        String landing    = loadResource("/public/landing.html");
        String tutorial   = TutorialPage.render(loadResource("/public/tutorial.html"));
        String sandbox    = loadResource("/public/sandbox.html");
        String admin      = loadResource("/public/admin.html");
        String libraryPage = loadResource("/public/library.html");
        String favicon    = loadResource("/public/favicon.svg");
        String theme      = loadResource("/public/theme.js");
        String statusDot  = loadResource("/public/status-dot.js");
        String llms       = loadResource("/public/llms.txt");
        String docsLogo   = loadResource(DocsSite.RESOURCE_ROOT + "nelumbo.svg");
        DocsSite docs     = DocsSite.load();
        Library library   = Library.load(docs);
        // the sandbox's example list; the sudokus run far past the eval deadline, so they are left out
        List<String> examples = NelumboServer.exampleNames().stream().filter(n -> !n.startsWith("sudoku")).toList();
        LspWebSocket    lsp       = new LspWebSocket(baseKb, timeoutMs, maxLspSessions);
        StatsHistory    history   = new StatsHistory(statsDir, System::currentTimeMillis);
        SolutionStore   store     = new SolutionStore(solutionsDir, System::currentTimeMillis);
        SolutionService solutions = new SolutionService(store, BlockedWords.load(),
                System::currentTimeMillis, SolutionService.MAX_TOTAL_BYTES);
        stream               = new StatsStream();
        recorder             = new StatsRecorder(history, () -> ServerStats.read(lsp.sessionCount(), EvalGate.GLOBAL, solutions.usage()),
                sample -> stream.broadcast(statsJson(sample)));
        recorder.start();
        app = Javalin.create(config -> {
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
            // before-handlers run for every method (also HEAD, which skips the GET handler): a non-admin gets exactly the 404 of an unknown route
            config.routes.before("/admin", this::guardAdmin);
            config.routes.before("/admin/*", this::guardAdmin);
            config.routes.get("/", ctx -> ctx.html(landing));
            config.routes.get("/favicon.svg", ctx -> ctx.contentType("image/svg+xml").result(favicon));
            config.routes.get("/theme.js", ctx -> ctx.contentType("text/javascript; charset=utf-8").result(theme));
            config.routes.get("/status-dot.js", ctx -> ctx.contentType("text/javascript; charset=utf-8").result(statusDot));
            config.routes.get("/llms.txt", ctx -> ctx.contentType("text/plain; charset=utf-8").result(llms));
            config.routes.get("/tutorial.html", ctx -> ctx.html(tutorial));
            // the tutorial was published as the tour; keep old links and bookmarks working
            config.routes.get("/tour.html", ctx -> ctx.redirect("/tutorial.html", HttpStatus.MOVED_PERMANENTLY));
            config.routes.get("/sandbox.html", ctx -> ctx.html(sandbox));
            config.routes.get("/library.html", ctx -> ctx.html(libraryPage));
            // the sandbox was published as the playground; keep old links (llms.txt, bookmarks) working
            config.routes.get("/playground.html", ctx -> ctx.redirect("/sandbox.html", HttpStatus.MOVED_PERMANENTLY));
            // the docs index is /docs/ (its relative image link needs the trailing slash); <page> also matches slashes
            config.routes.get("/docs", ctx -> handleDocs(ctx, docs, docsLogo));
            config.routes.get("/docs/<page>", ctx -> handleDocs(ctx, docs, docsLogo));
            config.routes.get("/health", ctx -> ctx.json(EvalService.health()));
            config.routes.get("/examples", ctx -> ctx.json(Map.of("examples", examples)));
            config.routes.get("/examples/{name}", ctx -> {
                String name   = ctx.pathParam("name");
                String source = examples.contains(name) ? NelumboServer.exampleSource(name) : null;
                if (source == null) {
                    ctx.status(HttpStatus.NOT_FOUND).result("unknown example: " + name);
                } else {
                    ctx.contentType("text/plain; charset=utf-8").result(source);
                }
            });
            config.routes.get("/library", ctx -> ctx.json(library.json()));
            config.routes.get("/library/{name}", ctx -> {
                Optional<Library.Module> module = library.module(ctx.pathParam("name"));
                if (module.isEmpty()) {
                    ctx.status(HttpStatus.NOT_FOUND).result("unknown library module: " + ctx.pathParam("name"));
                } else {
                    ctx.contentType("text/plain; charset=utf-8").result(module.get().source());
                }
            });
            config.routes.post("/eval", ctx -> handleEval(ctx, false));
            config.routes.post("/eval/trace", ctx -> handleEval(ctx, true));
            config.routes.get("/metadata", ctx -> ctx.json(service.metadata()));
            config.routes.get("/stats", ctx -> ctx.json(ServerStats.json(latestSample(lsp, solutions), maxLspSessions, EvalGate.GLOBAL)));
            config.routes.get("/stats/history", ctx -> {
                StatsHistory.Range range;
                try {
                    range = StatsHistory.Range.parse(ctx.queryParam("range"));
                } catch (IllegalArgumentException e) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("range must be hour, day or week");
                    return;
                }
                ctx.json(Map.of("range", range.name().toLowerCase(java.util.Locale.ROOT), "resolutionSeconds", range.resolutionSeconds(),
                        "points", history.rows(range)));
            });
            config.routes.sse("/stats/stream", client -> {
                client.keepAlive();
                StatsSample latest = recorder.latest();
                stream.connect(StatsStream.Client.of(client), latest == null ? null : statsJson(latest));
            });
            config.routes.put("/solutions/{name}", ctx -> reply(ctx, solutions.save(ctx.pathParam("name"), ctx.body(),
                    ctx.header("X-Nelumbo-Browser"), clientIp(ctx))));
            config.routes.get("/solutions/{name}", ctx -> reply(ctx, solutions.load(ctx.pathParam("name"))));
            config.routes.get("/solutions", ctx -> ctx.json(Map.of("enabled", store.sharingEnabled())));
            config.routes.get("/admin/me", ctx -> ctx.status(HttpStatus.NO_CONTENT));
            config.routes.get("/admin", ctx -> ctx.html(admin));
            config.routes.get("/admin/solutions", ctx -> {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Solution s : store.list()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", s.name());
                    row.put("savedAt", s.savedAt());
                    row.put("bytes", s.text().getBytes(StandardCharsets.UTF_8).length);
                    row.put("owner", s.ownerHash().substring(0, 6));
                    row.put("ip", s.ip());
                    rows.add(row);
                }
                ctx.json(rows);
            });
            config.routes.get("/admin/solutions/{name}", ctx -> {
                Optional<Solution> found = store.get(ctx.pathParam("name"));
                if (found.isEmpty()) {
                    ctx.status(HttpStatus.NOT_FOUND).result("unknown solution");
                } else {
                    ctx.contentType("text/plain; charset=utf-8").result(found.get().text());
                }
            });
            config.routes.delete("/admin/solutions/{name}", ctx ->
                    ctx.status(store.delete(ctx.pathParam("name")) ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND));
            config.routes.delete("/admin/solutions", ctx -> ctx.json(Map.of("deleted", store.deleteAll())));
            config.routes.get("/admin/sharing", ctx -> ctx.json(Map.of("enabled", store.sharingEnabled())));
            config.routes.put("/admin/sharing", ctx -> {
                JsonNode body = null;
                try {
                    body = statsMapper.readTree(ctx.body());
                } catch (JsonProcessingException e) {
                    // answered below
                }
                if (body == null || !body.has("enabled") || !body.get("enabled").isBoolean()) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("expected {\"enabled\": true|false}");
                    return;
                }
                store.setSharingEnabled(body.get("enabled").asBoolean());
                ctx.json(Map.of("enabled", store.sharingEnabled()));
            });
            config.routes.ws("/lsp", lsp::configure);
        });
        app.start(port);
        return app.port();
    }

    // before the first scheduled sample has landed, read one directly
    private StatsSample latestSample(LspWebSocket lsp, SolutionService solutions) {
        StatsSample latest = recorder.latest();
        return latest != null ? latest : ServerStats.read(lsp.sessionCount(), EvalGate.GLOBAL, solutions.usage());
    }

    private String statsJson(StatsSample sample) {
        try {
            return statsMapper.writeValueAsString(ServerStats.json(sample, maxLspSessions, EvalGate.GLOBAL));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void stop() {
        if (recorder != null) {
            recorder.close();
        }
        if (stream != null) {
            stream.close();
        }
        if (app != null) {
            app.stop();
        }
        service.close();
    }

    private static void handleDocs(Context ctx, DocsSite docs, String docsLogo) {
        String path = ctx.path();
        if (path.equals("/docs")) {
            ctx.redirect("/docs/");
        } else if (path.equals(DocsSite.URL_PREFIX + "nelumbo.svg")) {
            ctx.contentType("image/svg+xml").result(docsLogo);
        } else {
            Optional<String> page = docs.page(path);
            if (page.isPresent()) {
                ctx.html(page.get());
            } else {
                ctx.status(404).html(docs.notFoundPage());
            }
        }
    }

    private String clientIp(Context ctx) {
        List<String> lines = Collections.list(ctx.req().getHeaders("X-Forwarded-For"));
        return access.clientIp(lines.isEmpty() ? null : String.join(",", lines), ctx.header(ClientAccess.PROXY_HEADER), ctx.ip());
    }

    private void guardAdmin(Context ctx) {
        if (!access.isAdmin(clientIp(ctx), ctx.host())) {
            throw new EndpointNotFound(ctx.method(), ctx.path());
        }
        ctx.header("Cache-Control", "no-store");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Content-Security-Policy", "frame-ancestors 'none'");
        ctx.header("Cross-Origin-Resource-Policy", "same-origin");
    }

    private static void reply(Context ctx, SolutionService.Response response) {
        ctx.status(response.status()).contentType("text/plain; charset=utf-8").result(response.body());
    }

    private void handleEval(Context ctx, boolean pathTrace) {
        EvalService.Response response = service.eval(ctx.body(), ctx.contentType(), pathTrace);
        ctx.status(response.status()).json(response.body());
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
