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

public class ExamplesTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "true");
        setProp("TRACE_NELUMBO", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @RepeatedTest(10)
    public void friends() {
        exampleResource("friends.nl");
    }

    @RepeatedTest(10)
    public void whoIs() {
        exampleResource("whoIs.nl");
    }

    @RepeatedTest(10)
    public void family() {
        exampleResource("family.nl");
    }

    @RepeatedTest(10)
    public void queryOnly() {
        exampleResource("queryOnly.nl");
    }

    @RepeatedTest(10)
    public void belasting() {
        exampleResource("belasting.nl");
    }

    @RepeatedTest(10)
    public void fibonacci() {
        exampleResource("fibonacci.nl");
    }

    @RepeatedTest(10)
    public void transformation() {
        exampleResource("transformation.nl");
    }

    @RepeatedTest(10)
    public void max() {
        exampleResource("max.nl");
    }

    @RepeatedTest(10)
    public void deHet() {
        exampleResource("deHet.nl");
    }

    @RepeatedTest(10)
    public void maxFib() {
        exampleResource("maxFib.nl");
    }

    @RepeatedTest(10)
    public void scoping() {
        exampleResource("scoping.nl");
    }

    @RepeatedTest(10)
    public void hidden() {
        exampleResource("hidden.nl");
    }

    @RepeatedTest(10)
    public void power() {
        exampleResource("power.nl");
    }

    @RepeatedTest(10)
    public void even() {
        exampleResource("even.nl");
    }

    @RepeatedTest(10)
    public void ternary() {
        exampleResource("ternary.nl");
    }

    @RepeatedTest(10)
    public void clubFees() {
        exampleResource("clubFees.nl");
    }

}
