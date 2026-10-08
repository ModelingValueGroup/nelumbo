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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
        assertTrue(html.contains("href=\"/tour.html\""), "landing page should link to the tour");
        assertTrue(html.contains("href=\"/sandbox.html\""), "landing page should link to the sandbox");
    }

    @Test
    void tourIsServedAtItsPath() throws Exception {
        HttpResponse<String> response = get("/tour.html");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"),
                "tour should be served as HTML");
        String html = response.body();
        assertTrue(html.contains("data-section=\"logic\""), "tour should render the sidebar navigation");
        assertTrue(html.contains("nelumbo-field"), "tour should mount Nelumbo editor fields");
        assertTrue(html.contains("/assets/nelumbo-fields.js"), "tour should load the frontend bundle");
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
        assertEquals(404, get("/examples/sudoku-4x4").statusCode(), "unlisted examples are not served");
        assertEquals(404, get("/examples/noSuchExample").statusCode());
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
        for (String page : new String[]{"/", "/tour.html", "/sandbox.html", "/docs/"}) {
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
     * The tour's editors are hand-written Nelumbo that no other test evaluates, so a language change breaks them
     * silently (78e006b5 did: {@code fact} takes a FactType since). Every editor must evaluate on a server like
     * production's (empty base KB): an exercise field may only fail on its expectation, a solution not at all.
     */
    @Test
    void everyTourEditorEvaluates() throws Exception {
        NelumboHttpServer tour     = new NelumboHttpServer(KnowledgeBaseLoader.load(List.of()), List.of());
        int               tourPort = tour.start(0);
        try {
            Pattern editor  = Pattern.compile("<(div|pre) class=\"(nelumbo-field|nelumbo-solution)\"[^>]*>(.*?)</\\1>", Pattern.DOTALL);
            Matcher matcher = editor.matcher(get("/tour.html").body());
            int     checked = 0;
            while (matcher.find()) {
                String      kind     = matcher.group(2);
                String      source   = matcher.group(3).replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
                HttpRequest request  = HttpRequest.newBuilder(URI.create("http://localhost:" + tourPort + "/eval"))
                        .header("Content-Type", "text/plain").POST(BodyPublishers.ofString(source)).build();
                JsonNode    result   = mapper.readTree(client.send(request, BodyHandlers.ofString()).body());
                String      where    = "tour " + kind + " #" + checked + ":\n" + source;
                assertTrue(0 < result.get("queries").size(), "no query evaluated in " + where);
                for (JsonNode error : result.get("errors")) {
                    String message = error.get("line") + ":" + error.get("column") + " " + error.get("message").asText();
                    assertTrue(kind.equals("nelumbo-field") && error.get("message").asText().startsWith("Expected result "), message + "\nin " + where);
                }
                checked++;
            }
            assertTrue(20 < checked, "expected the tour's editors to be checked, but only found " + checked);
        } finally {
            tour.stop();
        }
    }

    @Test
    void pagesLinkToTheDocs() throws Exception {
        for (String page : List.of("/", "/tour.html", "/sandbox.html")) {
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
        assertTrue(stats.has("uptimeSeconds"), "uptimeSeconds");
    }

    @Test
    void statusPageAndDotScriptAreServed() throws Exception {
        HttpResponse<String> page = get("/status.html");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("src=\"/assets/status-chart.js\""), "the status page loads its chart bundle");
        HttpResponse<String> dot = get("/status-dot.js");
        assertEquals(200, dot.statusCode());
        assertTrue(dot.headers().firstValue("Content-Type").orElse("").contains("javascript"), "the dot script is served as JavaScript");
        assertEquals(200, get("/assets/status-chart.js").statusCode(), "the chart bundle is served");
    }

    @Test
    void everyPageWithAVersionShowsTheStatusDotButNoStatusLink() throws Exception {
        for (String page : List.of("/", "/tour.html", "/sandbox.html", "/docs/")) {
            String body = get(page).body();
            assertTrue(body.contains("<span class=\"status-dot\""), page + " shows the status dot");
            assertTrue(body.contains("src=\"/status-dot.js\""), page + " loads the status dot script");
            assertFalse(body.contains("href=\"/status.html\""), page + " has no visible link to the internal status page");
            assertTrue(body.contains("<span class=\"status-dot\" data-status-link"), page + " makes its status dot the shift+click shortcut to the status page");
        }
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
}
