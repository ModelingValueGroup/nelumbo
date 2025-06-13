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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Logic.Type;
import org.modelingvalue.nelumbo.Logic.Variable;
import org.modelingvalue.nelumbo.impl.FunctorImpl.FunctorImpl2;

public final class TypeImpl extends StructureImpl<Type> {
    private static final long                                   serialVersionUID = -4583279157841144493L;

    private static final FunctorImpl2<Type, String, List<Type>> TYPE_FUNCTOR     = FunctorImpl.of2(TypeImpl::type);

    private static Type type(String name, List<Type> supers) {
        return null;
    }

    public TypeImpl(String name, List<TypeImpl> supers) {
        super(TYPE_FUNCTOR, name, supers);
    }

    private TypeImpl(Object[] array) {
        super(array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Type proxy() {
        return (Type) Proxy.newProxyInstance(type().getClassLoader(), new Class[]{type(), Variable.class}, this);
    }

    @Override
    public String toString() {
        return get(1).toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected TypeImpl struct(Object[] array) {
        return new TypeImpl(array);
    }

    @Override
    public TypeImpl set(int i, Object... a) {
        return (TypeImpl) super.set(i, a);
    }

}
