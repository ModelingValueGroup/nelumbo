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
import org.modelingvalue.collections.util.SerializableFunction;
import org.modelingvalue.nelumbo.Integers.IntegerCons;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Logic.Constant;
import org.modelingvalue.nelumbo.Logic.Function;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.RenderLambda;
import org.modelingvalue.nelumbo.Logic.Structure;

public class FamilyTest extends NelumboTestBase {

    static {
        System.setProperty("PARALLEL_COLLECTIONS", "false");

        System.setProperty("REVERSE_NELUMBO", "false");
        System.setProperty("RANDOM_NELUMBO", "true");

        System.setProperty("TRACE_NELUMBO", "false");
        System.setProperty("PRETTY_NELUMBO", "true");
    }

    static final int NR_OF_REPEATS = 32;

    // Family Language

    interface Person extends Structure {
    }

    interface PersonCons extends Person, Constant<Person> {
    }

    interface PersonFunc extends Person, Function<Person> {
    }

    static Functor<PersonCons> STRING_PERSON = functor((SerializableFunction<String, PersonCons>) FamilyTest::person, //
            (RenderLambda) p -> p.get(1).toString());

    static PersonCons person(String name) {
        return constant(STRING_PERSON, name);
    }

    static Functor<PersonCons> INTEGER_PERSON = functor((SerializableFunction<IntegerCons, PersonCons>) FamilyTest::person, //
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

    static Functor<Relation> PARENT_CHILD = functor(FamilyTest::parentChild, //
            (RenderLambda) s -> "pc(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation parentChild(PersonCons parent, PersonCons child) {
        return rel(PARENT_CHILD, parent, child);
    }

    static Functor<PersonFunc> PARENT = functor(FamilyTest::parent, //
            (RenderLambda) s -> "p(" + s.toString(1) + ")");

    static PersonFunc parent(Person child) {
        return function(PARENT, child);
    }

    static Functor<PersonFunc> CHILD = functor(FamilyTest::child, //
            (RenderLambda) s -> "c(" + s.toString(1) + ")");

    static PersonFunc child(Person parent) {
        return function(CHILD, parent);
    }

    static Functor<Relation> ANCESTOR_DESCENTANT = functor(FamilyTest::ancestorDescendant, //
            (RenderLambda) s -> "ad(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation ancestorDescendant(PersonCons ancestor, PersonCons descendant) {
        return rel(ANCESTOR_DESCENTANT, ancestor, descendant);
    }

    static Functor<PersonFunc> ANCESTOR = functor(FamilyTest::ancestor, //
            (RenderLambda) s -> "a(" + s.toString(1) + ")");

    static PersonFunc ancestor(Person descendant) {
        return function(ANCESTOR, descendant);
    }

    static Functor<PersonFunc> DESCENDANT = functor(FamilyTest::descendant, //
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

    static Functor<RootCons> ROOT = functor((SerializableFunction<IntegerCons, RootCons>) FamilyTest::root, //
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

    static Functor<Relation> ROOT_PERSON = functor(FamilyTest::rootPerson, //
            (RenderLambda) s -> "rp(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation rootPerson(RootCons root, PersonCons person) {
        return rel(ROOT_PERSON, root, person);
    }

    static Functor<RootFunc> ROOT_FUNC = functor((SerializableFunction<Person, RootFunc>) FamilyTest::root, //
            (RenderLambda) p -> "r(" + p.get(1).toString() + ")");

    static RootFunc root(Person person) {
        return function(ROOT_FUNC, person);
    }

    // Collect Example Language

    static Functor<Relation> PERSON_AMOUNT = functor(FamilyTest::personAmount, //
            (RenderLambda) s -> "pa(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation personAmount(PersonCons person, IntegerCons amount) {
        return rel(PERSON_AMOUNT, person, amount);
    }

    static Functor<Relation> PERSON_TOTAL = functor(FamilyTest::personTotal, //
            (RenderLambda) s -> "pt(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation personTotal(PersonCons person, IntegerCons amount) {
        return rel(PERSON_TOTAL, person, amount);
    }

    static Functor<Relation> PERSON_NUMBER = functor(FamilyTest::personNumber, //
            (RenderLambda) s -> "pn(" + s.toString(1) + "," + s.toString(2) + ")");

    static Relation personNumber(PersonCons person, IntegerCons number) {
        return rel(PERSON_NUMBER, person, number);
    }

    // Variables

    IntegerCons P        = iConsVar("P");
    IntegerCons Q        = iConsVar("Q");
    IntegerCons R        = iConsVar("R");

    PersonCons  A        = personConsVar("A");
    PersonCons  B        = personConsVar("B");
    PersonCons  C        = personConsVar("C");

    Person      X        = personVar("X");
    Person      Y        = personVar("Y");
    Person      Z        = personVar("Z");

    RootCons    V        = rootConsVar("V");
    Root        W        = rootVar("W");

    // Constants

    PersonCons  Carel    = person("Carel");
    PersonCons  Jan      = person("Jan");
    PersonCons  Elske    = person("Elske");
    PersonCons  Wim      = person("Wim");
    PersonCons  Joppe    = person("Joppe");
    PersonCons  Heleen   = person("Heleen");
    PersonCons  Marijn   = person("Marijn");
    PersonCons  Jannette = person("Jannette");

    RootCons    Root     = root(1);

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

    // Collect Example Rules

    private void collectExampleRules() {
        integerRules();
        familyRules();

        rule(personTotal(A, R), coll(and(parentChild(A, B), personAmount(B, P)), is(plus(i(0), P), R)));
        rule(personNumber(A, R), coll(and(parentChild(A, B), eq(i(1), P)), is(plus(i(0), P), R)));
    }

    // Tests

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
    public void notFamTest() {
        run(() -> {
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

            hasBindings(personTotal(Jan, R), binding(R, i(17)));
            hasBindings(personNumber(Jan, R), binding(R, i(3)));

            isTrue(personTotal(Elske, i(17)));
            isFalse(personNumber(Elske, i(4)));
        });
    }

    // @RepeatedTest(1)
    public void factsAndRulesPrintTest() {
        KnowledgeBase db = run(() -> {
            fact(parentChild(Jan, Wim));
            fact(parentChild(Jan, Carel));
            fact(parentChild(Jan, Jannette));
            fact(parentChild(Elske, Wim));
            fact(parentChild(Elske, Carel));
            fact(parentChild(Elske, Jannette));
            fact(personAmount(Wim, i(3)));
            fact(personAmount(Carel, i(7)));
            fact(personAmount(Jannette, i(7)));

            collectExampleRules();
        });
        print(db);
    }

}
