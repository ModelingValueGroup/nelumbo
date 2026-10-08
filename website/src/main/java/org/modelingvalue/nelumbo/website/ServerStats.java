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

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

import org.modelingvalue.nelumbo.lsp.EvalGate;

import com.sun.management.OperatingSystemMXBean;

/** The /stats snapshot: LSP sessions, evaluation load (the shared {@link EvalGate}) and JVM resources. */
final class ServerStats {
    static final double BUSY_CPU_LOAD = 0.7;
    static final long   CACHE_MS      = 2000;

    private Map<String, Object> cached;
    private long                cachedAtNanos;

    // the process CPU load is measured since the previous call by anyone, so concurrent pollers share one sample
    synchronized Map<String, Object> snapshot(int openSessions, int maxSessions, EvalGate gate) {
        long now = System.nanoTime();
        if (cached == null || now - cachedAtNanos >= CACHE_MS * 1_000_000L) {
            cached        = sample(openSessions, maxSessions, gate);
            cachedAtNanos = now;
        }
        return cached;
    }

    private static Map<String, Object> sample(int openSessions, int maxSessions, EvalGate gate) {
        OperatingSystemMXBean os      = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Runtime               runtime = Runtime.getRuntime();
        double                load    = cpuLoad(os.getProcessCpuLoad());
        int                   running = gate.running();
        Map<String, Object>   stats   = new LinkedHashMap<>();
        stats.put("status", status(running, gate.threshold(), load));
        stats.put("sessions", Map.of("open", openSessions, "max", maxSessions));
        stats.put("evaluations", Map.of("running", running, "threshold", gate.threshold(), "total", gate.total(), "overloaded", gate.overloaded()));
        stats.put("cpu", Map.of("load", load, "cpus", runtime.availableProcessors()));
        stats.put("heap", Map.of("usedMb", (runtime.totalMemory() - runtime.freeMemory()) >> 20, "maxMb", runtime.maxMemory() >> 20));
        stats.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        return stats;
    }

    static String status(int running, int threshold, double cpuLoad) {
        if (running >= threshold) {
            return "overloaded";
        }
        if (cpuLoad >= BUSY_CPU_LOAD || running >= threshold / 2) {
            return "busy";
        }
        return "ok";
    }

    // the JVM reports a negative value when the process CPU load is not available
    static double cpuLoad(double reported) {
        return Math.max(0.0, reported);
    }
}
