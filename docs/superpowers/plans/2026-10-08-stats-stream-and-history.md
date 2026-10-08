# Live Stats Stream and Stats History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `/status.html` updates live over Server-Sent Events and shows the last hour/day/week of server stats in two uPlot charts. History is stored per minute on disk, so it survives deploys.

**Architecture:**
- A `StatsRecorder` in the website module samples the server every 5 s.
- Every sample is broadcast to `GET /stats/stream` clients and becomes the `/stats` answer.
- Samples are aggregated into `MinuteStats` rows in a `StatsHistory`. That history keeps 7 days in memory, appends each minute as a JSON line to a daily file when `--stats-dir` is set, and serves `GET /stats/history?range=hour|day|week`.
- A new esbuild entry (`status-chart.ts`, uPlot) drives the status page.

**Tech Stack:** Java 21, Javalin 7.2.3 (built-in SSE), Jackson (already on the website classpath), JUnit 5, TypeScript + esbuild, uPlot 1.6.32, Playwright.

**Spec:** `docs/superpowers/specs/2026-10-08-stats-stream-and-history-design.md`

## Global Constraints

**Timing and retention**
- Sample interval: 5000 ms. Minute rows: one per UTC minute.
- History kept in memory: 7 days. Daily files `stats-YYYY-MM-DD.jsonl` (UTC date); files more than 8 days old are deleted.

**`/stats/history` ranges**
- `hour`: last 60 min, resolution 60 s
- `day`: last 24 h, resolution 60 s
- `week`: last 7 days, resolution 600 s (10-minute aggregates)
- Any other value: HTTP 400.

**Fields**
- `MinuteStats`: `t` (minute start, epoch ms), `sessionsMax`, `runningMax`, `cpuAvg`, `cpuMax` (0..1), `heapMaxMb`, `evaluations`, `overloads`.
  - `evaluations` and `overloads` are the counter increase within the minute. A counter that went down means a restart; the new value then counts as the increase.
- Week aggregate: max of the maxes, average of `cpuAvg`, sum of `evaluations` and `overloads`, `t` = 10-minute start.

**Stream**
- `GET /stats/stream`: event name `stats`, data = the `/stats` JSON object.
- A new client gets the latest sample immediately.
- Cap: 50 clients. Over the cap: one `error` event `too many stream clients`, then close.

**Responses and options**
- `/stats` keeps its JSON shape and adds `time` (sample time, epoch ms).
- CLI option `--stats-dir <path>`. Without it, history stays in memory only. Production: `--stats-dir /data/stats` on a compose volume `./stats:/data/stats`.

**Chart colours** (validated with the dataviz validator, `--pairs all`; light on `#fbfafc`, dark on `#23262e`)
- CPU chart: one entity, violet `#4a3aa7` (light) / `#9085e9` (dark). `cpuAvg` solid 2px, `cpuMax` dashed 1px.
- Activity chart:
  - sessions: blue `#2a78d6` / `#3987e5`
  - running: orange `#eb6834` / `#d95926`
  - overloads: aqua `#1baf7a` / `#199e70`
- Light aqua is below 3:1 contrast, so a data-table view is required (relief rule).
- Two charts; never a dual y-axis.

**Code style**
- Java:
  - New `.java` files start with the LGPL header (lines 1-15 of an existing website source file, for example `ServerStats.java`), then a blank line.
  - Variable declarations aligned in columns.
  - Arrow-form switches in new code. The existing colon switch in website `Main` stays colon-style; add the new case in that style.
  - Comments only where logic is unclear.
- TypeScript: type every variable and argument; one variable per declaration; `if`/`for` always multi-line with braces; aligned declarations.
- ASCII only in new text.
- The site has a light/dark theme: `public/theme.js` sets `data-theme` (`light`/`dark`) on `<html>`. Page CSS keys off `[data-theme="light"]`; `status.html`'s `:root` holds the dark values.

**Process**
- Never `git push`. Commit on branch `local/stats-history` only.
- Run focused tests while iterating, and the module suite once before each commit.

## Review Focus

- **Overloaded or slow stream clients must not stall sampling or `/stats`.** Broadcast happens outside the recorder's lock, and a failing `sendEvent` removes that client. Pinned in Task 3 (`streamDropsAClientThatFails`).
- **A container restart resets the gate counters.** The first minute after a restart must not show a huge negative or positive `evaluations` value. Pinned in Task 1 (`counterResetCountsTheNewValue`).
- **A half-written last line of a history file must not lose that day.** Pinned in Task 1 (`malformedLineIsSkipped`).
- **The status page must keep showing numbers when the stream is unavailable** (proxy timeout, cap reached). It falls back to polling. Pinned in Task 4 (e2e: the numbers render, plus the fallback code path; the cap path is covered in Task 3).
- **The charts must stay readable at phone width and in both themes.** Pinned in Task 4 (`status.spec.ts` at 390px width, and the theme redraw).

---

### Task 1: Stats data model and history store

**Files:**
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/StatsSample.java`
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/MinuteStats.java`
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/StatsHistory.java`
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/StatsRecorder.java` (aggregation only, scheduler in Task 2)
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/StatsHistoryTest.java`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/StatsRecorderTest.java`

**Interfaces (produced):**
- `record StatsSample(long time, int sessions, int running, long total, long overloaded, double cpuLoad, long heapUsedMb, long uptimeSeconds)`
- `record MinuteStats(long t, int sessionsMax, int runningMax, double cpuAvg, double cpuMax, long heapMaxMb, long evaluations, long overloads)`
- `StatsHistory`:
  - `StatsHistory(Path dir, LongSupplier clock)`, where `dir` may be null
  - `void add(MinuteStats row)`
  - `List<MinuteStats> rows(Range range)`
  - `static List<MinuteStats> aggregate(List<MinuteStats> rows, long bucketMs)`
  - `enum Range { HOUR, DAY, WEEK; long spanMs(); int resolutionSeconds(); static Range parse(String) }`. `parse` throws `IllegalArgumentException` on an unknown value.
- `StatsRecorder`:
  - `StatsRecorder(StatsHistory history, Supplier<StatsSample> source, Consumer<StatsSample> listener)`
  - `synchronized void record(StatsSample sample)`
  - `StatsSample latest()`
  - `static long increase(long from, long to)`

- [ ] **Step 1: Write the failing tests**

`StatsHistoryTest.java` (LGPL header first):

