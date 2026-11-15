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


package org.modelingvalue.nelumbo.lsp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.InlayHintLabelPart;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.tools.PreparedQuery;
import org.modelingvalue.nelumbo.tools.QueryDiff;

/**
 * The query results of the open documents, one slot per query of a document's current version. An edit keeps the
 * slots of the queries above it (their result, or their queued or running task), replaces the others (cancelling
 * their tasks) and queues the new slots after a debounce. A fixed number of driver threads run the queued queries,
 * one each, the document with the fewest running queries first. Every result reaches the client at once (a throttled
 * inlay-hint refresh plus the document's diagnostics). A query without a result shows its moved old result or nothing
 * for {@link #CALCULATING_DELAY_MS} after it became invalid, then {@code ...calculating}. A query stopped by the
 * overload budget is retried on its own later, so its overload marker clears without an edit.
 */
public class QueryResultCache {
    private static final long DEBOUNCE_MS          = 100;
    private static final long CALCULATING_DELAY_MS = 300;
    private static final long REFRESH_THROTTLE_MS  = 200;
    private static final long OVERLOAD_RETRY_MS    = 5000;

    private enum State {
        DEBOUNCING,
        PENDING,
        RUNNING,
        RETRYING,
        DONE
    }

    /** A query of a document version; a kept query keeps its slot object in the next version. */
    private static final class Slot {
        private final String        uri;
        private final PreparedQuery query;
        private final QueryResult   moved;
        private final long          invalidSince;
        private       QueryResult   result;
        private       State         state = State.DEBOUNCING;

        private Slot(String uri, PreparedQuery query, QueryResult moved, long invalidSince) {
            this.uri          = uri;
            this.query        = query;
            this.moved        = moved;
            this.invalidSince = invalidSince;
        }
    }

    private record Version(NlDocument document, List<Slot> slots, long sequence) {
    }

    private record Candidate(Slot slot, int running, int index, long sequence) {
    }

    // the document with the fewest running queries, then the query highest in its document, then the document scheduled first
    private static final Comparator<Candidate> FIRST = Comparator.comparingInt(Candidate::running)//
                                                                 .thenComparingInt(Candidate::index)//
                                                                 .thenComparingLong(Candidate::sequence);

