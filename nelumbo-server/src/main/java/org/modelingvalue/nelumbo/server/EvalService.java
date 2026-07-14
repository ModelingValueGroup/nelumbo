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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.json.Json;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

/**
 * Transport-independent Nelumbo eval service: turns a posted {@code .nl} document into JSON-ready result maps, and
 * renders the loaded knowledge base's metadata. The base KB is loaded once at startup; every evaluating request runs
 * against a throwaway child KB so requests never mutate shared state and are safe to serve concurrently. HTTP fronts
 * ({@link NelumboServer} on the JDK server, the website on Javalin) only map {@link Response} onto their own stack.
 */
public final class EvalService implements AutoCloseable {

    /** Default per-request inference budget, in milliseconds. */
    public static final  long DEFAULT_TIMEOUT_MS = 30_000;
    /** Extra wall-clock the HTTP backstop waits beyond the engine deadline before giving up. */
    private static final long GRACE_MS           = 2_000;

    /** An HTTP-ready outcome: the status code and the JSON-serializable body. */
    public record Response(int status, Map<String, Object> body) {
    }

    private final KnowledgeBase   baseKb;
    private final List<String>    loadedFiles;
    private final long            timeoutMs;
    private final ExecutorService evalExecutor;

    /** {@code timeoutMs} is the per-request inference budget; 0 (or less) disables the timeout. */
    public EvalService(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs) {
        this.baseKb       = baseKb;
        this.loadedFiles  = List.copyOf(loadedFiles);
        this.timeoutMs    = timeoutMs;
        this.evalExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "nelumbo-eval");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void close() {
        evalExecutor.shutdownNow();
    }

    /** The response for a liveness check. */
    public static Map<String, Object> health() {
        return Map.of("status", "ok");
    }

    /**
     * Handles one eval request body. The request is either a raw {@code .nl} document (any non-JSON content type) or a
     * JSON envelope {@code {"document": "...", "trace": bool, "limit": int}} when the content type is JSON.
     */
    public Response eval(String body, String contentType, boolean pathTrace) {
        EvalRequest request;
        try {
            request = parseRequest(body, contentType, pathTrace);
        } catch (IllegalArgumentException e) {
            return new Response(400, Map.of("error", "bad-request", "message", "malformed JSON request body"));
        }
        boolean trace = request.trace();
        Map<String, Object> response = new LinkedHashMap<>();
        if (request.document() == null || request.document().isBlank()) {
            response.put("queries", List.of());
            response.put("errors", List.of(Map.of("message", "no document in request")));
            if (trace) {
                addTraceStub(response);
            }
            return new Response(400, response);
        }
        EvalResult result;
        try {
            result = evaluate(request.document(), request.limit());
        } catch (EvalTimeoutException e) {
            Map<String, Object> timeout = new LinkedHashMap<>();
            timeout.put("error", "timeout");
            timeout.put("timeoutMs", timeoutMs);
            timeout.put("message", "inference exceeded " + timeoutMs + " ms");
            if (trace) {
                addTraceStub(timeout);
            }
            return new Response(408, timeout);
        }
        response.put("queries", result.queries);
        response.put("errors", result.errors);
        if (trace) {
            addTraceStub(response);
        }
        // A document that produced no queries but did report errors is treated as a client error.
        boolean ok = result.errors.isEmpty() || !result.queries.isEmpty();
        return new Response(ok ? 200 : 400, response);
    }

    private EvalRequest parseRequest(String body, String contentType, boolean pathTrace) {
        if (contentType != null && contentType.toLowerCase().contains("json") && body != null && !body.isBlank()) {
            // Json.fromJson throws IllegalArgumentException on malformed JSON; integral numbers come back as Long
            Map<?, ?> node = Json.fromJson(body) instanceof Map<?, ?> m ? m : Map.of();
            String document = node.get("document") instanceof String s ? s : null;
            boolean trace = pathTrace || Boolean.TRUE.equals(node.get("trace"));
            Integer limit = node.get("limit") instanceof Long l && 0 <= l && l <= Integer.MAX_VALUE ? l.intValue()
                    : null;
            return new EvalRequest(document, trace, limit);
        }
        return new EvalRequest(body, pathTrace, null);
    }

    private record EvalRequest(String document, boolean trace, Integer limit) {
    }

    /** Signals that a request exceeded its inference budget. */
    private static final class EvalTimeoutException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static void addTraceStub(Map<String, Object> response) {
        response.put("trace", null);
        response.put("traceStatus", "not-implemented");
    }

