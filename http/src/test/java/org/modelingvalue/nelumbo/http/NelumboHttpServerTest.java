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

package org.modelingvalue.nelumbo.http;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class NelumboHttpServerTest {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

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

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "text/plain").POST(BodyPublishers.ofString(body)).build();
        return client.send(request, BodyHandlers.ofString());
    }

    @Test
    void healthReportsOk() throws Exception {
        HttpResponse<String> response = get("/health");
        assertEquals(200, response.statusCode());
        assertEquals("ok", mapper.readTree(response.body()).get("status").asText());
    }

    @Test
    void playgroundIsServedAtRoot() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"),
                "playground should be served as HTML");
        String html = response.body();
        assertTrue(html.contains("<textarea"), "playground should have a text input");
        assertTrue(html.contains("/eval"), "playground should post documents to /eval");
    }

    @Test
    void evalSolvesQueryAgainstLoadedKnowledgeBase() throws Exception {
        // A posted document is self-contained: it declares the variable it queries with. The fib rule
        // it relies on comes from the knowledge base loaded at startup.
        HttpResponse<String> response = post("/eval", "Integer r\nfib(5)=r ?\n");
        assertEquals(200, response.statusCode());

        JsonNode body = mapper.readTree(response.body());
        assertTrue(body.get("errors").isEmpty(), "no parse errors expected: " + body.get("errors"));

        JsonNode queries = body.get("queries");
        assertEquals(1, queries.size(), "exactly one query in the document");
        JsonNode query = queries.get(0);

        // The whole point: the rule loaded at startup is used to solve fib(5)=5.
        assertEquals("true", query.get("status").asText());
        // The binding must actually carry the answer r=5 — not merely "a result".
        assertEquals("5", query.get("bindings").get(0).get("r").asText());
        assertTrue(query.get("result").asText().contains("r=5"), "canonical result should show r=5: " + query.get("result"));
    }

    @Test
    void evalTimesOutOnLongRunningInference() throws Exception {
        // A 1 ms inference budget: fib(20000) cannot finish in time, so the engine deadline trips.
        NelumboHttpServer tight = new NelumboHttpServer(
                KnowledgeBaseLoader.load(List.of(new NamedSource("fibonacci.nl", FIB_BASE))),
                List.of("fibonacci.nl"), 1);
        int tightPort = tight.start(0);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + tightPort + "/eval"))
                    .header("Content-Type", "text/plain")
                    .POST(BodyPublishers.ofString("Integer r\nfib(20000)=r ?\n")).build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            assertEquals(408, response.statusCode(), "a query exceeding the budget should time out");
            assertEquals("timeout", mapper.readTree(response.body()).get("error").asText());
        } finally {
            tight.stop();
        }
    }

    @Test
    void evalReportsFalseForARefutedQuery() throws Exception {
        HttpResponse<String> response = post("/eval", "fib(5)=6 ?\n");
        assertEquals(200, response.statusCode());

        JsonNode query = mapper.readTree(response.body()).get("queries").get(0);
        assertEquals("false", query.get("status").asText(), "fib(5) is 5, so fib(5)=6 is refuted");
        assertTrue(query.get("bindings").isEmpty(), "a refuted query has no solutions");
    }

    @Test
    void evalReportsParseErrorsWithLocation() throws Exception {
        HttpResponse<String> response = post("/eval", "fib(\n");
        assertEquals(400, response.statusCode(), "a document that does not parse is a client error");

        JsonNode errors = mapper.readTree(response.body()).get("errors");
        assertFalse(errors.isEmpty(), "expected a parse error entry");
        JsonNode error = errors.get(0);
        assertTrue(error.has("line") && error.has("column") && error.has("message"),
                "error must carry location + message: " + error);
    }

    @Test
    void metadataExposesLoadedTypesAndFiles() throws Exception {
        HttpResponse<String> response = get("/metadata");
        assertEquals(200, response.statusCode());

        JsonNode body = mapper.readTree(response.body());
        List<String> types = mapper.convertValue(body.get("types"), STRING_LIST);
        assertTrue(types.contains("Integer"), "loaded types should include Integer: " + types);

        List<String> files = mapper.convertValue(body.get("files"), STRING_LIST);
        assertTrue(files.contains("fibonacci.nl"), "metadata should list loaded files: " + files);
    }

    @Test
    void traceEndpointIsStubbedButStillEvaluates() throws Exception {
        HttpResponse<String> response = post("/eval/trace", "Integer r\nfib(5)=r ?\n");
        assertEquals(200, response.statusCode());

        JsonNode body = mapper.readTree(response.body());
        assertEquals("not-implemented", body.get("traceStatus").asText());
        assertTrue(body.get("trace").isNull(), "trace is not implemented yet");
        // The query is still evaluated even though tracing is stubbed.
        assertEquals("true", body.get("queries").get(0).get("status").asText());
    }
}
