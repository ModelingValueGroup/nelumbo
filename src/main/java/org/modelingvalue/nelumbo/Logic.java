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

package org.modelingvalue.nelumbo;

import java.lang.reflect.Proxy;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.LambdaReflection;
import org.modelingvalue.collections.util.SerializableBiFunction;
import org.modelingvalue.collections.util.SerializableFunction;
import org.modelingvalue.collections.util.SerializableQuadFunction;
import org.modelingvalue.collections.util.SerializableSupplier;
import org.modelingvalue.collections.util.SerializableTriFunction;
import org.modelingvalue.nelumbo.impl.*;

public final class Logic {

    private Logic() {
    }

    // Run

    public static final KnowledgeBase run(Runnable runnable) {
        return KnowledgeBaseImpl.run(runnable, null);
    }

    public static final KnowledgeBase run(Runnable runnable, KnowledgeBase init) {
        return KnowledgeBaseImpl.run(runnable, (KnowledgeBaseImpl) init);
    }

    // Structures

    public interface Structure {
    }

    @SuppressWarnings("unchecked")
    public static <C extends Constant<T>, T extends Structure> C constant(Functor<C> functor, Object... args) {
        return new StructureImpl<C>(functor, args).normal().proxy();
    }

    @SuppressWarnings("unchecked")
    public static <F extends Function<T>, T extends Structure> F function(Functor<F> functor, Object... args) {
        return new StructureImpl<F>(functor, args).proxy();
    }

    // Functor

