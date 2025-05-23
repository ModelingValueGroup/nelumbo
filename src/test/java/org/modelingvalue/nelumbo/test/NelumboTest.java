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
import static org.modelingvalue.nelumbo.Integers.gt;
import static org.modelingvalue.nelumbo.Integers.le;
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
import org.modelingvalue.collections.util.SerializableBiFunction;
import org.modelingvalue.collections.util.SerializableFunction;
import org.modelingvalue.nelumbo.Integers.Integer;
import org.modelingvalue.nelumbo.Integers.IntegerCons;
import org.modelingvalue.nelumbo.Integers.IntegerFunc;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Logic.Constant;
import org.modelingvalue.nelumbo.Logic.Function;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.RenderLambda;
import org.modelingvalue.nelumbo.Logic.Structure;
import org.modelingvalue.nelumbo.Rationals.RationalCons;

public class NelumboTest extends NelumboTestBase {

    static {
        System.setProperty("PARALLEL_COLLECTIONS", "false");

        System.setProperty("REVERSE_NELUMBO", "false");
        System.setProperty("RANDOM_NELUMBO", "true");

        System.setProperty("TRACE_NELUMBO", "false");
        System.setProperty("PRETTY_NELUMBO", "true");
    }

    static final int NR_OF_REPEATS     = 32;
    static final int NR_OF_FIB_REPEATS = 8;

    // Family Language

    interface Person extends Structure {
    }

    interface PersonCons extends Person, Constant<Person> {
    }

    interface PersonFunc extends Person, Function<Person> {
    }

    static Functor<PersonCons> STRING_PERSON = functor((SerializableFunction<String, PersonCons>) NelumboTest::person, //
            (RenderLambda) p -> p.get(1).toString());

    static PersonCons person(String name) {
        return constant(STRING_PERSON, name);
    }

    static Functor<PersonCons> INTEGER_PERSON = functor((SerializableFunction<IntegerCons, PersonCons>) NelumboTest::person, //
            (RenderLambda) p -> "P(" + p.get(1).toString() + ")");

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

    static Functor<Relation> PARENT_CHILD = functor(NelumboTest::parentChild, //
            (RenderLambda) s -> "pc(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation parentChild(PersonCons parent, PersonCons child) {
        return rel(PARENT_CHILD, parent, child);
    }

    static Functor<PersonFunc> PARENT = functor(NelumboTest::parent, //
            (RenderLambda) s -> "p(" + s.toString(1) + ")");

    static PersonFunc parent(Person child) {
        return function(PARENT, child);
    }

    static Functor<PersonFunc> CHILD = functor(NelumboTest::child, //
            (RenderLambda) s -> "c(" + s.toString(1) + ")");

    static PersonFunc child(Person parent) {
        return function(CHILD, parent);
    }

    static Functor<Relation> ANCESTOR_DESCENTANT = functor(NelumboTest::ancestorDescendant, //
            (RenderLambda) s -> "ad(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation ancestorDescendant(PersonCons ancestor, PersonCons descendant) {
        return rel(ANCESTOR_DESCENTANT, ancestor, descendant);
    }

    static Functor<PersonFunc> ANCESTOR = functor(NelumboTest::ancestor, //
            (RenderLambda) s -> "a(" + s.toString(1) + ")");

    static PersonFunc ancestor(Person descendant) {
        return function(ANCESTOR, descendant);
    }

    static Functor<PersonFunc> DESCENDANT = functor(NelumboTest::descendant, //
            (RenderLambda) s -> "d(" + s.toString(1) + ")");

    static PersonFunc descendant(Person ancestor) {
        return function(DESCENDANT, ancestor);
    }

    // Root Language

    interface Root extends Structure {
    }

    interface RootCons extends Root, Constant<Root> {
    }

    interface RootFunc extends Root, Function<Root> {
    }

    static Functor<RootCons> ROOT = functor((SerializableFunction<IntegerCons, RootCons>) NelumboTest::root, //
            (RenderLambda) p -> "R(" + p.get(1).toString() + ")");

    static RootCons root(IntegerCons i) {
        return constant(ROOT, i);
    }

    static RootCons root(int i) {
        return root(i(i));
    }

    static RootCons rootConsVar(String name) {
        return variable(RootCons.class, name);
    }

    static Root rootVar(String name) {
        return variable(Root.class, name);
    }

    static Functor<Relation> ROOT_PERSON = functor(NelumboTest::rootPerson, //
            (RenderLambda) s -> "rp(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation rootPerson(RootCons root, PersonCons person) {
        return rel(ROOT_PERSON, root, person);
    }

    static Functor<RootFunc> ROOT_FUNC = functor((SerializableFunction<Person, RootFunc>) NelumboTest::root, //
            (RenderLambda) p -> "r(" + p.get(1).toString() + ")");

    static RootFunc root(Person person) {
        return function(ROOT_FUNC, person);
    }

    // Fibonacci Language