```java
package org.modelingvalue.nelumbo.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StatsHistoryTest {
    private static final long NOW = 1_800_000_000_000L - 1_800_000_000_000L % 60_000; // a whole minute

    private static MinuteStats row(long t, int sessions, double cpu, long evaluations) {
        return new MinuteStats(t, sessions, sessions, cpu, cpu, 100, evaluations, 0);
    }

    @Test
    void rangesSelectTheirSpan() {
        StatsHistory history = new StatsHistory(null, () -> NOW);
        history.add(row(NOW - 2 * 86_400_000L, 1, 0.1, 1));
        history.add(row(NOW - 2 * 3_600_000L, 2, 0.2, 2));
        history.add(row(NOW - 60_000, 3, 0.3, 3));
        assertEquals(1, history.rows(StatsHistory.Range.HOUR).size());
        assertEquals(2, history.rows(StatsHistory.Range.DAY).size());
        assertEquals(3, history.rows(StatsHistory.Range.WEEK).size(), "three different 10-minute buckets");
    }

    @Test
    void rowsOlderThanAWeekAreDropped() {
        StatsHistory history = new StatsHistory(null, () -> NOW);
        history.add(row(NOW - 8 * 86_400_000L, 1, 0.1, 1));
        history.add(row(NOW - 60_000, 1, 0.1, 1));
        assertEquals(1, history.rows(StatsHistory.Range.WEEK).size());
    }

    @Test
    void weekAggregatesTenMinuteBuckets() {
        List<MinuteStats> rows = List.of(
                new MinuteStats(NOW, 2, 1, 0.2, 0.5, 100, 10, 1),
                new MinuteStats(NOW + 60_000, 5, 3, 0.4, 0.9, 300, 20, 0),
                new MinuteStats(NOW + 600_000, 1, 1, 0.1, 0.1, 50, 1, 0));
        List<MinuteStats> week = StatsHistory.aggregate(rows, 600_000);
        assertEquals(2, week.size());
        MinuteStats first = week.get(0);
        assertEquals(NOW - NOW % 600_000, first.t());
        assertEquals(5, first.sessionsMax());
        assertEquals(3, first.runningMax());
        assertEquals(0.3, first.cpuAvg(), 1e-9);
        assertEquals(0.9, first.cpuMax(), 1e-9);
        assertEquals(300, first.heapMaxMb());
        assertEquals(30, first.evaluations());
        assertEquals(1, first.overloads());
    }

    @Test
    void rowsSurviveARestartThroughTheStatsDirectory(@TempDir Path dir) {
        new StatsHistory(dir, () -> NOW).add(row(NOW - 60_000, 7, 0.5, 42));
        List<MinuteStats> reloaded = new StatsHistory(dir, () -> NOW).rows(StatsHistory.Range.HOUR);
        assertEquals(List.of(row(NOW - 60_000, 7, 0.5, 42)), reloaded);
    }

    @Test
    void malformedLineIsSkipped(@TempDir Path dir) throws Exception {
        new StatsHistory(dir, () -> NOW).add(row(NOW - 60_000, 7, 0.5, 42));
        Path file = dir.resolve("stats-" + LocalDate.ofInstant(java.time.Instant.ofEpochMilli(NOW - 60_000), ZoneOffset.UTC) + ".jsonl");
        Files.writeString(file, "{\"t\":12", java.nio.file.StandardOpenOption.APPEND);
        assertEquals(1, new StatsHistory(dir, () -> NOW).rows(StatsHistory.Range.HOUR).size());
    }

    @Test
    void filesOlderThanEightDaysAreDeleted(@TempDir Path dir) throws Exception {
        LocalDate today = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(NOW), ZoneOffset.UTC);
        Path      old   = dir.resolve("stats-" + today.minusDays(9) + ".jsonl");
        Path      kept  = dir.resolve("stats-" + today.minusDays(3) + ".jsonl");
        Files.writeString(old, "");
        Files.writeString(kept, "");
        new StatsHistory(dir, () -> NOW);
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(kept));
    }

    @Test
    void rangeParsing() {
        assertEquals(StatsHistory.Range.WEEK, StatsHistory.Range.parse("week"));
        assertEquals(600, StatsHistory.Range.WEEK.resolutionSeconds());
        assertEquals(60, StatsHistory.Range.DAY.resolutionSeconds());
        assertThrows(IllegalArgumentException.class, () -> StatsHistory.Range.parse("month"));
    }
}
```

`StatsRecorderTest.java` (LGPL header first):

```java
package org.modelingvalue.nelumbo.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class StatsRecorderTest {
    private static final long MINUTE = 1_800_000_000_000L - 1_800_000_000_000L % 60_000;

    private static StatsSample sample(long time, int sessions, int running, long total, long overloaded, double cpu, long heap) {
        return new StatsSample(time, sessions, running, total, overloaded, cpu, heap, 0);
    }

    @Test
    void aMinuteRollsOverIntoOneRow() {
        StatsHistory      history  = new StatsHistory(null, () -> MINUTE + 120_000);
        List<StatsSample> heard    = new ArrayList<>();
        StatsRecorder     recorder = new StatsRecorder(history, () -> null, heard::add);
        recorder.record(sample(MINUTE + 5_000, 2, 1, 100, 3, 0.2, 200));
        recorder.record(sample(MINUTE + 30_000, 4, 3, 110, 5, 0.6, 300));
        recorder.record(sample(MINUTE + 65_000, 1, 0, 115, 5, 0.1, 150));
        List<MinuteStats> rows = history.rows(StatsHistory.Range.HOUR);
        assertEquals(1, rows.size(), "the second minute is still open");
        assertEquals(new MinuteStats(MINUTE, 4, 3, 0.4, 0.6, 300, 10, 2), rows.get(0));
        assertEquals(MINUTE + 65_000, recorder.latest().time());
        assertEquals(0, heard.size(), "record() does not broadcast; sampleNow() does");
    }

    @Test
    void nextMinuteCountsFromThePreviousMinutesLastValue() {
        StatsHistory  history  = new StatsHistory(null, () -> MINUTE + 180_000);
        StatsRecorder recorder = new StatsRecorder(history, () -> null, s -> {
        });
        recorder.record(sample(MINUTE + 5_000, 1, 0, 100, 0, 0.1, 100));
        recorder.record(sample(MINUTE + 65_000, 1, 0, 130, 1, 0.1, 100));
        recorder.record(sample(MINUTE + 125_000, 1, 0, 131, 1, 0.1, 100));
        assertEquals(30, history.rows(StatsHistory.Range.HOUR).get(1).evaluations());
        assertEquals(1, history.rows(StatsHistory.Range.HOUR).get(1).overloads());
    }

    @Test
    void counterResetCountsTheNewValue() {
        assertEquals(10, StatsRecorder.increase(100, 110));
        assertEquals(5, StatsRecorder.increase(110, 5));
    }

    @Test
    void sampleNowBroadcastsTheSample() {
        StatsSample       s        = sample(MINUTE + 5_000, 1, 0, 1, 0, 0.1, 100);
        List<StatsSample> heard    = new ArrayList<>();
        StatsRecorder     recorder = new StatsRecorder(new StatsHistory(null, () -> MINUTE), () -> s, heard::add);
        recorder.sampleNow();
        assertSame(s, heard.get(0));
        assertSame(s, recorder.latest());
    }
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.StatsHistoryTest" --tests "org.modelingvalue.nelumbo.website.StatsRecorderTest"`
Expected: compilation FAILS (`StatsSample`, `MinuteStats`, `StatsHistory` and `StatsRecorder` do not exist).

- [ ] **Step 3: Implement**

`StatsSample.java`:

```java
package org.modelingvalue.nelumbo.website;

/** One reading of the server's numbers: LSP sessions, the shared evaluation gate, CPU and heap. */
record StatsSample(long time, int sessions, int running, long total, long overloaded, double cpuLoad, long heapUsedMb, long uptimeSeconds) {
}
```

`MinuteStats.java`:

```java
package org.modelingvalue.nelumbo.website;

/**
 * The samples of one UTC minute (or, aggregated, of a longer bucket): maxima, the average CPU load, and how much the
 * evaluation and overload counters grew. {@code t} is the bucket start in epoch milliseconds.
 */
record MinuteStats(long t, int sessionsMax, int runningMax, double cpuAvg, double cpuMax, long heapMaxMb, long evaluations, long overloads) {
}
```

`StatsHistory.java`:

