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
import org.modelingvalue.nelumbo.Integers.Integer;
import org.modelingvalue.nelumbo.Integers.IntegerCons;
import org.modelingvalue.nelumbo.Integers.IntegerFunc;
import org.modelingvalue.nelumbo.Logic;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Relation;

public class FibonacciTest extends NelumboTestBase {

    static {
        System.setProperty("PARALLEL_COLLECTIONS", "false");

        System.setProperty("REVERSE_NELUMBO", "false");
        System.setProperty("RANDOM_NELUMBO", "true");

        System.setProperty("TRACE_NELUMBO", "false");
        System.setProperty("PRETTY_NELUMBO", "true");
    }

    static final int         NR_OF_REPEATS = 16;

    // Relation

    static Functor<Relation> FIB_REL       = Logic.<Relation, IntegerCons, IntegerCons> functor(FibonacciTest::fib);

    static Relation fib(IntegerCons i, IntegerCons f) {
        return relation(FIB_REL, i, f);
    }

    // Function

    static Functor<IntegerFunc> FIB_FUNC = Logic.<IntegerFunc, Integer> functor(FibonacciTest::fib);

    static IntegerFunc fib(Integer i) {
        return function(FIB_FUNC, i);
    }

    // Variables

    IntegerCons P = iConsVar("P");
    IntegerCons Q = iConsVar("Q");

    Integer     R = iVar("R");
    Integer     S = iVar("S");

    // Rules

    private void fibonacciRules() {
        integerRules();

        rule(fib(P, Q), and(ge(P, i(0)), le(P, i(1)), eq(Q, P)));
        rule(fib(P, Q), and(gt(P, i(1)), eq(plus(fib(minus(P, i(1))), fib(minus(P, i(2)))), Q)));

        rule(eq(fib(R), S), and(eq(R, P), eq(S, Q), fib(P, Q)));
    }

    // Tests

    @RepeatedTest(NR_OF_REPEATS * 2)
    public void smallFibonacciTest() {
        run(() -> {
            fibonacciRules();

            hasBindings(eq(fib(i(1)), P), binding(P, i(1)));
            hasBindings(eq(fib(i(6)), P), binding(P, i(8)));

            isTrue(eq(fib(i(0)), i(0)));
            isTrue(eq(fib(i(1)), i(1)));
            isTrue(eq(fib(i(6)), i(8)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void bigFibonacciTest() {
        run(() -> {
            fibonacciRules();

            hasBindings(eq(fib(i(7)), P), binding(P, i(13)));
            hasBindings(eq(fib(i(21)), P), binding(P, i(10946)));
            hasBindings(eq(fib(i(1000)), P), binding(P, i("18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf", Character.MAX_RADIX)));
        });
    }

}
