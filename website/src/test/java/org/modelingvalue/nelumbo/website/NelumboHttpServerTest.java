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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        assertTrue(html.contains("href=\"/playground.html\""), "landing page should link to the playground");
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
    void playgroundIsServedAtItsPath() throws Exception {
        HttpResponse<String> response = get("/playground.html");
        assertEquals(200, response.statusCode());
        String html = response.body();
        assertTrue(html.contains("nelumbo-field"), "playground should mount a Nelumbo editor field");
        assertTrue(html.contains("initNelumboFields"), "playground should initialize the editor fields");
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
}