```java
package org.modelingvalue.nelumbo.website;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The last week of {@link MinuteStats}, oldest first. With a directory, every row is also appended as one JSON line
 * to {@code stats-YYYY-MM-DD.jsonl} (UTC) and the last week is read back at start-up, so history survives restarts.
 */
final class StatsHistory {
    static final long KEEP_MS        = 7 * 86_400_000L;
    static final int  KEEP_FILE_DAYS = 8;

    enum Range {
        HOUR(3_600_000L, 60),
        DAY(86_400_000L, 60),
        WEEK(KEEP_MS, 600);

        private final long spanMs;
        private final int  resolutionSeconds;

        Range(long spanMs, int resolutionSeconds) {
            this.spanMs            = spanMs;
            this.resolutionSeconds = resolutionSeconds;
        }

        long spanMs() {
            return spanMs;
        }

        int resolutionSeconds() {
            return resolutionSeconds;
        }

        static Range parse(String name) {
            return valueOf(name == null ? "" : name.toUpperCase(Locale.ROOT));
        }
    }

    private final Path                     dir;
    private final LongSupplier             clock;
    private final ObjectMapper             mapper = new ObjectMapper();
    private final ArrayDeque<MinuteStats>  rows   = new ArrayDeque<>();
    private LocalDate                      prunedDay;

    StatsHistory(Path dir, LongSupplier clock) {
        this.dir   = dir;
        this.clock = clock;
        if (dir != null) {
            prune(today());
            load();
        }
    }

    synchronized void add(MinuteStats row) {
        rows.addLast(row);
        long cutoff = clock.getAsLong() - KEEP_MS;
        while (!rows.isEmpty() && rows.peekFirst().t() < cutoff) {
            rows.removeFirst();
        }
        if (dir != null) {
            append(row);
            if (!today().equals(prunedDay)) {
                prune(today());
            }
        }
    }

    synchronized List<MinuteStats> rows(Range range) {
        long              from     = clock.getAsLong() - range.spanMs();
        List<MinuteStats> selected = rows.stream().filter(r -> r.t() >= from).toList();
        return range == Range.WEEK ? aggregate(selected, range.resolutionSeconds() * 1000L) : selected;
    }

    static List<MinuteStats> aggregate(List<MinuteStats> rows, long bucketMs) {
        List<MinuteStats> result = new ArrayList<>();
        List<MinuteStats> bucket = new ArrayList<>();
        long              start  = -1;
        for (MinuteStats row : rows) {
            long rowStart = row.t() - row.t() % bucketMs;
            if (rowStart != start && !bucket.isEmpty()) {
                result.add(merge(start, bucket));
                bucket.clear();
            }
            start = rowStart;
            bucket.add(row);
        }
        if (!bucket.isEmpty()) {
            result.add(merge(start, bucket));
        }
        return result;
    }

    private static MinuteStats merge(long start, List<MinuteStats> bucket) {
        return new MinuteStats(start,
                bucket.stream().mapToInt(MinuteStats::sessionsMax).max().orElse(0),
                bucket.stream().mapToInt(MinuteStats::runningMax).max().orElse(0),
                bucket.stream().mapToDouble(MinuteStats::cpuAvg).average().orElse(0),
                bucket.stream().mapToDouble(MinuteStats::cpuMax).max().orElse(0),
                bucket.stream().mapToLong(MinuteStats::heapMaxMb).max().orElse(0),
                bucket.stream().mapToLong(MinuteStats::evaluations).sum(),
                bucket.stream().mapToLong(MinuteStats::overloads).sum());
    }

    private void append(MinuteStats row) {
        try {
            Files.createDirectories(dir);
            Files.writeString(file(LocalDate.ofInstant(Instant.ofEpochMilli(row.t()), ZoneOffset.UTC)), mapper.writeValueAsString(row) + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("stats history: cannot append to " + dir + ": " + e);
        }
    }

    private void load() {
        long              cutoff = clock.getAsLong() - KEEP_MS;
        LocalDate         first  = today().minusDays(7);
        List<MinuteStats> loaded = new ArrayList<>();
        for (int i = 0; i <= 7; i++) {
            Path file = file(first.plusDays(i));
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    try {
                        MinuteStats row = mapper.readValue(line, MinuteStats.class);
                        if (row.t() >= cutoff) {
                            loaded.add(row);
                        }
                    } catch (IOException malformed) {
                        // a line cut off by a crash or a full disk; the rest of the file is still good
                    }
                }
            } catch (IOException e) {
                System.err.println("stats history: cannot read " + file + ": " + e);
            }
        }
        loaded.sort(Comparator.comparingLong(MinuteStats::t));
        rows.addAll(loaded);
    }

    private void prune(LocalDate today) {
        prunedDay = today;
        LocalDate oldest = today.minusDays(KEEP_FILE_DAYS);
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(f -> f.getFileName().toString().matches("stats-\\d{4}-\\d{2}-\\d{2}\\.jsonl"))//
                 .filter(f -> LocalDate.parse(f.getFileName().toString().substring(6, 16)).isBefore(oldest))//
                 .forEach(f -> {
                     try {
                         Files.deleteIfExists(f);
                     } catch (IOException e) {
                         System.err.println("stats history: cannot delete " + f + ": " + e);
                     }
                 });
        } catch (IOException e) {
            // the directory does not exist yet; the first append creates it
        }
    }

    private Path file(LocalDate date) {
        return dir.resolve("stats-" + date + ".jsonl");
    }

    private LocalDate today() {
        return LocalDate.ofInstant(Instant.ofEpochMilli(clock.getAsLong()), ZoneOffset.UTC);
    }
}
```

The filter `isBefore(today.minusDays(8))` deletes files more than 8 days old. The test creates a file 9 days old (deleted) and one 3 days old (kept).

`StatsRecorder.java` (the scheduler is added in Task 2):

```java
package org.modelingvalue.nelumbo.website;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Turns the 5-second samples into {@link MinuteStats} rows of a {@link StatsHistory} and hands every sample to a
 * listener (the live stream). The listener is called outside the lock, so a slow stream client cannot stall sampling.
 */
final class StatsRecorder {
    private final StatsHistory          history;
    private final Supplier<StatsSample> source;
    private final Consumer<StatsSample> listener;
    private final List<StatsSample>     bucket         = new ArrayList<>();
    private long                        bucketMinute   = -1;
    private long                        baseTotal      = -1;
    private long                        baseOverloaded = -1;
    private volatile StatsSample        latest;

    StatsRecorder(StatsHistory history, Supplier<StatsSample> source, Consumer<StatsSample> listener) {
        this.history  = history;
        this.source   = source;
        this.listener = listener;
    }

    StatsSample latest() {
        return latest;
    }

    void sampleNow() {
        StatsSample sample = source.get();
        record(sample);
        listener.accept(sample);
    }

    synchronized void record(StatsSample sample) {
        long minute = sample.time() - sample.time() % 60_000;
        if (bucketMinute >= 0 && minute != bucketMinute) {
            history.add(finishMinute());
        }
        bucketMinute = minute;
        bucket.add(sample);
        latest = sample;
    }

    private MinuteStats finishMinute() {
        StatsSample first = bucket.get(0);
        StatsSample last  = bucket.get(bucket.size() - 1);
        MinuteStats row   = new MinuteStats(bucketMinute,
                bucket.stream().mapToInt(StatsSample::sessions).max().orElse(0),
                bucket.stream().mapToInt(StatsSample::running).max().orElse(0),
                bucket.stream().mapToDouble(StatsSample::cpuLoad).average().orElse(0),
                bucket.stream().mapToDouble(StatsSample::cpuLoad).max().orElse(0),
                bucket.stream().mapToLong(StatsSample::heapUsedMb).max().orElse(0),
                increase(baseTotal >= 0 ? baseTotal : first.total(), last.total()),
                increase(baseOverloaded >= 0 ? baseOverloaded : first.overloaded(), last.overloaded()));
        baseTotal      = last.total();
        baseOverloaded = last.overloaded();
        bucket.clear();
        return row;
    }

    // the counters restart at 0 with the JVM, so a drop means a restart and the new value is the increase
    static long increase(long from, long to) {
        return to >= from ? to - from : to;
    }
}
```

The test value `cpuAvg` 0.4 is the average of 0.2 and 0.6, and `evaluations` 10 is 110 - 100 (the first sample is the base when there is no previous minute).

- [ ] **Step 4: Run the tests to verify they pass**

Run: the same command as in Step 2.
Expected: PASS (11 tests).

- [ ] **Step 5: Commit**

