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

package org.modelingvalue.logic.test;

import static org.modelingvalue.logic.Integers.*;
import static org.modelingvalue.logic.Integers.compare;
import static org.modelingvalue.logic.Integers.divide;
import static org.modelingvalue.logic.Integers.ge;
import static org.modelingvalue.logic.Integers.gt;
import static org.modelingvalue.logic.Integers.le;
import static org.modelingvalue.logic.Integers.lt;
import static org.modelingvalue.logic.Integers.minus;
import static org.modelingvalue.logic.Integers.multiply;
import static org.modelingvalue.logic.Integers.plus;
import static org.modelingvalue.logic.Integers.sqrt;
import static org.modelingvalue.logic.Lists.add;
import static org.modelingvalue.logic.Lists.l;
import static org.modelingvalue.logic.Logic.*;
import static org.modelingvalue.logic.Rationals.*;
import static org.modelingvalue.logic.Rationals.divide;
import static org.modelingvalue.logic.Rationals.lt;
import static org.modelingvalue.logic.Rationals.minus;
import static org.modelingvalue.logic.Rationals.multiply;
import static org.modelingvalue.logic.Rationals.plus;
import static org.modelingvalue.logic.Rationals.sqrt;

import org.junit.jupiter.api.RepeatedTest;
import org.modelingvalue.collections.util.SerializableBiFunction;
import org.modelingvalue.collections.util.SerializableFunction;
import org.modelingvalue.logic.Integers.Integer;
import org.modelingvalue.logic.Integers.IntegerCons;
import org.modelingvalue.logic.Integers.IntegerFunc;
import org.modelingvalue.logic.KnowledgeBase;
import org.modelingvalue.logic.Lists.ListCons;
import org.modelingvalue.logic.Logic.Constant;
import org.modelingvalue.logic.Logic.Function;
import org.modelingvalue.logic.Logic.Functor;
import org.modelingvalue.logic.Logic.Relation;
import org.modelingvalue.logic.Logic.Structure;
import org.modelingvalue.logic.Rationals.RationalCons;

public class LogicTest extends LogicTestBase {

    static {
        System.setProperty("TRACE_NELUMBO", "true");
    }

    // Root

    interface Root extends Structure {
    }

    interface RootCons extends Root, Constant<Root> {
    }

    interface RootFunc extends Root, Function<Root> {
    }

    static Functor<RootCons> ROOT = functor((SerializableFunction<String, RootCons>) LogicTest::root);

    static RootCons root(String name) {
        return constant(ROOT, name);
    }

    static RootCons rootConsVar(String name) {
        return variable(RootCons.class, name);
    }

    static Root rootVar(String name) {
        return variable(Root.class, name);
    }

    static Functor<Relation> ROOT_PERSON = functor(LogicTest::rootPerson);

    static Relation rootPerson(RootCons root, PersonCons person) {
        return pred(ROOT_PERSON, root, person);
    }

    static Functor<RootFunc> ROOT_FUNC = functor((SerializableFunction<Person, RootFunc>) LogicTest::root);

    static RootFunc root(Person person) {
        return function(ROOT_FUNC, person);
    }

    // Family Tree

    interface Person extends Structure {
    }

    interface PersonCons extends Person, Constant<Person> {
    }

    interface PersonFunc extends Person, Function<Person> {
    }

    static Functor<PersonCons> STRING_PERSON = functor((SerializableFunction<String, PersonCons>) LogicTest::person);

    static PersonCons person(String name) {
        return constant(STRING_PERSON, name);
    }

    static Functor<PersonCons> INTEGER_PERSON = functor((SerializableFunction<IntegerCons, PersonCons>) LogicTest::person);

    static PersonCons person(IntegerCons i) {
        return constant(INTEGER_PERSON, i);
    }

    static PersonCons person(int i) {
        return person(i(i));
    }

    static PersonCons personConsVar(String name) {
        return variable(PersonCons.class, name);
    }

