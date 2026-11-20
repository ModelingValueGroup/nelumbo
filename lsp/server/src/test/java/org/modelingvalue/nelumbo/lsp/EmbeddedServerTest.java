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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.InlayHintLabelPart;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;
import org.modelingvalue.collections.Collection;
import org.modelingvalue.nelumbo.KnowledgeBase;

public class EmbeddedServerTest {

    @Test
    public void embeddedConstructorSeedsWorkspace() {
        KnowledgeBase         kb     = KnowledgeBase.BASE.invoke(() -> {
        });
        NelumboLanguageServer server = new NelumboLanguageServer(kb, 1234, () -> {
        });
        assertSame(kb, server.getWorkspace().getBaseKnowledgeBase());
        assertEquals(1234, server.getWorkspace().getEvalDeadlineMs());
    }

    @Test
    public void connectStoresClientPerInstance() {
        NelumboLanguageServer serverA = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        NelumboLanguageServer serverB = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       clientA = new RecordingClient();
        RecordingClient       clientB = new RecordingClient();
        serverA.connect(clientA);
        serverB.connect(clientB);
        assertSame(clientA, serverA.getWorkspace().getClient());
        assertSame(clientB, serverB.getWorkspace().getClient());
    }

    @Test
    public void defaultConstructorUsesBase() {
        assertSame(KnowledgeBase.BASE, new NelumboLanguageServer().getWorkspace().getBaseKnowledgeBase());
        assertEquals(0, new NelumboLanguageServer().getWorkspace().getEvalDeadlineMs());
    }

