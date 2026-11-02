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

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.modelingvalue.nelumbo.KnowledgeBase;

// All sudoku examples run (2026-10-28): every file green on the CLI in default,
// reversed and random rule order. The 9x9 ones need a memoization of 100000
// (set below); with the default 10000 the brute-force 9x9 runs 2+ hours.
// Solvers and puzzles are modules in sudoku/; the example files of one size are
// identical except for the solver import.
// The slow ones only run with SLOW_SUDOKU=true in the environment.
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
    public void sudokuBrute4x4Easy() {
        exampleResource("sudoku-brute-4x4-easy.nl");
    }

    @RepeatedTest(10)
    public void sudokuSmart4x4Easy() {
        exampleResource("sudoku-smart-4x4-easy.nl");
    }

    // ~8s per run, so not repeated (10/10 green as @RepeatedTest(10)).
    @Test
    public void sudokuCsp4x4Easy() {
        exampleResource("sudoku-csp-4x4-easy.nl");
    }

    // ~95-145s with MAX_LOGIC_MEMOIZ=100000, most of it the 26-clue puzzle.
    @Test
    public void sudokuBrute9x9Easy() {
        exampleResource("sudoku-brute-9x9-easy.nl");
    }

    // ~65-95s.
    @Test
    public void sudokuSmart9x9Easy() {
        exampleResource("sudoku-smart-9x9-easy.nl");
    }

    // ~1 hour (estimated from single-puzzle runs: 72 clues ~4 min, the rest ~17-19 min).
    @Test
    @EnabledIfEnvironmentVariable(named = "SLOW_SUDOKU", matches = "true")
    public void sudokuCsp9x9Easy() {
        exampleResource("sudoku-csp-9x9-easy.nl");
    }

    // A puzzle where the CSP wins (25 clues, naked + hidden singles only), one file
    // per solver: csp ~290s, brute force and singles-first did not finish within
    // 60 min each (2026-11-02, CLI, memo 100000).
    @Test
    @EnabledIfEnvironmentVariable(named = "SLOW_SUDOKU", matches = "true")
    public void sudokuCsp9x9Hard() {
        exampleResource("sudoku-csp-9x9-hard.nl");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SLOW_SUDOKU", matches = "true")
    public void sudokuSmart9x9Hard() {
        exampleResource("sudoku-smart-9x9-hard.nl");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SLOW_SUDOKU", matches = "true")
    public void sudokuBrute9x9Hard() {
        exampleResource("sudoku-brute-9x9-hard.nl");
    }

    // unit tests of the solver modules (the example files hold only puzzles)

    @Test
    public void brute4x4Units() {
        testResource("sudokuBrute4x4Test.nl");
    }

    @Test
    public void csp4x4Units() {
        testResource("sudokuCsp4x4Test.nl");
    }

    @Test
    public void brute9x9Units() {
        testResource("sudokuBrute9x9Test.nl");
    }

    @Test
    public void smart9x9Units() {
        testResource("sudokuSmart9x9Test.nl");
    }

    @Test
    public void csp9x9Units() {
        testResource("sudokuCsp9x9Test.nl");
    }

}