    private final NlDocumentManager        documentManager;
    // guards the versions, the results and states of their slots, and the refresh throttle
    private final Object                   lock           = new Object();
    // held while diagnostics go out, so that remove can wait for a publish under way
    private final Object                   publishing     = new Object();
    private final Map<String, Version>     versions       = new HashMap<>();
    private final ScheduledExecutorService timer          = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nelumbo-query-timer");
        t.setDaemon(true);
        return t;
    });
    private       Set<String>              dirty          = new HashSet<>();
    private       boolean                  flushScheduled;
    private       long                     lastFlush      = now() - REFRESH_THROTTLE_MS;
    private       long                     sequence;
    private       boolean                  driversStarted;
    private       boolean                  stopped;

    public QueryResultCache(NlDocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    /** The hints of the document's current version, built from its slots and the clock. */
    public List<InlayHint> hints(String uri) {
        long            now   = now();
        List<InlayHint> hints = new ArrayList<>();
        synchronized (lock) {
            Version version = versions.get(uri);
            if (version == null) {
                return hints;
            }
            for (Slot slot : version.slots()) {
                QueryResult shown = slot.result != null ? slot.result
                        : now - slot.invalidSince < CALCULATING_DELAY_MS ? slot.moved : QueryResult.calculating();
                if (shown != null) {
                    hints.add(hint(slot.query.query().lastToken(), shown, shown == slot.result));
                }
            }
        }
        return hints;
    }

    /** Takes the document's new version (called right after it is stored): keeps, moves or replaces its queries. */
    public void schedule(String uri) {
        NlDocument          document = documentManager.getDocument(uri);
        List<PreparedQuery> queries  = document.prepared().queries();
        long                now      = now();
        synchronized (lock) {
            if (stopped) {
                return;
            }
            Version               old      = versions.get(uri);
            String                oldText  = old == null ? "" : old.document().content();
            List<Slot>            oldSlots = old == null ? List.of() : old.slots();
            List<QueryDiff.Match> matches  = QueryDiff.of(oldText, oldSlots.stream().map(s -> s.query.span()).toList(), //
                                                          document.content(), queries.stream().map(PreparedQuery::span).toList());
            List<Slot>            slots    = new ArrayList<>();
            for (int i = 0; i < queries.size(); i++) {
                QueryDiff.Match match = matches.get(i);
                PreparedQuery   query = queries.get(i);
                slots.add(switch (match.kind()) {
                    case KEPT    -> oldSlots.get(match.oldIndex());
                    case MOVED   -> moved(oldSlots.get(match.oldIndex()), query, now);
                    case CHANGED -> new Slot(uri, query, null, now);
                });
            }
            for (Slot slot : oldSlots) {
                if (!slots.contains(slot)) {
                    slot.query.cancel();
                }
            }
            Version version = new Version(document, slots, ++sequence);
            versions.put(uri, version);
            startDrivers();
            timer.schedule(() -> debounced(version), DEBOUNCE_MS, TimeUnit.MILLISECONDS);
            timer.schedule(this::delayed, CALCULATING_DELAY_MS, TimeUnit.MILLISECONDS);
            request(uri);
        }
    }

    /** Drops a closed document: its queries are cancelled and nothing is published for it any more. */
    public void remove(String uri) {
        synchronized (publishing) {
            synchronized (lock) {
                Version version = versions.remove(uri);
                if (version != null) {
                    version.slots().forEach(slot -> slot.query.cancel());
                }
            }
        }
    }

    /** Cancels every query and stops the drivers and timers; used when an embedded server's connection closes. */
    public void shutdown() {
        synchronized (lock) {
            stopped = true;
            versions.values().forEach(version -> version.slots().forEach(slot -> slot.query.cancel()));
            versions.clear();
            lock.notifyAll();
        }
        timer.shutdownNow();
    }

    // the new slot of a moved query shows the old result during the delay; without one it stays invalid since the old slot was
    private static Slot moved(Slot old, PreparedQuery query, long now) {
        QueryResult result = old.result != null ? old.result : old.moved;
        return new Slot(old.uri, query, result, old.result == null ? old.invalidSince : now);
    }

    private void debounced(Version version) {
        synchronized (lock) {
            if (versions.get(version.document().uri()) == version) {
                for (Slot slot : version.slots()) {
                    if (slot.state == State.DEBOUNCING) {
                        slot.state = State.PENDING;
                    }
                }
                lock.notifyAll();
            }
        }
    }

    // a refresh when the delay is over, so that the client pulls the ...calculating placeholders
    private void delayed() {
        synchronized (lock) {
            if (!stopped) {
                request(null);
            }
        }
    }

    private void startDrivers() {
        if (driversStarted) {
            return;
        }
        driversStarted = true;
        for (int i = 1; i <= documentManager.workspace().getMaxParallelQueries(); i++) {
            Thread driver = new Thread(this::drive, "nelumbo-query-" + i);
            driver.setDaemon(true);
            driver.start();
        }
    }

    private void drive() {
        while (true) {
            Slot slot;
            synchronized (lock) {
                while ((slot = next()) == null) {
                    if (stopped) {
                        return;
                    }
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                slot.state = State.RUNNING;
            }
            try {
                run(slot);
            } catch (Throwable t) {
                failed(slot, t);
            }
        }
    }

    private Slot next() {
        Candidate best = null;
        for (Version version : versions.values()) {
            List<Slot> slots   = version.slots();
            int        running = (int) slots.stream().filter(s -> s.state == State.RUNNING).count();
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).state == State.PENDING) {
                    Candidate candidate = new Candidate(slots.get(i), running, i, version.sequence());
                    if (best == null || FIRST.compare(candidate, best) < 0) {
                        best = candidate;
                    }
                    break;
                }
            }
        }
        return best == null ? null : best.slot();
    }

    private void run(Slot slot) {
        Workspace             workspace   = documentManager.workspace();
        EvalGate              gate        = workspace.getEvalGate();
        long                  workspaceMs = workspace.getEvalDeadlineMs();
        boolean               busy        = gate.enter();
        // busy: the gate's short budget replaces the workspace deadline, and a timeout reads as an overload
        boolean               budget      = busy && (workspaceMs <= 0 || gate.budgetMs() < workspaceMs);
        long                  deadlineMs  = budget ? gate.budgetMs() : workspaceMs;
        PreparedQuery.Outcome outcome;
        try {
            outcome = slot.query.evaluate(deadlineMs);
        } finally {
            gate.exit();
        }
        synchronized (lock) {
            if (outcome.cancelled() || !current(slot)) {
                return;
            }
            slot.result = QueryEvaluator.toResult(slot.query, outcome, budget, deadlineMs);
            slot.state  = State.DONE;
            if (slot.result.kind() == QueryResult.Kind.OVERLOADED) {
                gate.recordOverload();
                slot.state = State.RETRYING;
                retryLater(slot);
            }
            request(slot.uri);
        }
    }

    // a throwable that escaped the evaluation (an Error, or one thrown outside the engine's own catch): the query shows it, the driver goes on
    private void failed(Slot slot, Throwable t) {
        Throwable innermost = t;
        while (innermost.getCause() != null) {
            innermost = innermost.getCause();
        }
        System.err.println("query evaluation failed for " + slot.uri + ": " + t);
        synchronized (lock) {
            if (current(slot)) {
                slot.result = QueryResult.error("Internal error: " + innermost, -1);
                slot.state  = State.DONE;
                request(slot.uri);
            }
        }
    }

    private void retryLater(Slot slot) {
        timer.schedule(() -> retry(slot), OVERLOAD_RETRY_MS, TimeUnit.MILLISECONDS);
    }

    // a new version that does not keep the slot, or a close, supersedes the retry
    private void retry(Slot slot) {
        synchronized (lock) {
            if (current(slot)) {
                EvalGate gate = documentManager.workspace().getEvalGate();
                if (gate.running() < gate.threshold()) {
                    slot.state = State.PENDING;
                    lock.notifyAll();
                } else {
                    retryLater(slot);
                }
            }
        }
    }

    private boolean current(Slot slot) {
        Version version = versions.get(slot.uri);
        return version != null && version.slots().contains(slot);
    }

    // a refresh, and with a uri also its diagnostics: at once, or at the end of the throttle window (called under the lock)
    private void request(String uri) {
        if (uri != null) {
            dirty.add(uri);
        }
        if (!flushScheduled) {
            flushScheduled = true;
            timer.schedule(this::flush, Math.max(0, lastFlush + REFRESH_THROTTLE_MS - now()), TimeUnit.MILLISECONDS);
        }
    }

    private void flush() {
        Workspace   workspace = documentManager.workspace();
        Set<String> uris;
        synchronized (lock) {
            uris           = dirty;
            dirty          = new HashSet<>();
            flushScheduled = false;
            lastFlush      = now();
        }
        synchronized (publishing) {
            for (String uri : uris) {
                List<Diagnostic> diagnostics = diagnostics(uri);
                if (diagnostics != null) {
                    NlDocument.publishDiagnostics(workspace, uri, diagnostics);
                }
            }
        }
        LanguageClient client = workspace.getClient();
        if (client != null) {
            try {
                client.refreshInlayHints();
            } catch (Exception ex) {
                // client may not support inlay-hint refresh; the next pull picks up the new results anyway.
            }
        }
    }

    // the parse diagnostics, the failed facts and rules, and the mismatches and overloads of the results; null when closed
    private List<Diagnostic> diagnostics(String uri) {
        synchronized (lock) {
            Version version = versions.get(uri);
            if (version == null) {
                return null;
            }
            NlDocument       document    = version.document();
            List<Diagnostic> diagnostics = NlDocument.baseDiagnostics(document.tokenizerResult(), document.parserResult());
            for (ParseException error : document.prepared().statementErrors()) {
                diagnostics.add(statementDiagnostic(error));
            }
            for (Slot slot : version.slots()) {
                QueryResult result = slot.result;
                if (result != null && result.kind() == QueryResult.Kind.MISMATCH && result.expectedRange() != null) {
                    diagnostics.add(new Diagnostic(result.expectedRange(), result.message(), DiagnosticSeverity.Error, "nelumbo"));
                }
                if (result != null && result.kind() == QueryResult.Kind.OVERLOADED) {
                    diagnostics.add(overloadDiagnostic(queryRange(slot.query.query()), result.message()));
                }
            }
            return diagnostics;
        }
    }

    private static InlayHint hint(Token last, QueryResult result, boolean own) {
        // the tooltip goes on a label part, not on the hint: LSP4IJ (IntelliJ) shows only label-part
        // tooltips, and Monaco shows both, so a tooltip on the hint as well would show twice
        Position           pos  = new Position(last.lastLine(), last.positionEnd());
        InlayHintLabelPart part = new InlayHintLabelPart(result.inlineLabel());
        if (!result.tooltip().isEmpty()) {
            part.setTooltip(result.tooltip());
        }
        InlayHint          hint = new InlayHint(pos, Either.forRight(List.of(part)));
        // the kind selects the client-side style bucket (website theme: no kind = green
        // checkmark, Type = plain result chip, Parameter = failed expectation chip)
        switch (result.kind()) {
            case RESULT, CALCULATING -> hint.setKind(InlayHintKind.Type);
            case MISMATCH, ERROR, FAILED, OVERLOADED -> hint.setKind(InlayHintKind.Parameter);
            case MATCH -> {
            }
        }
        // accepting the hint (Monaco: double-click) writes the result into the query as its expected
        // result: after a plain query, or over a wrong expected result; DocumentCodeActionService offers
        // the same edits as code actions for the other editors. Never a moved old result: it may be stale
        if (own && result.kind() == QueryResult.Kind.RESULT) {
            hint.setTextEdits(List.of(new TextEdit(new Range(pos, pos), " " + result.inferred())));
        } else if (own && result.kind() == QueryResult.Kind.MISMATCH && result.expectedRange() != null) {
            hint.setTextEdits(List.of(new TextEdit(result.expectedRange(), result.inferred())));
        }
        hint.setPaddingLeft(true);
        // the hover at the end of the query (DocumentHoverService)
        hint.setData(result.hover());
        return hint;
    }

    // underlines the failing statement on its first line (ParseException.length() only measures within one line)
    private static Diagnostic statementDiagnostic(ParseException error) {
        int      line  = Math.max(0, error.line());
        int      start = Math.max(0, error.position());
        Position from  = new Position(line, start);
        Position to    = new Position(line, start + Math.max(0, error.length()));
        return new Diagnostic(new Range(from, to), error.getShortMessage(), DiagnosticSeverity.Error, "nelumbo");
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

    private static long now() {
        return System.nanoTime() / 1_000_000;
    }
}