    @Test
    public void diagnosticsGoToTheInstanceClient() throws InterruptedException {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        server.connect(client);
        try {
            server.getWorkspace().getDocumentManager().addDocument("inmemory://t.nl", "import nelumbo.logic\ntrue ?\n", 1);
            org.junit.jupiter.api.Assertions.assertTrue(client.awaitDiagnostics(10), "expected publishDiagnostics on the per-instance client");
            org.junit.jupiter.api.Assertions.assertTrue(client.awaitInlayHintRefresh(15), "expected refreshInlayHints after the debounced evaluation");
        } finally {
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void inlayHintsCarryTheTimeAsTooltip() throws InterruptedException {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        server.connect(client);
        try {
            server.getWorkspace().getDocumentManager().addDocument("inmemory://t.nl", "import nelumbo.logic\ntrue ?\n", 1);
            List<InlayHint> hints = LiveQueryResultsTest.awaitResults(server.getWorkspace().getDocumentManager().queryResultCache(), "inmemory://t.nl", 1, 15);
            // the tooltip on the one label part, not on the hint: LSP4IJ (IntelliJ) shows only label-part tooltips
            assertNull(hints.get(0).getTooltip(), "no hint-level tooltip, Monaco would show it twice");
            assertNotNull(part(hints.get(0)).getTooltip(), "the label part carries a tooltip");
            String tooltip = tooltip(hints.get(0));
            assertEquals("[()][]", part(hints.get(0)).getValue(), "the label is the result");
            assertTrue(tooltip.matches("[0-9,]+ \u03bcs"), "the tooltip adds only the time, the label shows the result: " + tooltip);
        } finally {
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void hintsOfAnEditedLineAreNotServedAtTheirStalePosition() throws InterruptedException {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        String                uri    = "inmemory://t.nl";
        server.connect(client);
        try {
            NlDocumentManager dm = server.getWorkspace().getDocumentManager();
            dm.addDocument(uri, "import nelumbo.logic\ntrue ?\nfalse ?\n", 1);
            LiveQueryResultsTest.awaitResults(dm.queryResultCache(), uri, 2, 15);
            // read before the debounced re-evaluation: the cached hints were placed on the previous text
            dm.updateDocument(uri, "import nelumbo.logic\ntrue & true ?\nfalse ?\n");
            List<InlayHint> hints = dm.queryResultCache().hints(uri);
            assertEquals(1, hints.size(), "the hint of the edited line is withheld, got " + hints);
            assertEquals(2, hints.get(0).getPosition().getLine(), "the hint of the unchanged line stays");
            // the re-evaluation of the new text serves both hints again
            LiveQueryResultsTest.awaitResults(dm.queryResultCache(), uri, 2, 15);
        } finally {
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void documentCacheIsCappedPerSession() {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        server.connect(new RecordingClient());
        try {
            NlDocumentManager dm = server.getWorkspace().getDocumentManager();
            for (int i = 0; i < NlDocumentManager.MAX_DOCUMENTS; i++) {
                dm.addDocument("inmemory://field-" + i + ".nl", "import nelumbo.logic\ntrue ?\n", 1);
            }
            assertNotNull(dm.getDocument("inmemory://field-0.nl"), "documents up to the cap are accepted");
            dm.addDocument("inmemory://overflow.nl", "import nelumbo.logic\ntrue ?\n", 1);
            assertNull(dm.getDocument("inmemory://overflow.nl"), "a new document past the cap is refused");
            assertEquals(NlDocumentManager.MAX_DOCUMENTS, dm.uris().size(), "the cap bounds the cache size");
        } finally {
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void heavyEvaluationWhileBusyPublishesAnOverloadDiagnostic() throws InterruptedException {
        KnowledgeBase         kb     = QueryEvaluatorSeedingTest.seeded(QueryEvaluatorSeedingTest.FACTORIAL_SEED);
        NelumboLanguageServer server = new NelumboLanguageServer(kb, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        server.connect(client);
        // threshold 0: always busy; workspace deadline 0 (unlimited, like the stdio IDE servers) must still get the budget
        server.getWorkspace().setEvalGate(new EvalGate(0, 300));
        try {
            server.getWorkspace().getDocumentManager().addDocument("inmemory://heavy.nl", QueryEvaluatorSeedingTest.HEAVY_QUERY, 1);
            assertTrue(awaitOverloadDiagnostic(client, "inmemory://heavy.nl", 20), "expected a server-overloaded diagnostic, got " + client.diagnostics);
            List<InlayHint> hints = server.getWorkspace().getDocumentManager().queryResultCache().hints("inmemory://heavy.nl");
            assertEquals(1, hints.size());
            assertEquals("\u26a0\ufe0f server busy", part(hints.get(0)).getValue());
        } finally {
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void gateIsReleasedAfterAnOverload() throws InterruptedException {
        KnowledgeBase         kb     = QueryEvaluatorSeedingTest.seeded(QueryEvaluatorSeedingTest.FACTORIAL_SEED);
        NelumboLanguageServer server = new NelumboLanguageServer(kb, 30_000, () -> {
        });
        RecordingClient       client = new RecordingClient();
        EvalGate              gate   = new EvalGate(0, 300);
        server.connect(client);
        server.getWorkspace().setEvalGate(gate);
        try {
            server.getWorkspace().getDocumentManager().addDocument("inmemory://heavy.nl", QueryEvaluatorSeedingTest.HEAVY_QUERY, 1);
            assertTrue(awaitOverloadDiagnostic(client, "inmemory://heavy.nl", 20), "expected a server-overloaded diagnostic");
            // the diagnostic is published inside the gated section, so the release follows a moment later
            long end = System.currentTimeMillis() + 5000;
            while (gate.running() != 0 && System.currentTimeMillis() < end) {
                Thread.sleep(50);
            }
            assertEquals(0, gate.running(), "the gate is released after the overloaded evaluation");
            assertEquals(1, gate.overloaded(), "the overload is counted once");
            assertEquals(0, gate.timedOut(), "an overload is no timeout");
        } finally {
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void lightEvaluationWhileBusyIsNotStopped() throws InterruptedException {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 30_000, () -> {
        });
        RecordingClient       client = new RecordingClient();
        server.connect(client);
        server.getWorkspace().setEvalGate(new EvalGate(0, 2000));
        try {
            server.getWorkspace().getDocumentManager().addDocument("inmemory://t.nl", "import nelumbo.logic\ntrue ?\n", 1);
            List<InlayHint> hints = LiveQueryResultsTest.awaitResults(server.getWorkspace().getDocumentManager().queryResultCache(), "inmemory://t.nl", 1, 15);
            assertEquals("[()][]", part(hints.get(0)).getValue(), "a light query completes within the short budget");
        } finally {
            server.getWorkspace().dispose();
        }
    }


    @Test
    public void lightEvaluationQueuedBehindAFullPoolIsNotStopped() throws InterruptedException {
        KnowledgeBase         kb       = QueryEvaluatorSeedingTest.seeded(QueryEvaluatorSeedingTest.FACTORIAL_SEED);
        int                   blockers = Collection.PARALLELISM;
        // the blockers stand in for heavy evaluations: each one counts on the gate and holds an inference pool worker
        EvalGate              gate     = new EvalGate(blockers, 300);
        CountDownLatch        started  = new CountDownLatch(blockers);
        AtomicBoolean         released = new AtomicBoolean();
        RecordingClient       client   = new RecordingClient();
        NelumboLanguageServer server   = new NelumboLanguageServer(kb, 30_000, () -> {
        });
        server.connect(client);
        server.getWorkspace().setEvalGate(gate);
        try {
            for (int i = 0; i < blockers; i++) {
                gate.enter();
            }
            server.getWorkspace().getDocumentManager().addDocument("inmemory://light.nl", "Integer x\nfactorial(5)=x ?\n", 1);
            for (int i = 0; i < blockers; i++) {
                Thread blocker = new Thread(() -> KnowledgeBase.BASE.invoke(() -> {
                    started.countDown();
                    sleepUntil(released);
                }));
                blocker.setDaemon(true);
                blocker.start();
            }
            assertTrue(started.await(10, TimeUnit.SECONDS), "every pool worker is held");
            assertEquals(blockers, gate.running(), "the pool is full before the light evaluation starts");
            assertTrue(awaitRunning(gate, blockers + 1, 5), "the light evaluation starts while busy, running=" + gate.running());
            // longer than the budget, shorter than the workspace deadline
            Thread.sleep(3000);
            released.set(true);
            List<InlayHint> hints = LiveQueryResultsTest.awaitResults(server.getWorkspace().getDocumentManager().queryResultCache(), "inmemory://light.nl", 1, 15);
            assertEquals(InlayHintKind.Type, hints.get(0).getKind(), "a normal result, got " + part(hints.get(0)).getValue());
            assertTrue(part(hints.get(0)).getValue().contains("120"), part(hints.get(0)).getValue());
        } finally {
            released.set(true);
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void requestsDoNotWaitForAFullPool() throws InterruptedException {
        int                   blockers = Collection.PARALLELISM;
        CountDownLatch        started  = new CountDownLatch(blockers);
        AtomicBoolean         released = new AtomicBoolean();
        NelumboLanguageServer server   = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        server.connect(new RecordingClient());
        try {
            for (int i = 0; i < blockers; i++) {
                Thread blocker = new Thread(() -> KnowledgeBase.BASE.invoke(() -> {
                    started.countDown();
                    sleepUntil(released);
                }));
                blocker.setDaemon(true);
                blocker.start();
            }
            assertTrue(started.await(10, TimeUnit.SECONDS), "every pool worker is held");
            String uri = "inmemory://full.nl";
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                // an import no other test loads: its knowledge base is built while the pool is full
                server.getWorkspace().getDocumentManager().addDocument(uri, "import org.modelingvalue.nelumbo.lsp.coldImport\ntrue ?\n", 1);
                assertTrue(server.getWorkspace().getDocumentManager().getDocument(uri).parserResult().exceptions().isEmpty(), "the import resolves");
                SemanticTokens tokens = server.getTextDocumentService().semanticTokensFull(new SemanticTokensParams(new TextDocumentIdentifier(uri))).get();
                assertFalse(tokens.getData().isEmpty());
            });
        } finally {
            released.set(true);
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void overloadedDocumentIsRetriedOnceTheServerIsNoLongerBusy() throws InterruptedException {
        KnowledgeBase         kb     = QueryEvaluatorSeedingTest.seeded(QueryEvaluatorSeedingTest.FACTORIAL_SEED);
        NelumboLanguageServer server = new NelumboLanguageServer(kb, 30_000, () -> {
        });
        RecordingClient       client = new RecordingClient();
        String                uri    = "inmemory://heavy.nl";
        EvalGate              busy   = new EvalGate(0, 300);
        server.connect(client);
        server.getWorkspace().setEvalGate(busy);
        try {
            server.getWorkspace().getDocumentManager().addDocument(uri, QueryEvaluatorSeedingTest.HEAVY_QUERY, 1);
            assertTrue(awaitOverloadDiagnostic(client, uri, 20), "expected a server-overloaded diagnostic");
            // past the first retry: while the server stays busy the retry does not evaluate
            Thread.sleep(6000);
            assertEquals(1, busy.total(), "no evaluation while busy");
            server.getWorkspace().setEvalGate(new EvalGate(4, 300));
            long end = System.currentTimeMillis() + 12_000;
            while (!hasResult(server, uri) && System.currentTimeMillis() < end) {
                Thread.sleep(100);
            }
            assertTrue(hasResult(server, uri), "the overloaded document gets a normal result without an edit, hints "
                    + server.getWorkspace().getDocumentManager().queryResultCache().hints(uri));
            PublishDiagnosticsParams last = client.diagnostics.get(client.diagnostics.size() - 1);
            assertTrue(last.getDiagnostics().stream().noneMatch(d -> d.getCode() != null && QueryResult.OVERLOAD_CODE.equals(d.getCode().getLeft())),
                    "the overload diagnostic is cleared: " + last);
        } finally {
            server.getWorkspace().dispose();
        }
    }

    // the one part of a hint's label
    private static InlayHintLabelPart part(InlayHint hint) {
        List<InlayHintLabelPart> parts = hint.getLabel().getRight();
        assertEquals(1, parts.size(), "one label part");
        return parts.getFirst();
    }

    private static String tooltip(InlayHint hint) {
        return part(hint).getTooltip().getLeft();
    }

    private static boolean hasResult(NelumboLanguageServer server, String uri) {
        List<InlayHint> hints = server.getWorkspace().getDocumentManager().queryResultCache().hints(uri);
        return hints.size() == 1 && hints.get(0).getKind() == InlayHintKind.Type;
    }

    // a sleep loop, not a latch: a parked pool worker could make the pool add a compensating worker.
    // Bounded, so a failing test never leaves the shared pool blocked.
    private static void sleepUntil(AtomicBoolean released) {
        long end = System.currentTimeMillis() + 20_000;
        while (!released.get() && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static boolean awaitRunning(EvalGate gate, int running, long seconds) throws InterruptedException {
        long end = System.currentTimeMillis() + seconds * 1000;
        while (gate.running() != running && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        return gate.running() == running;
    }

    private static final String CONSISTENT_WINNER = """
            import nelumbo.logic
            Person :: Object
            YesNo  :: Object
            YesNo  ::= Yes, No
            YesNo  ::= winner(<Person>)
            Person ::= Piet
            fact winner(Piet)=Yes
            winner(Piet) ?
            """;

    // The error of an inconsistent fact must be published, and the queries below it are still evaluated: the error once
    // only went to stderr, and the editor showed no diagnostic and no hint at all.
    @Test
    public void anInconsistentFactIsPublishedAsADiagnostic() throws InterruptedException {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        String                uri    = "inmemory://winner.nl";
        EvalGate              gate   = new EvalGate(100, 2000);
        server.connect(client);
        server.getWorkspace().setEvalGate(gate);
        try {
            NlDocumentManager dm = server.getWorkspace().getDocumentManager();
            dm.addDocument(uri, CONSISTENT_WINNER.replace("fact winner(Piet)=Yes\n", "fact winner(Piet)=Yes\nfact winner(Piet)=No\n"), 1);
            Diagnostic inconsistency = awaitDiagnostic(client, uri, d -> d.getMessage().getLeft().startsWith("Inconsistent result"), 15);
            assertNotNull(inconsistency, "expected the inconsistency as a diagnostic, got " + client.diagnostics);
            assertEquals(7, inconsistency.getRange().getStart().getLine(), "on the second fact");
            assertEquals(5, inconsistency.getRange().getStart().getCharacter(), "at its predicate");
            assertEquals(5 + "winner(Piet)=No".length(), inconsistency.getRange().getEnd().getCharacter(), "underlining the predicate");
            // the query below the inconsistent fact is still evaluated
            LiveQueryResultsTest.awaitResults(dm.queryResultCache(), uri, 1, 15);
            long evaluated = gate.total();

            dm.updateDocument(uri, CONSISTENT_WINNER);
            // the moved query shows its old result at once: wait for its new evaluation
            long end = System.currentTimeMillis() + 15_000;
            while (!(gate.total() > evaluated && gate.running() == 0) && System.currentTimeMillis() < end) {
                Thread.sleep(50);
            }
            assertTrue(gate.total() > evaluated && gate.running() == 0, "the query is evaluated again once the fact is removed");
            // past the refresh throttle: the publish that follows the new result is in
            Thread.sleep(500);
            LiveQueryResultsTest.awaitResults(dm.queryResultCache(), uri, 1, 1);
            PublishDiagnosticsParams last = client.diagnostics.get(client.diagnostics.size() - 1);
            assertTrue(last.getDiagnostics().isEmpty(), "the inconsistency diagnostic is cleared, got " + last.getDiagnostics());
        } finally {
            server.getWorkspace().dispose();
        }
    }

    private static Diagnostic awaitDiagnostic(RecordingClient client, String uri, java.util.function.Predicate<Diagnostic> test, long seconds) throws InterruptedException {
        long end = System.currentTimeMillis() + seconds * 1000;
        while (System.currentTimeMillis() < end) {
            Diagnostic found = client.diagnostics.stream()
                    .filter(p -> p.getUri().equals(uri))
                    .flatMap(p -> p.getDiagnostics().stream())
                    .filter(test)
                    .findFirst().orElse(null);
            if (found != null) {
                return found;
            }
            Thread.sleep(100);
        }
        return null;
    }

    private static boolean awaitOverloadDiagnostic(RecordingClient client, String uri, long seconds) throws InterruptedException {
        long end = System.currentTimeMillis() + seconds * 1000;
        while (System.currentTimeMillis() < end) {
            boolean found = client.diagnostics.stream()
                    .filter(p -> p.getUri().equals(uri))
                    .flatMap(p -> p.getDiagnostics().stream())
                    .map(Diagnostic::getCode)
                    .anyMatch(code -> code != null && QueryResult.OVERLOAD_CODE.equals(code.getLeft()));
            if (found) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }
}
