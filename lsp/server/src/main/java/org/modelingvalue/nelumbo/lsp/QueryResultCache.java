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
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.Token;

/**
 * Holds the inline query-result inlay hints per document and recomputes them on a debounce after
 * every edit. Evaluation is expensive (and can recurse), so it runs off the request thread on a
 * single worker that serialises all documents, and is collapsed to one run per quiet period.
 * Once results are ready the client is asked to re-pull its inlay hints.
 */
public class QueryResultCache {
    private static final long DEBOUNCE_MS = 300;

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
    private final ConcurrentHashMap<String, List<InlayHint>>   hints     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pending  = new ConcurrentHashMap<>();

    public QueryResultCache(NlDocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    /** Latest computed hints for the document, or an empty list if not evaluated yet. */
    public List<InlayHint> hints(String uri) {
        return hints.getOrDefault(uri, List.of());
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
        Workspace  workspace   = documentManager.workspace();
        NlDocument document    = documentManager.getDocument(uri);
        if (document == null) {
            return;
        }
        long             deadlineMs  = workspace.getEvalDeadlineMs();
        List<Diagnostic> diagnostics = NlDocument.baseDiagnostics(document.tokenizerResult(), document.parserResult());
        try {
            Map<Query, QueryResult> results;
            if (deadlineMs > 0) {
                String  content = document.content();
                String  docUri  = uri;
                Future<Map<Query, QueryResult>> future = backstop.submit(
                        () -> QueryEvaluator.evaluate(workspace.getBaseKnowledgeBase(), deadlineMs, content, docUri));
                try {
                    results = future.get(deadlineMs + 2000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException te) {
                    future.cancel(true);
                    hints.put(uri, List.of());
                    NlDocument.publishDiagnostics(workspace, uri, diagnostics);
                    LanguageClient client = workspace.getClient();
                    if (client != null) {
                        try {
                            client.refreshInlayHints();
                        } catch (Exception ex) {
                            // client may not support inlay-hint refresh
                        }
                    }
                    return;
                } catch (java.util.concurrent.ExecutionException ee) {
                    throw ee.getCause() instanceof RuntimeException re ? re : new RuntimeException(ee.getCause());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    hints.put(uri, List.of());
                    return;
                }
            } else {
                results = QueryEvaluator.evaluate(workspace.getBaseKnowledgeBase(), 0, document.content(), uri);
            }
            List<InlayHint>         list    = new ArrayList<>();
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
                        case MISMATCH, ERROR -> hint.setKind(InlayHintKind.Parameter);
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
            }
            hints.put(uri, list);
        } catch (Exception ex) {
            System.err.println("query evaluation failed for " + uri + ": " + ex);
            hints.put(uri, List.of());
        }
        // republish parse diagnostics together with the query mismatches so neither clobbers the other.
        NlDocument.publishDiagnostics(workspace, uri, diagnostics);
        LanguageClient client    = workspace.getClient();
        if (client != null) {
            try {
                client.refreshInlayHints();
            } catch (Exception ex) {
                // client may not support inlay-hint refresh; the next pull picks up the new results anyway.
            }
        }
    }
}