    static Functor<Relation> fib2 = functor((SerializableBiFunction<IntegerCons, IntegerCons, Relation>) NelumboTest::fib);

    static Relation fib(IntegerCons i, IntegerCons f) {
        return rel(fib2, i, f);
    }

    static Functor<IntegerFunc> fib1 = functor((SerializableFunction<Integer, IntegerFunc>) NelumboTest::fib);

    static IntegerFunc fib(Integer i) {
        return function(fib1, i);
    }

    // Collect Example Language

    static Functor<Relation> PERSON_AMOUNT = functor(NelumboTest::personAmount, //
            (RenderLambda) s -> "pa(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation personAmount(PersonCons person, IntegerCons amount) {
        return rel(PERSON_AMOUNT, person, amount);
    }

    static Functor<Relation> PERSON_TOTAL = functor(NelumboTest::personTotal, //
            (RenderLambda) s -> "pt(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation personTotal(PersonCons person, IntegerCons amount) {
        return rel(PERSON_TOTAL, person, amount);
    }

    static Functor<Relation> PERSON_NUMBER = functor(NelumboTest::personNumber, //
            (RenderLambda) s -> "pn(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation personNumber(PersonCons person, IntegerCons number) {
        return rel(PERSON_NUMBER, person, number);
    }

    // Variables

    IntegerCons  O        = iConsVar("O");
    IntegerCons  P        = iConsVar("P");
    IntegerCons  Q        = iConsVar("Q");

    Integer      R        = iVar("R");
    Integer      S        = iVar("S");

    RationalCons T        = rConsVar("T");
    RationalCons U        = rConsVar("U");

    PersonCons   A        = personConsVar("A");
    PersonCons   B        = personConsVar("B");
    PersonCons   C        = personConsVar("C");

    Person       X        = personVar("X");
    Person       Y        = personVar("Y");
    Person       Z        = personVar("Z");

    RootCons     V        = rootConsVar("V");
    Root         W        = rootVar("W");

    // Constants

    PersonCons   Carel    = person("Carel");
    PersonCons   Jan      = person("Jan");
    PersonCons   Elske    = person("Elske");
    PersonCons   Wim      = person("Wim");
    PersonCons   Joppe    = person("Joppe");
    PersonCons   Heleen   = person("Heleen");
    PersonCons   Marijn   = person("Marijn");
    PersonCons   Jannette = person("Jannette");

    RootCons     Root     = root(1);

    // Family Rules

    private void familyRules() {
        isRules();

        rule(ancestorDescendant(A, C), parentChild(A, C));
        rule(ancestorDescendant(A, C), and(ancestorDescendant(A, B), parentChild(B, C)));

        rule(is(parent(X), A), and(is(X, B), parentChild(A, B)));
        rule(is(child(X), A), and(is(X, B), parentChild(B, A)));

        rule(is(ancestor(X), A), and(is(X, B), ancestorDescendant(A, B)));
        rule(is(descendant(X), A), and(is(X, B), ancestorDescendant(B, A)));
    }

    // Root Rules

    private void rootRules() {
        integerRules();

        rule(parentChild(person(Q), person(P)), and(lt(Q, i(4)), ge(Q, i(0)), is(plus(Q, i(1)), P)));
        rule(rootPerson(V, person(0)), T());
        rule(rootPerson(V, C), and(rootPerson(V, A), parentChild(A, C)));

        rule(is(parent(X), A), and(is(X, B), parentChild(A, B)));
        rule(is(child(X), A), and(is(X, B), parentChild(B, A)));
        rule(is(root(X), V), and(is(X, B), rootPerson(V, B)));
    }

    // Fibonacci Rules

    private void fibonacciRules() {
        integerRules();

        rule(fib(P, Q), and(ge(P, i(0)), le(P, i(1)), eq(Q, P)));
        rule(fib(P, Q), and(gt(P, i(1)), is(plus(fib(minus(P, i(1))), fib(minus(P, i(2)))), Q)));

        rule(is(fib(R), Q), and(is(R, P), fib(P, Q)));
    }

    // Collect Example Rules

    private void collectExampleRules() {
        integerRules();
        familyRules();

        rule(personTotal(A, O), coll(and(parentChild(A, B), personAmount(B, P)), is(plus(i(0), P), O)));
        rule(personNumber(A, O), coll(and(parentChild(A, B), eq(i(1), P)), is(plus(i(0), P), O)));
    }

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
            isTrue(is(descendant(Carel), Marijn));

            isFalse(is(descendant(Marijn), Wim));
            isFalse(is(descendant(Heleen), Wim));
            isFalse(is(descendant(Joppe), Carel));
            isFalse(is(descendant(Carel), Carel));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
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

            isTrue(ancestorDescendant(Wim, Marijn));
            isTrue(ancestorDescendant(Carel, Marijn));