    private record EvalResult(List<Map<String, Object>> queries, List<Map<String, Object>> errors) {
    }

    private EvalResult evaluate(String document, Integer limit) {
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
                    queries.add(queryJson(query, limit));
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

    private static Map<String, Object> queryJson(Query query, Integer limit) {
        InferResult result = query.inferResult();
        boolean hasFacts = !result.allFacts().isEmpty();
        boolean hasFalsehoods = !result.allFalsehoods().isEmpty();
        // "true"  = at least one solution and no counterexample,
        // "false" = a counterexample and no solution,
        // "unknown" = neither (or both) — the canonical "result" string carries the full detail.
        String status = hasFacts && !hasFalsehoods ? "true" : hasFalsehoods && !hasFacts ? "false" : "unknown";

        List<Map<String, String>> bindings = bindings(result.trueBindings());
        List<Map<String, String>> counterexamples = bindings(result.falseBindings());
        boolean truncated = limit != null && (bindings.size() > limit || counterexamples.size() > limit);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("query", deparse(query.predicate()));
        json.put("status", status);
        json.put("bindings", capped(bindings, limit));
        json.put("counterexamples", capped(counterexamples, limit));
        if (truncated) {
            json.put("truncated", true);
        }
        json.put("result", String.valueOf(result));
        Map<String, Object> complete = new LinkedHashMap<>();
        complete.put("facts", result.completeFacts());
        complete.put("falsehoods", result.completeFalsehoods());
        json.put("complete", complete);
        return json;
    }

    private static List<Map<String, String>> capped(List<Map<String, String>> values, Integer limit) {
        if (limit == null || values.size() <= limit) {
            return values;
        }
        return new ArrayList<>(values.subList(0, limit));
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

    public Map<String, Object> metadata() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("files", loadedFiles);
        // Only declarations that originate from the loaded files (matched by source file name): this drops
        // both the bootstrap/library vocabulary (sourced from /nelumbo/...) and synthetic compiled nodes.
        json.put("types", declaredTypes());
        // Functors render cleanly via deparse (the pattern, e.g. "fib(<Integer>)"); rules/transforms via
        // toString (deparse of the compiled clause loses the head and "if" keywords).
        addFunctors(json);
        json.put("rules", declaredSources(baseKb.rules(), n -> collapse(String.valueOf(n))));
        json.put("transforms", declaredSources(baseKb.transforms(), n -> collapse(String.valueOf(n))));
        json.put("facts", declaredFacts());
        return json;
    }

    private void addFunctors(Map<String, Object> json) {
        LinkedHashSet<String> functors = new LinkedHashSet<>();
        LinkedHashSet<String> constants = new LinkedHashSet<>();
        for (Functor functor : baseKb.functors()) {
            if (!fromLoadedFile(functor)) {
                continue;
            }
            String rendered = deparse(functor);
            if (rendered.contains("::")) {
                continue; // a type declaration registered as a functor — already shown under "types"
            }
            (functor.argTypes().isEmpty() ? constants : functors).add(rendered);
        }
        json.put("functors", sorted(functors));
        json.put("constants", sorted(constants));
    }

    private static List<String> sorted(Collection<String> values) {
        List<String> out = new ArrayList<>(values);
        Collections.sort(out);
        return out;
    }

    private List<String> declaredFacts() {
        List<Predicate> predicates = new ArrayList<>();
        for (Entry<Predicate, InferResult> fact : baseKb.facts()) {
            predicates.add(fact.getKey());
        }
        return declaredSources(predicates, EvalService::deparse);
    }

    private boolean fromLoadedFile(Node node) {
        Token first = node.firstToken();
        return first != null && loadedFiles.contains(first.fileName());
    }

    private List<String> declaredSources(Iterable<? extends Node> nodes, Function<Node, String> render) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        for (Node node : nodes) {
            if (fromLoadedFile(node)) {
                sources.add(render.apply(node));
            }
        }
        List<String> out = new ArrayList<>(sources);
        Collections.sort(out);
        return out;
    }

    private List<Map<String, Object>> declaredTypes() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Type type : baseKb.types()) {
            if (!fromLoadedFile(type)) {
                continue;
            }
            List<String> supers = new ArrayList<>();
            for (Type sup : type.supersDeclaration()) {
                supers.add(sup.name());
            }
            Collections.sort(supers);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", type.name());
            entry.put("supertypes", supers);
            out.add(entry);
        }
        out.sort(Comparator.comparing(e -> String.valueOf(e.get("name"))));
        return out;
    }

    private static String collapse(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }
}
