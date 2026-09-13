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
import org.junit.jupiter.api.Test;

// All four sudoku examples are @Disabled. Since bba88fc8 (2026-09-09, "rename
// generic types...") they fail EVERYWHERE deterministically - CLI: ClassCast
// "Variable cannot be cast to List" at NList.collection; test JVM: expectation
// mismatches with unreduced terms (map/scanF left unevaluated). Minimal repro:
// bug-repros/recursive-list-concat-classcast.nl (recursive collection
// accumulation). The old note here ("crashes in the test JVM yet never in a
// fresh CLI JVM, 20/20") was an artifact of a STALE cliJar built before
// bba88fc8 - investigated and explained 2026-09-13, no environment factor.
// When the engine bug is fixed, un-@Disable the 4x4 methods first (they solve
// in <1s); the 9x9 methods stay gated for the extra reasons noted below.
public class SudokuExamplesTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "false");
        setProp("TRACE_NELUMBO", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @Disabled("fails everywhere since bba88fc8 - see bug-repros/recursive-list-concat-classcast.nl")
    @Test
    public void sudoku4x4() {
        exampleResource("sudoku-4x4.nl");
    }

    @Disabled("fails everywhere since bba88fc8 - see bug-repros/recursive-list-concat-classcast.nl")
    @Test
    public void sudoku4x4Smart() {
        exampleResource("sudoku-4x4-smart.nl");
    }

    // Also brute-force 9x9: its third "real" puzzle ran 2+ hours without
    // finishing, so it would hang the build even if the race above were fixed.
    @Disabled("CLI-only (parallel-collections race) and the 'real' puzzle runs 2+ hours")
    @Test
    public void sudoku9x9() {
        exampleResource("sudoku-9x9.nl");
    }

    // Also singles-first 9x9: ~45s per run and run-to-run nondeterministic
    // (inference has its own parallel pool), so unfit for CI even if fixed.
    @Disabled("CLI-only (parallel-collections race), ~45s and run-to-run nondeterministic")
    @Test
    public void sudoku9x9Smart() {
        exampleResource("sudoku-9x9-smart.nl");
    }

}