    static Person personVar(String name) {
        return variable(Person.class, name);
    }

    static Functor<Relation> PARENT_CHILD = functor(LogicTest::parentChild);

    static Relation parentChild(PersonCons parent, PersonCons child) {
        return pred(PARENT_CHILD, parent, child);
    }

    static Functor<PersonFunc> PARENT = functor(LogicTest::parent);

    static PersonFunc parent(Person child) {
        return function(PARENT, child);
    }

    static Functor<PersonFunc> CHILD = functor(LogicTest::child);

    static PersonFunc child(Person parent) {
        return function(CHILD, parent);
    }

    static Functor<Relation> ANCESTOR_DESCENTENT = functor(LogicTest::ancestorDescendent);

    static Relation ancestorDescendent(PersonCons ancestor, PersonCons descendent) {
        return pred(ANCESTOR_DESCENTENT, ancestor, descendent);
    }

    static Functor<PersonFunc> ANCESTOR = functor(LogicTest::ancestor);

    static PersonFunc ancestor(Person descendent) {
        return function(ANCESTOR, descendent);
    }

    static Functor<PersonFunc> DESCENDENT = functor(LogicTest::descendent);

    static PersonFunc descendent(Person ancestor) {
        return function(DESCENDENT, ancestor);
    }

    // Variables

    IntegerCons              P      = iConsVar("P");
    IntegerCons              Q      = iConsVar("Q");

    Integer                  R      = iVar("R");
    Integer                  S      = iVar("S");

    RationalCons             T      = rConsVar("T");
    RationalCons             U      = rConsVar("U");

    PersonCons               A      = personConsVar("A");
    PersonCons               B      = personConsVar("B");
    PersonCons               C      = personConsVar("C");

    Person                   X      = personVar("X");
    Person                   Y      = personVar("Y");
    Person                   Z      = personVar("Z");

    RootCons                 V      = rootConsVar("V");
    Root                     W      = rootVar("W");

    @SuppressWarnings("unchecked")
    ListCons<Person>         PL     = variable(ListCons.class, "PL");

    // Terms

    PersonCons               Carel  = person("Carel");
    PersonCons               Jan    = person("Jan");
    PersonCons               Elske  = person("Elske");
    PersonCons               Wim    = person("Wim");
    PersonCons               Joppe  = person("Joppe");
    PersonCons               Heleen = person("Heleen");
    PersonCons               Marijn = person("Marijn");

    RootCons                 Root   = root("Root");

    // Fibonacci

    static Functor<Relation> fib2   = functor((SerializableBiFunction<IntegerCons, IntegerCons, Relation>) LogicTest::fib);

    static Relation fib(IntegerCons i, IntegerCons f) {
        return pred(fib2, i, f);
    }

    static Functor<IntegerFunc> fib1 = functor((SerializableFunction<Integer, IntegerFunc>) LogicTest::fib);

    static IntegerFunc fib(Integer i) {
        return function(fib1, i);
    }

    private void fibonacciRules() {
        integerRules();

        rule(fib(P, Q), and(ge(P, i(0)), le(P, i(1)), eq(Q, P)));
        rule(fib(P, Q), and(gt(P, i(1)), is(plus(fib(minus(P, i(1))), fib(minus(P, i(2)))), Q)));

        rule(is(fib(R), Q), and(is(R, P), fib(P, Q)));
    }

    // Root Rules

    private void rootRules() {
        integerRules();

        rule(parentChild(person(Q), person(P)), and(compare(Q, i(4), i(-1)), compare(Q, i(-1), i(1)), plus(Q, i(1), P)));
        // rule(parentChild(person(Q), person(P)), and(lt(Q, i(4)), ge(Q, i(0)), is(plus(Q, i(1)), P)));
        rule(rootPerson(V, person(0)), T());
        rule(rootPerson(V, C), and(rootPerson(V, A), parentChild(A, C)));

        rule(is(parent(X), A), and(is(X, B), parentChild(A, B)));
        rule(is(child(X), A), and(is(X, B), parentChild(B, A)));
        rule(is(root(X), V), and(is(X, B), rootPerson(V, B)));
    }

