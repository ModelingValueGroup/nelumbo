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
import static org.modelingvalue.nelumbo.Integers.divide;
import static org.modelingvalue.nelumbo.Integers.ge;
import static org.modelingvalue.nelumbo.Integers.lt;
import static org.modelingvalue.nelumbo.Integers.minus;
import static org.modelingvalue.nelumbo.Integers.multiply;
import static org.modelingvalue.nelumbo.Integers.plus;
import static org.modelingvalue.nelumbo.Integers.sqrt;
import static org.modelingvalue.nelumbo.Logic.*;
import static org.modelingvalue.nelumbo.Rationals.*;
import static org.modelingvalue.nelumbo.Rationals.divide;
import static org.modelingvalue.nelumbo.Rationals.ge;
import static org.modelingvalue.nelumbo.Rationals.minus;
import static org.modelingvalue.nelumbo.Rationals.multiply;
import static org.modelingvalue.nelumbo.Rationals.plus;
import static org.modelingvalue.nelumbo.Rationals.sqrt;

import org.junit.jupiter.api.RepeatedTest;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Integers.IntegerCons;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Rationals.RationalCons;

public class NelumboTest extends NelumboTestBase {

    static {
        System.setProperty("PARALLEL_COLLECTIONS", "false");

        System.setProperty("REVERSE_NELUMBO", "false");
        System.setProperty("RANDOM_NELUMBO", "true");

        System.setProperty("TRACE_NELUMBO", "false");
        System.setProperty("PRETTY_NELUMBO", "true");
    }

    static final int NR_OF_REPEATS = 32;

    // Variables

    IntegerCons      P             = iConsVar("P");
    IntegerCons      Q             = iConsVar("Q");
    IntegerCons      R             = iConsVar("R");

    RationalCons     T             = rConsVar("T");

    // Tests

