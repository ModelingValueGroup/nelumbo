# LSP Overload Signal and Server Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Heavy LSP evaluations that start while the server is busy are stopped after a short budget with a recognisable `server-overloaded` diagnostic (website: banner), and the site shows the server's load via a status dot and a `/status.html` page.

**Architecture:**
- A JVM-wide `EvalGate` counts running evaluations.
- `QueryResultCache` asks it at the start of each evaluation whether the server is busy. If so, it uses the gate's short budget as the engine deadline, and a timeout becomes an `OVERLOADED` query result plus a warning diagnostic with code `server-overloaded`.
- The website reads the gate, the LSP session count and JVM metrics for `GET /stats`. A tiny script colours a dot on every page; `/status.html` shows the numbers.

**Tech Stack:** Java 21, lsp4j 1.0.0, Javalin 7 (website), Monaco 0.34 + monaco-languageclient 1.0.1 (frontend TS), Playwright (e2e), JUnit 5, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-06-lsp-overload-and-status-design.md`

## Global Constraints

- Diagnostic code (the client contract): `server-overloaded`, as the constant `QueryResult.OVERLOAD_CODE`.
- System properties:
  - `NELUMBO_OVERLOAD_THRESHOLD`, default `Runtime.getRuntime().availableProcessors()`
  - `NELUMBO_OVERLOAD_BUDGET_MS`, default `2000`
- Busy: the number of evaluations running when one starts is `>= threshold`.
- Overload message: `Server busy: evaluation stopped after <budget> ms to protect other users - try again in a moment`.
- Inlay label `⚠ server busy`. Tooltip `⚠ <message>`.
- `/stats` status: `overloaded` if `running >= threshold`; else `busy` if `cpuLoad >= 0.7` or `running >= threshold / 2`; else `ok`.
- Dot polling: 15 s. Status page polling: 5 s.
- Java style:
  - Every new `.java` file starts with the LGPL header block (copy lines 1-15 of `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResult.java` verbatim).
  - Variable declarations aligned in columns.
  - Arrow-form switches only.
  - Comments only where logic is unclear.
- TypeScript style: type every variable and argument; one variable per declaration; `if`/`for` always multi-line with braces; aligned declarations.
- ASCII only in new text. The one exception is the `⚠` glyph, which the existing ERROR hint already uses.
- No new third-party dependencies.
- Every new `.js` file under `website/src/main/resources/public/` starts with the LGPL header of `public/theme.js` (its lines 1-15).
- The site has a light/dark theme: `public/theme.js` (loaded blocking in `<head>`) sets `data-theme` on `<html>`, and page CSS keys off `[data-theme="light"]`. New pages load `/theme.js` and define light variables.
- Never `git push`; commit on the current local branch `local/lsp-overload` only.
- After every task, update `CLAUDE.md` where the task changes documented behaviour (Task 6 does the final pass).

## Review Focus

- A light query while the server is busy must still finish normally (no overload marker). Pinned in Task 2 (`lightEvaluationWhileBusyIsNotStopped`).
- The gate counter must return to 0 after an overloaded, timed-out or failing evaluation; a leak would make the server look busy forever. Pinned in Task 2 (`gateIsReleasedAfterAnOverload`).
- An unlimited workspace deadline (0, the stdio IDE servers) plus busy must still apply the short budget. Pinned in Task 2 (the overload integration test uses deadline 0).
- The website banner must disappear once the overload is over, or users think the site is broken. Pinned in Task 3 (e2e: light document after the heavy one hides the banner).
- `/stats` must answer when the JVM cannot report CPU load (`getProcessCpuLoad()` returns -1): clamp to 0. Pinned in Task 4 (`ServerStatsTest.unknownCpuLoadCountsAsIdle`).

---

### Task 1: OVERLOADED query result

**Files:**
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResult.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryEvaluator.java:57-107`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/workspaceService/WorkspaceExecuteCommandService.java:88-93`
- Test: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryResultTest.java`
- Test: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryEvaluatorSeedingTest.java`

**Interfaces:**
- Produces:
  - `QueryResult.OVERLOAD_CODE` (String)
  - `QueryResult.Kind.OVERLOADED`
  - `static QueryResult QueryResult.overloaded(long budgetMs)`
  - `static Map<Query, QueryResult> QueryEvaluator.evaluate(KnowledgeBase base, long deadlineMs, String content, String uri, boolean overloadBudget)`. Under `overloadBudget` a per-query timeout yields `overloaded(deadlineMs)`; a timeout before any query (while parsing) is rethrown as `NelumboTimeoutException`.
  - The test helper `QueryEvaluatorSeedingTest.seeded(String)` becomes package-private (Task 2 reuses it).

- [ ] **Step 1: Write the failing tests**

Append to `QueryResultTest.java` (inside the class):

```java
    @Test
    public void overloadedNamesTheBudgetAndCarriesTheCode() {
        QueryResult r = QueryResult.overloaded(2000);
        assertEquals(QueryResult.Kind.OVERLOADED, r.kind());
        assertEquals("⚠ server busy", r.inlineLabel());
        assertEquals("⚠ Server busy: evaluation stopped after 2000 ms to protect other users - try again in a moment", r.tooltip());
        assertEquals("Server busy: evaluation stopped after 2000 ms to protect other users - try again in a moment", r.message());
        assertEquals("server-overloaded", QueryResult.OVERLOAD_CODE);
    }