```bash
git add website/src/main/java/org/modelingvalue/nelumbo/website/StatsSample.java website/src/main/java/org/modelingvalue/nelumbo/website/MinuteStats.java website/src/main/java/org/modelingvalue/nelumbo/website/StatsHistory.java website/src/main/java/org/modelingvalue/nelumbo/website/StatsRecorder.java website/src/test/java/org/modelingvalue/nelumbo/website/StatsHistoryTest.java website/src/test/java/org/modelingvalue/nelumbo/website/StatsRecorderTest.java
git commit -m "website: per-minute stats history with a JSON-lines store

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Recorder in the server, /stats from it, /stats/history, --stats-dir

**Files:**
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/ServerStats.java` (whole file body)
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/StatsRecorder.java` (scheduler)
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java` (constructors, `start`, `stop`)
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/Main.java` (option, usage)
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java`

**Interfaces:**
- Consumes everything from Task 1.
- Produces:
  - `ServerStats.read(int openSessions, EvalGate gate) -> StatsSample`
  - `ServerStats.json(StatsSample sample, int maxSessions, EvalGate gate) -> Map<String, Object>`. It is the `/stats` shape plus `"time"`.
  - `StatsRecorder.start()` / `close()`; `StatsRecorder.INTERVAL_MS = 5000`
  - `NelumboHttpServer(KnowledgeBase, List<String>, long, int, Path statsDir)`
  - `GET /stats/history`
  - In `NelumboHttpServer`: a private `ObjectMapper` and a `String statsJson(StatsSample)` helper (Task 3 uses it for the stream).

- [ ] **Step 1: Write the failing tests**

Append to `NelumboHttpServerTest.java` (inside the class):

```java
    @Test
    void statsCarryTheSampleTime() throws Exception {
        JsonNode stats = mapper.readTree(get("/stats").body());
        assertTrue(stats.get("time").asLong() > 1_700_000_000_000L, "time is the sample time in epoch ms");
    }

    @Test
    void historyHasTheDocumentedShape() throws Exception {
        for (String range : List.of("hour", "day", "week")) {
            HttpResponse<String> response = get("/stats/history?range=" + range);
            assertEquals(200, response.statusCode(), range);
            JsonNode history = mapper.readTree(response.body());
            assertEquals(range, history.get("range").asText());
            assertEquals(range.equals("week") ? 600 : 60, history.get("resolutionSeconds").asInt());
            assertTrue(history.get("points").isArray(), range + " points");
        }
        assertEquals(400, get("/stats/history?range=month").statusCode());
    }
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.NelumboHttpServerTest"`
Expected: FAIL. `time` is missing, and `/stats/history` gives 404.

- [ ] **Step 3: Rewrite ServerStats around StatsSample**

Replace the class body of `ServerStats.java` (keep the header and imports, and add `import java.util.Map;`, which is already there) with:

```java
/** Reads the server's numbers and renders them as the /stats JSON object. */
final class ServerStats {
    static final double BUSY_CPU_LOAD = 0.7;

    private ServerStats() {
    }

    // the process CPU load covers the time since the previous read, so only the StatsRecorder reads it
    static StatsSample read(int openSessions, EvalGate gate) {
        OperatingSystemMXBean os      = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Runtime               runtime = Runtime.getRuntime();
        return new StatsSample(System.currentTimeMillis(), openSessions, gate.running(), gate.total(), gate.overloaded(),
                cpuLoad(os.getProcessCpuLoad()), (runtime.totalMemory() - runtime.freeMemory()) >> 20,
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
    }

    static Map<String, Object> json(StatsSample sample, int maxSessions, EvalGate gate) {
        Runtime             runtime = Runtime.getRuntime();
        Map<String, Object> stats   = new LinkedHashMap<>();
        stats.put("status", status(sample.running(), gate.threshold(), sample.cpuLoad()));
        stats.put("time", sample.time());
        stats.put("sessions", Map.of("open", sample.sessions(), "max", maxSessions));
        stats.put("evaluations", Map.of("running", sample.running(), "threshold", gate.threshold(), "total", sample.total(), "overloaded", sample.overloaded()));
        stats.put("cpu", Map.of("load", sample.cpuLoad(), "cpus", runtime.availableProcessors()));
        stats.put("heap", Map.of("usedMb", sample.heapUsedMb(), "maxMb", runtime.maxMemory() >> 20));
        stats.put("uptimeSeconds", sample.uptimeSeconds());
        return stats;
    }
```

Keep `status(...)` and `cpuLoad(...)` unchanged below it. The 2 s snapshot cache (`CACHE_MS`, `cached`, `snapshot`) is removed: every sample now comes from the single `StatsRecorder`, which reads every 5 s. That also removes the shared-CPU-window noise the cache worked around.

- [ ] **Step 4: Give StatsRecorder a scheduler**

In `StatsRecorder.java`:
- add `import java.util.concurrent.Executors;`, `import java.util.concurrent.ScheduledExecutorService;` and `import java.util.concurrent.TimeUnit;`
- add `implements AutoCloseable` to the class
- add these members:

```java
    static final long INTERVAL_MS = 5000;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nelumbo-stats");
        t.setDaemon(true);
        return t;
    });

    void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sampleNow();
            } catch (RuntimeException e) {
                System.err.println("stats sample failed: " + e);
            }
        }, 0, INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
```

- [ ] **Step 5: Wire it into NelumboHttpServer**

- Add the field `private final Path statsDir;`.
- Add the field `private StatsRecorder recorder;`, next to `private Javalin app;`.
- Add the field `private final ObjectMapper statsMapper = new ObjectMapper();`.
- Imports: `java.nio.file.Path`, `com.fasterxml.jackson.databind.ObjectMapper` and `com.fasterxml.jackson.core.JsonProcessingException`.
- The 4-arg constructor delegates to a new 5-arg one with `null`:

```java
    /** {@code maxLspSessions} caps the number of concurrent LSP WebSocket sessions. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions) {
        this(baseKb, loadedFiles, timeoutMs, maxLspSessions, null);
    }

    /** {@code statsDir} keeps the per-minute stats history across restarts; null keeps it in memory only. */
    public NelumboHttpServer(KnowledgeBase baseKb, List<String> loadedFiles, long timeoutMs, int maxLspSessions, Path statsDir) {
        this.service        = new EvalService(baseKb, loadedFiles, timeoutMs);
        this.baseKb         = baseKb;
        this.timeoutMs      = timeoutMs;
        this.maxLspSessions = maxLspSessions;
        this.statsDir       = statsDir;
    }
```

- In `start`:
  - Replace the line `ServerStats stats = new ServerStats();` with:

```java
        StatsHistory history = new StatsHistory(statsDir, System::currentTimeMillis);
        recorder             = new StatsRecorder(history, () -> ServerStats.read(lsp.sessionCount(), EvalGate.GLOBAL), sample -> {
        });
        recorder.start();
```

  - Replace the `/stats` route with these two routes:

```java
            config.routes.get("/stats", ctx -> ctx.json(ServerStats.json(latestSample(lsp), maxLspSessions, EvalGate.GLOBAL)));
            config.routes.get("/stats/history", ctx -> {
                StatsHistory.Range range;
                try {
                    range = StatsHistory.Range.parse(ctx.queryParam("range"));
                } catch (IllegalArgumentException e) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("range must be hour, day or week");
                    return;
                }
                ctx.json(Map.of("range", range.name().toLowerCase(java.util.Locale.ROOT), "resolutionSeconds", range.resolutionSeconds(),
                        "points", history.rows(range)));
            });
```

- Add these helpers:

```java
    // before the first scheduled sample has landed, read one directly
    private StatsSample latestSample(LspWebSocket lsp) {
        StatsSample latest = recorder.latest();
        return latest != null ? latest : ServerStats.read(lsp.sessionCount(), EvalGate.GLOBAL);
    }

    private String statsJson(StatsSample sample) {
        try {
            return statsMapper.writeValueAsString(ServerStats.json(sample, maxLspSessions, EvalGate.GLOBAL));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
```

  `statsJson` is used by Task 3; Task 2 only needs it to compile. If the compiler warns about it being unused, keep it anyway.

- In `stop()`, before `app.stop()`, add:

```java
        if (recorder != null) {
            recorder.close();
        }
```

