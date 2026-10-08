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

package org.modelingvalue.nelumbo.lsp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.Token;

/**
 * Holds the inline query-result inlay hints per document and recomputes them on a debounce after
 * every edit. Evaluation is expensive (and can recurse), so it runs off the request thread on a
 * single worker that serialises all documents, and is collapsed to one run per quiet period.
 * Once results are ready the client is asked to re-pull its inlay hints. A document stopped by the
 * overload budget is evaluated again later, so its overload marker clears without an edit.
 */
public class QueryResultCache {
    private static final long DEBOUNCE_MS       = 300;
    private static final long OVERLOAD_RETRY_MS = 5000;

    private final NlDocumentManager                             documentManager;
    private final ScheduledExecutorService                      scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nelumbo-query-eval");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService                               backstop  = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "nelumbo-query-eval-backstop");
        t.setDaemon(true);
        return t;
    });
    private final ConcurrentHashMap<String, Evaluated>          hints     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pending  = new ConcurrentHashMap<>();

    /** Hints with the text they were computed for: their positions are only valid on that text. */
    private record Evaluated(String content, List<InlayHint> hints) {
    }

    public QueryResultCache(NlDocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    /**
     * Latest computed hints for the document, or an empty list if not evaluated yet. After an edit, until the
     * re-evaluation finishes, only the hints on lines that are unchanged keep a valid position; the others are
     * withheld instead of being drawn at their old column in the new text.
     */
    public List<InlayHint> hints(String uri) {
        Evaluated  evaluated = hints.get(uri);
        NlDocument document  = documentManager.getDocument(uri);
        if (evaluated == null) {
            return List.of();
        }
        if (document == null || document.content().equals(evaluated.content())) {
            return evaluated.hints();
        }
        String[] before = evaluated.content().split("\n", -1);
        String[] now    = document.content().split("\n", -1);
        return evaluated.hints().stream()//
                        .filter(h -> {
                            int line = h.getPosition().getLine();
                            return line < before.length && line < now.length && before[line].equals(now[line]);
                        })//
                        .toList();
    }

    /** Schedule a (debounced) re-evaluation of the document, cancelling any pending one. */
    public void schedule(String uri) {
        ScheduledFuture<?> prev = pending.put(uri, scheduler.schedule(() -> evaluate(uri), DEBOUNCE_MS, TimeUnit.MILLISECONDS));
        if (prev != null) {
            prev.cancel(false);
        }
    }

    /** Drop everything for a closed document. */
    public void remove(String uri) {
        ScheduledFuture<?> prev = pending.remove(uri);
        if (prev != null) {
            prev.cancel(false);
        }
        hints.remove(uri);
    }

    /** Stop the debounce scheduler and backstop executor; used when an embedded server's connection closes. */
    public void shutdown() {
        scheduler.shutdownNow();
        backstop.shutdownNow();
    }

    private void evaluate(String uri) {
        ScheduledFuture<?> current   = pending.get(uri);
        Workspace          workspace = documentManager.workspace();
        NlDocument         document  = documentManager.getDocument(uri);
        if (document == null) {
            return;
        }
        EvalGate gate       = workspace.getEvalGate();
        boolean  busy       = gate.enter();
        boolean  overloaded;
        try {
            overloaded = evaluate(workspace, uri, document, gate, busy);
        } finally {
            gate.exit();
        }
        if (overloaded) {
            retryLater(uri, current);
        }
    }

    private void retry(String uri) {
        EvalGate gate = documentManager.workspace().getEvalGate();
        if (gate.running() >= gate.threshold()) {
            retryLater(uri, pending.get(uri));
        } else {
            evaluate(uri);
        }
    }

    // takes the document's pending slot only while it still holds the run that asks, so a later edit supersedes the retry
    private void retryLater(String uri, ScheduledFuture<?> current) {
        pending.computeIfPresent(uri, (key, slot) -> slot != current ? slot
                : scheduler.schedule(() -> retry(uri), OVERLOAD_RETRY_MS, TimeUnit.MILLISECONDS));
    }

    // busy: the gate's short budget replaces the workspace deadline, and a timeout reads as an overload.
    // Returns whether the evaluation was stopped by that budget.
    private boolean evaluate(Workspace workspace, String uri, NlDocument document, EvalGate gate, boolean busy) {
        long             workspaceMs    = workspace.getEvalDeadlineMs();
        boolean          overloadBudget = busy && (workspaceMs <= 0 || gate.budgetMs() < workspaceMs);
        long             deadlineMs     = overloadBudget ? gate.budgetMs() : workspaceMs;
        String           content        = document.content();
        List<Diagnostic> diagnostics    = NlDocument.baseDiagnostics(document.tokenizerResult(), document.parserResult());
        boolean          overloaded     = false;
        try {
            Map<Query, QueryResult> results = null;
            if (deadlineMs > 0) {
                Future<Map<Query, QueryResult>> future = backstop.submit(
                        () -> QueryEvaluator.evaluate(workspace.getBaseKnowledgeBase(), deadlineMs, content, uri, overloadBudget));
                try {
                    // horizon from the workspace deadline, also under the short budget: that one only starts on a pool worker
                    results = workspaceMs > 0 ? future.get(workspaceMs + 2000, TimeUnit.MILLISECONDS) : future.get();
                } catch (TimeoutException te) {
                    future.cancel(true);
                } catch (ExecutionException ee) {
                    // under the overload budget QueryEvaluator rethrows a timeout hit before any query (while parsing)
                    if (!(overloadBudget && ee.getCause() instanceof NelumboTimeoutException)) {
                        throw ee.getCause() instanceof RuntimeException re ? re : new RuntimeException(ee.getCause());
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    hints.put(uri, new Evaluated(content, List.of()));
                    return false;
                }
            } else {
                results = QueryEvaluator.evaluate(workspace.getBaseKnowledgeBase(), 0, content, uri);
            }
            if (results == null) {
                // no query was reached in time
                if (overloadBudget) {
                    diagnostics.add(overloadDiagnostic(new Range(new Position(0, 0), new Position(0, 0)), QueryResult.overloaded(deadlineMs).message()));
                    gate.recordOverload();
                    overloaded = true;
                }
                hints.put(uri, new Evaluated(content, List.of()));
            } else {
                List<InlayHint> list = new ArrayList<>();
                for (Map.Entry<Query, QueryResult> e : results.entrySet()) {
                    QueryResult result = e.getValue();
                    Token       last   = e.getKey().lastToken();
                    if (last != null) {
                        Position  pos  = new Position(last.lastLine(), last.positionEnd());
                        InlayHint hint = new InlayHint(pos, Either.forLeft(result.inlineLabel()));
                        // the kind selects the client-side style bucket (website theme: no kind = green
                        // checkmark, Type = plain result chip, Parameter = failed expectation chip)
                        switch (result.kind()) {
                            case RESULT -> hint.setKind(InlayHintKind.Type);
                            case MISMATCH, ERROR, OVERLOADED -> hint.setKind(InlayHintKind.Parameter);
                            case MATCH -> {
                            }
                        }
                        hint.setPaddingLeft(true);
                        hint.setTooltip(result.tooltip());
                        list.add(hint);
                    }
                    if (result.kind() == QueryResult.Kind.MISMATCH && result.expectedRange() != null) {
                        diagnostics.add(new Diagnostic(result.expectedRange(), result.message(), DiagnosticSeverity.Error, "nelumbo"));
                    }
                    if (result.kind() == QueryResult.Kind.OVERLOADED) {
                        diagnostics.add(overloadDiagnostic(queryRange(e.getKey()), result.message()));
                        gate.recordOverload();
                        overloaded = true;
                    }
                }
                hints.put(uri, new Evaluated(content, list));
            }
        } catch (Exception ex) {
            System.err.println("query evaluation failed for " + uri + ": " + ex);
            hints.put(uri, new Evaluated(content, List.of()));
        }
        // republish parse diagnostics together with the query mismatches so neither clobbers the other.
        NlDocument.publishDiagnostics(workspace, uri, diagnostics);
        LanguageClient client = workspace.getClient();
        if (client != null) {
            try {
                client.refreshInlayHints();
            } catch (Exception ex) {
                // client may not support inlay-hint refresh; the next pull picks up the new results anyway.
            }
        }
        return overloaded;
    }

    private static Diagnostic overloadDiagnostic(Range range, String message) {
        Diagnostic diagnostic = new Diagnostic(range, message, DiagnosticSeverity.Warning, "nelumbo");
        diagnostic.setCode(QueryResult.OVERLOAD_CODE);
        return diagnostic;
    }

    private static Range queryRange(Query query) {
        Token first = query.firstToken();
        Token last  = query.lastToken();
        if (first == null || last == null) {
            return new Range(new Position(0, 0), new Position(0, 0));
        }
        return new Range(new Position(first.line(), first.position()), new Position(last.lastLine(), last.positionEnd()));
    }
}
