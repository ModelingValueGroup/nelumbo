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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
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

    /** Default per-request inference budget, in milliseconds. */
    public static final long DEFAULT_TIMEOUT_MS = 30_000;
    /** Extra wall-clock the HTTP backstop waits beyond the engine deadline before giving up. */
    private static final long GRACE_MS = 2_000;

    private final KnowledgeBase baseKb;
    private final List<String>  loadedFiles;
    private final long          timeoutMs;

    private Javalin         app;
    private ExecutorService evalExecutor;

    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles) {
        this(baseKb, loadedFiles, DEFAULT_TIMEOUT_MS);
    }

    /** {@code timeoutMs} is the per-request inference budget; 0 (or less) disables the timeout. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs) {
        this.baseKb = baseKb;
        this.loadedFiles = List.copyOf(loadedFiles);
        this.timeoutMs = timeoutMs;
    }

    /** Starts the server on {@code port} (use 0 for an ephemeral port) and returns the actually bound port. */
    public int start(int port) {
        String playground = loadResource("/public/playground.html");
        evalExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "nelumbo-http-eval");
            thread.setDaemon(true);
            return thread;
        });
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
        if (evalExecutor != null) {
            evalExecutor.shutdownNow();
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
        EvalResult result;
        try {
            result = evaluate(document);
        } catch (EvalTimeoutException e) {
            Map<String, Object> timeout = new LinkedHashMap<>();
            timeout.put("error", "timeout");
            timeout.put("timeoutMs", timeoutMs);
            timeout.put("message", "inference exceeded " + timeoutMs + " ms");
            if (trace) {
                addTraceStub(timeout);
            }
            ctx.status(408).json(timeout);
            return;
        }
        response.put("queries", result.queries);
        response.put("errors", result.errors);
        if (trace) {
            addTraceStub(response);
        }
        // A document that produced no queries but did report errors is treated as a client error.
        boolean ok = result.errors.isEmpty() || !result.queries.isEmpty();
        ctx.status(ok ? 200 : 400).json(response);
    }

    /** Signals that a request exceeded its inference budget. */
    private static final class EvalTimeoutException extends RuntimeException {
        private static final long serialVersionUID = 1L;
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
        // A throwaway child of the loaded base: a request's own declarations never leak into the shared
        // base, concurrent requests stay isolated, and the deadline is carried into the inference.
        KnowledgeBase requestKb = new KnowledgeBase(baseKb);
        Runnable work = () -> {
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
        };
        if (timeoutMs <= 0) {
            requestKb.run(work);
        } else {
            runWithTimeout(requestKb, work);
        }
        return new EvalResult(queries, errors);
    }

    private void runWithTimeout(KnowledgeBase requestKb, Runnable work) {
        // The engine deadline makes the inference self-abort (via fixpoint -> NelumboTimeoutException); the
        // future.get backstop guarantees the HTTP handler returns even if some step never re-checks the clock.
        requestKb.setDeadlineNanos(System.nanoTime() + timeoutMs * 1_000_000L);
        Future<?> future = evalExecutor.submit(() -> requestKb.run(work));
        try {
            future.get(timeoutMs + GRACE_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new EvalTimeoutException();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NelumboTimeoutException) {
                throw new EvalTimeoutException();
            }
            throw e.getCause() instanceof RuntimeException re ? re : new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EvalTimeoutException();
        }
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
