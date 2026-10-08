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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.lsp.EvalGate;

class ServerStatsTest {

    @Test
    void overloadedWhenAsManyEvaluationsRunAsTheThreshold() {
        assertEquals("overloaded", ServerStats.status(16, 16, 0.1));
    }

    @Test
    void busyFromHalfTheThresholdOrHighCpu() {
        assertEquals("busy", ServerStats.status(8, 16, 0.1));
        assertEquals("busy", ServerStats.status(0, 16, 0.7));
    }

    @Test
    void okBelowBoth() {
        assertEquals("ok", ServerStats.status(7, 16, 0.69));
    }

    @Test
    void pollersWithinTheCacheTimeShareOneSnapshot() {
        ServerStats         stats = new ServerStats();
        EvalGate            gate  = new EvalGate(16, 2000);
        Map<String, Object> first = stats.snapshot(0, 32, gate);
        gate.enter();
        assertSame(first, stats.snapshot(1, 32, gate));
    }

    @Test
    void unknownCpuLoadCountsAsIdle() {
        assertEquals(0.0, ServerStats.cpuLoad(-1.0));
        assertEquals(0.5, ServerStats.cpuLoad(0.5));
    }
}
