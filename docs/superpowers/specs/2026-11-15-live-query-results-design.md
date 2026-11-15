# Live query results in the editors - design

Date: 2026-11-15

## Intent

A document with several `?` queries can take a long time. Today the editors show results only when ALL queries are done, and while the user edits, the old results stay until the new ones arrive. The user cannot tell whether the editor is done or still busy.

Tom wants, in the LSP editors (IntelliJ, VS Code, the website's Monaco fields) and in the Swing editor (`tools/NelumboEditor`):
- a result that is no longer valid replaced by `...calculating`,
- every result shown as soon as it is in, one by one,
- parsing and coloring never waiting for queries,
- without changing the Nelumbo core much.

## Current state

Parsing and evaluation are already two steps:
1. `Parser.parseNonThrowing()`: tokens, patterns, types, transformations, variables. No query is evaluated. Colors come from this step.
2. A loop over the roots in document order: a fact -> `KnowledgeBase.addFact`, a rule -> `addRule` (both cheap map updates), a query -> inference (expensive). A query sees only the facts and rules ABOVE it: document order is semantics (`QueryExecutionFlowTest.equalQueriesKeepTheirOwnResults`).

LSP server:
- `NlDocument.of` parses on every `didChange` (colors); `QueryEvaluator` parses again and runs the loop.
- `QueryResultCache`: one evaluation thread per connection (`nelumbo-query-eval`) for all documents, debounce 300 ms. All hints of a document are replaced at once, after the last query.
- A new edit cancels only a run that has not started (`prev.cancel(false)`); a running one runs to its end (no deadline locally).
- After an edit, `hints()` keeps the old hints on unchanged lines: stale results look valid.
- The website page opens up to 25 documents on one connection: a slow field delays all others.

Swing editor (`EditorWindow`):
- One loop per window inside `KnowledgeBase.BASE.invoke`, one KB re-`init()`ed per edit: parse, colors, all queries, then all messages.
- An edit during the queries waits until they are all done; only then the new text is parsed and colored.

## Goals

- Results become invalid only for queries BELOW the edit; the ones above keep their result and are not evaluated again.
- 200..400 ms after a result became invalid, `...calculating` shows in its place, unless the new result arrived first (no flicker on short recalculations).
- Results appear one by one as they come in.
- Queries run in parallel, with a limit (Tom: "say 5").
- An edit drops the queued work and stops the running work of the queries below it.
- Parsing and coloring run right away on every edit, whatever queries are running.
- Both the LSP editors and the Swing editor.

## Non-goals

- Changes to inference or to the language semantics.
- The CLI, the MCP server, REST `/eval` and `NelumboEvaluator`: they stay sequential (they can use the new API later).
- Priority for visible editors; a visible difference between queued and running queries.
- Keeping results across versions other than "above the edit".

## Anti-goals

- An old result shown as if valid after the delay.
- Flicker (result -> `...calculating` -> same result) on a short recalculation.
- A query seeing facts or rules below it.
- One website visitor filling the shared inference pool for everybody.
- Coloring waiting for a query.

## Design

### Core: prepare, then evaluate per query

A new core API (names are suggestions):

- `prepare(base, tokenizerResult)`: on the CALLING thread, not on `KnowledgeBase.POOL` (`new KnowledgeBase(base).run(...)`: off the pool the context falls back to a ThreadLocal, as `EvalService` already uses), parse (as `parseNonThrowing`) and then walk the roots in document order: facts and rules are evaluated into the document KB; at every query a snapshot `new KnowledgeBase(documentKb)` is taken. The constructor runs `init()`, which copies the current facts and rules (immutable collections, so cheap), so the snapshot holds exactly the facts and rules above the query. Off the pool, because every running query holds a pool worker for its whole run: a parse that needs a worker would wait for a query.
- A fact or rule that fails: its error is recorded and the walk goes on, as the Swing editor does today (Tom: the LSP behaves the same; it used to stop there).
- Result `PreparedDocument`: the `ParserResult`, the statement errors, and the queries in document order, each a `PreparedQuery` with its `Query`, source range and snapshot.
- `PreparedQuery.evaluate(deadlineMs)`: runs the query in `snapshot.invoke(...)` and returns its outcome (infer result, micros, mismatch or error), the same information `QueryEvaluator` builds today.
- `PreparedQuery.cancel()`: sets the deadline of the KB that runs it (`KnowledgeBase.current()` inside the runnable) to now; inference then throws `NelumboTimeoutException` at its next deadline check (`Predicate.inferRules`). Cancel before the start means: do not start.
- The only change in the engine itself: `KnowledgeBase.deadlineNanos` becomes `volatile`, so a cancel from another thread is seen.

The caller decides how queries run: sequential or parallel, with or without a deadline.

### Invalidation on an edit

Per document version:
- `editStart` = length of the common prefix of the old and new text; `delta` = new length - old length.
- A new query KEEPS the old query's state when the old version had a query with the same start and end offset and that end is <= `editStart`. The state can be a result, a queued task or a running task: a task of a kept query goes on and its result lands in the new version (its snapshot is still right: the text above it did not change).
- Every other query is invalid: its old queued task is dropped, its running task cancelled, a new task queued.
- During the delay, an invalid query whose text lies in the unchanged tail (old range + `delta` = new range, same text) shows its old result at the new position. An invalid query whose own text changed shows nothing during the delay.
- The delay (300 ms) starts when a query's result first becomes invalid; later edits do not restart it. After it, the query shows `...calculating` until its new result is in.
- Ruling (build): a CHANGED query (the one being edited) has no old query to inherit from, so its delay starts at each edit; matching it to a guessed predecessor would add logic for the query the user is typing in. While typing inside one query, `...calculating` shows 300 ms after the last keystroke there.
- A moved old result shown during the delay carries no text edit: a double-click would write a stale expected result. The text edit belongs only to a query's own current result.

### LSP server

- Parse once: `NlDocument.of` runs `prepare` (with the workspace deadline, which stops only inference: it is checked only in `Predicate`, so it never bounds a parse); colors, hover etc. use its `ParserResult`, the evaluation its snapshots. The second parse in `QueryEvaluator` goes away.
- The request handlers leave the pool too: `inKb` (`NlTextDocumentService`) runs its fresh child of the base KB with `run` on the lsp4j thread instead of `invoke`, so semantic tokens, hovers and hint pulls never wait for a query. On the website, parse CPU is then limited by the session cap and the text size (64 KB frames), no longer by the pool width; the deadline does not stop a parse (see above). An import met by the parse is built on the calling thread too (`ResourceImportResolver`), or a cold import would still wait for a pool worker. `EmbeddedServerTest.lightEvaluationQueuedBehindAFullPoolIsNotStopped` assumes a parse needs a pool worker: adapt it.
- `QueryResultCache` keeps per document: the version's queries and per query its state (result, task, time it became invalid).
- Scheduling: the single `nelumbo-query-eval` thread is replaced by a per-connection executor of N driver threads; each blocks in `snapshot.invoke`, the work itself runs on the shared `KnowledgeBase.POOL`. Queue: the document with the fewest running queries first, document order within one, then the document scheduled first (so a slow document does not starve another).
- N = `maxParallelQueries` on `Workspace`, default `max(1, min(5, EvalGate threshold))` (5 is Tom's number); it can be set by the system property `NELUMBO_MAX_PARALLEL_QUERIES`. Capped at the threshold so that one connection alone never trips the overload budget: the threshold is `Collection.PARALLELISM` (15 in production, 3 in every test JVM, CI and e2e), and a query that starts while >= threshold run gets the short budget, also in the stdio IDE servers (`heavyEvaluationWhileBusyPublishesAnOverloadDiagnostic`).
- Debounce before queuing new tasks: 100 ms (was 300), so that a short recalculation ends within the 300 ms delay.
- The workspace deadline applies per query (was per document): a query past it gets the ERROR `evaluation exceeded the deadline`, the others go on.
- Overload (`EvalGate`) per query instead of per document: enter/exit around every query; a query that starts while >= threshold run gets the budget; past it the query gets `QueryResult.Kind.OVERLOADED` with diagnostic code `server-overloaded` and is retried alone after 5 s once `running < threshold`; a new version supersedes the retry. The client contract (code, banner) is unchanged.
- Every finished query updates the document's hints and diagnostics at once. The client is told with `workspace/inlayHint/refresh`, at most one per 200 ms per connection (the last one always goes out); diagnostics per document with the same throttle. A refresh is also sent when a delay expires, so `...calculating` appears without a client pull.
- `hints(uri)` builds the list on demand: per query of the current version its result, its moved old result (during the delay), `...calculating` (after the delay), or nothing. A `...calculating` hint has the result style (`InlayHintKind.Type`), no emoji, no text edit.
- A failing fact or rule no longer stops the evaluation: its diagnostic is published as now, the queries below it get results (`EmbeddedServerTest.anInconsistentFactIsPublishedAsADiagnostic` and CLAUDE.md change accordingly).
- `QueryEvaluator.evaluate(...)` stays as a sequential wrapper on `prepare` for the code lens and the tests.

### Swing editor

- On every edit: cancel the invalid tasks, run `prepare` on the new text right away, apply the colors, queue the new tasks on a per-window executor (N = 5; no `EvalGate` here).
- A failing fact or rule shows its error on its line and the queries below it still run, as today.
- Every result is written to its line in the messages pane on the EDT as it comes in; the same invalidation, delay and `...calculating` rules as the LSP.
- Every version gets a fresh KB from `prepare` (no shared re-`init()`ed KB), so a running query never shares its KB with a new parse.
- The window loop leaves the pool: today every window holds a `POOL` worker for its whole life, also while idle (a 4-core CI-like JVM has 3 workers).
- The KB viewer shows the latest version's document KB, the tree viewer its parse. Editor imports (`editor.N`): a changed import invalidates the whole window, as now.
- A crash: in the parse as today (crash token, colors cleared); in a query task `Internal error: ...` on that query's line, the other queries go on.

### Kept as is

- What a query sees: the facts and rules above it.
- Expected results, mismatch diagnostics, `micros`, hovers, the text edits on hints.
- Evaluation settings (random/reverse order, memo size, ...): every snapshot copies them from its parent.

## Risks the builder verifies first

1. Keeping results above the edit is only right if a declaration (type, functor, pattern) BELOW a query cannot change its result. The whole document's types are in the KB when queries run, today as well as in this design. A test appends declarations, facts and rules after a query and checks that its result stays the same. If it can change, every edit invalidates all queries; tell Tom.
2. `Query.evaluate` stores its result in fields of the node (`inferResult`, `inferMicros`). Check that two equal queries are distinct objects; if one object can be evaluated twice at once, serialise those.
3. Memoization: each snapshot has its own. Today consecutive queries without a fact or rule between them share it (every `addFact`/`addRule` resets it). Measure the sudoku example files (several puzzles, one solver) sequential vs parallel and report the difference.
4. Cancel latency: the deadline is checked only in `Predicate.inferRules`. Measure on a slow sudoku query how long a cancel takes (the 9x9 CSP spends most of its time in `nrOfUnbound`/`allVars`). If it takes more than about a second, add the same `isPastDeadline` check at the hot spots (one line each, no other engine change). A cancelled query keeps its slot of N until it has stopped.

## Testing

- Core: snapshots see only the facts and rules above (`QueryExecutionFlowTest` stays green); parallel results equal sequential ones (random order, `@RepeatedTest`); a cancel stops a long query quickly.
- LSP: the first hint arrives before a slow last query ends; an edit below a slow query keeps it running and keeps the results above; an edit above cancels it; `...calculating` appears after the delay and not on a fast recalculation; at most N queries run; per-query deadline; the overload tests of `EmbeddedServerTest` per query.
- e2e (`inlay-hints.spec.ts`): a slow query shows `...calculating`, then its result; a quick edit shows no `...calculating`.
- Swing: checked by hand.

## Decided by Tom

- `...calculating` as soon as a result is no longer valid, only for queries below the edit.
- A delay of 200..400 ms before `...calculating`, against flicker.
- Results shown one by one, without waiting for all.
- Parsing and coloring never wait for queries.
- A pass that parses and does the semantic analysis without evaluating queries; the queries in its result separately callable; the caller decides sequential or parallel, with or without a timeout.
- Parallel evaluation with a limit ("say 5").
- An edit above a query drops its queued work and stops its running work.
- Few changes in the core.
- A failing fact or rule: the LSP goes on like the Swing editor; its error stays a diagnostic on its line, the queries below it get results.

## Calls made by Claude

- Delay 300 ms, counted from when a result first became invalid.
- Debounce 100 ms (was 300).
- Refresh throttle 200 ms.
- Limit 5, in the LSP capped at the overload threshold (3 in tests, CI and e2e), configurable per workspace (system property `NELUMBO_MAX_PARALLEL_QUERIES`).
- Deadline and overload per query instead of per document.
- Parse once in the LSP (`NlDocument.of` runs `prepare`).
- `prepare` and the LSP request handlers run off the pool; only queries use it.
- A slow cancel may get extra one-line deadline checks in the engine.
- Tasks of kept queries go on across versions.
- During the delay: unchanged queries below the edit show their moved old result, the edited query shows nothing.
- `...calculating` styled as a result.
- The Swing window loop leaves the pool.

## Left to the builder

- Names and package of the new API.
- The backstop for a query that ignores its deadline (today `future.get` with deadline + 2 s).
- How the throttles and timers are built.
- Updating CLAUDE.md (LSP server, Swing editor).
