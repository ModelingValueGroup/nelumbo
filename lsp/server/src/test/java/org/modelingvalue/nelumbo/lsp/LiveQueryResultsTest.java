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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * Query results come in one by one: an edit invalidates only the queries it changes or moves, an invalid result
 * shows "...calculating" after a delay, and a running query is cancelled only when an edit invalidates it.
 * factorial(5000) takes about 1 s, factorial(10000) about 2 s, factorial(100000) far longer than any test.
 */
public class LiveQueryResultsTest {
    static final String CALCULATING = "...calculating";

    private static final String        URI       = "inmemory://live.nl";
    private static final KnowledgeBase FACTORIAL = QueryEvaluatorSeedingTest.seeded(QueryEvaluatorSeedingTest.FACTORIAL_SEED);

    private NelumboLanguageServer server;
    private RecordingClient       client;
    private EvalGate              gate;
    private NlDocumentManager     documents;

    @AfterEach
    void dispose() {
        // also cancels the queries still running
        if (server != null) {
            server.getWorkspace().dispose();
        }
    }

    @Test
    void resultsArriveOneByOne() throws InterruptedException {
        start(FACTORIAL, 0);
        documents.addDocument(URI, factorials(5, 100000), 1);
        assertTrue(await(() -> {
            Map<Integer, String> labels = labels(URI);
            return contains(labels.get(1), "120") && CALCULATING.equals(labels.get(2));
        }, 5000), "the quick result shows while the slow query is calculating: " + labels(URI));
    }

