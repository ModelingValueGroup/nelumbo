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

    public interface FunctorModifier {
    }

    public enum FunctorModifierEnum implements FunctorModifier {
        factual,
        derived,
    }

    @SuppressWarnings("rawtypes")
    @FunctionalInterface
    public interface LogicLambda extends BiFunction<PredicateImpl, InferContext, InferResult>, LambdaReflection, FunctorModifier {

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
            public final InferResult apply(PredicateImpl predicate, InferContext context) {
                return f.apply(predicate, context);
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

    public static Set<Predicate> getFacts(Predicate pred) {
        return infer(pred).facts().replaceAll(StructureImpl::proxy);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Set<Map<Variable, Object>> getBindings(Predicate pred) {
        PredicateImpl impl = StructureImpl.<Predicate, PredicateImpl> unproxy(pred);
        InferResult result = infer(pred);
        Set<Map<VariableImpl, Object>> bindings = result.facts().replaceAll(m -> impl.getBinding(m, Map.of()));
        return bindings.replaceAll(m -> m.replaceAll(e -> Entry.of((Variable) e.getKey().proxy(), proxy(e.getValue()))));
    }

    public static <T extends Structure> Map<Variable, Object> binding(T var, Constant<T> val) {
        return Map.of(Entry.of((Variable) var, val));
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
    public static <P extends Predicate> P pred(Functor<P> functor, Object... args) {
        return (P) new PredicateImpl((Functor<Predicate>) functor, args).proxy();
    }

    // Collect

    @SuppressWarnings("unchecked")
    public static Relation collect(Predicate pred, Predicate accum) {
        return (Relation) new CollectImpl(pred, accum).proxy();
    }

    // True

    private static final Relation TRUE_PROXY = (Relation) Proxy.newProxyInstance(Predicate.class.getClassLoader(), new Class[]{Relation.class}, TrueImpl.TRUE);

    @SuppressWarnings("unchecked")
    public static Relation T() {
        return TRUE_PROXY;
    }

    // False

    private static final Relation FALSE_PROXY = (Relation) Proxy.newProxyInstance(Predicate.class.getClassLoader(), new Class[]{Relation.class}, FalseImpl.FALSE);

    @SuppressWarnings("unchecked")
    public static Relation F() {
        return FALSE_PROXY;
    }

    // Not

    @SuppressWarnings("unchecked")
    public static Predicate not(Predicate pred) {
        return new NotImpl(pred).proxy();
    }

    // Or

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Predicate or(Predicate... ps) {
        PredicateImpl impl = FalseImpl.FALSE;
        for (int i = ps.length - 1; i >= 0; i--) {
            impl = impl == FalseImpl.FALSE ? StructureImpl.unproxy(ps[i]) : new OrImpl(StructureImpl.unproxy(ps[i]), impl);
        }
        return impl.proxy();
    }

    // And

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Predicate and(Predicate... ps) {
        PredicateImpl impl = TrueImpl.TRUE;
        for (int i = ps.length - 1; i >= 0; i--) {
            impl = impl == TrueImpl.TRUE ? StructureImpl.unproxy(ps[i]) : new AndImpl(StructureImpl.unproxy(ps[i]), impl);
        }
        return impl.proxy();
    }

    // Rules

    public interface Rule extends Constant<Rule> {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Rule rule(Relation consequence, Predicate condition) {
        RuleImpl ruleImpl = new RuleImpl(consequence, condition);
        KnowledgeBaseImpl.CURRENT.get().addRule(ruleImpl);
        return ruleImpl.proxy();
    }

    // Facts

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void fact(Relation pred) {
        KnowledgeBaseImpl.CURRENT.get().addFact(StructureImpl.<Predicate, PredicateImpl> unproxy(pred));
    }

    // Equals

    public interface Constant<T extends Structure> extends Structure {
    }

    @SuppressWarnings("rawtypes")
    private static Functor<Relation> EQ_FUNCTOR = Logic.<Relation, Constant, Constant> functor(Logic::eq, (LogicLambda) Logic::eqLogic, //
            (ToStringLambda) s -> s.toString(1) + "=" + s.toString(2));

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static InferResult eqLogic(PredicateImpl predicate, InferContext context) {
        StructureImpl constant1 = predicate.getVal(1);
        StructureImpl constant2 = predicate.getVal(2);
        if (constant1 == null && constant2 == null) {
            return predicate.incomplete();
        } else if (constant1 == null) {
            return InferResult.trueFalse(predicate.set(1, constant2).singleton(), predicate.singleton());
        } else if (constant2 == null) {
            return InferResult.trueFalse(predicate.set(2, constant1).singleton(), predicate.singleton());
        } else {
            StructureImpl eq = constant1.eq(constant2);
            return eq != null ? //
                    InferResult.trueFalse(predicate.set(1, eq, eq).singleton(), Set.of()) : //
                    InferResult.trueFalse(Set.of(), predicate.singleton());
        }
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Structure> Relation eq(Constant<T> a, Constant<T> b) {
        return pred(EQ_FUNCTOR, a, b);
    }

    // Is

    public interface Function<T extends Structure> extends Structure {
    }

    @SuppressWarnings("rawtypes")
    private static final Functor<Relation> IS_FUNCTOR = Logic.<Relation, Structure, Structure> functor(Logic::is, //
            (ToStringLambda) s -> s.toString(1) + "=" + s.toString(2));

    private static <T extends Structure> Relation is(T a, T b) {
        return pred(IS_FUNCTOR, a, b);
    }

    // Use this one for function definitions
    public static <T extends Structure> Relation is(T a, Constant<T> b) {
        return pred(IS_FUNCTOR, a, b);
    }

    // Implied by the above using the generic rules here
    public static <T extends Structure> Relation is(Constant<T> a, T b) {
        return pred(IS_FUNCTOR, a, b);
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
