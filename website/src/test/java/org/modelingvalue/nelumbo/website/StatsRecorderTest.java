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
        MinuteStats row = rows.get(0);
        assertEquals(new MinuteStats(MINUTE, 4, 3, row.cpuAvg(), 0.6, 300, 10, 2), row);
        assertEquals(0.4, row.cpuAvg(), 1e-9);
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
    void aFailingHistoryDoesNotStopSampling() {
        List<MinuteStats> kept     = new ArrayList<>();
        boolean[]         failing  = {true};
        StatsHistory      history  = new StatsHistory(null, () -> MINUTE + 300_000) {
                                       @Override
                                       synchronized void add(MinuteStats row) {
                                           if (failing[0]) {
                                               throw new IllegalStateException("disk on fire");
                                           }
                                           kept.add(row);
                                       }
                                   };
        StatsRecorder     recorder = new StatsRecorder(history, () -> null, s -> {
        });
        recorder.record(sample(MINUTE + 5_000, 1, 0, 100, 0, 0.1, 100));
        recorder.record(sample(MINUTE + 65_000, 1, 0, 110, 0, 0.1, 100));
        assertEquals(MINUTE + 65_000, recorder.latest().time());
        failing[0] = false;
        recorder.record(sample(MINUTE + 125_000, 1, 0, 120, 0, 0.1, 100));
        recorder.record(sample(MINUTE + 185_000, 1, 0, 130, 0, 0.1, 100));
        assertEquals(MINUTE + 185_000, recorder.latest().time());
        assertEquals(2, kept.size());
        assertEquals(MINUTE + 60_000, kept.get(0).t());
        assertEquals(10, kept.get(0).evaluations());
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