```

In `QueryEvaluatorSeedingTest.java`:
- Change `private static KnowledgeBase seeded(String source)` to `static KnowledgeBase seeded(String source)`.
- Add, inside the class:

```java
    static final String FACTORIAL_SEED = """
            import nelumbo.integers
            Integer ::= factorial(<Integer>)
            Integer n, r
            factorial(n)=r <=> r=1 if n<=0, r=n*factorial(n-1) if n>0
            """;

    static final String HEAVY_QUERY = "Integer x\nfactorial(5000)=x ?\n";

    @Test
    public void timeoutUnderTheOverloadBudgetIsAnOverload() {
        KnowledgeBase           kb      = seeded(FACTORIAL_SEED);
        Map<Query, QueryResult> results = QueryEvaluator.evaluate(kb, 300, HEAVY_QUERY, "inmemory://heavy.nl", true);
        assertEquals(1, results.size(), "the heavy query gets a result: " + kb.get(() -> results.toString()));
        QueryResult result = results.values().iterator().next();
        assertEquals(QueryResult.Kind.OVERLOADED, result.kind());
        assertTrue(result.message().contains("300 ms"), result.message());
    }

    @Test
    public void timeoutUnderTheNormalDeadlineStaysAnError() {
        KnowledgeBase           kb      = seeded(FACTORIAL_SEED);
        Map<Query, QueryResult> results = QueryEvaluator.evaluate(kb, 300, HEAVY_QUERY, "inmemory://heavy.nl", false);
        assertEquals(1, results.size(), "the heavy query gets a result: " + kb.get(() -> results.toString()));
        assertEquals(QueryResult.Kind.ERROR, results.values().iterator().next().kind());
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.QueryResultTest" --tests "org.modelingvalue.nelumbo.lsp.QueryEvaluatorSeedingTest"`
Expected: compilation FAILS (`OVERLOADED`, `overloaded`, `OVERLOAD_CODE` and the 5-arg `evaluate` do not exist).

- [ ] **Step 3: Implement QueryResult**

In `QueryResult.java`:
- In the class javadoc list, add after the ERROR item:
  `*   <li>{@code OVERLOADED} - stopped by the short budget of a busy server ({@code EvalGate}); {@code inferred} holds the message.</li>`
- Add `OVERLOADED` to the enum after `ERROR,`.
- Add these members after the `error(...)` factory:

```java
    /** Diagnostic code of an evaluation stopped because the server was busy; the contract for clients. */
    public static final String OVERLOAD_CODE = "server-overloaded";

    public static QueryResult overloaded(long budgetMs) {
        String message = "Server busy: evaluation stopped after " + budgetMs + " ms to protect other users - try again in a moment";
        return new QueryResult(Kind.OVERLOADED, message, message, null);
    }
```

- Change the two switches:

```java
    public String inlineLabel() {
        return switch (kind) {
            case RESULT -> cap(inferred);
            case MATCH -> "✅";
            case MISMATCH -> cap("❌ " + inferred);
            case ERROR -> cap("⚠ " + inferred);
            case OVERLOADED -> "⚠ server busy";
        };
    }

    /** Full result for the inlay-hint tooltip; never capped. */
    public String tooltip() {
        return switch (kind) {
            case RESULT, MISMATCH, MATCH -> inferred;
            case ERROR, OVERLOADED -> "⚠ " + inferred;
        };
    }
```

- [ ] **Step 4: Implement QueryEvaluator**

Replace the 4-arg `evaluate` method (javadoc included) with these two methods. The body is the old one with three marked changes: the signature, the per-query timeout result, and the outer catch.

```java
    /**
     * Same, but declarations are resolved against {@code base} (a loaded KB for
     * embedded servers) and, when {@code deadlineMs > 0}, inference self-aborts
     * past the deadline. On timeout, queries already evaluated keep their results;
     * the first unreached query gets an ERROR result; remaining queries are absent
     * from the map.
     */
    public static Map<Query, QueryResult> evaluate(KnowledgeBase base, long deadlineMs, String content, String uri) {
        return evaluate(base, deadlineMs, content, uri, false);
    }

    /**
     * Same, where {@code overloadBudget} means {@code deadlineMs} is the short budget of a busy server: the first
     * unreached query gets an OVERLOADED result instead of an ERROR, and a timeout before any query was reached
     * (while parsing) is rethrown instead of returning empty results, so the caller can still report the overload.
     */
    public static Map<Query, QueryResult> evaluate(KnowledgeBase base, long deadlineMs, String content, String uri, boolean overloadBudget) {
        Map<Query, QueryResult> results = new LinkedHashMap<>();
        KnowledgeBase evalKb = new KnowledgeBase(base);
        if (deadlineMs > 0) {
            evalKb.setDeadlineNanos(System.nanoTime() + deadlineMs * 1_000_000L);
        }
        try {
            evalKb.run(() -> {
                KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
                ParserResult  parsed        = new Parser(new Tokenizer(content, uri).tokenize()).parseNonThrowing();
                ParserResult  throwing      = new ParserResult(null, true);
                for (Node root : parsed.roots()) {
                    if (!(root instanceof Evaluatable eval)) {
                        continue;
                    }
                    try {
                        eval.evaluate(knowledgeBase, throwing);
                        if (eval instanceof Query query) {
                            InferResult ir = query.inferResult();
                            if (ir == null) {
                                results.put(query, QueryResult.error("Infer resulted in nothing"));
                            } else if (query.hasExpected()) {
                                results.put(query, QueryResult.match(ir.toString()));
                            } else {
                                results.put(query, QueryResult.result(ir.toString()));
                            }
                        }
                    } catch (NelumboTimeoutException tex) {
                        if (eval instanceof Query query) {
                            results.put(query, overloadBudget ? QueryResult.overloaded(deadlineMs) : QueryResult.error("evaluation exceeded the deadline"));
                        }
                        break;
                    } catch (ParseException exc) {
                        if (eval instanceof Query query) {
                            results.put(query, toResult(query, exc));
                        } else {
                            // a fact/rule failed to evaluate: later queries can't be trusted, stop here.
                            System.err.println("query evaluation aborted at " + eval.getClass().getSimpleName() + ": " + exc.getMessage());
                            break;
                        }
                    }
                }
            });
        } catch (NelumboTimeoutException tex) {
            if (overloadBudget && results.isEmpty()) {
                throw tex;
            }
            // partial results already in the map; return them as-is
        }
        return results;
    }
```

Before replacing, check that the old body (lines 64-107) matches the above apart from the three changes. If it differs, keep the current body and apply only those three changes.

- [ ] **Step 5: Handle the new kind in the code-lens popup**

In `WorkspaceExecuteCommandService.java`, add a case to the switch at line 88:

```java
            case OVERLOADED -> new MessageParams(MessageType.Warning, result.message());
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.QueryResultTest" --tests "org.modelingvalue.nelumbo.lsp.QueryEvaluatorSeedingTest"`
Expected: PASS (all tests in both classes, including the existing ones).

- [ ] **Step 7: Commit**

```bash
git add lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResult.java lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryEvaluator.java lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/workspaceService/WorkspaceExecuteCommandService.java lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryResultTest.java lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryEvaluatorSeedingTest.java
git commit -m "lsp: OVERLOADED query result for a timeout under the overload budget

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: EvalGate and the short budget in QueryResultCache

**Files:**
- Create: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/EvalGate.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/Workspace.java:52,93-99`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResultCache.java:17-40,96-174`
- Test: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EvalGateTest.java` (create)
- Test: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EmbeddedServerTest.java`

**Interfaces:**
- Consumes: everything Task 1 produces.
- Produces:
  - `EvalGate`:
    - `public static final EvalGate GLOBAL`
    - `public EvalGate(int threshold, long budgetMs)`
    - `public boolean enter()` (true = busy)
    - `public void exit()`
    - `public void recordOverload()`
    - getters `int threshold()`, `long budgetMs()`, `int running()`, `long total()`, `long overloaded()`
  - `Workspace.getEvalGate()` / `Workspace.setEvalGate(EvalGate)`

- [ ] **Step 1: Write the failing EvalGate test**

Create `EvalGateTest.java` (LGPL header first, see Global Constraints):

```java
package org.modelingvalue.nelumbo.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EvalGateTest {

    @Test
    public void busyOnceThresholdEvaluationsRun() {
        EvalGate gate = new EvalGate(2, 2000);
        assertFalse(gate.enter(), "first of two runs freely");
        assertFalse(gate.enter(), "second of two runs freely");
        assertTrue(gate.enter(), "a third one starts while two run: busy");
        assertEquals(3, gate.running());
        gate.exit();
        gate.exit();
        gate.exit();
        assertEquals(0, gate.running());
        assertEquals(3, gate.total());
    }

    @Test
    public void thresholdZeroIsAlwaysBusy() {
        assertTrue(new EvalGate(0, 2000).enter());
    }

    @Test
    public void overloadsAreCounted() {
        EvalGate gate = new EvalGate(4, 2000);
        gate.recordOverload();
        gate.recordOverload();
        assertEquals(2, gate.overloaded());
        assertEquals(2000, gate.budgetMs());
        assertEquals(4, gate.threshold());
    }
}
```

- [ ] **Step 2: Write the failing integration tests**

Append to `EmbeddedServerTest.java` (inside the class; add `import static org.junit.jupiter.api.Assertions.assertTrue;` and `import org.eclipse.lsp4j.Diagnostic;` to the imports):

```java
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
            assertEquals("⚠ server busy", hints.get(0).getLabel().getLeft());
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
            assertTrue(client.awaitInlayHintRefresh(15), "expected refreshInlayHints after the debounced evaluation");
            List<InlayHint> hints = server.getWorkspace().getDocumentManager().queryResultCache().hints("inmemory://t.nl");
            assertEquals(1, hints.size(), "one hint for the single query");
            assertEquals("[()][]", hints.get(0).getTooltip().getLeft(), "a light query completes within the short budget");
        } finally {
            server.getWorkspace().dispose();
        }
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
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.EvalGateTest" --tests "org.modelingvalue.nelumbo.lsp.EmbeddedServerTest"`
Expected: compilation FAILS (`EvalGate`, `setEvalGate` do not exist).

