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
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.test;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.modelingvalue.nelumbo.KnowledgeBase;

// One test per solver (examples/sudoku-<solver>-<size>.nl, all puzzles of that size); the time
// of each sudoku is its query's micros in the CLI's --json output. The solvers' unit tests are
// in SudokuUnitsTest. The 9x9 ones need a memoization of 100000 (set below); with the default
// 10000 the brute-force 9x9 'real' puzzle runs 2+ hours. The slow ones only run with
// SLOW_SUDOKU=true in the environment.
// Times: M4 Max, test JVM pinned to 4 cores (root build.gradle.kts), 2026-11-14; CI runners take
// 0.85x, 1.3x or 1.75x these times, depending on the runner.
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

    // ~0.17s per run (per sudoku, CLI --json in a cold JVM: 0.13-0.23s)
    @RepeatedTest(10)
    public void sudokuBrute4x4() {
        exampleResource("sudoku-brute-4x4.nl");
    }

    // ~0.7s per run (per sudoku, CLI: 0.24-0.63s)
    @RepeatedTest(10)
    public void sudokuSmart4x4() {
        exampleResource("sudoku-smart-4x4.nl");
    }

    // ~5s (per sudoku, CLI: 1.4-1.8s)
    @Test
    public void sudokuCsp4x4() {
        exampleResource("sudoku-csp-4x4.nl");
    }

    // ~110s (per sudoku, CLI: oneBlankPerUnit 0.5s, firstRowEmpty 0.8s, real 6s, guessing 110s)
    @Test
    public void sudokuBrute9x9() {
        exampleResource("sudoku-brute-9x9.nl");
    }

    // ~70s (per sudoku, CLI: 1.6 / 2.5 / 12 / 59s)
    @Test
    public void sudokuSmart9x9() {
        exampleResource("sudoku-smart-9x9.nl");
    }

    // ~880s: ~175s per sudoku (CLI) whatever the clue count, nearly all of it parse
    @Test
    @EnabledIfEnvironmentVariable(named = "SLOW_SUDOKU", matches = "true")
    public void sudokuCsp9x9() {
        exampleResource("sudoku-csp-9x9.nl");
    }

}
