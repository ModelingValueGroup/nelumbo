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

package org.modelingvalue.nelumbo.lsp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.modelingvalue.collections.Collection;

/**
 * Counts the LSP evaluations running in this JVM (all sessions share {@link #GLOBAL}). An evaluation that starts
 * while {@code threshold} others already run is "busy": it gets the short {@code budgetMs} instead of the
 * workspace deadline, so heavy queries are stopped with an overload result while light ones still finish.
 * {@link #GLOBAL}'s threshold defaults to the width of the shared inference pool ({@link Collection#PARALLELISM}):
 * from there on evaluations queue for a pool worker. The system properties {@code NELUMBO_OVERLOAD_THRESHOLD}
 * and {@code NELUMBO_OVERLOAD_BUDGET_MS} (default 2000) override the defaults.
 */
public final class EvalGate {
    public static final EvalGate GLOBAL = new EvalGate(
            Integer.getInteger("NELUMBO_OVERLOAD_THRESHOLD", Collection.PARALLELISM),
            Long.getLong("NELUMBO_OVERLOAD_BUDGET_MS", 2000L));

    private final int           threshold;
    private final long          budgetMs;
    private final AtomicInteger running    = new AtomicInteger();
    private final AtomicLong    total      = new AtomicLong();
    private final AtomicLong    overloaded = new AtomicLong();

    // threshold 0 means always busy; a budget below 1 ms would mean no deadline at all, so it is raised to 1
    public EvalGate(int threshold, long budgetMs) {
        this.threshold = threshold;
        this.budgetMs  = Math.max(1, budgetMs);
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
