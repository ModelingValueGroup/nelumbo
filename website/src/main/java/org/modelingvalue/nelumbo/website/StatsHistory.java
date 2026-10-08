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

package org.modelingvalue.nelumbo.website;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
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
class StatsHistory {
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
                        if (row == null) {
                            continue;
                        }
                        if (row.t() >= cutoff) {
                            loaded.add(row);
                        }
                    } catch (IOException | RuntimeException malformed) {
                        // a line cut off by a crash or a full disk, or a null row; the rest of the file is still good
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
                 .filter(f -> isBefore(f, oldest))//
                 .forEach(f -> {
                     try {
                         Files.deleteIfExists(f);
                     } catch (IOException e) {
                         System.err.println("stats history: cannot delete " + f + ": " + e);
                     }
                 });
        } catch (IOException | UncheckedIOException e) {
            // no directory yet (the first append creates it) or an unreadable one; pruning is only housekeeping
        }
    }

    private static boolean isBefore(Path file, LocalDate oldest) {
        try {
            return LocalDate.parse(file.getFileName().toString().substring(6, 16)).isBefore(oldest);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private Path file(LocalDate date) {
        return dir.resolve("stats-" + date + ".jsonl");
    }

    private LocalDate today() {
        return LocalDate.ofInstant(Instant.ofEpochMilli(clock.getAsLong()), ZoneOffset.UTC);
    }
}