- [ ] **Step 4: Create EvalGate**

Create `EvalGate.java` (LGPL header first):

```java
package org.modelingvalue.nelumbo.lsp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts the LSP evaluations running in this JVM (all sessions share {@link #GLOBAL}). An evaluation that starts
 * while {@code threshold} others already run is "busy": it gets the short {@code budgetMs} instead of the
 * workspace deadline, so heavy queries are stopped with an overload result while light ones still finish.
 */
public final class EvalGate {
    public static final EvalGate GLOBAL = new EvalGate(
            Integer.getInteger("NELUMBO_OVERLOAD_THRESHOLD", Runtime.getRuntime().availableProcessors()),
            Long.getLong("NELUMBO_OVERLOAD_BUDGET_MS", 2000L));

    private final int           threshold;
    private final long          budgetMs;
    private final AtomicInteger running    = new AtomicInteger();
    private final AtomicLong    total      = new AtomicLong();
    private final AtomicLong    overloaded = new AtomicLong();

    public EvalGate(int threshold, long budgetMs) {
        this.threshold = threshold;
        this.budgetMs  = budgetMs;
    }

    /** Registers a starting evaluation; returns whether the server was already busy. Pair with {@link #exit()}. */
    public boolean enter() {
        total.incrementAndGet();
        return running.getAndIncrement() >= threshold;
    }

    public void exit() {
        running.decrementAndGet();
    }

    public void recordOverload() {
        overloaded.incrementAndGet();
    }

    public int threshold() {
        return threshold;
    }

    public long budgetMs() {
        return budgetMs;
    }

    public int running() {
        return running.get();
    }

    public long total() {
        return total.get();
    }

    public long overloaded() {
        return overloaded.get();
    }
}
```

- [ ] **Step 5: Add the gate to Workspace**

In `Workspace.java`, after the line `private          long              evalDeadlineMs;` add:

```java
    private          EvalGate          evalGate          = EvalGate.GLOBAL;
```

After `setEvalDeadlineMs(...)` add:

```java
    public EvalGate getEvalGate() {
        return evalGate;
    }

    public void setEvalGate(EvalGate evalGate) {
        this.evalGate = evalGate;
    }
```

- [ ] **Step 6: Use the gate in QueryResultCache**

In the imports of `QueryResultCache.java`:
- add `import java.util.concurrent.ExecutionException;`
- add `import org.eclipse.lsp4j.Range;`
- add `import org.modelingvalue.nelumbo.NelumboTimeoutException;`

