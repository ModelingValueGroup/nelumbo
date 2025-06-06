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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.*;
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

    // Constants

    public interface Constant extends Structure {
    }

    @SuppressWarnings("unchecked")
    public static <F extends Constant> F constant(Functor0<F> functor) {
        return new StructureImpl<F>(functor).normal().proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, F extends Constant> F constant(Functor1<F, A> functor, A a) {
        return new StructureImpl<F>(functor, a).normal().proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, B, F extends Constant> F constant(Functor2<F, A, B> functor, A a, B b) {
        return new StructureImpl<F>(functor, a, b).normal().proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C, F extends Constant> F constant(Functor3<F, A, B, C> functor, A a, B b, C c) {
        return new StructureImpl<F>(functor, a, b, c).normal().proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C, D, F extends Constant> F constant(Functor4<F, A, B, C, D> functor, A a, B b, C c, D d) {
        return new StructureImpl<F>(functor, a, b, c, d).normal().proxy();
    }

    // Functions

    public interface Function extends Structure {
    }

    @SuppressWarnings("unchecked")
    public static <F extends Function> F function(Functor0<F> functor) {
        return new StructureImpl<F>(functor).normal().proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, F extends Function> F function(Functor1<F, A> functor, A a) {
        return new StructureImpl<F>(functor, a).normal().proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, B, F extends Function> F function(Functor2<F, A, B> functor, A a, B b) {
        return new StructureImpl<F>(functor, a, b).normal().proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C, F extends Function> F function(Functor3<F, A, B, C> functor, A a, B b, C c) {
        return new StructureImpl<F>(functor, a, b, c).normal().proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C, D, F extends Function> F function(Functor4<F, A, B, C, D> functor, A a, B b, C c, D d) {
        return new StructureImpl<F>(functor, a, b, c, d).normal().proxy();
    }

    // Functor

    public interface Functor<T extends Structure> extends Structure {
    }

    public interface Functor0<T extends Structure> extends Functor<T>, Supplier<T> {
    }

    public interface Functor1<T extends Structure, A> extends Functor<T>, java.util.function.Function<A, T> {
    }

    public interface Functor2<T extends Structure, A, B> extends Functor<T>, BiFunction<A, B, T> {
    }

    public interface Functor3<T extends Structure, A, B, C> extends Functor<T>, TriFunction<A, B, C, T> {
    }

    public interface Functor4<T extends Structure, A, B, C, D> extends Functor<T>, QuadFunction<A, B, C, D, T> {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure> Functor0<T> functor0(SerializableSupplier<T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of0(method, modifiers).proxy();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A> Functor1<T, A> functor1(SerializableFunction<A, T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of1(method, modifiers).proxy();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B> Functor2<T, A, B> functor2(SerializableBiFunction<A, B, T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of2(method, modifiers).proxy();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B, C> Functor3<T, A, B, C> functor3(SerializableTriFunction<A, B, C, T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of3(method, modifiers).proxy();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B, C, D> Functor4<T, A, B, C, D> functor4(SerializableQuadFunction<A, B, C, D, T> method, FunctorModifier... modifiers) {
        return FunctorImpl.of4(method, modifiers).proxy();
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

    public static FunctorModifier logic(LogicLambda logic) {
        return logic;
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

    public static FunctorModifier normalize(NormalizeLambda normalize) {
        return normalize;
    }

    @SuppressWarnings("rawtypes")
    @FunctionalInterface
    public interface NormalizeLambda extends UnaryOperator<StructureImpl<?>>, LambdaReflection, FunctorModifier {

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
            public final StructureImpl<?> apply(StructureImpl<?> constant) {
                return f.apply(constant);
            }
        }
    }

    public static FunctorModifier render(RenderLambda render) {
        return render;
    }

    @SuppressWarnings("rawtypes")
    @FunctionalInterface
    public interface RenderLambda extends java.util.function.Function<StructureImpl<Structure>, String>, LambdaReflection, FunctorModifier {

        @Override
        default RenderLambdaImpl of() {
            return this instanceof RenderLambdaImpl ? (RenderLambdaImpl) this : new RenderLambdaImpl(this);
        }

        class RenderLambdaImpl extends LambdaImpl<RenderLambda> implements RenderLambda {
            private static final long serialVersionUID = -9099528018203410620L;

            public RenderLambdaImpl(RenderLambda f) {
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

    public interface Variable extends Structure {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <F extends Structure> F variable(Class<F> type, String name) {
        return new VariableImpl<F>(type, name).proxy();
    }

    // Predicates

    public interface Predicate extends Structure {
    }

    public interface Relation extends Predicate {
    }

    public static boolean isTrue(Predicate pred) {
        InferResult result = infer(pred);
        return result.isTrue();
    }

    public static boolean isFalse(Predicate pred) {
        InferResult result = infer(pred);
        return result.isFalse();
    }

    public static Result getResult(Predicate pred) {
        return new Result(infer(pred));
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

    public static <T extends Structure> Map<Variable, Object> binding(T var, Class<T> type) {
        return Map.of(Entry.of((Variable) var, type));
    }

    public static <T extends Constant> Map<Variable, Object> binding(T var, T val) {
        return Map.of(Entry.of((Variable) var, (Constant) val));
    }

    public static <T1 extends Constant, T2 extends Constant> Map<Variable, Object> binding(T1 var1, T1 val1, T2 var2, T2 val2) {
        return Map.of(Entry.of((Variable) var1, (Constant) val1), Entry.of((Variable) var2, (Constant) val2));
    }

    public static <T1 extends Constant, T2 extends Constant, T3 extends Constant> Map<Variable, Object> binding(T1 var1, T1 val1, T2 var2, T2 val2, T3 var3, T3 val3) {
        return Map.of(Entry.of((Variable) var1, (Constant) val1), Entry.of((Variable) var2, (Constant) val2), Entry.of((Variable) var3, (Constant) val3));
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
    public static Relation relation(Functor0<Relation> functor) {
        return new RelationImpl(functor).proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A> Relation relation(Functor1<Relation, A> functor, A a) {
        return new RelationImpl(functor, a).proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, B> Relation relation(Functor2<Relation, A, B> functor, A a, B b) {
        return new RelationImpl(functor, a, b).proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C> Relation relation(Functor3<Relation, A, B, C> functor, A a, B b, C c) {
        return new RelationImpl(functor, a, b, c).proxy();
    }

    @SuppressWarnings("unchecked")
    public static <A, B, C, D> Relation relation(Functor4<Relation, A, B, C, D> functor, A a, B b, C c, D d) {
        return new RelationImpl(functor, a, b, c, d).proxy();
    }

    // True

    private static final Predicate TRUE_PROXY = (Predicate) Proxy.newProxyInstance(Predicate.class.getClassLoader(), new Class[]{Predicate.class}, BooleanImpl.TRUE);

    @SuppressWarnings("unchecked")
    public static Predicate T() {
        return TRUE_PROXY;
    }

    // False

    private static final Predicate FALSE_PROXY = (Predicate) Proxy.newProxyInstance(Predicate.class.getClassLoader(), new Class[]{Predicate.class}, BooleanImpl.FALSE);

    @SuppressWarnings("unchecked")
    public static Predicate F() {
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
        PredicateImpl<? extends Predicate> impl = BooleanImpl.FALSE;
        int l = ps.length - 1;
        for (int i = l; i >= 0; i--) {
            impl = i == l ? StructureImpl.unproxy(ps[i]) : new OrImpl(StructureImpl.unproxy(ps[i]), impl);
        }
        return impl.proxy();
    }

    // And

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

    public interface Rule extends Structure {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Rule rule(Relation consequence, Predicate condition, RuleModifier... modifiers) {
        RuleImpl ruleImpl = new RuleImpl(consequence, condition, modifiers);
        KnowledgeBaseImpl.CURRENT.get().addRule(ruleImpl);
        return ruleImpl.proxy();
    }

    // Collect

    public static Predicate coll(Predicate condition, Predicate collector) {
        return new CollectImpl(condition, collector).proxy();
    }

    // Facts

    public static void fact(Relation relation) {
        RelationImpl unproxy = StructureImpl.<Relation, RelationImpl> unproxy(relation);
        KnowledgeBaseImpl.CURRENT.get().addFact(unproxy);
    }

    // Is

    @SuppressWarnings("rawtypes")
    private static Functor2<Relation, Constant, Constant> IS_FUNCTOR = functor2(Logic::is, //
            logic(Logic::isLogic), render(s -> s.toString(1) + "\u2261" + s.toString(2)));

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static InferResult isLogic(RelationImpl relation, InferContext context) {
        StructureImpl constant1 = relation.getVal(1);
        StructureImpl constant2 = relation.getVal(2);
        if (constant1 == null && constant2 == null) {
            return relation.unknown();
        } else if (constant1 == null) {
            return relation.set(1, constant2).factCI();
        } else if (constant2 == null) {
            return relation.set(2, constant1).factCI();
        } else {
            StructureImpl eq = constant1.is(constant2);
            return eq != null ? relation.set(1, eq, eq).factCC() : relation.falsehoodCC();
        }
    }

    private static <T extends Constant> Relation is(T a, T b) {
        Class<? extends Constant> ac = a.getClass();
        Class<? extends Constant> bc = b.getClass();
        if (!ac.isAssignableFrom(bc) && !bc.isAssignableFrom(ac)) {
            throw new IllegalArgumentException("Cannot compare " + ac.getName() + " with " + bc.getName());
        }
        return relation(IS_FUNCTOR, a, b);
    }

    // Equals

    @SuppressWarnings("rawtypes")
    private static final Functor2<Relation, Structure, Structure> EQ_FUNCTOR = functor2(Logic::eq, //
            render(s -> s.toString(1) + "=" + s.toString(2)));

    public static <T extends Structure> Relation eq(T a, T b) {
        return relation(EQ_FUNCTOR, a, b);
    }

    @SuppressWarnings("rawtypes")
    private static final Functor2<Relation, Structure, Structure> NE_FUNCTOR = functor2(Logic::ne, //
            render(s -> s.toString(1) + "\u2260" + s.toString(2)));

    public static <T extends Structure> Relation ne(T a, T b) {
        return relation(NE_FUNCTOR, a, b);
    }

    @SuppressWarnings("rawtypes")
    private static final Constant  C1 = variable(Constant.class, "C1");
    @SuppressWarnings("rawtypes")
    private static final Constant  C2 = variable(Constant.class, "C2");

    @SuppressWarnings("rawtypes")
    private static final Function  F1 = variable(Function.class, "F1");
    @SuppressWarnings("rawtypes")
    private static final Function  F2 = variable(Function.class, "F2");

    @SuppressWarnings("rawtypes")
    private static final Structure S1 = variable(Structure.class, "T1");
    @SuppressWarnings("rawtypes")
    private static final Structure S2 = variable(Structure.class, "T2");

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void isRules() {
        rule(eq(C1, C2), is(C1, C2));
        rule(eq(F1, F2), and(eq(F1, C1), eq(F2, C1)));
        rule(eq(C1, F1), eq(F1, C1));
        rule(ne(S1, S2), not(eq(S1, S2)));
    }
}
