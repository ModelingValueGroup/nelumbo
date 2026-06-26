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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * HTTP front-end for a Nelumbo knowledge base. The base KB is loaded once at startup; every evaluating request runs
 * against a throwaway child KB so requests never mutate shared state and are safe to serve concurrently.
 */
public final class NelumboHttpServer {

    private final KnowledgeBase baseKb;
    private final List<String>  loadedFiles;

    private Javalin app;

    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles) {
        this.baseKb = baseKb;
        this.loadedFiles = List.copyOf(loadedFiles);
    }

    /** Starts the server on {@code port} (use 0 for an ephemeral port) and returns the actually bound port. */
    public int start(int port) {
        String playground = loadResource("/public/playground.html");
        app = Javalin.create();
        app.get("/", ctx -> ctx.html(playground));
        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));
        app.post("/eval", ctx -> handleEval(ctx, false));
        app.post("/eval/trace", ctx -> handleEval(ctx, true));
        app.get("/metadata", ctx -> ctx.json(metadata()));
        app.start(port);
        return app.port();
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    private void handleEval(Context ctx, boolean trace) {
        String document = ctx.body();
        Map<String, Object> response = new LinkedHashMap<>();
        if (document == null || document.isBlank()) {
            response.put("queries", List.of());
            response.put("errors", List.of(Map.of("message", "empty request body")));
            if (trace) {
                addTraceStub(response);
            }
            ctx.status(400).json(response);
            return;
        }
        EvalResult result = evaluate(document);
        response.put("queries", result.queries);
        response.put("errors", result.errors);
        if (trace) {
            addTraceStub(response);
        }
        // A document that produced no queries but did report errors is treated as a client error.
        boolean ok = result.errors.isEmpty() || !result.queries.isEmpty();
        ctx.status(ok ? 200 : 400).json(response);
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

    private static void addTraceStub(Map<String, Object> response) {
        response.put("trace", null);
        response.put("traceStatus", "not-implemented");
    }

    private record EvalResult(List<Map<String, Object>> queries, List<Map<String, Object>> errors) {
    }

    private EvalResult evaluate(String document) {
        String src = document.endsWith("\n") ? document : document + "\n";
        List<Map<String, Object>> queries = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        // baseKb.run(...) evaluates against a throwaway child of the loaded base, so a request's own
        // declarations never leak into the shared base and concurrent requests stay isolated.
        baseKb.run(() -> {
            ParserResult parsed = new Parser(new Tokenizer(src, "<request>").tokenize()).parseNonThrowing();
            try {
                parsed.evaluate();
            } catch (ParseException e) {
                errors.add(error(e));
            }
            for (ParseException e : parsed.exceptions()) {
                errors.add(error(e));
            }
            for (Node root : parsed.roots()) {
                if (root instanceof Query query && query.inferResult() != null) {
                    queries.add(queryJson(query));
                }
            }
        });
        return new EvalResult(queries, errors);
    }

    private static Map<String, Object> queryJson(Query query) {
        InferResult result = query.inferResult();
        boolean hasFacts = !result.allFacts().isEmpty();
        boolean hasFalsehoods = !result.allFalsehoods().isEmpty();
        // "true"  = at least one solution and no counterexample,
        // "false" = a counterexample and no solution,
        // "unknown" = neither (or both) — the canonical "result" string carries the full detail.
        String status = hasFacts && !hasFalsehoods ? "true" : hasFalsehoods && !hasFacts ? "false" : "unknown";

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("query", deparse(query.predicate()));
        json.put("status", status);
        json.put("bindings", bindings(result.trueBindings()));
        json.put("counterexamples", bindings(result.falseBindings()));
        json.put("result", String.valueOf(result));
        Map<String, Object> complete = new LinkedHashMap<>();
        complete.put("facts", result.completeFacts());
        complete.put("falsehoods", result.completeFalsehoods());
        json.put("complete", complete);
        return json;
    }

    private static List<Map<String, String>> bindings(
            org.modelingvalue.collections.Set<org.modelingvalue.collections.Map<Variable, Object>> bindings) {
        List<Map<String, String>> out = new ArrayList<>();
        for (org.modelingvalue.collections.Map<Variable, Object> binding : bindings) {
            Map<String, String> json = new LinkedHashMap<>();
            for (Entry<Variable, Object> entry : binding) {
                // Literal values (e.g. NInteger) render through toString(); deparse() yields "" for them.
                json.put(entry.getKey().name(), String.valueOf(entry.getValue()));
            }
            out.add(json);
        }
        return out;
    }

    private static String deparse(Node node) {
        StringBuffer sb = new StringBuffer();
        node.deparse(sb);
        return sb.toString().trim();
    }

    private static Map<String, Object> error(ParseException e) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("line", e.line() + 1);
        json.put("column", e.position() + 1);
        json.put("message", e.getShortMessage());
        return json;
    }

    private Map<String, Object> metadata() {
        Map<String, Object> json = new LinkedHashMap<>();
        TreeSet<String> typeNames = new TreeSet<>();
        for (Type type : baseKb.types()) {
            typeNames.add(type.name());
        }
        json.put("types", new ArrayList<>(typeNames));
        json.put("functorCount", baseKb.functors().size());
        json.put("ruleCount", baseKb.rules().size());
        json.put("factCount", baseKb.facts().size());
        json.put("transformCount", baseKb.transforms().size());
        json.put("files", loadedFiles);
        return json;
    }
}