    public interface Functor<T extends Structure> extends Constant<Functor<T>> {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure> Functor<T> functor(SerializableSupplier<T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of(method, modifiers).proxy();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A> Functor<T> functor(SerializableFunction<A, T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of(method, modifiers).proxy();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B> Functor<T> functor(SerializableBiFunction<A, B, T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of(method, modifiers).proxy();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B, C> Functor<T> functor(SerializableTriFunction<A, B, C, T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of(method, modifiers).proxy();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B, C, D> Functor<T> functor(SerializableQuadFunction<A, B, C, D, T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of(method, modifiers).proxy();
    }

    public enum RuleModifier {
        trace;
    }

    public interface FunctorModifier {
    }

    public enum FunctorModifierEnum implements FunctorModifier {
        factual,
        derived,
    }

    @SuppressWarnings("rawtypes")
    @FunctionalInterface
    public interface LogicLambda extends BiFunction<RelationImpl, InferContext, InferResult>, LambdaReflection, FunctorModifier {

        @Override
        default LogicLambdaImpl of() {
            return this instanceof LogicLambdaImpl ? (LogicLambdaImpl) this : new LogicLambdaImpl(this);
        }

        class LogicLambdaImpl extends LambdaImpl<LogicLambda> implements LogicLambda {
            private static final long serialVersionUID = 3085315666688472574L;

            public LogicLambdaImpl(LogicLambda f) {
                super(f);
            }

            @SuppressWarnings("unchecked")
            @Override
            public final InferResult apply(RelationImpl relation, InferContext context) {
                return f.apply(relation, context);
            }

        }
    }

    @SuppressWarnings("rawtypes")
    @FunctionalInterface
    public interface NormalizeLambda extends UnaryOperator<StructureImpl<Structure>>, LambdaReflection, FunctorModifier {

        @Override
        default NormalizeLambdaImpl of() {
            return this instanceof NormalizeLambdaImpl ? (NormalizeLambdaImpl) this : new NormalizeLambdaImpl(this);
        }

        class NormalizeLambdaImpl extends LambdaImpl<NormalizeLambda> implements NormalizeLambda {
            private static final long serialVersionUID = -9099528018203410620L;

            public NormalizeLambdaImpl(NormalizeLambda f) {
                super(f);
            }

            @SuppressWarnings("unchecked")
            @Override
            public final StructureImpl<Structure> apply(StructureImpl<Structure> structure) {
                return f.apply(structure);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    @FunctionalInterface
    public interface ToStringLambda extends java.util.function.Function<StructureImpl<Structure>, String>, LambdaReflection, FunctorModifier {

        @Override
        default ToStringLambdaImpl of() {
            return this instanceof ToStringLambdaImpl ? (ToStringLambdaImpl) this : new ToStringLambdaImpl(this);
        }

        class ToStringLambdaImpl extends LambdaImpl<ToStringLambda> implements ToStringLambda {
            private static final long serialVersionUID = -9099528018203410620L;

            public ToStringLambdaImpl(ToStringLambda f) {
                super(f);
            }

            @SuppressWarnings("unchecked")
            @Override
            public final String apply(StructureImpl<Structure> structure) {
                return f.apply(structure);
            }
        }
    }

    // Variables

    public interface Variable extends Constant<Variable> {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <F extends Structure> F variable(Class<F> type, String id) {
        return new VariableImpl<F>(type, id).proxy();
    }

    // Predicates

    public interface Predicate extends Structure {
    }

    public interface Relation extends Predicate {
    }

    public static boolean isTrue(Predicate pred) {
        InferResult result = infer(pred);
        return !result.facts().isEmpty();
    }

    public static boolean isFalse(Predicate pred) {
        InferResult result = infer(pred);
        return result.facts().isEmpty();
    }

    public static boolean isEqual(Predicate pred1, Predicate pred2) {
        InferResult result1 = infer(pred1);
        InferResult result2 = infer(pred2);
        PredicateImpl<?> impl1 = StructureImpl.<Predicate, PredicateImpl<?>> unproxy(pred1);
        PredicateImpl<?> impl2 = StructureImpl.<Predicate, PredicateImpl<?>> unproxy(pred2);
        return getBindings(result1.facts(), impl1).equals(getBindings(result2.facts(), impl2)) && //
                getBindings(result1.falsehoods(), impl1).equals(getBindings(result2.falsehoods(), impl2));
    }

    public static Set<Predicate> getFacts(Predicate pred) {
        return infer(pred).facts().replaceAll(StructureImpl::proxy);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Set<Map<Variable, Object>> getBindings(Predicate pred) {
        InferResult result = infer(pred);
        PredicateImpl impl = StructureImpl.<Predicate, PredicateImpl> unproxy(pred);
        return getBindings(result.facts(), impl);
    }

    @SuppressWarnings("rawtypes")
    private static Set<Map<Variable, Object>> getBindings(Set<PredicateImpl<?>> set, PredicateImpl<?> declaration) {
        Set<Map<VariableImpl, Object>> bindings = set.replaceAll(PredicateImpl::getBinding);
        return bindings.replaceAll(m -> m.replaceAll(e -> Entry.of((Variable) e.getKey().proxy(), proxy(e.getValue()))));
    }

    public static <T extends Structure> Map<Variable, Object> binding(T var, Constant<T> val) {
        return Map.of(Entry.of((Variable) var, val));
    }

    public static <T1 extends Structure, T2 extends Structure> Map<Variable, Object> binding(T1 var1, Constant<T1> val1, T2 var2, Constant<T2> val2) {
        return Map.of(Entry.of((Variable) var1, val1), Entry.of((Variable) var2, val2));
    }

    public static <T1 extends Structure, T2 extends Structure, T3 extends Structure> Map<Variable, Object> binding(T1 var1, Constant<T1> val1, T2 var2, Constant<T2> val2, T3 var3, Constant<T3> val3) {
        return Map.of(Entry.of((Variable) var1, val1), Entry.of((Variable) var2, val2), Entry.of((Variable) var3, val3));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static InferResult infer(Predicate pred) {
        return StructureImpl.<Predicate, PredicateImpl> unproxy(pred).infer();
    }

    @SuppressWarnings("rawtypes")
    private static final Object proxy(Object object) {
        return object instanceof StructureImpl ? ((StructureImpl) object).proxy() : object;
    }

    @SuppressWarnings("unchecked")
    public static <R extends Relation> R rel(Functor<R> functor, Object... args) {
        return (R) new RelationImpl((Functor<Relation>) functor, args).proxy();
    }

    // True

    public interface Bool extends Predicate {
    }

    private static final Bool TRUE_PROXY = (Bool) Proxy.newProxyInstance(Bool.class.getClassLoader(), new Class[]{Bool.class}, BooleanImpl.TRUE);

    @SuppressWarnings("unchecked")
    public static Bool T() {
        return TRUE_PROXY;
    }

    // False

    private static final Bool FALSE_PROXY = (Bool) Proxy.newProxyInstance(Bool.class.getClassLoader(), new Class[]{Bool.class}, BooleanImpl.FALSE);

    @SuppressWarnings("unchecked")
    public static Bool F() {
        return FALSE_PROXY;
    }

    // Not

    public interface Not extends Predicate {
    }

    @SuppressWarnings("unchecked")
    public static Not not(Predicate pred) {
        return new NotImpl(pred).proxy();
    }

    // Or

    public interface AndOr extends Predicate {
    }

    public interface Or extends AndOr {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Predicate or(Predicate... ps) {
        PredicateImpl<? extends Predicate> impl = BooleanImpl.FALSE;
        int l = ps.length - 1;
        for (int i = l; i >= 0; i--) {
            impl = i == l ? StructureImpl.unproxy(ps[i]) : new OrImpl(StructureImpl.unproxy(ps[i]), impl);
        }
        return impl.proxy();
    }

    // And

    public interface And extends AndOr {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Predicate and(Predicate... ps) {
        PredicateImpl<? extends Predicate> impl = BooleanImpl.TRUE;
        int l = ps.length - 1;
        for (int i = l; i >= 0; i--) {
            impl = i == l ? StructureImpl.unproxy(ps[i]) : new AndImpl(StructureImpl.unproxy(ps[i]), impl);
        }
        return impl.proxy();
    }

    // Rules

    public interface Rule extends Constant<Rule> {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Rule rule(Relation consequence, Predicate condition, RuleModifier... modifiers) {
        RuleImpl ruleImpl = new RuleImpl(consequence, condition, modifiers);
        KnowledgeBaseImpl.CURRENT.get().addRule(ruleImpl);
        return ruleImpl.proxy();
    }

    // Facts

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void fact(Relation relation) {
        RelationImpl unproxy = StructureImpl.<Relation, RelationImpl> unproxy(relation);
        KnowledgeBaseImpl.CURRENT.get().addFact(unproxy);
    }

    // Equals

    public interface Constant<T extends Structure> extends Structure {
    }

    @SuppressWarnings("rawtypes")
    private static Functor<Relation> EQ_FUNCTOR = Logic.<Relation, Constant, Constant> functor(Logic::eq, (LogicLambda) Logic::eqLogic, //
            (ToStringLambda) s -> s.toString(1) + "\u2A75" + s.toString(2));

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static InferResult eqLogic(RelationImpl relation, InferContext context) {
        StructureImpl constant1 = relation.getVal(1);
        StructureImpl constant2 = relation.getVal(2);
        if (constant1 == null && constant2 == null) {
            return relation.unknown();
        } else if (constant1 == null) {
            return InferResult.trueFalse(relation.set(1, constant2).singleton(), relation.singleton());
        } else if (constant2 == null) {
            return InferResult.trueFalse(relation.set(2, constant1).singleton(), relation.singleton());
        } else {
            StructureImpl eq = constant1.eq(constant2);
            return eq != null ? relation.set(1, eq, eq).fact() : relation.falsehood();
        }
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Structure> Relation eq(Constant<T> a, Constant<T> b) {
        return rel(EQ_FUNCTOR, a, b);
    }

    // Is

    public interface Function<T extends Structure> extends Structure {
    }

    @SuppressWarnings("rawtypes")
    private static final Functor<Relation> IS_FUNCTOR = Logic.<Relation, Structure, Structure> functor(Logic::is, //
            (ToStringLambda) s -> s.toString(1) + "=" + s.toString(2));

    private static <T extends Structure> Relation is(T a, T b) {
        return rel(IS_FUNCTOR, a, b);
    }

    // Use this one for function definitions
    public static <T extends Structure> Relation is(T a, Constant<T> b) {
        return rel(IS_FUNCTOR, a, b);
    }

    // Implied by the above using the generic rules here
    public static <T extends Structure> Relation is(Constant<T> a, T b) {
        return rel(IS_FUNCTOR, a, b);
    }

    @SuppressWarnings("rawtypes")
    private static final Constant A1 = variable(Constant.class, "A1");
    @SuppressWarnings("rawtypes")
    private static final Constant A2 = variable(Constant.class, "A2");
    @SuppressWarnings("rawtypes")
    private static final Function F1 = variable(Function.class, "F1");
    @SuppressWarnings("rawtypes")
    private static final Function F2 = variable(Function.class, "F2");

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void isRules() {
        rule(is((Structure) A1, (Structure) A2), eq(A1, A2));
        rule(is(F1, F2), and(is(F2, A2), is(F1, A2)));
        rule(is(A1, F1), is(F1, A1));
    }

}
