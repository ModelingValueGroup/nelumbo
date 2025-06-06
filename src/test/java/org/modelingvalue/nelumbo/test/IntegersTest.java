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

import static org.modelingvalue.nelumbo.Integers.*;
import static org.modelingvalue.nelumbo.Logic.*;

import org.junit.jupiter.api.RepeatedTest;
import org.modelingvalue.nelumbo.Integers.IntegerCons;

public class IntegersTest extends NelumboTestBase {

    static {
        System.setProperty("PARALLEL_COLLECTIONS", "false");

        System.setProperty("REVERSE_NELUMBO", "false");
        System.setProperty("RANDOM_NELUMBO", "true");

        System.setProperty("TRACE_NELUMBO", "false");
        System.setProperty("PRETTY_NELUMBO", "true");
    }

    static final int NR_OF_REPEATS = 32;

    // Variables

    IntegerCons      I             = iConsVar("I");

    // Tests

    @RepeatedTest(NR_OF_REPEATS)
    public void intTest1() {
        run(() -> {
            integerRules();

            hasBindings(eq(plus(i(7), i(3)), I), binding(I, i(10)));
            hasBindings(eq(plus(i(7), I), i(10)), binding(I, i(3)));
            hasBindings(eq(plus(I, i(3)), i(10)), binding(I, i(7)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void intTest2() {
        run(() -> {
            integerRules();

            isTrue(eq(plus(i(11), i(22)), i(33)));

            isTrue(eq(minus(i(33), i(22)), i(11)));
            isTrue(eq(plus(i(11), plus(plus(i(22), i(33)), i(44))), i(110)));

            isTrue(eq(plus(i(11), divide(multiply(i(44), i(33)), i(22))), i(77)));

            isTrue(eq(sqrt(i(49)), i(7)));
            isTrue(eq(sqrt(i(49)), i(-7)));

            hasBindings(eq(plus(i(11), plus(plus(i(22), i(33)), i(44))), I), binding(I, i(110)));
            hasBindings(eq(plus(i(11), plus(plus(i(22), I), i(44))), i(110)), binding(I, i(33)));
            hasBindings(eq(plus(i(7), i(3)), I), binding(I, i(10)));
            hasBindings(eq(plus(i(7), I), i(10)), binding(I, i(3)));
            hasBindings(eq(plus(I, i(3)), i(10)), binding(I, i(7)));

            hasBindings(eq(sqrt(i(49)), I), binding(I, i(7)), binding(I, i(-7)));

            hasBindings(and(eq(sqrt(i(49)), I), ge(I, i(0))), binding(I, i(7)));
        });
    }

}