Then replace the whole `private void evaluate(String uri)` method (lines 96-174, up to the closing brace before the class end) with:

```java
    private void evaluate(String uri) {
        Workspace  workspace = documentManager.workspace();
        NlDocument document  = documentManager.getDocument(uri);
        if (document == null) {
            return;
        }
        EvalGate gate = workspace.getEvalGate();
        boolean  busy = gate.enter();
        try {
            evaluate(workspace, uri, document, gate, busy);
        } finally {
            gate.exit();
        }
    }

    // busy: the gate's short budget replaces the workspace deadline, and a timeout reads as an overload.
    private void evaluate(Workspace workspace, String uri, NlDocument document, EvalGate gate, boolean busy) {
        long             workspaceMs    = workspace.getEvalDeadlineMs();
        boolean          overloadBudget = busy && (workspaceMs <= 0 || gate.budgetMs() < workspaceMs);
        long             deadlineMs     = overloadBudget ? gate.budgetMs() : workspaceMs;
        List<Diagnostic> diagnostics    = NlDocument.baseDiagnostics(document.tokenizerResult(), document.parserResult());
        try {
            Map<Query, QueryResult> results = null;
            if (deadlineMs > 0) {
                String                          content = document.content();
                Future<Map<Query, QueryResult>> future  = backstop.submit(
                        () -> QueryEvaluator.evaluate(workspace.getBaseKnowledgeBase(), deadlineMs, content, uri, overloadBudget));
                try {
                    results = future.get(deadlineMs + 2000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException te) {
                    future.cancel(true);
                } catch (ExecutionException ee) {
                    // under the overload budget QueryEvaluator rethrows a timeout hit before any query (while parsing)
                    if (!(overloadBudget && ee.getCause() instanceof NelumboTimeoutException)) {
                        throw ee.getCause() instanceof RuntimeException re ? re : new RuntimeException(ee.getCause());
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    hints.put(uri, List.of());
                    return;
                }
            } else {
                results = QueryEvaluator.evaluate(workspace.getBaseKnowledgeBase(), 0, document.content(), uri);
            }
            if (results == null) {
                // no query was reached in time
                if (overloadBudget) {
                    diagnostics.add(overloadDiagnostic(new Range(new Position(0, 0), new Position(0, 0)), QueryResult.overloaded(deadlineMs).message()));
                    gate.recordOverload();
                }
                hints.put(uri, List.of());
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
                    }
                }
                hints.put(uri, list);
            }
        } catch (Exception ex) {
            System.err.println("query evaluation failed for " + uri + ": " + ex);
            hints.put(uri, List.of());
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
```

Behaviour that stays the same:
- the backstop timeout empties the hints and publishes/refreshes (it used to do so in its own early-return branch; now it goes through `results == null`)
- the interrupted branch returns without publishing
- the no-deadline branch is unchanged

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.EvalGateTest" --tests "org.modelingvalue.nelumbo.lsp.EmbeddedServerTest"`
Expected: PASS (all).

- [ ] **Step 8: Run the whole LSP test suite**

Run: `./gradlew :lsp:server:test`
Expected: PASS (85+ tests, 0 failures).

- [ ] **Step 9: Commit**

```bash
git add lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/EvalGate.java lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/Workspace.java lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResultCache.java lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EvalGateTest.java lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EmbeddedServerTest.java
git commit -m "lsp: short evaluation budget and server-overloaded diagnostic when busy

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Website overload banner

**Files:**
- Modify: `website/src/main/frontend/src/nelumbo-fields.ts` (module state near line 98, `ensureServices()`, new function after `showBanner()`)
- Modify: `website/src/main/frontend/src/fields.css` (after `.nelumbo-lsp-banner.visible`)
- Modify: `website/src/main/frontend/playwright.config.ts`
- Test: `website/src/main/frontend/e2e/overload.spec.ts` (create)

**Interfaces:**
- Consumes: the diagnostic code `server-overloaded` (Task 2) and the system properties `NELUMBO_OVERLOAD_THRESHOLD`/`NELUMBO_OVERLOAD_BUDGET_MS` (Task 2).
- Produces: DOM element `div.nelumbo-lsp-banner.nelumbo-overload-banner`, which has class `visible` while any Monaco marker has code `server-overloaded`.

- [ ] **Step 1: Add the overload test server to the Playwright config**

In `playwright.config.ts`, below `const PORT: number = 8899;` add:

```ts
const OVERLOAD_PORT: number = 8898;
```

and replace the `webServer: {...}` entry with:

```ts
    webServer: [
        {
            command:             'java -jar "' + serverJar() + '" --port ' + PORT + ' --no-gui',
            url:                 'http://localhost:' + PORT + '/health',
            reuseExistingServer: !process.env.CI,
            timeout:             60_000,
        },
        {
            // every evaluation counts as busy and gets a 300 ms budget (e2e/overload.spec.ts)
            command:             'java -DNELUMBO_OVERLOAD_THRESHOLD=0 -DNELUMBO_OVERLOAD_BUDGET_MS=300 -jar "' + serverJar() + '" --port ' + OVERLOAD_PORT + ' --no-gui',
            url:                 'http://localhost:' + OVERLOAD_PORT + '/health',
            reuseExistingServer: !process.env.CI,
            timeout:             60_000,
        },
    ],
```

- [ ] **Step 2: Write the failing e2e test**

Create `e2e/overload.spec.ts`:

```ts
import { test, expect, Page } from '@playwright/test';

// served by the second webServer in playwright.config.ts: every evaluation is "busy" with a 300 ms budget
test.use({ baseURL: 'http://localhost:8898' });

const HEAVY: string = 'import nelumbo.integers\n'
                    + 'Integer ::= factorial(<Integer>)\n'
                    + 'Integer n, r\n'
                    + 'factorial(n)=r <=> r=1 if n<=0, r=n*factorial(n-1) if n>0\n'
                    + 'factorial(5000)=r ?\n';
const LIGHT: string = 'import nelumbo.logic\ntrue ?\n';