    // Family Rules

    private void familyRules() {
        isRules();

        rule(ancestorDescendent(A, C), parentChild(A, C));
        rule(ancestorDescendent(A, C), and(ancestorDescendent(A, B), parentChild(B, C)));

        rule(is(parent(X), A), and(is(X, B), parentChild(A, B)));
        rule(is(child(X), A), and(is(X, B), parentChild(B, A)));

        rule(is(ancestor(X), A), and(is(X, B), ancestorDescendent(A, B)));
        rule(is(descendent(X), A), and(is(X, B), ancestorDescendent(B, A)));
    }

    // @Test
    public void rulesTest() {
        KnowledgeBase db = run(() -> {
            familyRules();
            fibonacciRules();
        });
        print(db);
    }

    @RepeatedTest(10)
    public void notTest() {
        run(() -> {
            isFalse(and(F(), T()));
            isFalse(not(or(not(F()), not(T()))));

            integerRules();

            isTrue(not(plus(i(5), i(2), i(8))));
            isTrue(not(is(plus(i(5), i(2)), i(8))));
        });
    }

    @RepeatedTest(10)
    public void famTest0() {
        run(() -> {
            familyRules();

            fact(parentChild(Carel, Jan));
            fact(parentChild(Jan, Wim));
            fact(parentChild(Elske, Wim));
            fact(parentChild(Wim, Joppe));
            fact(parentChild(Heleen, Joppe));
            fact(parentChild(Wim, Marijn));
            fact(parentChild(Heleen, Marijn));

            isTrue(is(parent(Joppe), Heleen));
            isTrue(is(Wim, child(Jan)));

            isFalse(is(Marijn, parent(Wim)));
            isFalse(is(parent(Wim), Heleen));
            isFalse(is(child(Wim), Wim));

            isTrue(is(ancestor(Marijn), Wim));
            isTrue(is(descendent(Carel), Marijn));

            isFalse(is(descendent(Marijn), Wim));
            isFalse(is(descendent(Heleen), Wim));
            isFalse(is(descendent(Joppe), Carel));
            isFalse(is(descendent(Carel), Carel));
        });
    }

    @RepeatedTest(10)
    public void famTest1() {
        run(() -> {
            familyRules();

            fact(parentChild(Carel, Jan));
            fact(parentChild(Jan, Wim));
            fact(parentChild(Elske, Wim));
            fact(parentChild(Wim, Joppe));
            fact(parentChild(Heleen, Joppe));
            fact(parentChild(Wim, Marijn));
            fact(parentChild(Heleen, Marijn));

            isTrue(parentChild(Heleen, Joppe));
            isTrue(parentChild(Jan, Wim));

            isFalse(parentChild(Marijn, Wim));
            isFalse(parentChild(Heleen, Wim));
            isFalse(parentChild(Wim, Wim));

            isTrue(ancestorDescendent(Wim, Marijn));
            isTrue(ancestorDescendent(Carel, Marijn));

            isFalse(ancestorDescendent(Marijn, Wim));
            isFalse(ancestorDescendent(Heleen, Wim));
            isFalse(ancestorDescendent(Joppe, Carel));
            isFalse(ancestorDescendent(Carel, Carel));

            hasBindings(collect(parentChild(Wim, C), add(C, l(), PL)), binding(PL, l(Joppe, Marijn)));
        });
    }

    @RepeatedTest(10)
    public void famTest2() {
        run(() -> {
            familyRules();

            fact(parentChild(Carel, Jan));
            fact(parentChild(Jan, Wim));

            hasBindings(ancestorDescendent(A, Wim), binding(A, Jan), binding(A, Carel));
            hasBindings(ancestorDescendent(Carel, C), binding(C, Jan), binding(C, Wim));
        });
    }

