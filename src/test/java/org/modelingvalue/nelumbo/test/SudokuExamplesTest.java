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

package org.modelingvalue.nelumbo.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

// All sudoku examples run (2026-10-28): every file green on the CLI in default,
// reversed and random rule order. The 9x9 ones need a memoization of 100000
// (set below); with the default 10000 the brute-force 9x9 runs 2+ hours.
public class SudokuExamplesTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @Override
    protected void settings(KnowledgeBase kb) {
        kb.setMaxLogicMemoiz(env("MAX_LOGIC_MEMOIZ", 100000));
        kb.setReverseOrder(env("REVERSE_NELUMBO", false));
        kb.setRandomOrder(env("RANDOM_NELUMBO", true));
        kb.setTrace(env("TRACE_NELUMBO", false));
    }

    @RepeatedTest(10)
    public void sudoku4x4() {
        exampleResource("sudoku-4x4.nl");
    }

    @RepeatedTest(10)
    public void sudoku4x4Smart() {
        exampleResource("sudoku-4x4-smart.nl");
    }

    // ~16s per run, so not repeated (10/10 green as @RepeatedTest(10)).
    @Test
    public void sudoku4x4Csp() {
        exampleResource("sudoku-4x4-csp.nl");
    }

    // ~10s with MAX_LOGIC_MEMOIZ=100000.
    @Test
    public void sudoku9x9() {
        exampleResource("sudoku-9x9.nl");
    }

    // ~75-100s (four puzzles plus unit tests).
    @Test
    public void sudoku9x9Smart() {
        exampleResource("sudoku-9x9-smart.nl");
    }

    @Disabled("To Slow ~235s per run")
    @Test
    public void sudoku9x9Csp() {
        exampleResource("sudoku-9x9-csp.nl");
    }

}
