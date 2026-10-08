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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.modelingvalue.collections.Collection;

public class EvalGateTest {

    @Test
    public void busyOnceThresholdEvaluationsRun() {
        EvalGate gate = new EvalGate(2, 2000);
        assertFalse(gate.enter(), "first of two runs freely");
        assertFalse(gate.enter(), "second of two runs freely");
        assertTrue(gate.enter(), "a third one starts while two run: busy");
        assertEquals(3, gate.running());
        gate.exit();
        gate.exit();
        gate.exit();
        assertEquals(0, gate.running());
        assertEquals(3, gate.total());
    }

    @Test
    public void thresholdZeroIsAlwaysBusy() {
        assertTrue(new EvalGate(0, 2000).enter());
    }

    @Test
    public void overloadsAreCounted() {
        EvalGate gate = new EvalGate(4, 2000);
        gate.recordOverload();
        gate.recordOverload();
        assertEquals(2, gate.overloaded());
        assertEquals(2000, gate.budgetMs());
        assertEquals(4, gate.threshold());
    }

    @Test
    public void budgetIsAtLeastOneMillisecond() {
        assertEquals(1, new EvalGate(4, 0).budgetMs(), "0 must not mean unlimited");
        assertEquals(1, new EvalGate(4, -5).budgetMs());
        assertTrue(new EvalGate(0, 0).enter(), "threshold 0 stays valid: always busy");
    }

    @Test
    public void globalThresholdDefaultsToThePoolWidth() {
        assertEquals(Integer.getInteger("NELUMBO_OVERLOAD_THRESHOLD", Collection.PARALLELISM), EvalGate.GLOBAL.threshold());
    }
}
