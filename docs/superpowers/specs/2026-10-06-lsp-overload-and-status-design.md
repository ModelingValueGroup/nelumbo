# LSP overload signal and server status - design

Date: 2026-10-06

## Goal

- When CPU is scarce, heavy evaluations get a clear, machine-recognisable overload signal
  instead of silently getting slow.
- Light users keep working normally.
- The site shows the server's current load, unobtrusively.

## Context

- The load test (`--factorial 3000`) showed a handful of heavy clients saturating server1.
- The container is now capped at 16 cores / 16 GB (`website/docker-compose.yml`).
- All LSP sessions share one JVM, and with it one inference pool (`KnowledgeBase.POOL`).
- Every LSP evaluation already runs under a deadline (default 30 s):
  - `QueryResultCache.evaluate` sets it on a child knowledge base.
  - The engine checks it in `Predicate` and throws `NelumboTimeoutException`.
  - `QueryEvaluator` turns that into an ERROR result ("evaluation exceeded the deadline").
- Each session evaluates at most one document at a time (single-thread debounce scheduler).
  So the number of evaluations running in the JVM is a good measure of contention.

## Decisions

- Policy: "short budget when busy".
  - An evaluation that starts while the server is busy gets a short budget (default 2 s)
    instead of the normal deadline.
  - Light queries finish within it. Heavy ones are stopped with an overload result.
  - Not chosen: rejecting everything when busy (also hits light users), and aborting the
    longest-running evaluations mid-flight (needs an engine change).
- Client signal: a diagnostic with code `server-overloaded` (shown by every LSP editor) and,
  on the website, a banner.
- Status display: a small coloured dot next to the version on every page, linking to a
  `/status.html` page with the numbers.

## Design

### 1. EvalGate (lsp server, new)

- `org.modelingvalue.nelumbo.lsp.EvalGate`: one JVM-wide instance (`EvalGate.GLOBAL`), shared
  by all sessions.
- State:
  - `running`: evaluations in progress
  - `total`: evaluations started since start-up
  - `overloaded`: evaluations stopped by the short budget
- `enter()` increments `running` and `total`, and returns whether the server was busy at
  that moment: `running` before this one >= `threshold`.
- `exit()` decrements `running`.
- `recordOverload()` increments `overloaded`.
- Settings (system properties, in the style of `PARALLELISM`/`POOL_SIZE`):
  - `NELUMBO_OVERLOAD_THRESHOLD`: default `Collection.PARALLELISM`, the width of the shared
    inference pool (container-aware cpus - 1, so 15 on server1). From there on evaluations
    queue for a pool worker.
  - `NELUMBO_OVERLOAD_BUDGET_MS`: default 2000; values below 1 are raised to 1 (never unlimited).
- Tests use a constructor with explicit values, not the static instance.

### 2. Short budget (QueryResultCache)

- `evaluate(uri)` calls `gate.enter()` before evaluating and `gate.exit()` in a `finally`.
- Busy: `deadlineMs = min(budgetMs, workspace deadline)`, where a workspace deadline of
  0 = unlimited counts as infinite. Not busy: the workspace deadline as today.