async function setSandbox(page: Page, text: string): Promise<void> {
    await page.evaluate((t: string): void => {
        (window as any).NelumboFields.__editors[0].model.setValue(t);
    }, text);
}

test('a stopped evaluation shows the overload banner until an evaluation succeeds', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await expect.poll(async (): Promise<number> => page.evaluate((): number => (window as any).NelumboFields.__editors.length), { timeout: 20_000 })
        .toBeGreaterThan(0);
    await setSandbox(page, HEAVY);
    await expect(page.locator('.nelumbo-overload-banner')).toBeVisible({ timeout: 20_000 });
    await setSandbox(page, LIGHT);
    await expect(page.locator('.nelumbo-overload-banner')).toBeHidden({ timeout: 20_000 });
});
```

- [ ] **Step 3: Run it to verify it fails**

Run, from `website/src/main/frontend`: `(cd ../../../.. && ./gradlew :website:serverJar) && npx playwright test e2e/overload.spec.ts --reporter=line`
Expected: FAIL. The banner locator is not visible: the server already sends the diagnostic, but nothing shows it.

- [ ] **Step 4: Implement the banner**

In `nelumbo-fields.ts`, add with the other module-level declarations near line 98:

```ts
const OVERLOAD_CODE:  string                 = 'server-overloaded';
let   overloadBanner: HTMLDivElement | null  = null;
```

Add after `showBanner()`:

```ts
// The server marks evaluations it stopped to protect other users with this diagnostic code; show a
// banner while any editor carries such a marker (it clears with the next evaluation of that document).
function watchOverload(): void {
    monaco.editor.onDidChangeMarkers((): void => {
        const overloaded: boolean = monaco.editor.getModelMarkers({}).some((m: monaco.editor.IMarker): boolean => m.code === OVERLOAD_CODE);
        if (overloaded && overloadBanner === null) {
            overloadBanner             = document.createElement('div');
            overloadBanner.className   = 'nelumbo-lsp-banner nelumbo-overload-banner';
            overloadBanner.textContent = 'The server is busy - an evaluation was stopped. Try again in a moment.';
            document.body.prepend(overloadBanner);
        }
        if (overloadBanner !== null) {
            overloadBanner.classList.toggle('visible', overloaded);
        }
    });
}
```

In `ensureServices()`, add `watchOverload();` on the line before `servicesReady = true;`.

In `fields.css`, after the `.nelumbo-lsp-banner.visible { ... }` rule add:

```css
.nelumbo-overload-banner {
    color:      #f1707b;
    border-top: 1px solid rgba(241, 112, 123, .4);
}
```

- [ ] **Step 5: Typecheck, then run the e2e test to verify it passes**

Run, from `website/src/main/frontend`: `npm run check`
Expected: no errors.

Run: `(cd ../../../.. && ./gradlew :website:serverJar) && npx playwright test e2e/overload.spec.ts --reporter=line`
Expected: 1 passed.

If an old server is still running on 8898/8899 (`reuseExistingServer` locally), stop it first: `pkill -f "nelumbo-web-server-dev.jar --port 889"`.

- [ ] **Step 6: Run the whole e2e suite**

Run: `npm run test:e2e`
Expected: all passed (17 existing + 1 new).

- [ ] **Step 7: Commit**

```bash
git add website/src/main/frontend/src/nelumbo-fields.ts website/src/main/frontend/src/fields.css website/src/main/frontend/playwright.config.ts website/src/main/frontend/e2e/overload.spec.ts
git commit -m "website: banner while an editor carries a server-overloaded diagnostic

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: /stats endpoint

**Files:**
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/ServerStats.java`
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/LspWebSocket.java` (getter)
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java:75-123`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/ServerStatsTest.java` (create)
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java`

**Interfaces:**
- Consumes: `EvalGate.GLOBAL` and its getters (Task 2).
- Produces:
  - `GET /stats` returning JSON `{status, sessions:{open,max}, evaluations:{running,threshold,total,overloaded}, cpu:{load,cpus}, heap:{usedMb,maxMb}, uptimeSeconds}`
  - `static String ServerStats.status(int running, int threshold, double cpuLoad)`
  - `int LspWebSocket.sessionCount()`

- [ ] **Step 1: Write the failing tests**

Create `ServerStatsTest.java` (LGPL header first):

```java
package org.modelingvalue.nelumbo.website;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServerStatsTest {

    @Test
    void overloadedWhenAsManyEvaluationsRunAsTheThreshold() {
        assertEquals("overloaded", ServerStats.status(16, 16, 0.1));
    }

    @Test
    void busyFromHalfTheThresholdOrHighCpu() {
        assertEquals("busy", ServerStats.status(8, 16, 0.1));
        assertEquals("busy", ServerStats.status(0, 16, 0.7));
    }

    @Test
    void okBelowBoth() {
        assertEquals("ok", ServerStats.status(7, 16, 0.69));
    }

    @Test
    void unknownCpuLoadCountsAsIdle() {
        assertEquals(0.0, ServerStats.cpuLoad(-1.0));
        assertEquals(0.5, ServerStats.cpuLoad(0.5));
    }
}
```

Append to `NelumboHttpServerTest.java` (inside the class):

```java
    @Test
    void statsHaveTheDocumentedShape() throws Exception {
        HttpResponse<String> response = get("/stats");
        assertEquals(200, response.statusCode());
        JsonNode stats = mapper.readTree(response.body());
        assertTrue(List.of("ok", "busy", "overloaded").contains(stats.get("status").asText()), "status is ok, busy or overloaded");
        assertEquals(0, stats.get("sessions").get("open").asInt(), "no LSP session is open in this test");
        assertEquals(NelumboHttpServer.DEFAULT_MAX_LSP_SESSIONS, stats.get("sessions").get("max").asInt());
        for (String field : List.of("running", "threshold", "total", "overloaded")) {
            assertTrue(stats.get("evaluations").has(field), "evaluations." + field);
        }
        assertTrue(stats.get("cpu").has("load"), "cpu.load");
        assertTrue(0 < stats.get("cpu").get("cpus").asInt(), "cpu.cpus");
        assertTrue(0 < stats.get("heap").get("maxMb").asLong(), "heap.maxMb");
        assertTrue(stats.has("uptimeSeconds"), "uptimeSeconds");
    }
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.ServerStatsTest" --tests "org.modelingvalue.nelumbo.website.NelumboHttpServerTest.statsHaveTheDocumentedShape"`
Expected: compilation FAILS (`ServerStats` does not exist).