- `Map` is already imported. Javalin serializes the records (`MinuteStats`) with Jackson as `{t, sessionsMax, ...}`; Jackson 2.12+ supports records.

- [ ] **Step 6: The --stats-dir option**

In website `Main.java`:
- Add `Path statsDir = null;` to the aligned declarations at the top of `main` (column-aligned with the others).
- In the colon-style switch, before `case "--no-gui":`, add:

```java
            case "--stats-dir":
                if (i + 1 >= args.length) {
                    fail("missing value for " + a);
                }
                statsDir = Path.of(args[++i]);
                break;
```

- Pass it on: `new NelumboHttpServer(base, files, timeoutMs, maxLspSessions, statsDir);`. The GUI reload path keeps the 4-arg constructor: a desktop run keeps history in memory.
- In `printUsage()`, add a line in the style of the existing ones: `  --stats-dir <dir>       keep the per-minute server stats history in <dir> (default: memory only)`.

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :website:test`
Expected: PASS, all tests including `statsHaveTheDocumentedShape`, the two new tests, `ServerStatsTest` (status/cpuLoad only), and Task 1's tests.

If `ServerStatsTest` referenced the removed cache, it does not; it only tests `status` and `cpuLoad`.

- [ ] **Step 8: Commit**

```bash
git add website/src/main/java/org/modelingvalue/nelumbo/website/ServerStats.java website/src/main/java/org/modelingvalue/nelumbo/website/StatsRecorder.java website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java website/src/main/java/org/modelingvalue/nelumbo/website/Main.java website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java
git commit -m "website: sample stats every 5 s, serve /stats/history, --stats-dir

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: /stats/stream (Server-Sent Events)

**Files:**
- Modify: `website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java`
- Create: `website/src/main/java/org/modelingvalue/nelumbo/website/StatsStream.java`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/StatsStreamTest.java`
- Test: `website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java`

**Interfaces:**
- Consumes: `StatsRecorder` with its listener, and `statsJson` (Task 2).
- Produces:
  - `StatsStream`:
    - `static final int MAX_CLIENTS = 50`
    - `void connect(SseClient client, String latestJson)`
    - `void broadcast(String json)`
    - `int clients()`
  - Route `GET /stats/stream`.

- [ ] **Step 1: Write the failing tests**

`StatsStreamTest.java` (LGPL header first). It uses a small fake, because `SseClient` is a Javalin class: check whether it can be constructed for a test. If not, put the logic behind a tiny interface as shown.

```java
package org.modelingvalue.nelumbo.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class StatsStreamTest {
    private static final class FakeClient implements StatsStream.Client {
        final List<String> events = new ArrayList<>();
        boolean            closed;
        boolean            failing;
        Runnable           onClose;

        @Override
        public void send(String event, String data) {
            if (failing) {
                throw new IllegalStateException("broken pipe");
            }
            events.add(event + ":" + data);
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void onClose(Runnable runnable) {
            onClose = runnable;
        }
    }

    @Test
    void aNewClientGetsTheLatestSampleAndLaterBroadcasts() {
        StatsStream stream = new StatsStream();
        FakeClient  client = new FakeClient();
        stream.connect(client, "{\"a\":1}");
        stream.broadcast("{\"a\":2}");
        assertEquals(List.of("stats:{\"a\":1}", "stats:{\"a\":2}"), client.events);
    }

    @Test
    void clientsOverTheCapGetAnErrorAndAreClosed() {
        StatsStream stream = new StatsStream();
        for (int i = 0; i < StatsStream.MAX_CLIENTS; i++) {
            stream.connect(new FakeClient(), null);
        }
        FakeClient extra = new FakeClient();
        stream.connect(extra, null);
        assertEquals(List.of("error:too many stream clients"), extra.events);
        assertTrue(extra.closed);
        assertEquals(StatsStream.MAX_CLIENTS, stream.clients());
    }

    @Test
    void streamDropsAClientThatFails() {
        StatsStream stream = new StatsStream();
        FakeClient  good   = new FakeClient();
        FakeClient  bad    = new FakeClient();
        stream.connect(good, null);
        stream.connect(bad, null);
        bad.failing = true;
        stream.broadcast("{}");
        assertEquals(1, stream.clients());
        assertEquals(List.of("stats:{}"), good.events);
    }

    @Test
    void aClosedClientIsRemoved() {
        StatsStream stream = new StatsStream();
        FakeClient  client = new FakeClient();
        stream.connect(client, null);
        client.onClose.run();
        assertEquals(0, stream.clients());
    }
}
```

Append to `NelumboHttpServerTest.java` (add the imports `java.util.stream.Stream` and `org.junit.jupiter.api.Timeout`):

```java
    @Test
    @Timeout(20)
    void streamSendsAStatsEventRightAway() throws Exception {
        HttpRequest                  request  = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/stats/stream")).GET().build();
        HttpResponse<Stream<String>> response = client.send(request, BodyHandlers.ofLines());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/event-stream"));
        try (Stream<String> lines = response.body()) {
            // up to two events; tolerates a leading comment or retry line before the first event
            List<String> first = lines.filter(l -> !l.isBlank()).limit(4).toList();
            assertTrue(first.contains("event: stats"), "got " + first);
            assertTrue(first.stream().anyMatch(l -> l.startsWith("data: {") && l.contains("\"sessions\"")), "got " + first);
        }
    }
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.StatsStreamTest" --tests "org.modelingvalue.nelumbo.website.NelumboHttpServerTest.streamSendsAStatsEventRightAway"`
Expected: compilation FAILS (`StatsStream` does not exist).

- [ ] **Step 3: Implement StatsStream**

`StatsStream.java` (LGPL header first):

```java
package org.modelingvalue.nelumbo.website;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.javalin.http.sse.SseClient;

/** The /stats/stream clients: every sample goes to all of them; a client that fails a send is dropped. */
final class StatsStream {
    static final int MAX_CLIENTS = 50;

    /** The part of a Javalin {@link SseClient} the stream uses, so it can be tested without a server. */
    interface Client {
        void send(String event, String data);

        void close();

        void onClose(Runnable runnable);

        static Client of(SseClient sse) {
            return new Client() {
                @Override
                public void send(String event, String data) {
                    sse.sendEvent(event, data);
                }

                @Override
                public void close() {
                    sse.close();
                }

                @Override
                public void onClose(Runnable runnable) {
                    sse.onClose(runnable);
                }
            };
        }
    }

    private final Set<Client> clients = ConcurrentHashMap.newKeySet();

    void connect(Client client, String latestJson) {
        if (clients.size() >= MAX_CLIENTS) {
            client.send("error", "too many stream clients");
            client.close();
            return;
        }
        clients.add(client);
        client.onClose(() -> clients.remove(client));
        if (latestJson != null) {
            client.send("stats", latestJson);
        }
    }

    void broadcast(String json) {
        for (Client client : clients) {
            try {
                client.send("stats", json);
            } catch (RuntimeException e) {
                clients.remove(client);
            }
        }
    }

    int clients() {
        return clients.size();
    }
}
```

- [ ] **Step 4: Wire the route**

In `NelumboHttpServer.start`:
- Before the recorder, create `StatsStream stream = new StatsStream();`.
- Change the recorder's listener from `sample -> { }` to `sample -> stream.broadcast(statsJson(sample))`.
- Next to the `/stats` routes, add:

```java
            config.routes.sse("/stats/stream", client -> {
                client.keepAlive();
                StatsSample latest = recorder.latest();
                stream.connect(StatsStream.Client.of(client), latest == null ? null : statsJson(latest));
            });
```

`keepAlive()` must be called before the handler returns, or Javalin closes the response. A client rejected by the cap still gets its `error` event, and `close()` ends it.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :website:test`
Expected: PASS (all).

- [ ] **Step 6: Commit**

```bash
git add website/src/main/java/org/modelingvalue/nelumbo/website/StatsStream.java website/src/main/java/org/modelingvalue/nelumbo/website/NelumboHttpServer.java website/src/test/java/org/modelingvalue/nelumbo/website/StatsStreamTest.java website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java
git commit -m "website: /stats/stream pushes every stats sample (SSE, 50 clients max)

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Status page with live stream and charts

**Files:**
- Modify: `website/src/main/frontend/package.json` (dependency `"uplot": "1.6.32"`; then `npm install` updates `package-lock.json`)
- Modify: `website/src/main/frontend/esbuild.mjs` (second build)
- Create: `website/src/main/frontend/src/status-chart.ts`
- Modify: `website/src/main/resources/public/status.html` (markup, CSS variables, script)
- Modify: `website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java` (`statusPageAndDotScriptAreServed` assertion)
- Modify: `website/src/main/frontend/e2e/status.spec.ts`

**Interfaces:**
- Consumes:
  - `/stats` (now with `time`)
  - `/stats/stream` (event `stats`)
  - `/stats/history?range=` returning `{range, resolutionSeconds, points[{t, sessionsMax, runningMax, cpuAvg, cpuMax, heapMaxMb, evaluations, overloads}]}`
- Produces:
  - global `NelumboStatus.start()`
  - DOM ids:
    - `status`, `sessions`, `running`, `total`, `overloaded`, `cpu`, `heap`, `uptime`, `updated`
    - `chart-cpu`, `chart-activity`, `history-table`
  - range buttons `button[data-range]`

- [ ] **Step 1: Write the failing tests**

In `NelumboHttpServerTest.statusPageAndDotScriptAreServed`, replace

```java
        assertTrue(page.body().contains("fetch('/stats')"), "the status page polls /stats");
```

with

```java
        assertTrue(page.body().contains("src=\"/assets/status-chart.js\""), "the status page loads its chart bundle");
```

and append the asset check to the same test:

```java
        assertEquals(200, get("/assets/status-chart.js").statusCode(), "the chart bundle is served");
```

Replace `e2e/status.spec.ts` with:

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

test('the status page updates live and shows the history charts', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/status.html');
    const updated: Locator = page.locator('#updated');
    await expect(updated).toHaveText(/^\d{2}:\d{2}:\d{2}$/, { timeout: 10_000 });
    const first: string = await updated.innerText();
    await expect(updated).not.toHaveText(first, { timeout: 15_000 });
    await expect(page.locator('#chart-cpu .uplot')).toHaveCount(1);
    await expect(page.locator('#chart-activity .uplot')).toHaveCount(1);
    await page.locator('button[data-range="week"]').click();
    await expect(page.locator('button[data-range="week"]')).toHaveAttribute('aria-pressed', 'true');
});