    @RepeatedTest(NR_OF_REPEATS)
    public void simpleTest() {
        run(() -> {
            integerRules();

            isTrue(lt(i(0), i(1)));
            isTrue(ge(i(1), i(0)));

            hasBindings(is(plus(i(7), i(3)), P), binding(P, i(10)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void intTest1() {
        run(() -> {
            integerRules();

            hasBindings(plus(i(7), i(3), P), binding(P, i(10)));
            hasBindings(plus(i(7), P, i(10)), binding(P, i(3)));
            hasBindings(plus(P, i(3), i(10)), binding(P, i(7)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void intTest2() {
        run(() -> {
            integerRules();

            isTrue(is(plus(i(11), i(22)), i(33)));
            isTrue(is(minus(i(33), i(22)), i(11)));
            isTrue(is(plus(i(11), plus(plus(i(22), i(33)), i(44))), i(110)));

            isTrue(is(plus(i(11), divide(multiply(i(44), i(33)), i(22))), i(77)));

            isTrue(is(sqrt(i(49)), i(7)));
            isTrue(is(sqrt(i(49)), i(-7)));

            hasBindings(is(plus(i(11), plus(plus(i(22), i(33)), i(44))), P), binding(P, i(110)));
            hasBindings(is(plus(i(11), plus(plus(i(22), P), i(44))), i(110)), binding(P, i(33)));
            hasBindings(is(plus(i(7), i(3)), P), binding(P, i(10)));
            hasBindings(is(plus(i(7), P), i(10)), binding(P, i(3)));
            hasBindings(is(plus(P, i(3)), i(10)), binding(P, i(7)));

            hasBindings(is(sqrt(i(49)), P), binding(P, i(7)), binding(P, i(-7)));

            hasBindings(and(is(sqrt(i(49)), P), ge(P, i(0))), binding(P, i(7)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void rationalTest1() {
        run(() -> {
            rationalRules();

            isTrue(is(divide(r(7), r(5)), r(7, 5)));
            isTrue(is(r(7, 5), divide(r(7), r(5))));

            hasBindings(plus(r(7), r(3), T), binding(T, r(20, 2)));
            hasBindings(plus(r(7), T, r(20, 2)), binding(T, r(6, 2)));
            hasBindings(plus(T, r(3), r(40, 4)), binding(T, r(7)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void rationalTest2() {
        run(() -> {
            rationalRules();

            isTrue(is(plus(r(11), r(88, 4)), r(66, 2)));
            isTrue(is(minus(r(33), r(22)), r(11)));
            isTrue(is(plus(r(11), plus(plus(r(22), r(33)), r(44))), r(110)));

            isTrue(is(plus(r(44, 4), divide(multiply(r(88, 2), r(66, 2)), r(22))), r(77)));

            isTrue(is(sqrt(r(49)), r(-14, 2)));
            isTrue(is(sqrt(r(98, 2)), r(7)));

            hasBindings(is(plus(r(11), plus(plus(r(22), r(33)), r(44))), T), binding(T, r(110)));
            hasBindings(is(plus(r(11), plus(plus(r(22), T), r(44))), r(110)), binding(T, r(33)));
            hasBindings(is(plus(r(7), r(3)), T), binding(T, r(10)));
            hasBindings(is(plus(r(7), T), r(10)), binding(T, r(3)));
            hasBindings(is(plus(T, r(3)), r(10)), binding(T, r(7)));

            hasBindings(is(sqrt(r(49)), T), binding(T, r(7)), binding(T, r(-7)));

            hasBindings(and(is(sqrt(r(49)), T), ge(T, r(0))), binding(T, r(7)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void notTest() {
        run(() -> {
            isFalse(and(F(), T()));
            isFalse(not(or(not(F()), not(T()))));
            isTrue(not(and(F(), T())));
            isTrue(not(not(or(not(F()), not(T())))));

            integerRules();

            isFalse(plus(i(5), i(2), i(8)));
            isFalse(is(plus(i(5), i(2)), i(8)));
            isTrue(not(plus(i(5), i(2), i(8))));
            isTrue(not(is(plus(i(5), i(2)), i(8))));
            isTrue(and(not(is(plus(i(5), i(2)), i(8))), not(plus(i(5), i(2), i(8)))));

            hasBindings(not(is(plus(i(5), i(2)), R)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void resultTest() {
        run(() -> {
            integerRules();

            Predicate query1 = is(plus(i(7), i(3)), i(10));
            hasResult(query1, Set.of(query1), true, Set.of(), true);
            Predicate query2 = is(plus(i(7), i(3)), i(11));
            hasResult(query2, Set.of(), true, Set.of(query2), true);
            Predicate query3 = is(plus(i(7), i(3)), P);
            hasResult(query3, Set.of(is(plus(i(7), i(3)), i(10))), true, Set.of(), false);
            Predicate query4 = is(plus(i(7), P), i(10));
            hasResult(query4, Set.of(is(plus(i(7), i(3)), i(10))), true, Set.of(), false);
            Predicate query5 = is(plus(i(7), P), Q);
            hasResult(query5, Set.of(), false, Set.of(), false);

            query1 = and(is(plus(i(7), i(3)), i(10)), is(plus(i(8), i(2)), i(10)));
            hasResult(query1, Set.of(query1), true, Set.of(), true);
            query2 = and(is(plus(i(7), i(3)), i(11)), is(plus(i(8), i(2)), i(11)));
            hasResult(query2, Set.of(), true, Set.of(query2), true);
            query3 = and(is(plus(i(7), i(3)), P), is(plus(i(8), i(2)), P));
            hasResult(query3, Set.of(and(is(plus(i(7), i(3)), i(10)), is(plus(i(8), i(2)), i(10)))), true, Set.of(), false);
            query4 = and(is(plus(i(7), P), i(10)), is(plus(i(8), Q), i(10)));
            hasResult(query4, Set.of(and(is(plus(i(7), i(3)), i(10)), is(plus(i(8), i(2)), i(10)))), true, Set.of(), false);
            query5 = and(is(plus(i(7), P), R), is(plus(i(8), Q), R));
            hasResult(query5, Set.of(), false, Set.of(), false);
        });
    }

}