- [ ] **Step 3: Implement ServerStats**

Create `ServerStats.java` (LGPL header first):

```java
package org.modelingvalue.nelumbo.website;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

import org.modelingvalue.nelumbo.lsp.EvalGate;

import com.sun.management.OperatingSystemMXBean;

/** The /stats snapshot: LSP sessions, evaluation load (the shared {@link EvalGate}) and JVM resources. */
final class ServerStats {
    static final double BUSY_CPU_LOAD = 0.7;

    private ServerStats() {
    }

    static Map<String, Object> snapshot(int openSessions, int maxSessions, EvalGate gate) {
        OperatingSystemMXBean os      = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Runtime               runtime = Runtime.getRuntime();
        double                load    = cpuLoad(os.getProcessCpuLoad());
        Map<String, Object>   stats   = new LinkedHashMap<>();
        stats.put("status", status(gate.running(), gate.threshold(), load));
        stats.put("sessions", Map.of("open", openSessions, "max", maxSessions));
        stats.put("evaluations", Map.of("running", gate.running(), "threshold", gate.threshold(), "total", gate.total(), "overloaded", gate.overloaded()));
        stats.put("cpu", Map.of("load", load, "cpus", runtime.availableProcessors()));
        stats.put("heap", Map.of("usedMb", (runtime.totalMemory() - runtime.freeMemory()) >> 20, "maxMb", runtime.maxMemory() >> 20));
        stats.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        return stats;
    }

    static String status(int running, int threshold, double cpuLoad) {
        if (running >= threshold) {
            return "overloaded";
        }
        if (cpuLoad >= BUSY_CPU_LOAD || running >= threshold / 2) {
            return "busy";
        }
        return "ok";
    }

    // the JVM reports a negative value when the process CPU load is not available
    static double cpuLoad(double reported) {
        return Math.max(0.0, reported);
    }
}
```

`com.sun.management.OperatingSystemMXBean` is in module `jdk.management`. That module is part of the JDK and the `eclipse-temurin:21-jre` image, and the project has no `module-info.java`, so no build change is needed.

- [ ] **Step 4: Expose the session count and route /stats**

In `LspWebSocket.java`, add after the constructor:

```java
    int sessionCount() {
        return sessionCount.get();
    }
```

In `NelumboHttpServer.java`, in `start(int port)`:
- Before `app = Javalin.create(config -> {`, add:

```java
        LspWebSocket lsp        = new LspWebSocket(baseKb, timeoutMs, maxLspSessions);
```

- Replace the line `config.routes.ws("/lsp", new LspWebSocket(baseKb, timeoutMs, maxLspSessions)::configure);` with:

```java
            config.routes.get("/stats", ctx -> ctx.json(ServerStats.snapshot(lsp.sessionCount(), maxLspSessions, EvalGate.GLOBAL)));
            config.routes.ws("/lsp", lsp::configure);
```

- Add `import org.modelingvalue.nelumbo.lsp.EvalGate;` to the imports.

Align the new local with the existing locals in `start` (`String landing    = ...` block) using the same column widths.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.ServerStatsTest" --tests "org.modelingvalue.nelumbo.website.NelumboHttpServerTest"`
Expected: PASS (all).

- [ ] **Step 6: Commit**

```bash
git add website/src/main/java/org/modelingvalue/nelumbo/website/ServerStats.java website/src/main/java/org/modelingvalue/nelumbo/website/LspWebSocket.java website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java website/src/test/java/org/modelingvalue/nelumbo/website/ServerStatsTest.java website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java
git commit -m "website: /stats endpoint with sessions, evaluation load and JVM resources

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Status dot and status page

**Files:**
- Create: `website/src/main/resources/public/status-dot.js`
- Create: `website/src/main/resources/public/status.html`
- Modify: `website/src/main/resources/public/landing.html:407,419`
- Modify: `website/src/main/resources/public/tour.html:105,535`
- Modify: `website/src/main/resources/public/sandbox.html:124,195`
- Modify: `website/src/main/resources/public/docs.html:118,137`
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java` (two routes)
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java`
- Test: `website/src/main/frontend/e2e/status.spec.ts` (create)

**Interfaces:**
- Consumes: `GET /stats` (Task 4).
- Produces: `GET /status-dot.js`, `GET /status.html`, and `<a class="status-dot" href="/status.html">` on every page that shows the version.

- [ ] **Step 1: Write the failing tests**

Append to `NelumboHttpServerTest.java` (inside the class):

```java
    @Test
    void statusPageAndDotScriptAreServed() throws Exception {
        HttpResponse<String> page = get("/status.html");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("fetch('/stats')"), "the status page polls /stats");
        HttpResponse<String> dot = get("/status-dot.js");
        assertEquals(200, dot.statusCode());
        assertTrue(dot.headers().firstValue("Content-Type").orElse("").contains("javascript"), "the dot script is served as JavaScript");
    }

    @Test
    void everyPageWithAVersionShowsTheStatusDot() throws Exception {
        for (String page : List.of("/", "/tour.html", "/sandbox.html", "/docs/")) {
            String body = get(page).body();
            assertTrue(body.contains("class=\"status-dot\" href=\"/status.html\""), page + " links the status page from its status dot");
            assertTrue(body.contains("src=\"/status-dot.js\""), page + " loads the status dot script");
        }
    }
```

Create `website/src/main/frontend/e2e/status.spec.ts`:

```ts
import { test, expect, Locator, Page } from '@playwright/test';

