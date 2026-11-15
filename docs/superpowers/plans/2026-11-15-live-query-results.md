# Live Query Results Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Query results in the LSP editors and the Swing editor appear one by one as they come in, invalid results turn into `...calculating` after 300 ms, and parsing and coloring never wait for queries.

**Architecture:** A new core step `PreparedDocument.prepare` parses and walks the facts and rules in document order on the calling thread, taking a snapshot KB per query; each `PreparedQuery` is evaluated (on `KnowledgeBase.POOL`) and cancelled on its own. `QueryDiff` decides per query, from the old and new text, whether an edit keeps, moves or changes it. The LSP `QueryResultCache` and the Swing `EditorWindow` both schedule the queries on N driver threads, stream the results and cancel what an edit invalidates.

**Tech Stack:** Java 21, Gradle, JUnit 5, LSP4J, Swing, Playwright (e2e).

**Spec:** `docs/superpowers/specs/2026-11-15-live-query-results-design.md`

## Global Constraints

- Commit on the local branch `local/work` only; never push, never merge. Every commit message ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Java style: arrow-form `switch` only; one variable per definition; variable definitions aligned in columns; comments only where the logic is unclear; ASCII only (existing `\u03bc` escapes stay); LGPL header on new Java files (copy it from a neighbouring file).
- The engine itself changes only by `volatile` on `KnowledgeBase.deadlineNanos` (Task 1) and, only if Task 3's measurement asks for it, one-line `isPastDeadline` checks.
- A query sees exactly the facts and rules above it (document order).
- A failing fact or rule never stops the evaluation: its error is a diagnostic/message on its line, the queries below still run (LSP and Swing).
- Placeholder label, exactly: `...calculating`.
- Constants: `CALCULATING_DELAY_MS = 300` (from when a query's result first became invalid; later edits do not restart it), `DEBOUNCE_MS = 100` (LSP), `REFRESH_THROTTLE_MS = 200` (per connection, the last one always goes out), `OVERLOAD_RETRY_MS = 5000` (unchanged).
- Parallel queries: default 5; in the LSP `max(1, min(5, EvalGate threshold))`; system property `NELUMBO_MAX_PARALLEL_QUERIES` (> 0 overrides).
- Deadline and overload budget are per query and count from the start on a pool worker.
- Update CLAUDE.md in the task that changes the behavior it describes.
- Commands: core tests `./gradlew :test --tests <class>`, LSP tests `./gradlew :lsp:server:test --tests <class>`.

## Review Focus

1. Typing fast above a slow query: every keystroke cancels it; after the last keystroke it must finish, and no cancelled task may keep a slot (Task 5 `typingFastAboveASlowQueryEndsWithItsResult`).
2. Closing a document while its queries run: all its tasks stop, nothing is published for it afterwards (Task 5 `closingADocumentStopsItsQueries`).
3. One connection with a slow document next to a fast one (the tutorial page has 25 fields): the fast one gets the first free slot (Task 5 `aSlowDocumentDoesNotStarveAnother`).
4. Two queries with the same text at different positions: each keeps its own result and position (Task 1 `equalQueriesAreDistinctNodes`, Task 5 `equalQueriesKeepTheirOwnResultsInTheEditor`).
5. An edit that inserts or deletes a query between others: the queries below move with their OWN old result, matched by offset, not by index (Task 2 `anInsertedQueryShiftsTheOnesBelow`, `aDeletedQueryHasNoMatch`).

---

### Task 1: Core - prepare a document, evaluate and cancel one query

**Files:**
- Modify: `src/main/java/org/modelingvalue/nelumbo/KnowledgeBase.java:325`
- Create: `src/main/java/org/modelingvalue/nelumbo/tools/PreparedDocument.java`
- Create: `src/main/java/org/modelingvalue/nelumbo/tools/PreparedQuery.java`
- Test: `src/test/java/org/modelingvalue/nelumbo/test/PreparedDocumentTest.java`
- Modify: `CLAUDE.md` (Core, `tools/` bullet)

**Interfaces:**
- Produces:
  ```java
  public record PreparedDocument(KnowledgeBase knowledgeBase, ParserResult parserResult,
                                 java.util.List<ParseException> statementErrors, java.util.List<PreparedQuery> queries) {
      public static PreparedDocument prepare(KnowledgeBase base, TokenizerResult tokenizerResult, long parseDeadlineMs);
      public static PreparedDocument empty(KnowledgeBase base, TokenizerResult tokenizerResult); // no roots, no queries
      public static final class Crash extends RuntimeException { public Token crashToken(); }      // a parse crash
  }
  public final class PreparedQuery {
      public record Span(int start, int end) {}                                                     // text offsets
      public record Outcome(String result, long micros, ParseException exception, boolean timedOut, boolean cancelled) {}
      public Query query();
      public Span span();
      public Outcome evaluate(long deadlineMs);   // blocking; 0 = no deadline
      public void cancel();
  }
  ```

- [ ] **Step 1: Write the failing tests**

Sources (text blocks in the test): `ABOVE` and `BELOW` exactly as below; `EQUAL` = `EQUAL_QUERIES_AROUND_A_RULE` copied from `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/workspaceService/QueryExecutionFlowTest.java`; `FACTORIAL` = `QueryEvaluatorSeedingTest.FACTORIAL_SEED` copied.

```text
ABOVE:                                   BELOW (appended to ABOVE):
import nelumbo.logic                     fact likes(Kees)
Person  :: Object                        Person  ::= Jan
Person  ::= Piet, Kees                   fact knows(Jan)
Boolean ::= likes(<Person>), knows(<Person>)   likes(p) <=> knows(p)
Person  p                                likes(Kees) ?
fact likes(Piet)                         E[p](knows(p)) ?
likes(Kees) ?
E[p](knows(p)) ?
```

```java
@Test void aQueryIgnoresEverythingBelowIt() {
    List<String> above = results(ABOVE);
    List<String> below = results(ABOVE + BELOW);
    assertEquals(above, below.subList(0, 2));
    assertEquals("[()][]", below.get(2));                       // the appended part does take effect below it
}
@Test void equalQueriesAreDistinctNodes() {
    PreparedDocument doc = prepare(EQUAL);
    assertNotSame(doc.queries().get(0).query(), doc.queries().get(1).query());
    assertEquals(List.of("[(f=Apple),..][..]", "[(f=Apple),..][(f=Nut),..]"), results(EQUAL));
}
@RepeatedTest(10) void parallelResultsEqualSequentialOnes() {
    // FACTORIAL + "factorial(5)=r ?" .. "factorial(12)=r ?", evaluated by 4 threads at once
    assertEquals(sequential, parallel);
}
@Test void aFailingFactDoesNotStopTheWalk() {
    // "import nelumbo.logic\nPerson :: Object\nPerson ::= Piet\nBoolean ::= likes(<Person>)\nPerson p\nfact likes(p)\nlikes(Piet) ?\n"
    assertEquals(1, doc.statementErrors().size());
    assertTrue(doc.statementErrors().get(0).getShortMessage().contains("has variables"));
    assertEquals(1, doc.queries().size());
    assertNotNull(doc.queries().get(0).evaluate(0).result());
}
@Test void aCancelStopsARunningQuery() {
    // FACTORIAL + "factorial(100000)=r ?": evaluate on a thread, cancel() after 500 ms
    assertTrue(thread joined within 2 s after the cancel);
    assertTrue(outcome.cancelled());
}
@Test void aCancelBeforeTheStartSkipsTheQuery() {
    query.cancel();
    // evaluate(0) returns within 100 ms
    assertTrue(outcome.cancelled());
    assertNull(outcome.result());
}
@Test void aDeadlineStopsAQuery() {
    // factorial(100000), evaluate(300): returns within 2 s
    assertTrue(outcome.timedOut());
    assertFalse(outcome.cancelled());
}
```

`results(src)` prepares `src` on `KnowledgeBase.BASE` with deadline 0 and maps every query, in order, to `evaluate(0).result()`. Every test that starts `factorial(100000)` cancels it in `finally`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :test --tests org.modelingvalue.nelumbo.test.PreparedDocumentTest`
Expected: compilation FAILS (`PreparedDocument` does not exist).

- [ ] **Step 3: Make `deadlineNanos` volatile**

`KnowledgeBase.java:325`: `private volatile long deadlineNanos;`

- [ ] **Step 4: Implement `PreparedDocument.prepare`**

- `KnowledgeBase doc = new KnowledgeBase(base)`; with `parseDeadlineMs > 0` set its deadline; all work in `doc.run(...)` on the CALLING thread (never `invoke`: a running query holds a pool worker).
- Parse with `new Parser(tokenizerResult).parseNonThrowing()` inside the `run`. A `NelumboTimeoutException` propagates unchanged; any other `RuntimeException`/`StackOverflowError` from the parse becomes `Crash(parser.crashToken(), cause)`.
- Walk `parserResult.roots()` in order. A `Fact`/`Rule` (an `Evaluatable` that is not a `Query`): `evaluate(doc, new ParserResult(null, true))`; a `ParseException` goes to `statementErrors`, a `RuntimeException`/`StackOverflowError` too, as `new ParseException(e, "Internal error: " + <innermost cause>, root)`; the walk goes on. A `Query`: `new PreparedQuery(query, span, new KnowledgeBase(doc))` (the constructor copies the facts and rules so far). Span = first token `index()` .. last token `index() + text().length()`.
- After the walk: `doc.setDeadlineNanos(0)`.

- [ ] **Step 5: Implement `PreparedQuery.evaluate` and `cancel`**

The order inside matters (a cancel from another thread must never be lost):

```text
evaluate(deadlineMs):
    if cancelled: return Outcome(null, -1, null, true, true)
    snapshot.invoke(() -> {
        kb = KnowledgeBase.current()
        kb.setDeadlineNanos(deadlineMs > 0 ? nanoTime + deadlineMs * 1_000_000 : 0)
        running = kb                                   // volatile, published after the deadline is set
        if cancelled: kb.setDeadlineNanos(nanoTime)
        query.evaluate(kb, new ParserResult(null, true))
        catch ParseException e                -> exception = e
        catch NelumboTimeoutException         -> timedOut = true    // before RuntimeException: it is one
        catch RuntimeException | StackOverflowError e -> exception = new ParseException(e, "Internal error: " + innermost, query)
        result = query.inferResult() == null ? null : query.inferResult().toString()   // set also on a mismatch
    })
    return Outcome(result, query.inferMicros(), exception, timedOut, cancelled)
cancel():
    cancelled = true                                   // volatile
    kb = running; if kb != null: kb.setDeadlineNanos(nanoTime)
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :test --tests org.modelingvalue.nelumbo.test.PreparedDocumentTest`
Expected: PASS. If `aQueryIgnoresEverythingBelowIt` fails, stop and tell Tom (spec risk 1: then every edit must invalidate all queries). If `equalQueriesAreDistinctNodes` fails on `assertNotSame`, stop and tell Tom (spec risk 2).

- [ ] **Step 7: CLAUDE.md and commit**

Add under Core `tools/`: `PreparedDocument.prepare(base, tokens, parseDeadlineMs)` parses and walks facts/rules in document order on the calling thread, one snapshot KB per query (a failing fact/rule is recorded, the walk goes on); `PreparedQuery.evaluate(deadlineMs)` runs one query on `POOL`, `cancel()` sets its KB's deadline to now (`deadlineNanos` is volatile).

```bash
git add src/main/java/org/modelingvalue/nelumbo/KnowledgeBase.java src/main/java/org/modelingvalue/nelumbo/tools/PreparedDocument.java src/main/java/org/modelingvalue/nelumbo/tools/PreparedQuery.java src/test/java/org/modelingvalue/nelumbo/test/PreparedDocumentTest.java CLAUDE.md
git commit -m "core: prepare a document, evaluate and cancel its queries one by one"
```

---

### Task 2: Core - which queries an edit keeps, moves or changes

**Files:**
- Create: `src/main/java/org/modelingvalue/nelumbo/tools/QueryDiff.java`
- Test: `src/test/java/org/modelingvalue/nelumbo/test/QueryDiffTest.java`

**Interfaces:**
- Consumes: `PreparedQuery.Span` (Task 1).
- Produces:
  ```java
  public final class QueryDiff {
      public enum Kind { KEPT, MOVED, CHANGED }
      public record Match(Kind kind, int oldIndex) {}   // oldIndex -1 for CHANGED
      // one Match per new span, in order
      public static java.util.List<Match> of(String oldText, java.util.List<PreparedQuery.Span> oldSpans,
                                             String newText, java.util.List<PreparedQuery.Span> newSpans);
  }
  ```

- [ ] **Step 1: Write the failing tests**

`span(text, q)` = `new Span(text.indexOf(q), text.indexOf(q) + q.length())`; every case lists the queries of each text in order.

| test | old text | new text | expected |
|---|---|---|---|
| `anEditBelowKeepsEverythingAbove` | `alpha ?\nbeta ?\n` | `alpha ?\nbeta ?\n// c\n` | KEPT 0, KEPT 1 |
| `anEditInsideAQueryChangesOnlyThatQuery` | `alpha ?\nbeta ?\ndelta ?\n` | `alpha ?\nbetax ?\ndelta ?\n` | KEPT 0, CHANGED, MOVED 2 |
| `anInsertedQueryShiftsTheOnesBelow` | `alpha ?\nbeta ?\ndelta ?\n` | `alpha ?\ngamma ?\nbeta ?\ndelta ?\n` | KEPT 0, CHANGED, MOVED 1, MOVED 2 |
| `aDeletedQueryHasNoMatch` | `alpha ?\ngamma ?\nbeta ?\ndelta ?\n` | `alpha ?\nbeta ?\ndelta ?\n` | KEPT 0, MOVED 2, MOVED 3 |
| `typingRightAfterAQueryChangesIt` | `alpha ?\nbeta ?\n` | `alpha ? [()][]\nbeta ?\n` (spans `alpha ? [()][]`, `beta ?`) | CHANGED, MOVED 1 |
| `anEditAboveEverythingMovesAll` | `alpha ?\nbeta ?\n` | `// c\nalpha ?\nbeta ?\n` | MOVED 0, MOVED 1 |

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :test --tests org.modelingvalue.nelumbo.test.QueryDiffTest`
Expected: compilation FAILS (`QueryDiff` does not exist).

- [ ] **Step 3: Implement `QueryDiff.of`**

```text
prefix = length of the common prefix of oldText and newText
suffix = length of the common suffix, at most min(oldLength, newLength) - prefix
oldTail = oldLength - suffix;  delta = newLength - oldLength
new span j:  KEPT(i)  if old span i equals it and its end <= prefix
             MOVED(i) if old span i starts at >= oldTail and, shifted by delta, equals it
             CHANGED  otherwise
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :test --tests org.modelingvalue.nelumbo.test.QueryDiffTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/modelingvalue/nelumbo/tools/QueryDiff.java src/test/java/org/modelingvalue/nelumbo/test/QueryDiffTest.java
git commit -m "core: QueryDiff tells which queries an edit keeps, moves or changes"
```

---

### Task 3: Measure memoization and cancel latency (spec risks 3 and 4)

**Files:**
- Create: `src/test/java/org/modelingvalue/nelumbo/test/PreparedDocumentMeasurement.java`
- Possibly modify: `src/main/java/org/modelingvalue/nelumbo/logic/Predicate.java:315`
- Modify: `CLAUDE.md` (sudoku section)

**Interfaces:**
- Consumes: `PreparedDocument.prepare`, `PreparedQuery.evaluate`/`cancel` (Task 1).

- [ ] **Step 1: Write the measurement class**

`@EnabledIfEnvironmentVariable(named = "MEASURE_PREPARED", matches = "true")`, base = a child of `KnowledgeBase.BASE` with `setMaxLogicMemoiz(100000)`. Each method prints a table on stdout:
- `memoizationSequentialVsParallel`: for `examples/sudoku-smart-4x4.nl`, `examples/sudoku-csp-4x4.nl`, `examples/sudoku-smart-9x9.nl`: per query the `micros` evaluated one after another, then all at once on 4 threads.
- `cancelLatency`: for the first query of `examples/sudoku-csp-9x9.nl` and the `guessing` query of `examples/sudoku-brute-9x9.nl`: start it, `cancel()` after 10 s, print the ms until `evaluate` returns.

- [ ] **Step 2: Run the old path for comparison**

Run: `./gradlew :cli:cliJar && for f in sudoku-smart-4x4 sudoku-csp-4x4 sudoku-smart-9x9; do ./java-ci -DMAX_LOGIC_MEMOIZ=100000 -jar cli/build/libs/nelumbo-cli-dev.jar --json src/main/resources/org/modelingvalue/nelumbo/examples/$f.nl | grep -o '"micros":[0-9]*'; done`
Expected: one `micros` per query (one shared KB, today's memo sharing).

- [ ] **Step 3: Run the measurement**

Run: `MEASURE_PREPARED=true ./gradlew :test --tests org.modelingvalue.nelumbo.test.PreparedDocumentMeasurement`
Expected: both tables printed (several minutes).

- [ ] **Step 4: Only if a cancel took more than 1000 ms: add a deadline check**

At the start of `Predicate.infer(int nrOfUnbound, InferContext context)` (`Predicate.java:315`): the same check as `inferRules` (`if (context.knowledgebase().isPastDeadline()) { throw new NelumboTimeoutException(); }`). Rerun Step 3; if still > 1000 ms, stop and report to Tom.

- [ ] **Step 5: CLAUDE.md and commit**

Add the three timings (old shared KB, prepared sequential, prepared parallel) and the cancel latencies, with date and machine, to the sudoku section. Tell Tom the memoization difference in the task report.

```bash
git add src/test/java/org/modelingvalue/nelumbo/test/PreparedDocumentMeasurement.java CLAUDE.md
git add src/main/java/org/modelingvalue/nelumbo/logic/Predicate.java   # only if Step 4 changed it
git commit -m "measure memoization and cancel latency of prepared queries"
```

---

### Task 4: LSP groundwork - prepare in NlDocument, requests off the pool, placeholder result

**Files:**
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/NlDocument.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/NlTextDocumentService.java:196-200`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/Workspace.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResult.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryEvaluator.java`
- Test: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryResultTest.java`, `EmbeddedServerTest.java`
- Modify: `CLAUDE.md` (LSP server), `docs/superpowers/specs/2026-11-15-live-query-results-design.md` (the website option became a system property)

**Interfaces:**
- Consumes: `PreparedDocument`, `PreparedQuery` (Task 1).
- Produces:
  - `NlDocument(Workspace workspace, String content, int version, String uri, TokenizerResult tokenizerResult, PreparedDocument prepared)` plus method `ParserResult parserResult()` (= `prepared.parserResult()`), so existing callers keep compiling.
  - `Workspace.getMaxParallelQueries()`: the value set by `setMaxParallelQueries(int)` or by system property `NELUMBO_MAX_PARALLEL_QUERIES` when > 0, else `Math.max(1, Math.min(5, getEvalGate().threshold()))`.
  - `QueryResult.Kind.CALCULATING`, `QueryResult.calculating()`.
  - `public static QueryResult QueryEvaluator.toResult(PreparedQuery query, PreparedQuery.Outcome outcome, boolean overloadBudget, long deadlineMs)`.

- [ ] **Step 1: Write the failing tests**

```java
// QueryResultTest
@Test void calculatingIsOnlyItsLabel() {
    QueryResult r = QueryResult.calculating();
    assertEquals("...calculating", r.inlineLabel());
    assertEquals("", r.tooltip());
    assertEquals("", r.hover());
}
// EmbeddedServerTest: fill the pool like lightEvaluationQueuedBehindAFullPoolIsNotStopped
// (Collection.PARALLELISM blockers in KnowledgeBase.BASE.invoke, sleepUntil(released)), then:
@Test void requestsDoNotWaitForAFullPool() {
    assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
        dm.addDocument(uri, "import nelumbo.logic\ntrue ?\n", 1);
        SemanticTokens tokens = server.getTextDocumentService()
                .semanticTokensFull(new SemanticTokensParams(new TextDocumentIdentifier(uri))).get();
        assertFalse(tokens.getData().isEmpty());
    });
}
```

In `anInconsistentFactIsPublishedAsADiagnostic` replace `assertTrue(dm.queryResultCache().hints(uri).isEmpty(), ...)` by: poll up to 15 s, then `assertEquals(1, dm.queryResultCache().hints(uri).size(), "the query below the inconsistent fact is still evaluated")`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :lsp:server:test --tests org.modelingvalue.nelumbo.lsp.QueryResultTest --tests org.modelingvalue.nelumbo.lsp.EmbeddedServerTest`
Expected: compilation FAILS (`calculating` missing); after a stub, `requestsDoNotWaitForAFullPool` and `anInconsistentFactIsPublishedAsADiagnostic` FAIL.

- [ ] **Step 3: `QueryResult.calculating()`**

Label `...calculating`, tooltip `""`, hover `""`, `micros` -1. Add the new `Kind` to every switch the compiler flags (`QueryResultCache`: hint kind `InlayHintKind.Type`, no text edit; `DocumentCodeActionService`: no action).

- [ ] **Step 4: `NlDocument.of` runs `prepare`; `inKb` leaves the pool**

- `NlDocument.of`: `PreparedDocument.prepare(workspace.getBaseKnowledgeBase(), tokenizerResult, workspace.getEvalDeadlineMs())`; a `NelumboTimeoutException` gives `PreparedDocument.empty(...)` (as the parse timeout does today). Keep its immediate diagnostics publish for now (Task 5 moves it).
- `NlTextDocumentService.inKb`: `return new KnowledgeBase(workspace.getBaseKnowledgeBase()).get(request);` (runs on the lsp4j thread, context via ThreadLocal; was `invoke` on `POOL`).
- `Workspace`: `maxParallelQueries` as in Interfaces.

- [ ] **Step 5: `QueryEvaluator` on top of `prepare`**

- `evaluate(base, deadlineMs, content, uri, overloadBudget, statementErrors)`: tokenize, `prepare(base, tokens, overloadBudget ? 0 : deadlineMs)`; a parse timeout: rethrow under `overloadBudget`, else return an empty result map. Every `statementErrors()` entry goes to the consumer; then every query is evaluated in order with `evaluate(deadlineMs)` and put into the `QueryResults` map via `toResult`. No early stop: neither at a statement error nor at a timeout.
- `toResult`: timed out -> `overloadBudget ? QueryResult.overloaded(deadlineMs) : QueryResult.error("evaluation exceeded the deadline", -1)`; a `ParseException` -> the current `toResult(query, exc)` logic (mismatch when the short message starts with `Expected result `, inferred = `outcome.result()`); `result() == null` -> `error("Infer resulted in nothing", micros)`; expected clause -> `match`; else `result`.
- The parse in `QueryEvaluator` is the only one left there; `QueryResultCache` still calls this wrapper until Task 5.

- [ ] **Step 6: Run all LSP tests**

Run: `./gradlew :lsp:server:test`
Expected: PASS, including `QueryEvaluatorSeedingTest`, `QueryExecutionFlowTest`, `LspRequestContextTest` unchanged.

- [ ] **Step 7: CLAUDE.md, spec, commit**

CLAUDE.md, LSP server section: request handlers run `inKb` with `get` on the lsp4j thread, not on `POOL`; `NlDocument.of` runs `prepare`; a failing fact/rule no longer stops the evaluation (rewrite the bullet that says it does). In `lightEvaluationQueuedBehindAFullPoolIsNotStopped` drop the comment "parsing needs a pool worker too" (no longer true). Spec, "Calls made by Claude": the limit is set by system property `NELUMBO_MAX_PARALLEL_QUERIES`, not a website option.

```bash
git add lsp/server/src CLAUDE.md docs/superpowers/specs/2026-11-15-live-query-results-design.md
git commit -m "lsp: prepare documents off the pool, placeholder result, statement errors go on"
```

---

### Task 5: LSP - per-query scheduling, streaming, invalidation and placeholders

**Files:**
- Modify (rewrite): `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResultCache.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/NlDocument.java` (drop the immediate diagnostics publish)
- Modify: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/RecordingClient.java` (add `public final AtomicInteger inlayHintRefreshes`, incremented in `refreshInlayHints`)
- Test: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/LiveQueryResultsTest.java`; `EmbeddedServerTest.java` must stay green
- Modify: `CLAUDE.md` (LSP server, overload budget, website frontend hint bullet, `/stats`)

**Interfaces:**
- Consumes: `NlDocument.prepared()`, `Workspace.getMaxParallelQueries()`, `QueryResult.calculating()`, `QueryEvaluator.toResult(...)` (Task 4); `QueryDiff.of` (Task 2); `PreparedQuery` (Task 1).
- Produces: unchanged public API `hints(String uri)`, `schedule(String uri)`, `remove(String uri)`, `shutdown()`.

- [ ] **Step 1: Write the failing tests**

Server on `QueryEvaluatorSeedingTest.seeded(FACTORIAL_SEED)`, workspace deadline 0; counting gate `new EvalGate(100, 2000)` unless stated. Every test disposes the workspace in `finally` (that cancels its running queries). `F(n)` = line `factorial(n)=x ?` after `Integer x`. Timings (CLI, warm-up included): `factorial(5000)` ~1 s, `factorial(10000)` ~2 s; `factorial(100000)` runs far longer than any test.

```java
@Test void resultsArriveOneByOne() {            // F(5), F(100000)
    // within 5 s, at the same moment: line 1 label contains "120", line 2 label is "...calculating"
}
@Test void aQuickRecalculationShowsNoCalculating() {
    // "import nelumbo.logic\ntrue ?\nfalse ?\n", wait for 2 hints, update to "...\ntrue & true ?\nfalse ?\n",
    // sample hints(uri) every 10 ms for 1 s: no label "...calculating"; ends with 2 hints
}
@Test void anEditBelowARunningQueryKeepsIt() {   // N=2; F(10000), F(3); wait until gate.running() == 1
    // update the last line to F(4): hint "24" arrives, the F(10000) result arrives
    assertEquals(3, gate.total());                // F(10000) evaluated once
}
@Test void anEditAboveARunningQueryCancelsIt() { // N=1; F(100000); wait until gate.running() == 1
    // update to F(5): hint "120" within 5 s (only possible if the slow query gave its one slot back)
}
@Test void atMostNQueriesRun() {                 // N=2; F(100000)..F(100003)
    // sample gate.running() for 2 s: max == 2
}
@Test void closingADocumentStopsItsQueries() {   // N=2; F(100000), F(100001); wait until running == 2
    dm.closeDocument(uri);
    // gate.running() == 0 within 3 s; no diagnostics for uri published after the close
}
@Test void aSlowDocumentDoesNotStarveAnother() { // N=2; doc A: F(5000), F(5001), F(5002); then doc B: F(5)
    // when B's "120" appears, A's third hint is not yet a result
}
@Test void typingFastAboveASlowQueryEndsWithItsResult() {  // N=1; F(5000)
    // 10 updates 50 ms apart, each adding a character to a comment line at the top
    // then: its result within 10 s, gate.running() == 0
}
@Test void equalQueriesKeepTheirOwnResultsInTheEditor() {  // EQUAL_QUERIES_AROUND_A_RULE on KnowledgeBase.BASE
    // hint on line 11 "[(f=Apple),..][..]", on line 13 "[(f=Apple),..][(f=Nut),..]"
}
@Test void refreshesAreThrottled() {             // F(1)..F(20)
    // after all 20 results: client.inlayHintRefreshes.get() <= 10
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :lsp:server:test --tests org.modelingvalue.nelumbo.lsp.LiveQueryResultsTest`
Expected: FAIL (today all hints come at once, nothing shows `...calculating`, a running evaluation is never cancelled).

- [ ] **Step 3: Rewrite `QueryResultCache`**

State and rules (the code is the implementer's):
- Per document the current version: its `NlDocument` and one slot per `prepared().queries()` entry. A slot: its `PreparedQuery`, result (or none), the moved old result (or none), the time it became invalid, its task state (pending/running/done).
- `schedule(uri)` (on every add/update, called right after the new `NlDocument` is stored): `QueryDiff.of` against the previous version (spans from `PreparedQuery.span()`). KEPT: the old slot object carries over as is (its running task too). MOVED and CHANGED: cancel the old slot's task (`PreparedQuery.cancel()`, or drop it when pending), new slot; MOVED also takes the old result as its moved result. A new slot's invalid time is the old slot's when that one had no result yet, else now. The new slots become pending after `DEBOUNCE_MS` (a superseded debounce does nothing). Then publish diagnostics and refresh (both throttled), and one more refresh `CALCULATING_DELAY_MS` later.
- `hints(uri)`, per slot of the current version: its result; else its moved result while less than `CALCULATING_DELAY_MS` since invalid; else `QueryResult.calculating()` from `CALCULATING_DELAY_MS` on; else nothing. Positions from the slot query's last token (a kept slot's old tokens have the same positions); the existing hint building (label part with tooltip, kind, text edits, `data` = hover) stays.
- Drivers: `getMaxParallelQueries()` daemon threads named `nelumbo-query-<n>`, created on first use (a gate set after construction counts). A free driver takes the pending slot of the document with the fewest running slots; ties by document order, then by when the document was scheduled.
- Running a slot: `boolean busy = gate.enter()`; deadline = the gate's budget when `busy && (workspaceMs <= 0 || budgetMs < workspaceMs)`, else the workspace deadline; `outcome = query.evaluate(deadline)`; `gate.exit()`. A cancelled outcome or a slot no longer in the current version is dropped. Else `QueryEvaluator.toResult(...)` becomes the slot's result; an OVERLOADED one calls `gate.recordOverload()` and the slot is pending again after `OVERLOAD_RETRY_MS` once `gate.running() < gate.threshold()` (else again 5 s later; a new version supersedes it). Then diagnostics and refresh (throttled).
- Diagnostics per document, published as one list: `NlDocument.baseDiagnostics(...)`, every `prepared().statementErrors()` entry (`statementDiagnostic`), per slot with a result its mismatch diagnostic (`expectedRange`) or overload diagnostic (code `server-overloaded`, `queryRange`). `NlDocument.of` no longer publishes; the cache publishes on every `schedule`.
- Refresh: `client.refreshInlayHints()` at most once per `REFRESH_THROTTLE_MS` per connection; a request inside the window goes out at its end. Diagnostics per document on the same tick.
- `remove(uri)`: cancel every slot of the document, drop the version; nothing is published for it afterwards. `shutdown()`: the same for all documents, then stop the drivers and timers.
- The old backstop thread pool (`future.get` with deadline + 2 s) goes: a query that ignores its deadline only holds its own slot.

- [ ] **Step 4: Run the LSP tests**

Run: `./gradlew :lsp:server:test`
Expected: PASS, including `EmbeddedServerTest` unchanged (its overload and stale-position tests pin the per-query overload and the moved results).

- [ ] **Step 5: CLAUDE.md and commit**

LSP server: per-query scheduling (N drivers, fewest-running-first), `QueryDiff` invalidation, the 300 ms `...calculating` delay, refresh throttle; overload budget section: per query, retry per query, no backstop; website frontend: replace "Until an edit's re-evaluation finishes, `QueryResultCache` serves only the hints on unchanged lines" by the new rule; `/stats` `evaluations` now counts queries.

```bash
git add lsp/server/src CLAUDE.md
git commit -m "lsp: query results one by one, ...calculating after 300 ms, edits cancel only what they invalidate"
```

---

### Task 6: Website e2e

**Files:**
- Modify: `website/src/main/frontend/e2e/inlay-hints.spec.ts`
- Modify: `CLAUDE.md` (E2E line only if a new hook or server was added; none is planned)

**Interfaces:**
- Consumes: the behavior of Task 5 through the website server (deadline 30 s).

- [ ] **Step 1: Write the failing tests**

```ts
const SLOW: string = [
    'import nelumbo.integers',
    'Integer ::= factorial(<Integer>)',
    'Integer n, r',
    'factorial(n)=r <=> r=1 if n<=0, r=n*factorial(n-1) if n>0',
    'factorial(5)=r ?',
    'factorial(20000)=r ?',
].join('\n');

test('a slow query shows ...calculating until its result is in', async ({ page }) => {
    // sandbox, __editors[0].model.setValue(SLOW)
    // '...calculating' visible (20 s) while a hint containing 'r=120' is visible too
    // then '...calculating' has count 0 (30 s)
});
test('a quick edit shows no ...calculating', async ({ page }) => {
    // setValue(DOC), wait for the three hints of DOC, setValue(DOC + '\ntrue ?')
    // in the page, sample document.body.innerText every 10 ms for 1000 ms: never contains '...calculating'
});
```

- [ ] **Step 2: Run the tests**

Run: `cd website/src/main/frontend && npm run test:e2e -- inlay-hints.spec.ts`
Expected: all tests in the file PASS (the run rebuilds `:website:serverJar` with Tasks 1-5). If the first test does not see `...calculating`, raise the factorial argument until the slow query takes 2-5 s on the e2e server.

- [ ] **Step 3: Run the whole e2e suite**

Run: `cd website/src/main/frontend && npm run test:e2e`
Expected: PASS (`tutorial.spec.ts` counts one `\u03bcs`, `overload.spec.ts` runs per query now).

- [ ] **Step 4: Commit**

```bash
git add website/src/main/frontend/e2e/inlay-hints.spec.ts
git commit -m "e2e: ...calculating for a slow query, none for a quick edit"
```

---

### Task 7: Swing editor

**Files:**
- Create: `src/main/java/org/modelingvalue/nelumbo/tools/EditorMessages.java`
- Modify: `src/main/java/org/modelingvalue/nelumbo/tools/EditorWindow.java` (`startExecutionLoop` :239, `run` :1153, `execute` :1269, `computeResults` :1342, `applyUIUpdates` :1413, `Highlight` :1479 moves out, close handling :1051/:1131)
- Test: `src/test/java/org/modelingvalue/nelumbo/test/EditorMessagesTest.java`
- Modify: `CLAUDE.md` (Core, the editor-window bullet)

**Interfaces:**
- Consumes: `PreparedDocument`, `PreparedQuery` (Task 1), `QueryDiff` (Task 2).
- Produces:
  ```java
  public final class EditorMessages {                       // the messages pane of one version
      public record Highlight(int index, int length, String error) {}
      public EditorMessages(int totalLines);
      public void setFixed(int line, String message, Highlight textHighlight); // parse or statement error; wins over a query on its line
      public void setQuery(int line, String message, Highlight textHighlight, boolean highlightMessage);
      public String text();                                  // lines joined by "\n"
      public java.util.List<Highlight> textHighlights();
      public java.util.List<Highlight> messageHighlights();  // offsets in text()
  }
  ```

- [ ] **Step 1: Write the failing tests**

```java
@Test void aQueryResultLandsOnItsLine() {
    EditorMessages m = new EditorMessages(3);
    m.setQuery(1, "[()][]", null, false);
    assertEquals("\n[()][]\n", m.text());
}
@Test void anErrorWinsOverAQueryOnItsLine() {
    EditorMessages m = new EditorMessages(2);
    m.setQuery(1, "[()][]", null, false);
    m.setFixed(1, "Unexpected token", new EditorMessages.Highlight(5, 1, "Unexpected token"));
    assertEquals("\nUnexpected token", m.text());
    assertEquals(1, m.textHighlights().size());
}
@Test void aMismatchHighlightsItsMessage() {
    EditorMessages m = new EditorMessages(3);
    m.setQuery(0, "abc", null, false);
    m.setQuery(2, "[][()]", new EditorMessages.Highlight(10, 5, "Expected result"), true);
    assertEquals(List.of(new EditorMessages.Highlight(5, 6, "Expected result")), m.messageHighlights());
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :test --tests org.modelingvalue.nelumbo.test.EditorMessagesTest`
Expected: compilation FAILS (`EditorMessages` does not exist).

- [ ] **Step 3: Implement `EditorMessages`**

The per-line logic of today's `computeResults` (parse exceptions override a line; a query's message highlight covers its message), recomputed on every call of `text()`/`messageHighlights()`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :test --tests org.modelingvalue.nelumbo.test.EditorMessagesTest`
Expected: PASS

- [ ] **Step 5: Rework `EditorWindow`**

- `startExecutionLoop` runs the loop on the window's own thread (no `KnowledgeBase.BASE.invoke`, so an idle window holds no pool worker); a per-window fixed pool of 5 daemon threads `EditorQuery-<windowId>` runs the queries.
- `execute()` per refresh: read the text, `prepare(KnowledgeBase.BASE, tokens, 0)` (a `PreparedDocument.Crash` -> `showCrash(crash.crashToken(), internalError(crash))`, as today), `knowledgeBase = prepared.knowledgeBase()` (the KB viewer and `editor.N` imports use it), `QueryDiff` against the previous version with the same keep/move/cancel/invalid-time rules as Task 5 (no debounce here), apply colors and the messages right away, submit the new queries, start a one-shot 300 ms `javax.swing.Timer` that re-renders the messages, then `updateImportDependencies` and `notifyDependentWindows`.
- Messages of a version: statement errors and parse exceptions via `setFixed`; per query its result (the outcome's `result()`, or its exception's short message with the red text highlight and, when there is a result, the green message highlight), else its moved old message while < 300 ms since invalid, else `...calculating` from 300 ms on. Each finished query re-renders only the messages pane on the EDT, and only while its version is still current; a cancelled outcome is dropped. A crash in a query shows as `Internal error: ...` on its line, the others go on.
- The `if (!refreshRequested)` skip goes away. Closing the window cancels every query and shuts the pool down.

- [ ] **Step 6: Check by hand**

Run: `./gradlew editorJar && java -jar build/libs/nelumbo-ide-dev.jar`, new window, paste `FACTORIAL_SEED` + `Integer x` + `factorial(5)=x ?`, `factorial(10000)=x ?`, `factorial(100000)=x ?`.
Expected:
- colors at once; `120` while the other two show `...calculating`; the second result after ~2 s;
- editing the last line keeps the first two results;
- typing on the first line turns all three into `...calculating` after 300 ms and colors keep up while typing;
- `fact` with a variable above the queries shows its error on its line and the results below still come.

- [ ] **Step 7: Full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: CLAUDE.md and commit**

Rewrite the editor-window bullet: own thread per window (not a pool worker), `prepare` per edit, queries on 5 threads per window, results line by line, `...calculating` after 300 ms, a failing fact/rule shows its error and the queries below still run.

```bash
git add src/main/java/org/modelingvalue/nelumbo/tools/EditorMessages.java src/main/java/org/modelingvalue/nelumbo/tools/EditorWindow.java src/test/java/org/modelingvalue/nelumbo/test/EditorMessagesTest.java CLAUDE.md
git commit -m "editor: results line by line, ...calculating after 300 ms, coloring never waits"
```
