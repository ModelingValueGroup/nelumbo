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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class NelumboServerTest {

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

    private NelumboServer server;
    private int           port;

    @BeforeEach
    void startServer() {
        KnowledgeBase base = KnowledgeBaseLoader.load(List.of(new NamedSource("fibonacci.nl", FIB_BASE)));
        server = new NelumboServer(base, List.of("fibonacci.nl"));
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

    private HttpResponse<String> postJson(int onPort, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + onPort + "/eval"))
                .header("Content-Type", "application/json").POST(BodyPublishers.ofString(json)).build();
        return client.send(request, BodyHandlers.ofString());
    }

    private String envelope(Map<String, Object> fields) throws Exception {
        return mapper.writeValueAsString(fields);
    }

    @Test
    void evalAcceptsJsonEnvelope() throws Exception {
        HttpResponse<String> response = postJson(port, envelope(Map.of("document", "Integer r\nfib(5)=r ?\n")));
        assertEquals(200, response.statusCode());
        JsonNode query = mapper.readTree(response.body()).get("queries").get(0);
        assertEquals("true", query.get("status").asText());
        assertEquals("5", query.get("bindings").get(0).get("r").asText());
    }

    @Test
    void envelopeTraceFlagEnablesTraceOnEval() throws Exception {
        HttpResponse<String> response = postJson(port,
                envelope(Map.of("document", "Integer r\nfib(5)=r ?\n", "trace", true)));
        assertEquals(200, response.statusCode());
        assertEquals("not-implemented", mapper.readTree(response.body()).get("traceStatus").asText());
    }

    @Test
    void malformedJsonEnvelopeReturns400() throws Exception {
        HttpResponse<String> response = postJson(port, "{ not valid json");
        assertEquals(400, response.statusCode());
    }

    @Test
    void jsonEnvelopeWithoutDocumentReturns400() throws Exception {
        HttpResponse<String> response = postJson(port, envelope(Map.of("trace", true)));
        assertEquals(400, response.statusCode());
    }

    @Test
    void envelopeLimitTruncatesBindings() throws Exception {
        String family = new String(getClass().getResourceAsStream(
                "/org/modelingvalue/nelumbo/examples/family.nl").readAllBytes(), StandardCharsets.UTF_8);
        NelumboServer fam = new NelumboServer(
                KnowledgeBaseLoader.load(List.of(new NamedSource("family.nl", family))), List.of("family.nl"));
        int famPort = fam.start(0);
        try {
            // Juliana has two parents; limit 1 must return a single binding and flag the truncation.
            HttpResponse<String> response = postJson(famPort,
                    envelope(Map.of("document", "Person q\npc(q, Juliana) ?\n", "limit", 1)));
            assertEquals(200, response.statusCode());
            JsonNode query = mapper.readTree(response.body()).get("queries").get(0);
            assertEquals(1, query.get("bindings").size(), "limit should cap bindings: " + query);
            assertTrue(query.get("truncated").asBoolean(), "truncation should be flagged: " + query);
        } finally {
            fam.stop();
        }
    }

    @Test
    void healthReportsOk() throws Exception {
        HttpResponse<String> response = get("/health");
        assertEquals(200, response.statusCode());
        assertEquals("ok", mapper.readTree(response.body()).get("status").asText());
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
        NelumboServer tight = new NelumboServer(
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
    void metadataExposesDeclaredVocabulary() throws Exception {
        HttpResponse<String> response = get("/metadata");
        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());

        List<String> files = mapper.convertValue(body.get("files"), STRING_LIST);
        assertTrue(files.contains("fibonacci.nl"), "metadata should list loaded files: " + files);

        // The lists hold only what the loaded file declared — not the imported integer/logic vocabulary.
        List<String> functors = mapper.convertValue(body.get("functors"), STRING_LIST);
        assertTrue(functors.contains("fib(<Integer>)"), "declared functors should be just fib: " + functors);

        List<String> rules = mapper.convertValue(body.get("rules"), STRING_LIST);
        assertTrue(rules.stream().anyMatch(r -> r.contains("fib(n)=f") && r.contains("<=>")),
                "rules should hold the readable fib rule(s): " + rules);

        // fib declares no new type (Integer is imported) and no facts, so those lists are empty here.
        assertTrue(body.get("types").isEmpty(), "fib declares no types: " + body.get("types"));
        assertTrue(body.get("facts").isEmpty(), "fib declares no facts: " + body.get("facts"));
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
