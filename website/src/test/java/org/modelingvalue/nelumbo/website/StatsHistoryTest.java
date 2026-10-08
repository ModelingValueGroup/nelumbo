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
    void invalidDateFileDoesNotBreakTheConstructor(@TempDir Path dir) throws Exception {
        new StatsHistory(dir, () -> NOW).add(row(NOW - 60_000, 7, 0.5, 42));
        Files.writeString(dir.resolve("stats-2026-02-30.jsonl"), "");
        assertEquals(1, new StatsHistory(dir, () -> NOW).rows(StatsHistory.Range.HOUR).size());
    }

    @Test
    void nullLineIsSkipped(@TempDir Path dir) throws Exception {
        new StatsHistory(dir, () -> NOW).add(row(NOW - 60_000, 7, 0.5, 42));
        Path file = dir.resolve("stats-" + LocalDate.ofInstant(java.time.Instant.ofEpochMilli(NOW - 60_000), ZoneOffset.UTC) + ".jsonl");
        Files.writeString(file, "null\n", java.nio.file.StandardOpenOption.APPEND);
        assertEquals(1, new StatsHistory(dir, () -> NOW).rows(StatsHistory.Range.HOUR).size());
    }

    @Test
    void invalidDateFileDoesNotBreakAddAcrossADayBoundary(@TempDir Path dir) throws Exception {
        long[]       now     = {NOW};
        StatsHistory history = new StatsHistory(dir, () -> now[0]);
        Files.writeString(dir.resolve("stats-2026-02-30.jsonl"), "");
        now[0] += 86_400_000L;
        history.add(row(now[0] - 60_000, 1, 0.1, 1));
        assertEquals(1, history.rows(StatsHistory.Range.HOUR).size());
    }

    @Test
    void rangeParsing() {
        assertEquals(StatsHistory.Range.WEEK, StatsHistory.Range.parse("week"));
        assertEquals(600, StatsHistory.Range.WEEK.resolutionSeconds());
        assertEquals(60, StatsHistory.Range.DAY.resolutionSeconds());
        assertThrows(IllegalArgumentException.class, () -> StatsHistory.Range.parse("month"));
    }
}