test('the status page fits a phone screen', async ({ page }: { page: Page }): Promise<void> => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/status.html');
    await expect(page.locator('#chart-cpu .uplot')).toHaveCount(1, { timeout: 10_000 });
    const overflow: boolean = await page.evaluate((): boolean => document.documentElement.scrollWidth > document.documentElement.clientWidth);
    expect(overflow).toBe(false);
});
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :website:test --tests "org.modelingvalue.nelumbo.website.NelumboHttpServerTest"`
Expected: FAIL on `statusPageAndDotScriptAreServed`.

The e2e tests also fail until the page is rebuilt. Run them in Step 7.

- [ ] **Step 3: Add uPlot and the build entry**

From `website/src/main/frontend`:
1. Run `npm install --save-exact uplot@1.6.32`. This updates `package.json` and `package-lock.json`.
2. Change `esbuild.mjs` to build both entries. Keep the existing options object for the fields bundle, and add after `await build(options);`:

```js
// the status page's live numbers and history charts; separate from the Monaco bundle so the page stays light
await build({
    entryPoints: ['src/status-chart.ts'],
    bundle:      true,
    outdir:      'dist',
    format:      'iife',
    globalName:  'NelumboStatus',
    sourcemap:   true,
    minify:      true,
    loader:      { '.css': 'css' },
    logLevel:    'info'
});
```

- [ ] **Step 4: Write status-chart.ts**

`website/src/main/frontend/src/status-chart.ts`:

```ts
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';

interface Stats {
    status:        string;
    time:          number;
    sessions:      { open: number; max: number };
    evaluations:   { running: number; threshold: number; total: number; overloaded: number };
    cpu:           { load: number; cpus: number };
    heap:          { usedMb: number; maxMb: number };
    uptimeSeconds: number;
}

interface Point {
    t:           number;
    sessionsMax: number;
    runningMax:  number;
    cpuAvg:      number;
    cpuMax:      number;
    heapMaxMb:   number;
    evaluations: number;
    overloads:   number;
}

interface History {
    range:             string;
    resolutionSeconds: number;
    points:            Point[];
}

const HISTORY_REFRESH_MS: number = 60_000;
const POLL_MS:            number = 5_000;
const CHART_HEIGHT:       number = 200;

let range:       string             = 'hour';
let history:     Point[]            = [];
let latest:      Stats | null       = null;
let cpuChart:    uPlot | null       = null;
let actChart:    uPlot | null       = null;
let pollTimer:   number | undefined = undefined;

function el(id: string): HTMLElement {
    return document.getElementById(id) as HTMLElement;
}

function set(id: string, text: string): void {
    el(id).textContent = text;
}

