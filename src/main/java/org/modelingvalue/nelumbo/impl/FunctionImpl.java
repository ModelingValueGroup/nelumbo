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

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Function;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Structure;

public final class FunctionImpl<F extends Function<T>, T extends Structure> extends StructureImpl<F> {
    private static final long serialVersionUID = -8174476116343969718L;

    public FunctionImpl(Functor<F> functor, Object... args) {
        super(functor, args);
    }

    private FunctionImpl(Object[] array) {
        super(array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final F proxy() {
        return (F) Proxy.newProxyInstance(type().getClassLoader(), new Class[]{type(), Function.class}, this);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected FunctionImpl<F, T> struct(Object[] array) {
        return new FunctionImpl<F, T>(array);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FunctionImpl<F, T> set(int i, Object... a) {
        return (FunctionImpl<F, T>) super.set(i, a);
    }

    protected FunctionImpl<F, T> signature() {
        Object[] array = signatureArray();
        return array != null ? struct(array) : this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Set<FunctionImpl<F, T>> generalize() {
        Set<FunctionImpl<F, T>> result = Set.of();
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            if (v instanceof FunctionImpl) {
                Set<FunctionImpl> gen = ((FunctionImpl) v).generalize();
                for (FunctionImpl s : gen) {
                    result = result.add(set(i, s));
                }
                if (gen.isEmpty()) {
                    result = result.add(set(i, typeOf(v)));
                }
            } else {
                assert (v instanceof Class);
                for (Class s : KnowledgeBaseImpl.generalizations((Class) v)) {
                    result = result.add(set(i, s));
                }
            }
        }
        return result;
    }

}
