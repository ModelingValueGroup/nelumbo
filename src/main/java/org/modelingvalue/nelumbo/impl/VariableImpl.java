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

import org.modelingvalue.nelumbo.Logic.Structure;
import org.modelingvalue.nelumbo.Logic.Variable;

public final class VariableImpl<F extends Structure> extends StructureImpl<F> {
    private static final long serialVersionUID = -8998368070388908726L;

    public VariableImpl(Class<F> type, String name) {
        super(type, name);
        KnowledgeBaseImpl.updateSpecializations(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final F proxy() {
        return (F) Proxy.newProxyInstance(type().getClassLoader(), new Class[]{type(), Variable.class}, this);
    }

    @Override
    public String toString() {
        return get(1).toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected VariableImpl<F> struct(Object[] array) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableImpl<F> set(int i, Object... a) {
        return (VariableImpl<F>) super.set(i, a);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<F> type() {
        return (Class<F>) get(0);
    }
}
