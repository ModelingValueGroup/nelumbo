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

package org.modelingvalue.logic.impl;

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
import org.modelingvalue.logic.Logic.Functor;
import org.modelingvalue.logic.Logic.FunctorModifier;
import org.modelingvalue.logic.Logic.FunctorModifierEnum;
import org.modelingvalue.logic.Logic.LogicLambda;
import org.modelingvalue.logic.Logic.NormalizeLambda;
import org.modelingvalue.logic.Logic.Structure;

public final class FunctorImpl<T extends Structure> extends StructureImpl<Functor<T>> {
    private static final long     serialVersionUID = 285147889847599160L;

    private final LogicLambda     logic;
    private final NormalizeLambda normal;
    private final boolean         factual;
    private final boolean         derived;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public FunctorImpl(Class<T> type, String name, List<Class<?>> args, FunctorModifier... modifiers) {
        super((Class) Functor.class, type, name, args);
        KnowledgeBaseImpl.updateSpecializations(type);
        for (Class arg : args) {
            KnowledgeBaseImpl.updateSpecializations(arg);
        }
        this.logic = logic(modifiers);
        this.normal = normal(modifiers);
        this.factual = has(FunctorModifierEnum.factual, modifiers);
        this.derived = has(FunctorModifierEnum.derived, modifiers);
    }

    private static LogicLambda logic(FunctorModifier... modifiers) {
        LogicLambda lambda = get(LogicLambda.class, modifiers);
        return lambda != null ? lambda.of() : null;
    }

    private static NormalizeLambda normal(FunctorModifier... modifiers) {
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private FunctorImpl(Object[] args) {
        super(args);
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return ((String) get(2));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected FunctorImpl<T> struct(Object[] array) {
        return new FunctorImpl<T>(array);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<Functor<T>> type() {
        return (Class) Functor.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Class> args() {
        return (List<Class>) get(3);
    }

    public LogicLambda logic() {
        return logic;
    }

    public NormalizeLambda functNormal() {
        return normal;
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

    @Override
    public FunctorImpl<T> set(int i, Object... a) {
        return (FunctorImpl<T>) super.set(i, a);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B, C, D> FunctorImpl<T> of(SerializableQuadFunction<A, B, C, D, T> method, FunctorModifier... modifiers) {
        SerializableQuadFunctionImpl<A, B, C, D, T> l = method.of();
        return new FunctorImpl<T>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B, C> FunctorImpl<T> of(SerializableTriFunction<A, B, C, T> method, FunctorModifier... modifiers) {
        SerializableTriFunctionImpl<A, B, C, T> l = method.of();
        return new FunctorImpl<T>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A, B> FunctorImpl<T> of(SerializableBiFunction<A, B, T> method, FunctorModifier... modifiers) {
        SerializableBiFunctionImpl<A, B, T> l = method.of();
        return new FunctorImpl<T>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure, A> FunctorImpl<T> of(SerializableFunction<A, T> method, FunctorModifier... modifiers) {
        SerializableFunctionImpl<A, T> l = method.of();
        return new FunctorImpl<T>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Structure> FunctorImpl<T> of(SerializableSupplier<T> method, FunctorModifier... modifiers) {
        SerializableSupplierImpl<T> l = method.of();
        return new FunctorImpl<T>((Class<T>) l.out(), l.getImplMethodName(), l.in(), modifiers);
    }

}