    @Test
    void aQuickRecalculationShowsNoCalculating() throws InterruptedException {
        start(FACTORIAL, 0);
        documents.addDocument(URI, "import nelumbo.logic\ntrue ?\nfalse ?\n", 1);
        awaitResults(documents.queryResultCache(), URI, 2, 15);
        documents.updateDocument(URI, "import nelumbo.logic\ntrue & true ?\nfalse ?\n");
        long end = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < end) {
            assertFalse(labels(URI).containsValue(CALCULATING), "a recalculation within the delay shows no placeholder");
            Thread.sleep(10);
        }
        assertEquals(2, labels(URI).size(), "both results: " + labels(URI));
    }

    @Test
    void anEditBelowARunningQueryKeepsIt() throws InterruptedException {
        start(FACTORIAL, 2);
        documents.addDocument(URI, factorials(10000, 3), 1);
        assertTrue(await(() -> gate.total() == 2 && gate.running() == 1, 15000),
                "factorial(3) is done while factorial(10000) runs: total " + gate.total() + ", running " + gate.running());
        documents.updateDocument(URI, factorials(10000, 4));
        assertTrue(await(() -> contains(labels(URI).get(2), "24"), 15000), "the edited query gets its result: " + labels(URI));
        assertTrue(await(() -> isResult(labels(URI).get(1)), 60000), "the query above the edit gets its result: " + labels(URI));
        assertEquals(3, gate.total(), "factorial(10000) is evaluated once");
    }

    @Test
    void anEditAboveARunningQueryCancelsIt() throws InterruptedException {
        start(FACTORIAL, 1);
        documents.addDocument(URI, factorials(100000), 1);
        assertTrue(await(() -> gate.running() == 1, 15000), "factorial(100000) runs");
        documents.updateDocument(URI, factorials(5));
        assertTrue(await(() -> contains(labels(URI).get(1), "120"), 5000), "the cancelled query gave its one slot back: " + labels(URI));
    }

    @Test
    void atMostNQueriesRun() throws InterruptedException {
        start(FACTORIAL, 2);
        documents.addDocument(URI, factorials(100000, 100001, 100002, 100003), 1);
        int  max = 0;
        long end = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < end) {
            max = Math.max(max, gate.running());
            Thread.sleep(5);
        }
        assertEquals(2, max, "queries running at once");
    }

    @Test
    void closingADocumentStopsItsQueries() throws InterruptedException {
        start(FACTORIAL, 2);
        documents.addDocument(URI, factorials(100000, 100001), 1);
        assertTrue(await(() -> gate.running() == 2, 15000), "both queries run, running " + gate.running());
        documents.closeDocument(URI);
        long published = published(URI);
        assertTrue(await(() -> gate.running() == 0, 3000), "the queries of the closed document stop, running " + gate.running());
        // past the delay refresh and the throttle
        Thread.sleep(1000);
        assertEquals(published, published(URI), "nothing is published for a closed document");
    }

    @Test
    void aSlowDocumentDoesNotStarveAnother() throws InterruptedException {
        String   slow  = "inmemory://slow.nl";
        String   fast  = "inmemory://fast.nl";
        String[] third = new String[1];
        start(FACTORIAL, 2);
        documents.addDocument(slow, factorials(5000, 5001, 5002), 1);
        documents.addDocument(fast, factorials(5), 1);
        assertTrue(await(() -> {
            boolean done = contains(labels(fast).get(1), "120");
            third[0] = labels(slow).get(3);
            return done;
        }, 30000), "the fast document gets its result: " + labels(fast));
        assertFalse(isResult(third[0]), "the fast document got a slot before the slow one's third query: " + third[0]);
    }

    @Test
    void typingFastAboveASlowQueryEndsWithItsResult() throws InterruptedException {
        start(FACTORIAL, 1);
        String comment = "//";
        documents.addDocument(URI, comment + "\n" + factorials(5000), 1);
        for (int i = 0; i < 10; i++) {
            // past the debounce: every edit cancels a running query
            Thread.sleep(150);
            comment += "a";
            documents.updateDocument(URI, comment + "\n" + factorials(5000));
        }
        assertTrue(await(() -> isResult(labels(URI).get(2)), 10000), "the query gets its result after the last keystroke: " + labels(URI));
        assertEquals(0, gate.running(), "no cancelled query keeps a slot");
        assertTrue(gate.total() > 1, "the edits cancelled running queries, evaluations " + gate.total());
    }

    @Test
    void aMovedOldResultWritesWhatItShowsAtItsNewPlace() throws InterruptedException {
        start(FACTORIAL, 0);
        documents.addDocument(URI, factorials(5000), 1);
        InlayHint own = awaitResults(documents.queryResultCache(), URI, 1, 15).getFirst();
        assertNotNull(own.getTextEdits(), "its own result writes the expected result");
        documents.updateDocument(URI, "// c\n" + factorials(5000));
        InlayHint moved = documents.queryResultCache().hints(URI).getFirst();
        assertEquals(label(own), label(moved), "the moved old result shows during the delay");
        assertNotNull(moved.getTextEdits(), "a double-click in the delay writes what it shows");
        Range ownRange   = own.getTextEdits().getFirst().getRange();
        Range movedRange = moved.getTextEdits().getFirst().getRange();
        assertEquals(ownRange.getStart().getLine() + 1, movedRange.getStart().getLine(), "at the query's new place");
        assertEquals(ownRange.getStart().getCharacter(), movedRange.getStart().getCharacter());
    }

    @Test
    void theWorkspaceDeadlineStopsOnlyTheSlowQuery() throws InterruptedException {
        start(FACTORIAL, 1000, 0);
        documents.addDocument(URI, factorials(100000, 5), 1);
        awaitResults(documents.queryResultCache(), URI, 2, 15);
        assertEquals("\u274c evaluation took too long (over 1 s)", labels(URI).get(1), "the slow query: " + labels(URI));
        assertEquals("[(x=120)][..]", labels(URI).get(2), "the fast query: " + labels(URI));
    }

    @Test
    void equalQueriesKeepTheirOwnResultsInTheEditor() throws InterruptedException {
        start(KnowledgeBase.BASE, 0);
        documents.addDocument(URI, EQUAL_QUERIES_AROUND_A_RULE, 1);
        awaitResults(documents.queryResultCache(), URI, 2, 15);
        assertEquals("[(f=Apple),..][..]", labels(URI).get(11), "before the second rule: no falsehoods yet");
        assertEquals("[(f=Apple),..][(f=Nut),..]", labels(URI).get(13), "after the second rule: Nut is false");
    }

    @Test
    void refreshesAreThrottled() throws InterruptedException {
        start(FACTORIAL, 0);
        documents.addDocument(URI, factorials(IntStream.rangeClosed(1, 20).toArray()), 1);
        awaitResults(documents.queryResultCache(), URI, 20, 30);
        assertTrue(client.inlayHintRefreshes.get() <= 10, "refreshes for 20 results: " + client.inlayHintRefreshes.get());
    }

    @Test
    void anEscapingThrowableDoesNotStopItsDriver() throws InterruptedException {
        AtomicBoolean thrown    = new AtomicBoolean();
        Workspace     workspace = new Workspace() {
            // once, on a driver: stands in for an Error that escapes an evaluation
            @Override
            public long getEvalDeadlineMs() {
                if (Thread.currentThread().getName().startsWith("nelumbo-query-") && thrown.compareAndSet(false, true)) {
                    throw new AssertionError("escaped");
                }
                return super.getEvalDeadlineMs();
            }
        };
        workspace.setBaseKnowledgeBase(FACTORIAL);
        workspace.setEvalGate(new EvalGate(100, 2000));
        workspace.setMaxParallelQueries(1);
        documents = workspace.getDocumentManager();
        try {
            documents.addDocument(URI, factorials(5, 6), 1);
            awaitResults(documents.queryResultCache(), URI, 2, 15);
            assertTrue(contains(labels(URI).get(1), "Internal error: java.lang.AssertionError"), "the query shows the error: " + labels(URI));
            assertTrue(contains(labels(URI).get(2), "720"), "the one driver goes on with the next query: " + labels(URI));
        } finally {
            workspace.dispose();
        }
    }

    /** The document's hints once there are {@code count} of them and none is the placeholder. */
    static List<InlayHint> awaitResults(QueryResultCache cache, String uri, int count, long seconds) throws InterruptedException {
        long            end   = System.currentTimeMillis() + seconds * 1000;
        List<InlayHint> hints = cache.hints(uri);
        while (!(hints.size() == count && hints.stream().noneMatch(h -> CALCULATING.equals(label(h)))) && System.currentTimeMillis() < end) {
            Thread.sleep(20);
            hints = cache.hints(uri);
        }
        assertEquals(count, hints.size(), "hints " + hints);
        for (InlayHint hint : hints) {
            assertFalse(CALCULATING.equals(label(hint)), "every query has its result: " + hints);
        }
        return hints;
    }

    private void start(KnowledgeBase base, int maxParallelQueries) {
        start(base, 0, maxParallelQueries);
    }

    private void start(KnowledgeBase base, long deadlineMs, int maxParallelQueries) {
        server    = new NelumboLanguageServer(base, deadlineMs, () -> {
        });
        client    = new RecordingClient();
        gate      = new EvalGate(100, 2000);
        server.connect(client);
        server.getWorkspace().setEvalGate(gate);
        server.getWorkspace().setMaxParallelQueries(maxParallelQueries);
        documents = server.getWorkspace().getDocumentManager();
    }

    // the label per line, from one pull of the hints
    private Map<Integer, String> labels(String uri) {
        Map<Integer, String> labels = new HashMap<>();
        for (InlayHint hint : documents.queryResultCache().hints(uri)) {
            labels.put(hint.getPosition().getLine(), label(hint));
        }
        return labels;
    }

    private long published(String uri) {
        return client.diagnostics.stream().filter(p -> p.getUri().equals(uri)).count();
    }

    private static String label(InlayHint hint) {
        return hint.getLabel().getRight().getFirst().getValue();
    }

    private static boolean contains(String label, String text) {
        return label != null && label.contains(text);
    }

    private static boolean isResult(String label) {
        return label != null && !CALCULATING.equals(label);
    }

    private static boolean await(BooleanSupplier condition, long millis) throws InterruptedException {
        long end = System.currentTimeMillis() + millis;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > end) {
                return false;
            }
            Thread.sleep(10);
        }
        return true;
    }

    // "Integer x" and one line factorial(n)=x ? per n
    private static String factorials(int... ns) {
        StringBuilder text = new StringBuilder("Integer x\n");
        for (int n : ns) {
            text.append("factorial(").append(n).append(")=x ?\n");
        }
        return text.toString();
    }

    // copy of QueryExecutionFlowTest's: a rule between two equal queries changes the answer of the second
    private static final String EQUAL_QUERIES_AROUND_A_RULE = """
            import nelumbo.logic
            Person  :: Object
            Person  ::= Piet
            Food    :: Object
            Food    ::= Apple, Nut
            Boolean ::= likes(<Person>,<Food>), allergic(<Person>,<Food>), eats(<Person>,<Food>)
            Person  p
            Food    f
            fact likes(Piet, Apple)
            fact allergic(Piet, Nut)
            eats(p,f) <=> likes(p,f)
            eats(Piet,f) ?
            eats(p,f) <=> false if allergic(p,f)
            eats(Piet,f) ?
            """;
}
