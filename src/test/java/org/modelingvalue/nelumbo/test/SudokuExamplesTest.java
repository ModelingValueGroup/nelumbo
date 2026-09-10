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

// All four sudoku examples are @Disabled: they run only via the standalone CLI,
// not in the shared test JVM. Even the tiny 4x4 grids crash here with a
// ClassCastException ("Variable cannot be cast to List") - the immutable-
// collections interning race under parallel inference. It reproduces 100% in
// the Gradle test worker yet never in a fresh `java -jar` CLI JVM (20/20), and
// -DPARALLEL_COLLECTIONS=false / -DPARALLELISM=1 do NOT stop it here (verified:
// the properties reach the JVM but the inference pool still parallelises). The
// underlying race is a known, unfixed collections defect. Run these via the CLI:
//   java -DPARALLEL_COLLECTIONS=false -jar cli/build/libs/nelumbo-cli-*.jar \
//        src/main/resources/org/modelingvalue/nelumbo/examples/sudoku-4x4.nl
// If that race is ever fixed, drop the @Disabled from the 4x4 methods first
// (they solve in <1s); the 9x9 methods stay gated for the extra reasons noted.
public class SudokuExamplesTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "false");
        setProp("TRACE_NELUMBO", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @Disabled("CLI-only: crashes in the test JVM under the parallel-collections interning race")
    @Test
    public void sudoku4x4() {
        exampleResource("sudoku-4x4.nl");
    }

    @Disabled("CLI-only: crashes in the test JVM under the parallel-collections interning race")
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
