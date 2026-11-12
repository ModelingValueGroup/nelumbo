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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.server.KnowledgeBaseLoader;
import org.modelingvalue.nelumbo.server.NamedSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The eval/metadata/health REST behavior is covered in the {@code server} module ({@code NelumboServerTest}); this
 * test covers what the website layers on top: the public pages, the bundled frontend, and that the embedded
 * {@code NelumboServer} still evaluates through the composed server.
 */
class NelumboHttpServerTest {

    // The startup knowledge base: fib declarations only (queries are posted per request).
    private static final String FIB_BASE = """
            import nelumbo.integers

            Integer ::= fib(<Integer>)

            Integer n, f

            fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                          f=fib(n-1)+fib(n-2) if n>1
            """;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient   client = HttpClient.newHttpClient();

    private NelumboHttpServer server;
    private int               port;

    @BeforeEach
    void startServer() {
        KnowledgeBase base = KnowledgeBaseLoader.load(List.of(new NamedSource("fibonacci.nl", FIB_BASE)));
        server = new NelumboHttpServer(base, List.of("fibonacci.nl"));
        port = server.start(0);
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(request, BodyHandlers.ofString());
    }

    @Test
    void evalIsServedByTheEmbeddedServer() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/eval"))
                .header("Content-Type", "text/plain").POST(BodyPublishers.ofString("Integer r\nfib(5)=r ?\n")).build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonNode query = mapper.readTree(response.body()).get("queries").get(0);
        assertEquals("true", query.get("status").asText());
        assertEquals("5", query.get("bindings").get(0).get("r").asText());
    }

    @Test
    void healthReportsOk() throws Exception {
        HttpResponse<String> response = get("/health");
        assertEquals(200, response.statusCode());
        assertEquals("ok", mapper.readTree(response.body()).get("status").asText());
    }

    @Test
    void landingIsServedAtRoot() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"),
                "landing page should be served as HTML");
        String html = response.body();
        assertTrue(html.contains("Nelumbo"), "landing page should introduce Nelumbo");
        assertTrue(html.contains("href=\"/tutorial.html\""), "landing page should link to the tutorial");
        assertTrue(html.contains("href=\"/sandbox.html\""), "landing page should link to the sandbox");
    }

    @Test
    void tutorialIsServedAtItsPath() throws Exception {
        HttpResponse<String> response = get("/tutorial.html");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"),
                "tutorial should be served as HTML");
        String html = response.body();
        assertTrue(html.contains("data-section=\"logic\""), "tutorial should render the sidebar navigation");
        assertTrue(html.contains("nelumbo-field"), "tutorial should mount Nelumbo editor fields");
        assertTrue(html.contains("/assets/nelumbo-fields.js"), "tutorial should load the frontend bundle");
    }

    @Test
    void sandboxIsServedAtItsPath() throws Exception {
        HttpResponse<String> response = get("/sandbox.html");
        assertEquals(200, response.statusCode());
        String html = response.body();
        assertTrue(html.contains("nelumbo-field"), "sandbox should mount a Nelumbo editor field");
        assertTrue(html.contains("initNelumboFields"), "sandbox should initialize the editor fields");
    }

    /**
     * The sandbox sidebar lists the bundled examples from /examples (same JSON shape as the cli eval server).
     * The sudokus are left out: the 9x9 ones run far past the eval deadline and would only show timeouts.
     */
    @Test
    void examplesAreListedForTheSandboxWithoutTheSudokus() throws Exception {
        HttpResponse<String> list = get("/examples");
        assertEquals(200, list.statusCode());
        assertTrue(list.headers().firstValue("Content-Type").orElse("").contains("json"), "the list is JSON");
        assertTrue(list.body().contains("\"examples\""), list.body());
        assertTrue(list.body().contains("\"fibonacci\""), "a finished example is listed");
        assertTrue(list.body().contains("\"familyAssignment\""), "an exercise is listed");
        assertFalse(list.body().contains("sudoku"), "no sudoku is listed: " + list.body());
    }

    @Test
    void anExampleIsServedAsPlainNelumboSource() throws Exception {
        HttpResponse<String> example = get("/examples/fibonacci");
        assertEquals(200, example.statusCode());
        assertTrue(example.headers().firstValue("Content-Type").orElse("").contains("text/plain"), "served as text, not HTML");
        assertTrue(example.body().contains("fib("), "the fibonacci example source");
        assertEquals(404, get("/examples/sudoku-brute-4x4-easy").statusCode(), "unlisted examples are not served");
        assertEquals(404, get("/examples/noSuchExample").statusCode());
    }

    @Test
    void libraryIsServedAtItsPath() throws Exception {
        HttpResponse<String> response = get("/library.html");
        assertEquals(200, response.statusCode());
        String html = response.body();
        assertTrue(html.contains("nelumbo-field"), "the library should mount a Nelumbo editor field");
        assertTrue(html.contains("initNelumboFields"), "the library should initialize the editor field");
        assertTrue(html.contains("href=\"/library.html\" aria-current=\"page\""), "the header marks the library as the current page");
    }

    /** Every page's header links the library, between the sandbox and the docs. */
    @Test
    void everyPageLinksTheLibraryBetweenSandboxAndDocs() throws Exception {
        for (String page : List.of("/", "/tutorial.html", "/sandbox.html", "/library.html", "/docs/")) {
            String html    = get(page).body();
            int    sandbox = html.indexOf("<a href=\"/sandbox.html\"");
            int    library = html.indexOf("<a href=\"/library.html\"");
            int    docs    = html.indexOf("<a href=\"/docs/\"");
            assertTrue(0 <= sandbox && sandbox < library && library < docs, page + " should link Sandbox, Library, Docs in that order");
        }
    }

    /** The library sidebar lists the stdlib modules; each explanation is the opening paragraph of its reference page. */
    @Test
    void libraryListsTheStdlibModulesWithTheirReferenceIntro() throws Exception {
        HttpResponse<String> list = get("/library");
        assertEquals(200, list.statusCode());
        assertTrue(list.headers().firstValue("Content-Type").orElse("").contains("json"), "the list is JSON");
        JsonNode modules = mapper.readTree(list.body()).get("modules");
        List<String> names = Stream.iterate(0, i -> i < modules.size(), i -> i + 1).map(i -> modules.get(i).get("name").asText()).toList();
        assertEquals(List.of("lang", "logic", "integers", "rationals", "strings", "collections", "datetime"), names);
        for (JsonNode module : modules) {
            String intro = module.get("intro").asText();
            assertTrue(intro.startsWith("<p>") && !intro.contains("Level:"), "the intro is the paragraph after the level banner: " + intro);
            assertEquals(200, get(module.get("docs").asText()).statusCode(), "the reference link of " + module.get("name") + " resolves");
        }
        JsonNode collections = modules.get(names.indexOf("collections"));
        assertEquals("/docs/reference/packages/collections.html", collections.get("docs").asText());
        assertTrue(collections.get("intro").asText().contains("href=\"/docs/reference/logic/lambdas.html\""), "links in the intro point at the served docs");
    }

    @Test
    void aLibraryModuleIsServedAsPlainNelumboSource() throws Exception {
        HttpResponse<String> module = get("/library/integers");
        assertEquals(200, module.statusCode());
        assertTrue(module.headers().firstValue("Content-Type").orElse("").contains("text/plain"), "served as text, not HTML");
        assertTrue(module.body().contains("Integer              :: Object"), "the integers module source");
        assertEquals(404, get("/library/noSuchModule").statusCode());
    }

    /**
     * The editor turns {@code \r\n} into {@code \n}, so a source served with {@code \r\n} (a Windows checkout with
     * core.autocrlf) never equals the editor text: examples and library modules are always served with {@code \n}.
     */
    @Test
    void sourcesAreServedWithUnixLineEndings() throws Exception {
        for (String path : List.of("/examples/fibonacci", "/examples/family", "/library/lang", "/library/integers", "/library/collections")) {
            String body = get(path).body();
            assertTrue(body.contains("\n"), path);
            assertFalse(body.contains("\r"), path + " is served with Windows line endings");
        }
    }

    /** A library module opened in the editor must not show errors on a server like production's (empty base KB). */
    @Test
    void everyLibraryModuleEvaluatesWithoutErrors() throws Exception {
        NelumboHttpServer empty     = new NelumboHttpServer(KnowledgeBaseLoader.load(List.of()), List.of());
        int               emptyPort = empty.start(0);
        try {
            for (JsonNode module : mapper.readTree(get("/library").body()).get("modules")) {
                String      source  = get("/library/" + module.get("name").asText()).body();
                HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + emptyPort + "/eval"))
                        .header("Content-Type", "text/plain").POST(BodyPublishers.ofString(source)).build();
                JsonNode    result  = mapper.readTree(client.send(request, BodyHandlers.ofString()).body());
                assertEquals(0, result.get("errors").size(), module.get("name") + ": " + result.get("errors"));
            }
        } finally {
            empty.stop();
        }
    }

    /** The tutorial was published as /tour.html (e.g. in llms.txt); old links must keep working. */
    @Test
    void formerTourUrlRedirectsPermanentlyToTheTutorial() throws Exception {
        HttpResponse<String> response = get("/tour.html");
        assertEquals(301, response.statusCode());
        assertEquals("/tutorial.html", response.headers().firstValue("Location").orElse(""));
    }

    /** The sandbox was published as /playground.html (e.g. in llms.txt); old links must keep working. */
    @Test
    void formerPlaygroundUrlRedirectsPermanentlyToTheSandbox() throws Exception {
        HttpResponse<String> response = get("/playground.html");
        assertEquals(301, response.statusCode());
        assertEquals("/sandbox.html", response.headers().firstValue("Location").orElse(""));
    }

    @Test
    void docsAreServedUnderDocs() throws Exception {
        HttpResponse<String> index = get("/docs/");
        assertEquals(200, index.statusCode());
        assertTrue(index.headers().firstValue("Content-Type").orElse("").contains("text/html"), "docs should be served as HTML");
        assertTrue(index.body().contains("Nelumbo documentation"), "the docs index is the documentation overview");
        assertTrue(index.body().contains("href=\"/docs/reference/lang/grammar.html\""), "the docs sidebar should link the reference pages");

        HttpResponse<String> grammar = get("/docs/reference/lang/grammar.html");
        assertEquals(200, grammar.statusCode(), "nested doc pages are served by the wildcard route");
        assertTrue(grammar.body().contains("<title>Grammar - Nelumbo docs</title>"), grammar.body().substring(0, 300));

        HttpResponse<String> overview = get("/docs/component-overview.html");
        assertEquals(200, overview.statusCode(), "the standalone component overview page is served under /docs/");
        assertTrue(overview.body().contains("Nelumbo, piece by piece"), "it is Wim's page as it is");
        assertTrue(index.body().contains("href=\"/docs/component-overview.html\""), "the docs sidebar links it");

        HttpResponse<String> logo = get("/docs/nelumbo.svg");
        assertEquals(200, logo.statusCode(), "the overview embeds the logo relative to /docs/");
        assertTrue(logo.headers().firstValue("Content-Type").orElse("").contains("image/svg+xml"));

        HttpResponse<String> missing = get("/docs/reference/nope.html");
        assertEquals(404, missing.statusCode());
        assertTrue(missing.body().contains("Page not found"), "unknown docs pages get the docs-styled 404");

        HttpResponse<String> bare = get("/docs");
        assertEquals(302, bare.statusCode(), "/docs redirects to /docs/ so relative links on the index resolve");
        assertEquals("/docs/", bare.headers().firstValue("Location").orElse(""));
    }

    /** Every page loads /theme.js blocking in its head; a 404 there leaves every page without a theme switch. */
    @Test
    void themeScriptIsServedAndLoadedByEveryPage() throws Exception {
        HttpResponse<String> script = get("/theme.js");
        assertEquals(200, script.statusCode());
        assertTrue(script.headers().firstValue("Content-Type").orElse("").contains("javascript"), "theme.js must be served as JavaScript");
        assertTrue(script.body().contains("prefers-color-scheme"), "the theme script follows the OS preference");
        for (String page : new String[]{"/", "/tutorial.html", "/sandbox.html", "/library.html", "/docs/"}) {
            String html = get(page).body();
            assertTrue(html.contains("<script src=\"/theme.js\"></script>"), page + " must load the theme script");
            assertTrue(html.contains("class=\"theme-toggle\""), page + " must show the theme switch");
        }
    }

    @Test
    void llmsTxtIsServedAtTheSiteRoot() throws Exception {
        HttpResponse<String> response = get("/llms.txt");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/plain"), "llms.txt must not be served as HTML");
        assertTrue(response.body().startsWith("# Nelumbo"), "the llms.txt format starts with the project H1");
        assertTrue(response.body().contains("https://nelumbo.nl/docs/"), "it should link the served docs");
    }

    /**
     * The llms.txt links are hand-written absolute URLs, so a renamed docs page would 404 silently; every
     * nelumbo.nl link must resolve on this server (the github.com ones cannot be checked offline).
     */
    @Test
    void everyLinkInLlmsTxtResolves() throws Exception {
        Matcher matcher = Pattern.compile("https://nelumbo\\.nl(/\\S*?)\\)").matcher(get("/llms.txt").body());
        int     checked = 0;
        while (matcher.find()) {
            String path = matcher.group(1);
            assertEquals(200, get(path).statusCode(), "llms.txt links " + path + ", which the site does not serve");
            checked++;
        }
        assertTrue(20 < checked, "expected the docs catalogue to be checked, but only found " + checked + " links");
    }

    /**
     * The tutorial's editors are hand-written Nelumbo that no other test evaluates, so a language change breaks them
     * silently (78e006b5 did: {@code fact} takes a Boolean since). Every editor must evaluate on a server like
     * production's (empty base KB): an exercise field may only fail on its expectation, a solution not at all.
     * Syntax-only editors ({@code data-syntax}) have no queries; a syntax exercise ({@code data-syntax="exercise"})
     * starts with a parse error that the learner fixes, its solution parses without errors.
     */
    /**
     * Every tutorial editor's text lives in a {@code .nl} file of the core jar ({@code data-src}), so it can be
     * edited and run as Nelumbo; the page itself holds no code. The server fills the files in.
     */
    @Test
    void tutorialEditorsComeFromNlFiles() throws Exception {
        String  raw    = new String(NelumboHttpServer.class.getResourceAsStream("/public/tutorial.html").readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        Pattern editor = Pattern.compile("<(div|pre) class=\"(nelumbo-field|nelumbo-solution)\"([^>]*)>(.*?)</\\1>", Pattern.DOTALL);
        Matcher inPage = editor.matcher(raw);
        int     count  = 0;
        while (inPage.find()) {
            count++;
            assertTrue(inPage.group(3).contains(" data-src=\"") && inPage.group(4).isEmpty(),
                    "a tutorial editor without a data-src .nl file: " + inPage.group());
        }
        assertTrue(20 < count, "expected the tutorial's editors, found " + count);
        String  served = get("/tutorial.html").body();
        assertFalse(served.contains("data-src="), "the served tutorial should have every editor filled in");
        Matcher filled = editor.matcher(served);
        int     texts  = 0;
        while (filled.find()) {
            assertFalse(filled.group(4).isBlank(), "an empty tutorial editor: " + filled.group());
            texts++;
        }
        assertEquals(count, texts);
    }

    @Test
    void everyTutorialEditorEvaluates() throws Exception {
        NelumboHttpServer tutorial     = new NelumboHttpServer(KnowledgeBaseLoader.load(List.of()), List.of());
        int               tutorialPort = tutorial.start(0);
        try {
            Pattern editor  = Pattern.compile("<(div|pre) class=\"(nelumbo-field|nelumbo-solution)\"([^>]*)>(.*?)</\\1>", Pattern.DOTALL);
            Matcher matcher = editor.matcher(get("/tutorial.html").body());
            int     checked = 0;
            while (matcher.find()) {
                String      kind     = matcher.group(2);
                boolean     syntax   = matcher.group(3).contains("data-syntax");
                boolean     exercise = matcher.group(3).contains("data-syntax=\"exercise\"");
                String      source   = matcher.group(4).replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
                HttpRequest request  = HttpRequest.newBuilder(URI.create("http://localhost:" + tutorialPort + "/eval"))
                        .header("Content-Type", "text/plain").POST(BodyPublishers.ofString(source)).build();
                JsonNode    result   = mapper.readTree(client.send(request, BodyHandlers.ofString()).body());
                String      where    = "tutorial " + kind + " #" + checked + ":\n" + source;
                assertTrue(syntax || 0 < result.get("queries").size(), "no query evaluated in " + where);
                if (exercise) {
                    assertTrue(0 < result.get("errors").size(), "a syntax exercise should start with a parse error in " + where);
                    checked++;
                    continue;
                }
                for (JsonNode error : result.get("errors")) {
                    String message = error.get("line") + ":" + error.get("column") + " " + error.get("message").asText();
                    assertTrue(kind.equals("nelumbo-field") && error.get("message").asText().startsWith("Expected result "), message + "\nin " + where);
                }
                checked++;
            }
            assertTrue(20 < checked, "expected the tutorial's editors to be checked, but only found " + checked);
        } finally {
            tutorial.stop();
        }
    }

    @Test
    void pagesLinkToTheDocs() throws Exception {
        for (String page : List.of("/", "/tutorial.html", "/sandbox.html", "/library.html")) {
            assertTrue(get(page).body().contains("href=\"/docs/\""), page + " should link to the docs");
        }
    }

    @Test
    void frontendBundleIsServed() throws Exception {
        HttpResponse<String> js = get("/assets/nelumbo-fields.js");
        assertEquals(200, js.statusCode(), "the frontend bundle referenced by the pages must actually be served");
        assertTrue(js.headers().firstValue("Content-Type").orElse("").contains("javascript"),
                "the bundle should be served as JavaScript");

        HttpResponse<String> css = get("/assets/nelumbo-fields.css");
        assertEquals(200, css.statusCode(), "the frontend stylesheet must actually be served");
    }

    @Test
    void statsHaveTheDocumentedShape() throws Exception {
        HttpResponse<String> response = get("/stats");
        assertEquals(200, response.statusCode());
        JsonNode stats = mapper.readTree(response.body());
        assertTrue(List.of("ok", "busy", "overloaded").contains(stats.get("status").asText()), "status is ok, busy or overloaded");
        assertEquals(0, stats.get("sessions").get("open").asInt(), "no LSP session is open in this test");
        assertEquals(NelumboHttpServer.DEFAULT_MAX_LSP_SESSIONS, stats.get("sessions").get("max").asInt());
        for (String field : List.of("running", "threshold", "total", "overloaded")) {
            assertTrue(stats.get("evaluations").has(field), "evaluations." + field);
        }
        assertTrue(stats.get("cpu").has("load"), "cpu.load");
        assertTrue(0 < stats.get("cpu").get("cpus").asInt(), "cpu.cpus");
        assertTrue(0 < stats.get("heap").get("maxMb").asLong(), "heap.maxMb");
        for (String field : List.of("count", "bytes", "maxBytes", "saves")) {
            assertTrue(stats.get("solutions").has(field), "solutions." + field);
        }
        assertEquals(SolutionService.MAX_TOTAL_BYTES, stats.get("solutions").get("maxBytes").asLong());
        assertTrue(stats.has("uptimeSeconds"), "uptimeSeconds");
    }

    @Test
    void theDotScriptIsServed() throws Exception {
        HttpResponse<String> dot = get("/status-dot.js");
        assertEquals(200, dot.statusCode());
        assertTrue(dot.headers().firstValue("Content-Type").orElse("").contains("javascript"), "the dot script is served as JavaScript");
    }

    @Test
    void everyPageWithAVersionShowsTheStatusDotButNoAdminLink() throws Exception {
        for (String page : List.of("/", "/tutorial.html", "/sandbox.html", "/library.html", "/docs/")) {
            String body = get(page).body();
            assertTrue(body.contains("<span class=\"status-dot\""), page + " shows the status dot");
            assertTrue(body.contains("src=\"/status-dot.js\""), page + " loads the status dot script");
            assertFalse(body.contains("href=\"/admin\""), page + " has no admin link in its HTML");
            assertFalse(body.contains("data-status-link"), page + " has no status link marker");
        }
    }

    @Test
    void adminPageLoadsItsBundle() throws Exception {
        String body = get("/admin").body();
        assertTrue(body.contains("src=\"/assets/admin.js\""), "the admin page loads its script");
        assertTrue(body.contains("href=\"/assets/admin.css\""), "the admin page loads its stylesheet");
        assertEquals(200, get("/assets/admin.js").statusCode());
        assertEquals(200, get("/assets/admin.css").statusCode());
    }

    @Test
    void statsCarryTheSampleTime() throws Exception {
        JsonNode stats = mapper.readTree(get("/stats").body());
        assertTrue(stats.get("time").asLong() > 1_700_000_000_000L, "time is the sample time in epoch ms");
    }

    @Test
    void historyHasTheDocumentedShape() throws Exception {
        for (String range : List.of("hour", "day", "week")) {
            HttpResponse<String> response = get("/stats/history?range=" + range);
            assertEquals(200, response.statusCode(), range);
            JsonNode history = mapper.readTree(response.body());
            assertEquals(range, history.get("range").asText());
            assertEquals(range.equals("week") ? 600 : 60, history.get("resolutionSeconds").asInt());
            assertTrue(history.get("points").isArray(), range + " points");
        }
        assertEquals(400, get("/stats/history?range=month").statusCode());
    }

    @Test
    @Timeout(20)
    void streamSendsAStatsEventRightAway() throws Exception {
        HttpRequest                  request  = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/stats/stream")).header("Accept", "text/event-stream").GET().build();
        HttpResponse<Stream<String>> response = client.send(request, BodyHandlers.ofLines());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/event-stream"));
        try (Stream<String> lines = response.body()) {
            // up to two events; tolerates a leading comment or retry line before the first event
            List<String> first = lines.filter(l -> !l.isBlank()).limit(4).toList();
            assertTrue(first.contains("event: stats"), "got " + first);
            assertTrue(first.stream().anyMatch(l -> l.startsWith("data: {") && l.contains("\"sessions\"")), "got " + first);
        }
    }

    private HttpResponse<String> put(String path, String browserId, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "text/plain; charset=utf-8").PUT(BodyPublishers.ofString(body));
        if (browserId != null) {
            request.header("X-Nelumbo-Browser", browserId);
        }
        return client.send(request.build(), BodyHandlers.ofString());
    }

    @Test
    void aSolutionIsSavedAndLoadedByName() throws Exception {
        String text = "fact p(A)\r\n// Zoë ✓\n";
        HttpResponse<String> saved = put("/solutions/Frits", "b1", text);
        assertEquals(200, saved.statusCode());
        assertEquals("Shared as \"Frits\" (kept 7 days)", saved.body());
        HttpResponse<String> loaded = get("/solutions/frits");
        assertEquals(200, loaded.statusCode());
        assertEquals(text, loaded.body());
        assertTrue(loaded.headers().firstValue("Content-Type").orElse("").startsWith("text/plain"));
    }

    @Test
    void anotherBrowserGetsTakenAndANoIdSaveIsRefused() throws Exception {
        put("/solutions/Frits", "b1", "x");
        HttpResponse<String> taken = put("/solutions/Frits", "b2", "y");
        assertEquals(409, taken.statusCode());
        assertEquals("\"Frits\" is taken", taken.body());
        assertEquals(400, put("/solutions/Other", null, "x").statusCode());
        assertEquals(404, get("/solutions/Nobody").statusCode());
    }

    @Test
    void solutionsSurviveARestart(@TempDir Path dir) throws Exception {
        server.stop();
        KnowledgeBase base = KnowledgeBaseLoader.load(List.of(new NamedSource("fibonacci.nl", FIB_BASE)));
        server = new NelumboHttpServer(base, List.of("fibonacci.nl"), NelumboHttpServer.DEFAULT_TIMEOUT_MS, NelumboHttpServer.DEFAULT_MAX_LSP_SESSIONS, null, dir);
        port = server.start(0);
        put("/solutions/Frits", "b1", "kept");
        server.stop();
        server = new NelumboHttpServer(base, List.of("fibonacci.nl"), NelumboHttpServer.DEFAULT_TIMEOUT_MS, NelumboHttpServer.DEFAULT_MAX_LSP_SESSIONS, null, dir);
        port = server.start(0);
        assertEquals("kept", get("/solutions/Frits").body());
    }

    private HttpResponse<String> request(String method, String path, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return client.send(request.build(), BodyHandlers.ofString());
    }

    @Test
    void adminPathsAre404ForANonAdmin() throws Exception {
        restartWith(ClientAccess.of(null, "s3cret"));
        String[] headers = {ClientAccess.PROXY_HEADER, "s3cret", "X-Forwarded-For", "203.0.113.5"};
        String[][] calls = {{"GET", "/admin"}, {"GET", "/admin/"}, {"HEAD", "/admin"}, {"HEAD", "/admin/me"}, {"HEAD", "/admin/solutions"}, {"HEAD", "/admin/solutions/Frits"}, {"HEAD", "/admin/sharing"}, {"OPTIONS", "/admin/solutions"}, {"GET", "/admin/me"}, {"GET", "/admin/solutions"}, {"GET", "/admin/solutions/Frits"},
                {"DELETE", "/admin/solutions/Frits"}, {"DELETE", "/admin/solutions"}, {"GET", "/admin/sharing"}, {"PUT", "/admin/sharing"}};
        for (String[] call : calls) {
            String                  body     = call[0].equals("PUT") ? "{\"enabled\":false}" : null;
            HttpResponse<String>    actual   = request(call[0], call[1], body, headers);
            HttpResponse<String>    expected = request(call[0], "/no-such-page", body, headers);
            assertEquals(expected.statusCode(), actual.statusCode(), call[0] + " " + call[1]);
            assertEquals(404, actual.statusCode(), call[0] + " " + call[1]);
            assertEquals(expected.body().replace("/no-such-page", call[1]), actual.body(), call[0] + " " + call[1]);
        }
    }

    @Test
    void localhostIsAdmin() throws Exception {
        HttpResponse<String> me = get("/admin/me");
        assertEquals(204, me.statusCode());
        assertEquals("no-store", me.headers().firstValue("Cache-Control").orElse(""));
        assertEquals("DENY", me.headers().firstValue("X-Frame-Options").orElse(""));
        assertEquals("frame-ancestors 'none'", me.headers().firstValue("Content-Security-Policy").orElse(""));
        assertEquals("same-origin", me.headers().firstValue("Cross-Origin-Resource-Policy").orElse(""));
        assertEquals(200, get("/admin").statusCode());
    }

    private int rawStatus(String... headerLines) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            StringBuilder request = new StringBuilder("GET /admin/me HTTP/1.1\r\n");
            for (String line : headerLines) {
                request.append(line).append("\r\n");
            }
            request.append("Connection: close\r\n\r\n");
            socket.getOutputStream().write(request.toString().getBytes(StandardCharsets.US_ASCII));
            String status = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)).readLine();
            return Integer.parseInt(status.split(" ")[1]);
        }
    }

    @Test
    void proxySecretAndHostDecideAdminOverHttp() throws Exception {
        restartWith(ClientAccess.of("198.51.100.0/24", "s3cret"));
        String host = "Host: evil.example";
        String xff  = "X-Forwarded-For: 198.51.100.9";
        assertEquals(404, rawStatus(host, xff));
        assertEquals(404, rawStatus(host, xff, "X-Nelumbo-Proxy: wrong"));
        assertEquals(204, rawStatus(host, xff, "X-Nelumbo-Proxy: s3cret"));
        assertEquals(404, rawStatus(host));
    }

    @Test
    void theLastForwardedForLineWins() throws Exception {
        restartWith(ClientAccess.of("198.51.100.0/24", "s3cret"));
        String host  = "Host: evil.example";
        String proxy = "X-Nelumbo-Proxy: s3cret";
        assertEquals(404, rawStatus(host, proxy, "X-Forwarded-For: 198.51.100.9", "X-Forwarded-For: 203.0.113.5"));
        assertEquals(204, rawStatus(host, proxy, "X-Forwarded-For: 203.0.113.5", "X-Forwarded-For: 198.51.100.9"));
    }

    @Test
    void adminListsOpensAndDeletesSnippets() throws Exception {
        put("/solutions/Frits", "b1", "abc");
        put("/solutions/Anna", "b2", "x");
        JsonNode rows = mapper.readTree(get("/admin/solutions").body());
        assertEquals(2, rows.size());
        JsonNode frits = null;
        for (JsonNode row : rows) {
            if (row.get("name").asText().equals("Frits")) {
                frits = row;
            }
        }
        assertEquals(3, frits.get("bytes").asInt());
        assertEquals(6, frits.get("owner").asText().length());
        assertFalse(frits.get("ip").isNull());
        assertEquals("abc", get("/admin/solutions/frits").body());
        assertEquals(204, request("DELETE", "/admin/solutions/Frits", null).statusCode());
        assertEquals(404, request("DELETE", "/admin/solutions/Frits", null).statusCode());
        assertEquals(404, get("/solutions/Frits").statusCode());
        assertEquals("{\"deleted\":1}", request("DELETE", "/admin/solutions", null).body().replace(" ", ""));
    }

    @Test
    void theSharingSwitchStopsShareAndLoad() throws Exception {
        put("/solutions/Frits", "b1", "x");
        assertEquals("{\"enabled\":true}", get("/solutions").body().replace(" ", ""));
        assertEquals("{\"enabled\":false}", request("PUT", "/admin/sharing", "{\"enabled\":false}").body().replace(" ", ""));
        assertEquals("{\"enabled\":false}", get("/solutions").body().replace(" ", ""));
        HttpResponse<String> refused = put("/solutions/Other", "b2", "y");
        assertEquals(403, refused.statusCode());
        assertEquals("Sharing is turned off", refused.body());
        assertEquals(403, get("/solutions/Frits").statusCode());
        HttpResponse<String> admin = get("/admin/solutions/Frits");
        assertEquals(200, admin.statusCode());
        assertEquals("x", admin.body());
        assertEquals(400, request("PUT", "/admin/sharing", "nonsense").statusCode());
        request("PUT", "/admin/sharing", "{\"enabled\":true}");
        assertEquals("x", get("/solutions/Frits").body());
    }

    @Test
    void theStatusPageIsGone() throws Exception {
        assertEquals(404, get("/status.html").statusCode());
    }

    private void restartWith(ClientAccess access) {
        server.stop();
        KnowledgeBase base = KnowledgeBaseLoader.load(List.of(new NamedSource("fibonacci.nl", FIB_BASE)));
        server = new NelumboHttpServer(base, List.of("fibonacci.nl"), NelumboHttpServer.DEFAULT_TIMEOUT_MS, NelumboHttpServer.DEFAULT_MAX_LSP_SESSIONS, null, null, access);
        port   = server.start(0);
    }

    private int putFrom(String name, String browserId, String forwardedFor, String secret) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/solutions/" + name))
                .header("X-Nelumbo-Browser", browserId).header("X-Forwarded-For", forwardedFor);
        if (secret != null) {
            request.header(ClientAccess.PROXY_HEADER, secret);
        }
        return client.send(request.PUT(BodyPublishers.ofString("x")).build(), BodyHandlers.ofString()).statusCode();
    }

    @Test
    void theNewNameLimitCountsTheAddressTraefikAppended() throws Exception {
        restartWith(ClientAccess.of(null, "s3cret"));
        for (int i = 0; i < SolutionService.NEW_NAMES_PER_IP_PER_HOUR; i++) {
            assertEquals(200, putFrom("n" + i, "b" + i, "1.1.1." + i + ", 9.9.9.9", "s3cret"));
        }
        assertEquals(429, putFrom("forged", "bf", "5.5.5.5, 9.9.9.9", "s3cret"));
        assertEquals(200, putFrom("other", "bo", "8.8.8.8", "s3cret"));
    }

    @Test
    void forwardedForWithoutTheSecretIsIgnored() throws Exception {
        restartWith(ClientAccess.of(null, "s3cret"));
        for (int i = 0; i < SolutionService.NEW_NAMES_PER_IP_PER_HOUR; i++) {
            assertEquals(200, putFrom("n" + i, "b" + i, "1.1.1." + i, null));
        }
        assertEquals(429, putFrom("one-more", "bm", "2.2.2.2", null));
    }

    @Test
    @Timeout(20)
    void statsCountTheSavedSolutions() throws Exception {
        put("/solutions/Frits", "b1", "x");
        JsonNode solutions = mapper.readTree(get("/stats").body()).get("solutions");
        while (solutions.get("count").asInt() == 0) {
            Thread.sleep(200); // the next 5-second sample
            solutions = mapper.readTree(get("/stats").body()).get("solutions");
        }
        assertEquals(1, solutions.get("count").asInt());
        assertEquals(Solution.ENTRY_BYTES + 1, solutions.get("bytes").asLong());
        assertEquals(1, solutions.get("saves").asLong());
    }
}
