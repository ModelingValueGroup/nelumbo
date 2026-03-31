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

public class NelumboTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "true");
        setProp("TRACE_NELUMBO", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @RepeatedTest(10)
    public void initTest() {
        String example = """
                // Init only
                """;
        testString(example, "NelumboTest.initTest");
    }

    @RepeatedTest(10)
    public void logicTest() {
        testResource("logicTest.nl");
    }

    @RepeatedTest(10)
    public void integersTest() {
        testResource("integersTest.nl");
    }

    @RepeatedTest(10)
    public void rationalsTest() {
        testResource("rationalsTest.nl");
    }

    @RepeatedTest(10)
    public void stringsTest() {
        testResource("stringsTest.nl");
    }

    @RepeatedTest(10)
    public void collectionsTest() {
        testResource("collectionsTest.nl");
    }

    @RepeatedTest(10)
    public void friends() {
        testResource("friends.nl");
    }

    @RepeatedTest(10)
    public void whoIs() {
        testResource("whoIs.nl");
    }

    @RepeatedTest(10)
    public void family() {
        testResource("family.nl");
    }

    @RepeatedTest(10)
    public void queryOnly() {
        testResource("queryOnly.nl");
    }

    @RepeatedTest(10)
    public void belasting() {
        testResource("belasting.nl");
    }

    @RepeatedTest(10)
    public void fibonacci() {
        testResource("fibonacci.nl");
    }

    @RepeatedTest(10)
    public void transformation() {
        testResource("transformation.nl");
    }

    @RepeatedTest(10)
    public void max() {
        testResource("max.nl");
    }

    @RepeatedTest(10)
    public void deHet() {
        testResource("deHet.nl");
    }

    @RepeatedTest(10)
    public void maxFib() {
        testResource("maxFib.nl");
    }

    @RepeatedTest(10)
    public void scoping() {
        testResource("scoping.nl");
    }

    @RepeatedTest(10)
    public void hidden() {
        testResource("hidden.nl");
    }

}