test('the status dot shows the server status and opens the status page', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    const dot: Locator = page.locator('a.status-dot');
    await expect(dot).toHaveClass(/\b(ok|busy|overloaded)\b/, { timeout: 10_000 });
    await dot.click();
    await expect(page).toHaveURL(/\/status\.html$/);
    await expect(page.locator('#sessions')).toHaveText(/^\d+ \/ \d+$/, { timeout: 10_000 });
});
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.NelumboHttpServerTest"`
Expected: FAIL on the two new tests (404 for `/status.html`, no `status-dot` on the pages).

- [ ] **Step 3: Create the dot script**

Create `website/src/main/resources/public/status-dot.js`. It starts with the LGPL header: lines 1-15 of `website/src/main/resources/public/theme.js`, verbatim, then an empty line, then:

```js
// Server status dot: every <a class="status-dot"> on the page shows the /stats status as a coloured dot
// (ok / busy / overloaded) with a short tooltip, refreshed every 15 s while the page is visible.
(function () {
    var POLL_MS = 15000;
    var style   = document.createElement('style');
    style.textContent = '.status-dot{display:inline-block;width:8px;height:8px;border-radius:50%;'
                      + 'background:#5c6270;margin-left:6px;vertical-align:middle}'
                      + '.status-dot.ok{background:#5fb87a}'
                      + '.status-dot.busy{background:#e0a050}'
                      + '.status-dot.overloaded{background:#f1707b}';
    document.head.appendChild(style);

    function show(stats) {
        var dots = document.querySelectorAll('.status-dot');
        for (var i = 0; i < dots.length; i++) {
            dots[i].classList.remove('ok', 'busy', 'overloaded');
            dots[i].classList.add(stats.status);
            dots[i].title = stats.sessions.open + ' sessions, CPU ' + Math.round(stats.cpu.load * 100) + '%';
        }
    }

    function update() {
        if (document.hidden) {
            return;
        }
        fetch('/stats').then(function (response) {
            return response.json();
        }).then(show).catch(function () {
        });
    }

    update();
    setInterval(update, POLL_MS);
    document.addEventListener('visibilitychange', update);
})();
```

- [ ] **Step 4: Create the status page**

Create `website/src/main/resources/public/status.html` (`@VERSION@` is filled in by `processResources`, which already matches `public/*.html`):

```html
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Nelumbo server status</title>
<script src="/theme.js"></script>
<link rel="icon" type="image/svg+xml" href="/favicon.svg">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700&family=Instrument+Sans:wght@400;500;600&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
<style>
  :root { --bg: #1b1d23; --panel: #23262e; --text: #e6e8ee; --muted: #9aa0ac; --accent: #c184d8; }
  [data-theme="light"] { --bg: #fbfafc; --panel: #f2eff5; --text: #1f1b24; --muted: #6b6475; --accent: #9246b0; }
  * { box-sizing: border-box; }
  body { margin: 0; background: var(--bg); color: var(--text); font: 15px/1.5 "Instrument Sans", system-ui, sans-serif; }
  main { max-width: 560px; margin: 0 auto; padding: 40px 16px; }
  h1 { font-family: "Bricolage Grotesque", system-ui, sans-serif; font-size: 28px; margin: 0 0 4px; }
  .sub { color: var(--muted); margin: 0 0 24px; }
  .sub a { color: var(--accent); }
  dl { display: grid; grid-template-columns: max-content 1fr; gap: 8px 24px; margin: 0; padding: 18px 20px; background: var(--panel); border-radius: 10px; }
  dt { color: var(--muted); }
  dd { margin: 0; font-family: "JetBrains Mono", monospace; }
  #status.ok { color: #5fb87a; }
  #status.busy { color: #e0a050; }
  #status.overloaded { color: #f1707b; }
  footer { color: var(--muted); font-size: 12px; text-align: center; padding: 24px 16px; }
</style>
</head>
<body>
<main>
  <h1>Server status</h1>
  <p class="sub">Live numbers of this Nelumbo server, refreshed every 5 seconds. <a href="/">Home</a></p>
  <dl>
    <dt>Status</dt>              <dd id="status">-</dd>
    <dt>LSP sessions</dt>        <dd id="sessions">-</dd>
    <dt>Evaluations running</dt> <dd id="running">-</dd>
    <dt>Evaluations total</dt>   <dd id="total">-</dd>
    <dt>Stopped (busy)</dt>      <dd id="overloaded">-</dd>
    <dt>CPU</dt>                 <dd id="cpu">-</dd>
    <dt>Heap</dt>                <dd id="heap">-</dd>
    <dt>Uptime</dt>              <dd id="uptime">-</dd>
  </dl>
</main>
<footer>Nelumbo @VERSION@</footer>
<script>
  (function () {
    function set(id, text) {
      document.getElementById(id).textContent = text;
    }
    function update() {
      fetch('/stats').then(function (response) {
        return response.json();
      }).then(function (s) {
        document.getElementById('status').className = s.status;
        set('status', s.status);
        set('sessions', s.sessions.open + ' / ' + s.sessions.max);
        set('running', s.evaluations.running + ' (busy at ' + s.evaluations.threshold + ')');
        set('total', String(s.evaluations.total));
        set('overloaded', String(s.evaluations.overloaded));
        set('cpu', Math.round(s.cpu.load * 100) + '% of ' + s.cpu.cpus + ' cores');
        set('heap', s.heap.usedMb + ' / ' + s.heap.maxMb + ' MB');
        set('uptime', Math.floor(s.uptimeSeconds / 3600) + 'h ' + Math.floor(s.uptimeSeconds % 3600 / 60) + 'm');
      }).catch(function () {
        set('status', 'unreachable');
      });
    }
    update();
    setInterval(update, 5000);
  })();
</script>
</body>
</html>
```

- [ ] **Step 5: Serve both**

In `NelumboHttpServer.start(int port)`, next to the other `loadResource` locals, add (aligned with them):

```java
        String status     = loadResource("/public/status.html");
        String statusDot  = loadResource("/public/status-dot.js");
```

and next to the `/favicon.svg` route add:

