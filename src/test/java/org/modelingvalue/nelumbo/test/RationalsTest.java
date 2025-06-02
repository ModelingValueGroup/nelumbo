//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.test;

import static org.modelingvalue.nelumbo.Logic.*;
import static org.modelingvalue.nelumbo.Rationals.*;

import org.junit.jupiter.api.RepeatedTest;
import org.modelingvalue.nelumbo.Rationals.RationalCons;

public class RationalsTest extends NelumboTestBase {

    static {
        System.setProperty("PARALLEL_COLLECTIONS", "false");

        System.setProperty("REVERSE_NELUMBO", "false");
        System.setProperty("RANDOM_NELUMBO", "true");

        System.setProperty("TRACE_NELUMBO", "false");
        System.setProperty("PRETTY_NELUMBO", "true");
    }

    static final int NR_OF_REPEATS = 32;

    // Variables

    RationalCons     R             = rConsVar("R");

    // Tests

    @RepeatedTest(NR_OF_REPEATS)
    public void rationalTest1() {
        run(() -> {
            rationalRules();

            isTrue(eq(divide(r(7), r(5)), r(7, 5)));
            isTrue(eq(r(7, 5), divide(r(7), r(5))));

            hasBindings(plus(r(7), r(3), R), binding(R, r(20, 2)));
            hasBindings(plus(r(7), R, r(20, 2)), binding(R, r(6, 2)));
            hasBindings(plus(R, r(3), r(40, 4)), binding(R, r(7)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void rationalTest2() {
        run(() -> {
            rationalRules();

            isTrue(eq(plus(r(11), r(88, 4)), r(66, 2)));
            isTrue(eq(minus(r(33), r(22)), r(11)));
            isTrue(eq(plus(r(11), plus(plus(r(22), r(33)), r(44))), r(110)));

            isTrue(eq(plus(r(44, 4), divide(multiply(r(88, 2), r(66, 2)), r(22))), r(77)));

            isTrue(eq(sqrt(r(49)), r(-14, 2)));
            isTrue(eq(sqrt(r(98, 2)), r(7)));

            hasBindings(eq(plus(r(11), plus(plus(r(22), r(33)), r(44))), R), binding(R, r(110)));
            hasBindings(eq(plus(r(11), plus(plus(r(22), R), r(44))), r(110)), binding(R, r(33)));
            hasBindings(eq(plus(r(7), r(3)), R), binding(R, r(10)));
            hasBindings(eq(plus(r(7), R), r(10)), binding(R, r(3)));
            hasBindings(eq(plus(R, r(3)), r(10)), binding(R, r(7)));

            hasBindings(eq(sqrt(r(49)), R), binding(R, r(7)), binding(R, r(-7)));

            hasBindings(and(eq(sqrt(r(49)), R), ge(R, r(0))), binding(R, r(7)));
        });
    }

}
