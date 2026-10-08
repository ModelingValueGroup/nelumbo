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
