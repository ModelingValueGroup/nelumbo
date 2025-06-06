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
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Integers.IntegerCons;
import org.modelingvalue.nelumbo.Logic.Predicate;

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

    // Tests

    @RepeatedTest(NR_OF_REPEATS)
    public void simpleTest() {
        run(() -> {
            integerRules();

            isTrue(lt(i(0), i(1)));
            isTrue(ge(i(1), i(0)));

            hasBindings(eq(plus(i(7), i(3)), P), binding(P, i(10)));
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
            isFalse(eq(plus(i(5), i(2)), i(8)));
            isTrue(not(plus(i(5), i(2), i(8))));
            isTrue(not(eq(plus(i(5), i(2)), i(8))));
            isTrue(ne(plus(i(5), i(2)), i(8)));
            isTrue(and(not(eq(plus(i(5), i(2)), i(8))), not(plus(i(5), i(2), i(8)))));
            isTrue(and(ne(plus(i(5), i(2)), i(8)), not(plus(i(5), i(2), i(8)))));

            hasBindings(not(eq(plus(i(5), i(2)), R)));
            hasBindings(ne(plus(i(5), i(2)), R));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void resultTest() {
        run(() -> {
            integerRules();

            Predicate query1 = eq(plus(i(7), i(3)), i(10));
            hasResult(query1, Set.of(query1), true, Set.of(), true);
            Predicate query2 = eq(plus(i(7), i(3)), i(11));
            hasResult(query2, Set.of(), true, Set.of(query2), true);
            Predicate query3 = eq(plus(i(7), i(3)), P);
            hasResult(query3, Set.of(eq(plus(i(7), i(3)), i(10))), true, Set.of(), false);
            Predicate query4 = eq(plus(i(7), P), i(10));
            hasResult(query4, Set.of(eq(plus(i(7), i(3)), i(10))), true, Set.of(), false);
            Predicate query5 = eq(plus(i(7), P), Q);
            hasResult(query5, Set.of(), false, Set.of(), false);

            query1 = and(eq(plus(i(7), i(3)), i(10)), eq(plus(i(8), i(2)), i(10)));
            hasResult(query1, Set.of(query1), true, Set.of(), true);
            query2 = and(eq(plus(i(7), i(3)), i(11)), eq(plus(i(8), i(2)), i(11)));
            hasResult(query2, Set.of(), true, Set.of(query2), true);
            query3 = and(eq(plus(i(7), i(3)), P), eq(plus(i(8), i(2)), P));
            hasResult(query3, Set.of(and(eq(plus(i(7), i(3)), i(10)), eq(plus(i(8), i(2)), i(10)))), true, Set.of(), false);
            query4 = and(eq(plus(i(7), P), i(10)), eq(plus(i(8), Q), i(10)));
            hasResult(query4, Set.of(and(eq(plus(i(7), i(3)), i(10)), eq(plus(i(8), i(2)), i(10)))), true, Set.of(), false);
            query5 = and(eq(plus(i(7), P), R), eq(plus(i(8), Q), R));
            hasResult(query5, Set.of(), false, Set.of(), false);
        });
    }

}
