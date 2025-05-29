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

import java.lang.reflect.Proxy;

import org.modelingvalue.nelumbo.Logic.Constant;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.NormalizeLambda;
import org.modelingvalue.nelumbo.Logic.Structure;

public final class ConstantImpl<C extends Constant<T>, T extends Structure> extends StructureImpl<C> {
    private static final long serialVersionUID = 3217952328495669539L;

    public ConstantImpl(Functor<C> functor, Object... args) {
        super(functor, args);
    }

    private ConstantImpl(Object[] array) {
        super(array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final C proxy() {
        return (C) Proxy.newProxyInstance(type().getClassLoader(), new Class[]{type(), Constant.class}, this);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ConstantImpl<C, T> struct(Object[] array) {
        return new ConstantImpl<C, T>(array).normal();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ConstantImpl<C, T> set(int i, Object... a) {
        return (ConstantImpl<C, T>) super.set(i, a);
    }

    @SuppressWarnings("unchecked")
    public final ConstantImpl<C, T> normal() {
        FunctorImpl<C> f = functor();
        NormalizeLambda n = f != null ? f.normalizeLambda() : null;
        return n != null ? (ConstantImpl<C, T>) n.apply((ConstantImpl<Constant<Structure>, Structure>) this) : this;
    }

    @SuppressWarnings("unchecked")
    public final ConstantImpl<C, T> is(ConstantImpl<C, T> other) {
        return (ConstantImpl<C, T>) super.eq(other);
    }

}
