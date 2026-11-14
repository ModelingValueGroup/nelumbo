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

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

// Unit tests of the sudoku solver modules (sudoku/), separate from SudokuExamplesTest so that
// its tests solve one sudoku each and nothing else. Same settings.
public class SudokuUnitsTest extends NelumboTestBase {

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
