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

package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.util.SerializableBiFunction;
import org.modelingvalue.collections.util.SerializableBiFunction.SerializableBiFunctionImpl;
import org.modelingvalue.collections.util.SerializableFunction;
import org.modelingvalue.collections.util.SerializableFunction.SerializableFunctionImpl;
import org.modelingvalue.collections.util.SerializableQuadFunction;
import org.modelingvalue.collections.util.SerializableQuadFunction.SerializableQuadFunctionImpl;
import org.modelingvalue.collections.util.SerializableSupplier;
import org.modelingvalue.collections.util.SerializableSupplier.SerializableSupplierImpl;
import org.modelingvalue.collections.util.SerializableTriFunction;
import org.modelingvalue.collections.util.SerializableTriFunction.SerializableTriFunctionImpl;
import org.modelingvalue.nelumbo.Logic.*;

public abstract class FunctorImpl<T extends Structure, F extends Functor<T>> extends StructureImpl<F> {
    private static final long     serialVersionUID = 285147889847599160L;

    private final LogicLambda     logicLambda;
    private final NormalizeLambda normalizeLambda;
    private final RenderLambda    renderLambda;
    private final boolean         factual;
    private final boolean         derived;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private FunctorImpl(Class functorClass, Class<T> type, String name, List<Class<?>> args, FunctorModifier[] modifiers) {
        super(functorClass, type, name, args);
        this.logicLambda = logic(modifiers);
        this.normalizeLambda = normal(modifiers);
        this.renderLambda = render(modifiers);
        this.factual = has(FunctorModifierEnum.factual, modifiers);
        this.derived = has(FunctorModifierEnum.derived, modifiers);
    }

    private RenderLambda render(FunctorModifier[] modifiers) {
        RenderLambda lambda = get(RenderLambda.class, modifiers);
        return lambda != null ? lambda.of() : null;
    }

    private static LogicLambda logic(FunctorModifier[] modifiers) {
        LogicLambda lambda = get(LogicLambda.class, modifiers);
        return lambda != null ? lambda.of() : null;
    }

    private static NormalizeLambda normal(FunctorModifier[] modifiers) {
        NormalizeLambda lambda = get(NormalizeLambda.class, modifiers);
        return lambda != null ? lambda.of() : null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends FunctorModifier> T get(Class<T> t, FunctorModifier[] modifiers) {
        for (FunctorModifier m : modifiers) {
            if (t.isInstance(m)) {
                return (T) m;
            }
        }
        return null;
    }

    private static boolean has(FunctorModifierEnum e, FunctorModifier[] modifiers) {
        for (FunctorModifier m : modifiers) {
            if (m == e) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return ((String) get(2));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected FunctorImpl<T, F> struct(Object[] array) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<F> type() {
        return (Class) get(0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Class> args() {
        return (List<Class>) get(3);
    }

    public LogicLambda logicLambda() {
        return logicLambda;
    }

    public NormalizeLambda normalizeLambda() {
        return normalizeLambda;
    }

    public RenderLambda renderLambda() {
        return renderLambda;
    }

    public boolean factual() {
        return factual;
    }

    public boolean derived() {
        return derived;
    }

    @SuppressWarnings("unchecked")
    protected Class<T> functType() {
        return (Class<T>) get(1);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FunctorImpl<T, F> set(int i, Object... a) {
        return (FunctorImpl<T, F>) super.set(i, a);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B, C, D> FunctorImpl4<T, A, B, C, D> of4(SerializableQuadFunction<A, B, C, D, T> method, FunctorModifier... modifiers) {
        SerializableQuadFunctionImpl<A, B, C, D, T> l = method.of();
        return new FunctorImpl4<T, A, B, C, D>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

    public static final class FunctorImpl4<T extends Structure, A, B, C, D> extends FunctorImpl<T, Functor4<T, A, B, C, D>> {
        private static final long serialVersionUID = 7923040687479253009L;

        private FunctorImpl4(Class<T> type, String name, List<Class<?>> args, FunctorModifier[] modifiers) {
            super(Functor4.class, type, name, args, modifiers);
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B, C> FunctorImpl3<T, A, B, C> of3(SerializableTriFunction<A, B, C, T> method, FunctorModifier... modifiers) {
        SerializableTriFunctionImpl<A, B, C, T> l = method.of();
        return new FunctorImpl3<T, A, B, C>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

    public static final class FunctorImpl3<T extends Structure, A, B, C> extends FunctorImpl<T, Functor3<T, A, B, C>> {
        private static final long serialVersionUID = -3000503193120874295L;

        private FunctorImpl3(Class<T> type, String name, List<Class<?>> args, FunctorModifier[] modifiers) {
            super(Functor3.class, type, name, args, modifiers);
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B> FunctorImpl2<T, A, B> of2(SerializableBiFunction<A, B, T> method, FunctorModifier... modifiers) {
        SerializableBiFunctionImpl<A, B, T> l = method.of();
        return new FunctorImpl2<T, A, B>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

    public static final class FunctorImpl2<T extends Structure, A, B> extends FunctorImpl<T, Functor2<T, A, B>> {
        private static final long serialVersionUID = -3183049692712735358L;

        private FunctorImpl2(Class<T> type, String name, List<Class<?>> args, FunctorModifier[] modifiers) {
            super(Functor2.class, type, name, args, modifiers);
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A> FunctorImpl1<T, A> of1(SerializableFunction<A, T> method, FunctorModifier... modifiers) {
        SerializableFunctionImpl<A, T> l = method.of();
        return new FunctorImpl1<T, A>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

    public static final class FunctorImpl1<T extends Structure, A> extends FunctorImpl<T, Functor1<T, A>> {
        private static final long serialVersionUID = 673008347258770580L;

        private FunctorImpl1(Class<T> type, String name, List<Class<?>> args, FunctorModifier[] modifiers) {
            super(Functor1.class, type, name, args, modifiers);
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure> FunctorImpl0<T> of0(SerializableSupplier<T> method, FunctorModifier... modifiers) {
        SerializableSupplierImpl<T> l = method.of();
        return new FunctorImpl0<T>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

    public static final class FunctorImpl0<T extends Structure> extends FunctorImpl<T, Functor0<T>> {
        private static final long serialVersionUID = 2054896270008795131L;

        private FunctorImpl0(Class<T> type, String name, List<Class<?>> args, FunctorModifier[] modifiers) {
            super(Functor0.class, type, name, args, modifiers);
        }

    }

}