            isFalse(ancestorDescendant(Marijn, Wim));
            isFalse(ancestorDescendant(Heleen, Wim));
            isFalse(ancestorDescendant(Joppe, Carel));
            isFalse(ancestorDescendant(Carel, Carel));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void famTest2() {
        run(() -> {
            familyRules();

            fact(parentChild(Carel, Jan));
            fact(parentChild(Jan, Wim));

            hasBindings(and(parentChild(Carel, B), parentChild(B, Wim)), binding(B, Jan));

            hasBindings(ancestorDescendant(Carel, C), binding(C, Jan), binding(C, Wim));
            hasBindings(ancestorDescendant(A, Wim), binding(A, Jan), binding(A, Carel));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void famTest3() {
        run(() -> {
            fact(parentChild(Carel, Jan));
            fact(parentChild(Jan, Wim));
            fact(parentChild(Wim, Marijn));

            isTrue(and(parentChild(Carel, A), parentChild(A, B), parentChild(B, Marijn)));
            isTrue(not(or(not(parentChild(Carel, A)), not(parentChild(A, B)), not(parentChild(B, Marijn)))));
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

            hasBindings(not(is(plus(i(5), i(2)), O)));

            familyRules();

            fact(parentChild(Carel, Jan));
            fact(parentChild(Jan, Wim));

            hasBindings(ancestorDescendant(Carel, C), //
                    binding(C, Jan), binding(C, Wim));

            hasBindings(not(not(ancestorDescendant(Carel, C))), //
                    binding(C, Jan), binding(C, Wim));

            hasBindings(ancestorDescendant(A, Wim), //
                    binding(A, Jan), binding(A, Carel));

            hasBindings(not(not(ancestorDescendant(A, Wim))), //
                    binding(A, Jan), binding(A, Carel));

            hasBindings(and(ancestorDescendant(A, Wim), ancestorDescendant(A, Wim)), //
                    binding(A, Jan), binding(A, Carel));

            hasBindings(not(or(not(ancestorDescendant(A, Wim)), not(ancestorDescendant(A, Wim)))), //
                    binding(A, Jan), binding(A, Carel));

            hasBindings(not(or(not(ancestorDescendant(A, Wim)), not(ancestorDescendant(Carel, C)))), //
                    binding(A, Jan, C, Jan), binding(A, Jan, C, Wim), //
                    binding(A, Carel, C, Jan), binding(A, Carel, C, Wim));
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
            query5 = and(is(plus(i(7), P), O), is(plus(i(8), Q), O));
            hasResult(query5, Set.of(), false, Set.of(), false);
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
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

    @RepeatedTest(NR_OF_REPEATS)
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

    @RepeatedTest(NR_OF_REPEATS)
    public void fibonacciTest0() {
        run(() -> {
            fibonacciRules();

            hasBindings(fib(i(1), P), binding(P, i(1)));
            hasBindings(fib(i(6), P), binding(P, i(8)));
        });
    }

    @RepeatedTest(NR_OF_FIB_REPEATS)
    public void fibonacciTest1() {
        run(() -> {
            fibonacciRules();

            hasBindings(fib(i(1), P), binding(P, i(1)));
            hasBindings(fib(i(7), P), binding(P, i(13)));
            hasBindings(fib(i(21), P), binding(P, i(10946)));
            hasBindings(fib(i(1000), P), binding(P, i("18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf", Character.MAX_RADIX)));
        });
    }

    @RepeatedTest(NR_OF_FIB_REPEATS)
    public void fibonacciTest2() {
        run(() -> {
            fibonacciRules();

            hasBindings(is(fib(i(7)), P), binding(P, i(13)));
            hasBindings(is(fib(i(21)), P), binding(P, i(10946)));
            hasBindings(is(fib(i(1000)), P), binding(P, i("18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf", Character.MAX_RADIX)));
        });
    }

    @RepeatedTest(NR_OF_REPEATS)
    public void collectExampleTest() {
        run(() -> {
            collectExampleRules();

            fact(parentChild(Jan, Wim));
            fact(parentChild(Jan, Carel));
            fact(parentChild(Jan, Jannette));
            fact(parentChild(Elske, Wim));
            fact(parentChild(Elske, Carel));
            fact(parentChild(Elske, Jannette));
            fact(personAmount(Wim, i(3)));
            fact(personAmount(Carel, i(7)));
            fact(personAmount(Jannette, i(7)));

            hasBindings(personTotal(Jan, O), binding(O, i(17)));
            hasBindings(personNumber(Jan, O), binding(O, i(3)));

            isTrue(personTotal(Elske, i(17)));
            isFalse(personNumber(Elske, i(4)));
        });
    }

    // @RepeatedTest(1)
    public void factsAndRulesPrintTest() {
        KnowledgeBase db = run(() -> {
            fact(parentChild(Carel, Jan));
            fact(parentChild(Jan, Wim));
            fact(parentChild(Elske, Wim));
            fact(parentChild(Wim, Joppe));
            fact(parentChild(Heleen, Joppe));
            fact(parentChild(Wim, Marijn));
            fact(parentChild(Heleen, Marijn));

            fibonacciRules();
            collectExampleRules();
        });
        print(db);
    }

}