    @RepeatedTest(10)
    public void famTest3() {
        run(() -> {
            fact(parentChild(Carel, Jan));
            fact(parentChild(Jan, Wim));
            fact(parentChild(Wim, Marijn));
            isTrue(and(parentChild(Carel, A), parentChild(A, B), parentChild(B, Marijn)));
        });
    }

    @RepeatedTest(10)
    public void intTest1() {
        run(() -> {
            integerRules();

            hasBindings(plus(i(7), i(3), P), binding(P, i(10)));
            hasBindings(plus(i(7), P, i(10)), binding(P, i(3)));
            hasBindings(plus(P, i(3), i(10)), binding(P, i(7)));
        });
    }

    @RepeatedTest(10)
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

            hasBindings(and(is(sqrt(i(49)), P), not(lt(P, i(0)))), binding(P, i(7)));

            hasBindings(collect(is(sqrt(i(49)), P), plus(P, i(0), Q)), binding(Q, i(0)));
        });
    }

    @RepeatedTest(10)
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

    @RepeatedTest(10)
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

            hasBindings(and(is(sqrt(r(49)), T), not(lt(T, r(0)))), binding(T, r(7)));

            hasBindings(collect(is(sqrt(r(49)), T), plus(T, r(0), U)), binding(U, r(0)));
        });
    }

    @RepeatedTest(10)
    public void rootTest1() {
        run(() -> {
            rootRules();

            isTrue(parentChild(person(0), person(1)));
            isTrue(parentChild(person(3), person(4)));
            isFalse(parentChild(person(4), person(5)));

            isTrue(rootPerson(Root, person(0)));
            isTrue(rootPerson(Root, person(1)));
            isTrue(rootPerson(Root, person(4)));
            isTrue(rootPerson(Root, person(3)));
            isTrue(rootPerson(Root, person(2)));

            hasBindings(rootPerson(Root, C), binding(C, person(0)), binding(C, person(1)), //
                    binding(C, person(2)), binding(C, person(3)), binding(C, person(4)));
        });
    }

    @RepeatedTest(10)
    public void rootTest2() {
        run(() -> {
            rootRules();

            isTrue(is(child(person(0)), person(1)));
            isTrue(is(child(person(3)), person(4)));
            isFalse(is(child(person(4)), person(5)));

            isTrue(is(root(person(0)), Root));
            isTrue(is(root(person(1)), Root));
            isTrue(is(root(person(4)), Root));
            isTrue(is(root(person(3)), Root));
            isTrue(is(root(person(2)), Root));

            hasBindings(is(root(C), Root), binding(C, person(0)), binding(C, person(1)), //
                    binding(C, person(2)), binding(C, person(3)), binding(C, person(4)));
        });
    }

    @RepeatedTest(10)
    public void fibonacciTest0() {
        run(() -> {
            fibonacciRules();

            hasBindings(fib(i(1), P), binding(P, i(1)));
            hasBindings(fib(i(7), P), binding(P, i(13)));
        });
    }

    @RepeatedTest(10)
    public void fibonacciTest1() {
        run(() -> {
            fibonacciRules();

            hasBindings(fib(i(1), P), binding(P, i(1)));
            hasBindings(fib(i(7), P), binding(P, i(13)));
            hasBindings(fib(i(21), P), binding(P, i(10946)));
            hasBindings(fib(i(1000), P), binding(P, i("18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf", Character.MAX_RADIX)));
        });
    }

    @RepeatedTest(10)
    public void fibonacciTest2() {
        run(() -> {
            fibonacciRules();

            hasBindings(is(fib(i(7)), P), binding(P, i(13)));
            hasBindings(is(fib(i(21)), P), binding(P, i(10946)));
            hasBindings(is(fib(i(1000)), P), binding(P, i("18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf", Character.MAX_RADIX)));
        });
    }

}
