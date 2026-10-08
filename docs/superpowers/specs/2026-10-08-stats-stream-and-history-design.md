# Live stats stream and stats history - design

Date: 2026-10-08

## Goal

- `/status.html` updates live without refreshing.
- The page shows how the numbers developed over the last hour, day and week, in a chart.
- History survives container restarts and deploys.

## Context

- `GET /stats` (`ServerStats`) returns a snapshot:
  - sessions, evaluations, cpu, heap, uptime
  - status (ok / busy / overloaded)
- The snapshot is cached for 2 s.
- `/status.html` polls it every 5 s. The status dot on the other pages polls every 15 s.
- There is no history: `EvalGate` only keeps running totals since start-up.
- The production container restarts on every deploy.
  - `website/docker-compose.yml` lives in `/data/sites/nelumbo.nl` on server1.
  - It has no volume yet.
- Javalin 7.2.3 has built-in Server-Sent Events: `config.routes.sse(path, Consumer<SseClient>)`, with `SseClient.keepAlive/sendEvent/onClose`.
- The website frontend is an npm/esbuild project. Its `dist/` is shipped under `/assets/`.

## Decisions

- Live updates: Server-Sent Events. Not WebSocket (heavier than needed) and not faster polling.
- Chart: uPlot (npm `uplot` 1.6.32, about 45 KB), bundled by esbuild into its own small entry. No CDN.
- History is stored on disk, one JSON line per minute, on a Docker volume. Without a configured directory (local runs, tests) it stays in memory.
- The status dot on the other pages is unchanged.

## Design

### 1. Sampling (website module)

`StatsRecorder` owns a single-thread scheduler. Every 5 s it takes a sample:
- time (epoch ms)
- open LSP sessions
- `EvalGate.GLOBAL` `running`, `total` and `overloaded`
- process CPU load (0..1)
- used heap in MB

The sample uses the same sources as `ServerStats`. `ServerStats` and the recorder share one way of reading them, so `/stats` and the stream report the same numbers.

Each sample:
- is broadcast to the stream clients (section 3), as the same JSON object that `/stats` returns
- goes into the current minute bucket

When the minute rolls over, the bucket becomes a `MinuteStats` row:
- `t`: the minute start, epoch ms
- `sessionsMax`
- `runningMax`
- `cpuAvg`, `cpuMax` (0..1)
- `heapMaxMb`
- `evaluations` and `overloads`: increase of the gate totals during the minute. If a counter went down (restart), the last value counts as the increase.

The interval is a constructor parameter, so tests can use a short one.

### 2. Storage

`StatsHistory` keeps the `MinuteStats` rows of the last 7 days in memory, oldest first. That is up to 10,080 rows.

With a stats directory:
- Every finished minute is appended as one JSON line (Jackson, already on the website classpath) to `stats-YYYY-MM-DD.jsonl` (UTC date of the minute).
- At start-up, the files of the last 7 days are read back. A malformed line is skipped, so one bad write does not lose the file.
- Files whose date is more than 8 days old are deleted, at start-up and at each UTC day change.

The directory comes from a new CLI option `--stats-dir <path>` of the website `Main`. `NelumboHttpServer` gets it through a 5-arg constructor; the 4-arg constructor passes none, meaning in memory only.

Production:
- The Dockerfile `CMD` adds `--stats-dir /data/stats`.
- `docker-compose.yml` mounts `./stats:/data/stats`, which becomes `/data/sites/nelumbo.nl/stats` on server1. Docker creates it on the first run.

### 3. Endpoints

`GET /stats/stream` (SSE):
- Each sample is sent as event `stats` with the `/stats` JSON object.
- A new client first gets the latest sample immediately.
- At most 50 stream clients at once. A client over the cap gets one `error` event ("too many stream clients") and is closed.
- Closed clients are removed through `onClose`.

`GET /stats/history?range=hour|day|week`:
- Returns `{range, resolutionSeconds, points: [...]}`, where each point has the `MinuteStats` fields.
  - `hour`: the minute rows of the last 60 minutes (resolution 60)
  - `day`: the minute rows of the last 24 hours (resolution 60)
  - `week`: 10-minute aggregates of the last 7 days (resolution 600, up to 1,008 points). Per aggregate: max of the maxes, average of `cpuAvg`, sum of `evaluations` and `overloads`, `t` = start of the 10 minutes.
- Any other `range` gives 400.

### 4. `/status.html`

The number cells:
- update on every `stats` event of an `EventSource('/stats/stream')`
- fall back to the current 5 s polling of `/stats` when the stream errors, and return to the stream when it reconnects
- show `unreachable` (as now) when neither works

Below the numbers, a chart section:
- range buttons `hour` / `day` / `week`, with `hour` as the default
- two stacked uPlot charts sharing the time axis:
  1. CPU %: `cpuAvg` and `cpuMax`
  2. Activity: `sessionsMax`, `runningMax` and `overloads` per point. Counts, one y-axis.

  Two charts instead of one chart with two y-axes keeps percentages and counts apart.

Behaviour:
- The history is fetched on load, on a range change, and every 60 s.
- In the `hour` view, the latest live sample is drawn as an extra last point, so the lines reach "now" between minute rows.
- Colours come from the page's CSS variables, and the charts redraw when `data-theme` on `<html>` changes.
- The charts resize with the page. The layout keeps working at phone width: 16px gutter, no horizontal scroll.

The chart code is a new frontend entry:
- `src/status-chart.ts`, built by esbuild into `dist/status-chart.js` (global `NelumboStatus`) plus its css (uPlot's stylesheet)
- shipped and served like the Monaco bundle, under `/assets/`
- `status.html` loads only that bundle, not Monaco

## Testing

Unit (website):
- aggregation of samples into a minute row, including the counter-reset case
- the JSON-lines round trip in a temp directory: write, then read back by a new instance
- skipping a malformed line
- deleting files older than 8 days
- the week view's 10-minute aggregation

HTTP (`NelumboHttpServerTest`):
- the shape of `/stats/history` for all three ranges, and 400 for an unknown range
- `/stats/stream` delivers a first `stats` event, read with the JDK HttpClient as `text/event-stream`

e2e (Playwright):
- `/status.html` shows the two charts
- a live number (uptime) changes without a reload, within 15 s

Afterwards:
- deploy
- check that `/data/sites/nelumbo.nl/stats` fills with a file
- check that the history survives a container restart

## Out of scope

- Per-user or per-IP statistics.
- Alerting.
- History for the status dot.
- Stats of the REST `/eval` endpoints.
