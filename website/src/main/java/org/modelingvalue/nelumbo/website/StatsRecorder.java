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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Turns the 5-second samples into {@link MinuteStats} rows of a {@link StatsHistory} and hands every sample to a
 * listener (the live stream). The listener is called outside the lock, so a slow stream client cannot stall sampling.
 */
final class StatsRecorder implements AutoCloseable {
    static final long INTERVAL_MS = 5000;

    private final StatsHistory          history;
    private final Supplier<StatsSample> source;
    private final Consumer<StatsSample> listener;
    private final List<StatsSample>     bucket         = new ArrayList<>();
    private long                        bucketMinute   = -1;
    private long                        baseTotal      = -1;
    private long                        baseOverloaded = -1;
    private volatile StatsSample        latest;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nelumbo-stats");
        t.setDaemon(true);
        return t;
    });

    StatsRecorder(StatsHistory history, Supplier<StatsSample> source, Consumer<StatsSample> listener) {
        this.history  = history;
        this.source   = source;
        this.listener = listener;
    }

    void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sampleNow();
            } catch (Throwable e) {
                System.err.println("stats sample failed: " + e);
            }
        }, 0, INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
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
        MinuteStats finished = bucketMinute >= 0 && minute != bucketMinute ? finishMinute() : null;
        bucketMinute = minute;
        bucket.add(sample);
        latest = sample;
        if (finished != null) {
            try {
                history.add(finished);
            } catch (RuntimeException e) {
                System.err.println("stats history failed: " + e);
            }
        }
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