- The budget counts from when the evaluation starts running on a pool worker, not from when
  it is queued (busy means the pool is full, so the queue wait is not the evaluation's cost).
  `QueryEvaluator` sets it at the start of the `invoke` runnable. The normal deadline is
  unchanged (it includes the queue wait).
- `QueryEvaluator.evaluate` gets a `boolean overloadBudget` parameter (the existing overloads
  pass `false`).
- A `NelumboTimeoutException` under `overloadBudget` yields
  `QueryResult.overloaded(budgetMs)` instead of `QueryResult.error(...)`, and the cache calls
  `gate.recordOverload()`.
- Under the short budget, a timeout before any query was reached gets a document-level
  `server-overloaded` diagnostic at 0:0. Two cases:
  - the backstop timeout (workspace deadline + 2 s, also under the budget; no timeout when
    the workspace deadline is 0)
  - a timeout while parsing: `QueryEvaluator` rethrows it under `overloadBudget` instead of
    returning empty results
- Under the normal deadline these cases stay as today (hints silently empty; out of scope).
- Retry: after an overload (an OVERLOADED result or the 0:0 diagnostic) the cache schedules
  the document again after 5 s in its `pending` slot (an edit supersedes it). If the gate
  is still busy then, it waits another 5 s; otherwise it evaluates as usual. So the marker
  and the banner clear without an edit.
- The gate lives on `Workspace` (`getEvalGate()`/`setEvalGate()`, default
  `EvalGate.GLOBAL`), like the eval deadline, so tests can give one server its own gate.

### 3. Overload result and diagnostic (lsp server)

- `QueryResult.Kind.OVERLOADED`, factory `overloaded(long budgetMs)`.
  - Message: `Server busy: evaluation stopped after <budget> ms to protect other users - try
    again in a moment`.
  - `inlineLabel()`: `⚠ server busy`.
  - `tooltip()`: `⚠ <message>`.
  - Inlay hint kind: `Parameter`, the same red style as ERROR and MISMATCH.
- `QueryResultCache` adds a diagnostic per OVERLOADED query:
  - Severity Warning, source `nelumbo`, code `server-overloaded`, the message above.
  - Range: the whole query (first to last token).
- The diagnostic code is the contract for clients. It goes in a constant
  `QueryResult.OVERLOAD_CODE = "server-overloaded"`.

### 4. Website banner (frontend)

- `nelumbo-fields.ts` subscribes to `monaco.editor.onDidChangeMarkers`.
- A marker whose `code` (string, or `{value}`) is `server-overloaded` on any model shows an
  overload banner: "The server is busy - an evaluation was stopped. Try again in a moment."
- It hides again when no such marker remains, e.g. after the next successful evaluation.
- The banner is a second fixed bottom strip in the existing `.nelumbo-lsp-banner` style. It
  must stay `position: fixed` (see the tour's flex-row body note in CLAUDE.md).

### 5. Stats endpoint and status UI (website)

- `GET /stats` (JSON):
  - `sessions`: `{open, max}`, from `LspWebSocket` (needs a `sessionCount()` getter).
  - `evaluations`: `{running, threshold, total, overloaded}`, from `EvalGate.GLOBAL`.
  - `cpu`: `{load, cpus}`. `load` is the process CPU load 0..1 relative to the JVM's
    processors (`com.sun.management.OperatingSystemMXBean.getProcessCpuLoad`); `cpus` is
    `availableProcessors`.
  - `heap`: `{usedMb, maxMb}`.
  - `uptimeSeconds`.
  - `status`: `overloaded` if `running >= threshold`; else `busy` if `load >= 0.7` or
    `running >= threshold / 2`; else `ok`.
- Status dot:
  - Script `status-dot.js` in `public/`, served at `/status-dot.js` by an explicit route
    (like `/favicon.svg`).
  - Every page that shows the version (landing, tour, playground, docs) gets
    `<a class="status-dot" href="/status.html"></a>` next to it and loads the script.
  - The script fetches `/stats` now and every 15 s.
  - It sets a class `ok` / `busy` / `overloaded` (green / orange / red) and the tooltip
    `<open> sessions, CPU <n>%`.
  - It does not refresh while the tab is hidden.
- `/status.html` (`public/status.html`, explicit route):
  - A small page in the site's style (fonts, palette, version footer).
  - Shows all `/stats` numbers and refreshes every 5 s.
  - No Monaco bundle.

## Testing

- Unit (lsp server):
  - `EvalGate`: counting, the busy flag at the threshold, overload counter.
  - `QueryEvaluator`: with factorial declared in a seeded base KB (so parsing is instant), a
    `factorial(3000)` query under a 300 ms budget gives OVERLOADED with
    `overloadBudget = true`, and the ERROR deadline result with `false`.
  - `QueryResultTest`: OVERLOADED label, tooltip and kind.
- Integration (lsp server, `EmbeddedServerTest` style with `RecordingClient`):
  - A server (seeded base KB) whose gate has threshold 0 (always busy) and a 300 ms budget.
  - Opening a factorial document publishes a diagnostic with code `server-overloaded`.
- Website (`NelumboHttpServerTest`):
  - `/stats` returns the documented shape.
  - `/status.html` and `/status-dot.js` are served.
  - Every page with a version links `/status.html`.
- e2e (Playwright):
  - The status dot exists and gets a status class.
  - `/status.html` shows numbers.
  - A second test server with `-DNELUMBO_OVERLOAD_THRESHOLD=0 -DNELUMBO_OVERLOAD_BUDGET_MS=300`:
    typing a factorial document in the playground shows the overload banner.
- Afterwards: the factorial load test against the local capped container. Expect overload
  diagnostics instead of only slow results once busy, and no errors for light tour edits.

## Out of scope

- An evaluation that started before the server got busy keeps its full deadline.
- The REST `/eval` endpoints (not used by the pages).
- The silent empty hints after a backstop timeout under the normal deadline.
- Per-user fairness beyond the short budget.