```java
            config.routes.get("/status.html", ctx -> ctx.html(status));
            config.routes.get("/status-dot.js", ctx -> ctx.contentType("text/javascript; charset=utf-8").result(statusDot));
```

- [ ] **Step 6: Put the dot on every page with a version**

Use the same anchor everywhere:

```html
<a class="status-dot" href="/status.html" aria-label="server status"></a>
```

and load the script once per page with `<script src="/status-dot.js" defer></script>`.

Line numbers are as of merge `798b4179`; find the lines by their `@VERSION@` text if they moved.

- `landing.html` line 407: becomes `<span>Nelumbo @VERSION@ <a class="status-dot" href="/status.html" aria-label="server status"></a> - a Modeling Value Group project</span>`. Add the script tag on the line before `</body>`.
- `tour.html` line 105: becomes `<span class="version">@VERSION@ <a class="status-dot" href="/status.html" aria-label="server status"></a></span>`. Add the script tag on the line before `</body>`.
- `sandbox.html` line 124: append the anchor after `Nelumbo @VERSION@` inside the existing `<footer>`. Keep the footer's metadata/health links exactly as they are. Add the script tag on the line before `</body>`.
- `docs.html` line 118: same change as `tour.html` line 105. Add the script tag on the line before `</body>`.

The Java test checks the substring `class="status-dot" href="/status.html"`, so keep the attribute order exactly as above.

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :website:test`
Expected: PASS (all, including `everyTourEditorEvaluates`, `DocsSiteTest`, and the two new tests).

Run, from `website/src/main/frontend`: `npm run test:e2e`
Expected: all passed (including `status.spec.ts` and `overload.spec.ts`).

- [ ] **Step 8: Commit**

```bash
git add website/src/main/resources/public/status-dot.js website/src/main/resources/public/status.html website/src/main/resources/public/landing.html website/src/main/resources/public/tour.html website/src/main/resources/public/sandbox.html website/src/main/resources/public/docs.html website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java website/src/main/frontend/e2e/status.spec.ts
git commit -m "website: status dot on every page and a /status.html page

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: Documentation and end-to-end verification

**Files:**
- Modify: `CLAUDE.md` (LSP Server section, Website Module section, Load test paragraph)

**Interfaces:**
- Consumes: everything above.
- Produces: documentation only.

- [ ] **Step 1: Document in CLAUDE.md**

In the `### LSP Server (lsp/server/)` section, after the paragraph that starts with `Every LSP request handler in NlTextDocumentService runs inside inKb(...)`, add:

```markdown
**Overload budget** (2026-10-06). `EvalGate.GLOBAL` counts the evaluations running in the JVM (all sessions; `Workspace.getEvalGate()`, tests inject their own). An evaluation that starts while `NELUMBO_OVERLOAD_THRESHOLD` (default `availableProcessors`, i.e. the container cpus limit) others run gets `NELUMBO_OVERLOAD_BUDGET_MS` (default 2000) as its deadline instead of the workspace deadline (also when that is 0 = unlimited). A timeout under that budget becomes `QueryResult.Kind.OVERLOADED` (hint `⚠ server busy`) plus a Warning diagnostic with code `server-overloaded` (`QueryResult.OVERLOAD_CODE`, the client contract) on the query - or at 0:0 when no query was reached (backstop timeout, or a parse-time timeout that `QueryEvaluator` rethrows only under the overload budget). Light queries finish within the budget and are unaffected; an evaluation that started before the server got busy keeps its full deadline.
```

In the `## Website Module - LSP over WebSocket` section, after the paragraph about the frontend (`The frontend is an npm project ...`), add:

```markdown
**Overload banner and status** (2026-10-06). `nelumbo-fields.ts` shows a second fixed bottom strip (`.nelumbo-overload-banner`) while any Monaco marker has code `server-overloaded`. `GET /stats` (`ServerStats`) returns `{status, sessions:{open,max}, evaluations:{running,threshold,total,overloaded}, cpu:{load,cpus}, heap:{usedMb,maxMb}, uptimeSeconds}`; status is `overloaded` if running >= threshold, else `busy` if CPU load >= 0.7 or running >= threshold/2, else `ok`. `public/status-dot.js` (served at `/status-dot.js`) colours every `<a class="status-dot" href="/status.html">` - on landing, tour, sandbox and docs next to the version - and polls every 15 s while visible; `/status.html` shows all numbers, polling every 5 s. The e2e suite starts a second server on 8898 with `-DNELUMBO_OVERLOAD_THRESHOLD=0 -DNELUMBO_OVERLOAD_BUDGET_MS=300` for `overload.spec.ts`.
```

- [ ] **Step 2: Run the complete test suites**

Run: `./gradlew test --continue`
Expected: BUILD SUCCESSFUL. Then check the XML results for failures:

```bash
for d in build cli/build mcp/build website/build lsp/server/build; do grep -ho 'failures="[0-9]*"' $d/test-results/test/*.xml | sort | uniq -c; done
```

Expected: only `failures="0"`.

Run, from `website/src/main/frontend`: `npm run test:e2e`
Expected: all passed.

- [ ] **Step 3: Load test against a capped local container**

```bash
cd /Users/tom/projects/mvg-nelumbo/nelumbo
./gradlew :website:serverJar
docker build -q -t nelumbo-website-overload-test website
docker run -d --name nelumbo-overload --cpus 8 --memory 8g --memory-swap 8g -p 8899:8080 nelumbo-website-overload-test
cd website/src/main/frontend
node load/loadtest.mts --factorial 5000 --max-clients 32 --step-seconds 40
curl -s localhost:8899/stats
node load/loadtest.mts --max-clients 32 --step-seconds 30
docker rm -f nelumbo-overload && docker rmi -f nelumbo-website-overload-test
```

Expected:
- The factorial run reaches a step where `/stats` reports `evaluations.overloaded > 0` (busy evaluations stopped instead of queueing up).
- The plain tour run stays OK up to 32 clients with 0 errors.

The load test treats an overload hint as a changed result, so overloads do not count as errors. Record the observed numbers in the commit message.

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: overload budget, banner and server status

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```