function css(name: string): string {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function two(n: number): string {
    return n < 10 ? '0' + n : String(n);
}

function clock(time: number): string {
    const d: Date = new Date(time);
    return two(d.getHours()) + ':' + two(d.getMinutes()) + ':' + two(d.getSeconds());
}

function showStats(s: Stats): void {
    latest = s;
    el('status').className = s.status;
    set('status', s.status);
    set('sessions', s.sessions.open + ' / ' + s.sessions.max);
    set('running', s.evaluations.running + ' (overloaded at ' + s.evaluations.threshold + ')');
    set('total', String(s.evaluations.total));
    set('overloaded', String(s.evaluations.overloaded));
    set('cpu', Math.round(s.cpu.load * 100) + '% of ' + s.cpu.cpus + ' cores');
    set('heap', s.heap.usedMb + ' / ' + s.heap.maxMb + ' MB');
    set('uptime', Math.floor(s.uptimeSeconds / 3600) + 'h ' + Math.floor(s.uptimeSeconds % 3600 / 60) + 'm');
    set('updated', clock(s.time));
    if (range === 'hour') {
        draw();
    }
}

function showUnreachable(): void {
    el('status').className = '';
    set('status', 'unreachable');
    for (const id of ['sessions', 'running', 'total', 'overloaded', 'cpu', 'heap', 'uptime']) {
        set(id, '-');
    }
}

function poll(): void {
    fetch('/stats').then((r: Response): Promise<Stats> => r.json()).then(showStats).catch(showUnreachable);
}

// the stream is the normal path; polling only covers the time the stream is down (proxy timeout, client cap)
function connectStream(): void {
    const source: EventSource = new EventSource('/stats/stream');
    source.addEventListener('stats', (e: MessageEvent): void => {
        if (pollTimer !== undefined) {
            window.clearInterval(pollTimer);
            pollTimer = undefined;
        }
        showStats(JSON.parse(e.data) as Stats);
    });
    source.addEventListener('error', (): void => {
        if (pollTimer === undefined) {
            poll();
            pollTimer = window.setInterval(poll, POLL_MS);
        }
    });
}

function loadHistory(): void {
    fetch('/stats/history?range=' + range).then((r: Response): Promise<History> => r.json()).then((h: History): void => {
        history = h.points;
        draw();
        fillTable();
    }).catch((): void => {
        history = [];
        draw();
    });
}

// in the hour view the latest live sample extends the lines to now
function series(): Point[] {
    const points: Point[] = history.slice();
    if (range === 'hour' && latest !== null) {
        points.push({ t: latest.time, sessionsMax: latest.sessions.open, runningMax: latest.evaluations.running, cpuAvg: latest.cpu.load,
                      cpuMax: latest.cpu.load, heapMaxMb: latest.heap.usedMb, evaluations: 0, overloads: 0 });
    }
    return points;
}

function axes(unit: (v: number) => string): uPlot.Axis[] {
    const ink:  string = css('--muted');
    const grid: string = css('--grid');
    return [
        { stroke: ink, grid: { stroke: grid, width: 1 }, ticks: { stroke: grid, width: 1 } },
        { stroke: ink, grid: { stroke: grid, width: 1 }, ticks: { stroke: grid, width: 1 }, values: (_u: uPlot, vals: number[]): string[] => vals.map(unit) },
    ];
}

function width(id: string): number {
    return el(id).clientWidth;
}

function draw(): void {
    const points: Point[]   = series();
    const xs:     number[]  = points.map((p: Point): number => p.t / 1000);
    const cpuData: uPlot.AlignedData = [xs, points.map((p: Point): number => p.cpuAvg * 100), points.map((p: Point): number => p.cpuMax * 100)];
    const actData: uPlot.AlignedData = [xs, points.map((p: Point): number => p.sessionsMax), points.map((p: Point): number => p.runningMax),
                                        points.map((p: Point): number => p.overloads)];
    if (cpuChart === null || actChart === null) {
        build(cpuData, actData);
        return;
    }
    cpuChart.setData(cpuData);
    actChart.setData(actData);
}

function build(cpuData: uPlot.AlignedData, actData: uPlot.AlignedData): void {
    cpuChart?.destroy();
    actChart?.destroy();
    el('chart-cpu').textContent      = '';
    el('chart-activity').textContent = '';
    const cpu: string = css('--series-cpu');
    cpuChart = new uPlot({
        width:  width('chart-cpu'),
        height: CHART_HEIGHT,
        scales: { x: { time: true }, y: { range: [0, 100] } },
        axes:   axes((v: number): string => v + '%'),
        series: [
            {},
            { label: 'CPU avg', stroke: cpu, width: 2 },
            { label: 'CPU max', stroke: cpu, width: 1, dash: [4, 4] },
        ],
    }, cpuData, el('chart-cpu'));
    actChart = new uPlot({
        width:  width('chart-activity'),
        height: CHART_HEIGHT,
        scales: { x: { time: true }, y: { range: (_u: uPlot, _min: number, max: number): [number, number] => [0, Math.max(4, max)] } },
        axes:   axes((v: number): string => String(v)),
        series: [
            {},
            { label: 'Sessions', stroke: css('--series-1'), width: 2 },
            { label: 'Evaluations running', stroke: css('--series-2'), width: 2 },
            { label: 'Stopped (busy)', stroke: css('--series-3'), width: 2 },
        ],
    }, actData, el('chart-activity'));
}

function fillTable(): void {
    const body: HTMLElement = el('history-table').querySelector('tbody') as HTMLElement;
    const rows: string[]    = history.slice().reverse().map((p: Point): string =>
        '<tr><td>' + new Date(p.t).toLocaleString() + '</td><td>' + Math.round(p.cpuAvg * 100) + '%</td><td>' + Math.round(p.cpuMax * 100)
        + '%</td><td>' + p.sessionsMax + '</td><td>' + p.runningMax + '</td><td>' + p.overloads + '</td><td>' + p.evaluations + '</td></tr>');
    body.innerHTML = rows.join('');
}

function selectRange(next: string): void {
    range = next;
    for (const b of Array.from(document.querySelectorAll<HTMLButtonElement>('button[data-range]'))) {
        b.setAttribute('aria-pressed', String(b.dataset.range === next));
    }
    loadHistory();
}

export function start(): void {
    for (const b of Array.from(document.querySelectorAll<HTMLButtonElement>('button[data-range]'))) {
        b.addEventListener('click', (): void => selectRange(b.dataset.range as string));
    }
    // colours are read from the theme's CSS variables, so a theme switch rebuilds the charts
    new MutationObserver((): void => {
        cpuChart?.destroy();
        actChart?.destroy();
        cpuChart = null;
        actChart = null;
        draw();
    }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    window.addEventListener('resize', (): void => {
        cpuChart?.setSize({ width: width('chart-cpu'), height: CHART_HEIGHT });
        actChart?.setSize({ width: width('chart-activity'), height: CHART_HEIGHT });
    });
    poll();
    connectStream();
    selectRange('hour');
    window.setInterval(loadHistory, HISTORY_REFRESH_MS);
}
```

Notes for the implementer:
- `uPlot.Axis.values` and `scales.y.range` signatures come from uPlot's bundled `.d.ts`. If `npm run check` rejects one, adjust the parameter types to what the `.d.ts` declares; do not use `any`.
- Plain string concatenation into `innerHTML` is safe here: every value is a number or a `Date` string from our own server.
- Run `npm run check` (`tsc --noEmit` over `src`).

- [ ] **Step 5: Rewrite status.html**

Replace the whole `website/src/main/resources/public/status.html` with:

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
<link rel="stylesheet" href="/assets/status-chart.css">
<style>
  :root { --bg: #1b1d23; --panel: #23262e; --text: #e6e8ee; --muted: #9aa0ac; --accent: #c184d8; --grid: #343843;
          --series-cpu: #9085e9; --series-1: #3987e5; --series-2: #d95926; --series-3: #199e70; }
  [data-theme="light"] { --bg: #fbfafc; --panel: #f2eff5; --text: #1f1b24; --muted: #6b6475; --accent: #9246b0; --grid: #e0dbe6;
          --series-cpu: #4a3aa7; --series-1: #2a78d6; --series-2: #eb6834; --series-3: #1baf7a; }
  * { box-sizing: border-box; }
  body { margin: 0; background: var(--bg); color: var(--text); font: 15px/1.5 "Instrument Sans", system-ui, sans-serif; }
  main { max-width: 960px; margin: 0 auto; padding: 40px 16px; }
  h1 { font-family: "Bricolage Grotesque", system-ui, sans-serif; font-size: 28px; margin: 0 0 4px; }
  h2 { font-family: "Bricolage Grotesque", system-ui, sans-serif; font-size: 18px; margin: 32px 0 8px; }
  .sub { color: var(--muted); margin: 0 0 24px; }
  .sub a { color: var(--accent); }
  .panel { padding: 18px 20px; background: var(--panel); border-radius: 10px; }
  dl { display: grid; grid-template-columns: max-content 1fr; gap: 8px 24px; margin: 0; }
  dt { color: var(--muted); }
  dd { margin: 0; font-family: "JetBrains Mono", monospace; overflow-wrap: anywhere; }
  #status.ok { color: #5fb87a; }
  #status.busy { color: #e0a050; }
  #status.overloaded { color: #f1707b; }
  .ranges { display: flex; gap: 8px; margin: 0 0 12px; }
  .ranges button { font: inherit; color: var(--text); background: transparent; border: 1px solid var(--grid); border-radius: 6px; padding: 4px 12px; cursor: pointer; }
  .ranges button[aria-pressed="true"] { border-color: var(--accent); color: var(--accent); }
  .chart { min-width: 0; margin: 0 0 16px; }
  .chart h3 { font-size: 13px; font-weight: 600; color: var(--muted); margin: 0 0 4px; }
  .uplot, .u-legend { color: var(--text); font-family: "Instrument Sans", system-ui, sans-serif; }
  details { margin-top: 8px; }
  summary { color: var(--muted); cursor: pointer; }
  .table-wrap { max-height: 320px; overflow: auto; margin-top: 8px; }
  table { border-collapse: collapse; font: 12px "JetBrains Mono", monospace; width: 100%; }
  th, td { text-align: right; padding: 2px 8px; border-bottom: 1px solid var(--grid); white-space: nowrap; }
  th:first-child, td:first-child { text-align: left; }
  footer { color: var(--muted); font-size: 12px; text-align: center; padding: 24px 16px; }
</style>
</head>
<body>
<main>
  <h1>Server status</h1>
  <p class="sub">Live numbers of this Nelumbo server. <a href="/">Home</a></p>
  <div class="panel">
    <dl>
      <dt>Status</dt>              <dd id="status">-</dd>
      <dt>LSP sessions</dt>        <dd id="sessions">-</dd>
      <dt>Evaluations running</dt> <dd id="running">-</dd>
      <dt>Evaluations total</dt>   <dd id="total">-</dd>
      <dt>Stopped (busy)</dt>      <dd id="overloaded">-</dd>
      <dt>CPU</dt>                 <dd id="cpu">-</dd>
      <dt>Heap</dt>                <dd id="heap">-</dd>
      <dt>Uptime</dt>              <dd id="uptime">-</dd>
      <dt>Last update</dt>         <dd id="updated">-</dd>
    </dl>
  </div>
  <h2>History</h2>
  <div class="panel">
    <div class="ranges">
      <button type="button" data-range="hour" aria-pressed="true">hour</button>
      <button type="button" data-range="day" aria-pressed="false">day</button>
      <button type="button" data-range="week" aria-pressed="false">week</button>
    </div>
    <div class="chart"><h3>CPU (% of the container's cores)</h3><div id="chart-cpu"></div></div>
    <div class="chart"><h3>Activity (per point: max sessions, max running evaluations, evaluations stopped as busy)</h3><div id="chart-activity"></div></div>
    <details>
      <summary>Data table</summary>
      <div class="table-wrap">
        <table id="history-table">
          <thead><tr><th>Time</th><th>CPU avg</th><th>CPU max</th><th>Sessions</th><th>Running</th><th>Stopped</th><th>Evaluations</th></tr></thead>
          <tbody></tbody>
        </table>
      </div>
    </details>
  </div>
</main>
<footer>Nelumbo @VERSION@</footer>
<script src="/assets/status-chart.js"></script>
<script>NelumboStatus.start();</script>
</body>
</html>
```

- [ ] **Step 6: Run the Java tests**

Run: `./gradlew :website:test`
Expected: PASS. The Gradle build runs `npm run dist`, so `/assets/status-chart.js` exists on the test classpath.

- [ ] **Step 7: Run the e2e suite**

From `website/src/main/frontend`:
1. `pkill -f "nelumbo-web-server-dev.jar --port 889"` (ignore "no process").
2. `npm run test:e2e`.

Expected: all pass, including the three status tests.

`highlighting.spec.ts:52` is a known pre-existing flake. If it is the only failure, re-run once and report both runs.

- [ ] **Step 8: Look at it**

1. Start the server: `java -jar website/build/libs/nelumbo-web-server-dev.jar --port 8899 --no-gui`.
2. Open `http://localhost:8899/status.html` and take screenshots of the light and dark themes and a 390px-wide view. Use Playwright with a small throwaway script in the scratchpad, or the Playwright MCP tools.
3. Check for:
   - label collisions
   - legend overlap
   - axis-label clipping
   - horizontal scroll at phone width
4. Fix what you see, then stop the server.

- [ ] **Step 9: Commit**

```bash
git add website/src/main/frontend/package.json website/src/main/frontend/package-lock.json website/src/main/frontend/esbuild.mjs website/src/main/frontend/src/status-chart.ts website/src/main/resources/public/status.html website/src/test/java/org/modelingvalue/nelumbo/website/NelumboHttpServerTest.java website/src/main/frontend/e2e/status.spec.ts
git commit -m "website: live status over the stats stream with hour/day/week charts

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Production config, documentation and verification

**Files:**
- Modify: `website/Dockerfile` (CMD)
- Modify: `website/docker-compose.yml` (volume)
- Modify: `CLAUDE.md`
- Modify: `website/src/main/frontend/README.md` (one line: the second esbuild entry)

**Interfaces:**
- Consumes: everything above.
- Produces: config and docs only.

- [ ] **Step 1: Production config**

`website/Dockerfile`: `CMD ["--port", "8080", "--max-lsp-sessions", "200", "--stats-dir", "/data/stats"]`.

`website/docker-compose.yml`: under the `nelumbo` service, after `memswap_limit: 16g`, add:

```yaml
    volumes:
      # per-minute stats history (/stats/history); lives in /data/sites/nelumbo.nl/stats on the host
      - ./stats:/data/stats
```

Validate: `docker compose -f website/docker-compose.yml config | grep -A3 volumes`.

- [ ] **Step 2: Documentation**

`CLAUDE.md`: in the Website Module section, after the paragraph that starts with `**Overload banner and status**`, add:

```markdown
**Stats stream and history** (2026-10-08).
- `StatsRecorder` samples every 5 s (`ServerStats.read`: sessions, the `EvalGate.GLOBAL` counters, process CPU load, heap). It is the only reader of `getProcessCpuLoad`, so `/stats` now returns its latest sample (plus `time`) instead of reading itself.
- Every sample is pushed to `GET /stats/stream` (SSE via Javalin's `config.routes.sse`, event `stats`, 50 clients max - over the cap one `error` event, then close; `StatsStream` drops a client whose send fails).
- Samples are aggregated per UTC minute into `MinuteStats` (maxima, CPU avg/max, counter increases; a counter drop = restart, the new value counts) in `StatsHistory`, which keeps 7 days in memory.
- With `--stats-dir` (production: `/data/stats`, compose volume `./stats` = `/data/sites/nelumbo.nl/stats` on server1), history appends one JSON line per minute to `stats-YYYY-MM-DD.jsonl` (UTC), reads the last week back at start-up (malformed lines skipped) and deletes files more than 8 days old.
- `GET /stats/history?range=hour|day|week`: minute rows (hour/day) or 10-minute aggregates (week), else 400.
- `/status.html` loads its own esbuild bundle `status-chart.js` (`src/status-chart.ts`, uPlot 1.6.32, global `NelumboStatus`, no Monaco): live numbers from the stream (polling fallback), and two charts (CPU %, and activity counts - never one dual-axis chart) with an hour/day/week switch and a data table.
- Colours: CPU violet, sessions/running/stopped = blue/orange/aqua. Validated with the dataviz validator for both themes; light aqua is < 3:1, so the data table is the required relief.
```

`website/src/main/frontend/README.md`: in the Build section, after the `npm run build` line, add a line:

`# esbuild builds two entries: nelumbo-fields.js (Monaco editors) and status-chart.js (status page, uPlot)`

- [ ] **Step 3: Full verification**

1. Run `./gradlew test --continue` (expected: BUILD SUCCESSFUL), then check the XML for failures:

```bash
for d in build cli/build mcp/build website/build lsp/server/build; do grep -ho 'failures="[0-9]*"' $d/test-results/test/*.xml | sort | uniq -c; done
```

   Expected: only `failures="0"`.
2. From `website/src/main/frontend`, run `npm run test:e2e`. Expected: all passed. The known `highlighting.spec.ts:52` flake rule applies.
3. Persistence across a restart, in Docker:

```bash
cd /Users/tom/projects/mvg-nelumbo/nelumbo
./gradlew :website:serverJar
S=$(mktemp -d)
docker build -q -t nelumbo-stats-test website
docker run -d --name nelumbo-stats -v "$S":/data/stats -p 8899:8080 nelumbo-stats-test
sleep 75
ls -la "$S"; curl -s 'localhost:8899/stats/history?range=hour' | head -c 300; echo
docker restart nelumbo-stats; sleep 10
curl -s 'localhost:8899/stats/history?range=hour' | head -c 300; echo
docker rm -f nelumbo-stats; docker rmi -f nelumbo-stats-test; rm -rf "$S"
```

   Expected:
   - a `stats-<today>.jsonl` file exists after ~75 s
   - `/stats/history?range=hour` has at least one point, both before and after `docker restart`

   If Docker is not available, skip this step and report it.

- [ ] **Step 4: Commit**

```bash
git add website/Dockerfile website/docker-compose.yml CLAUDE.md website/src/main/frontend/README.md
git commit -m "website: stats history on a compose volume; docs

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```
