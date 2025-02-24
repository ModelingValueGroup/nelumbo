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

import org.modelingvalue.nelumbo.Logic.Predicate;

public final class AndImpl extends AndOrImpl {
    private static final long                   serialVersionUID = -7248491569810098948L;

    private static final FunctorImpl<Predicate> AND_FUNCTOR      = FunctorImpl.<Predicate, Predicate, Predicate> of(AndImpl::and);

    private static Predicate and(Predicate p1, Predicate p2) {
        return new AndImpl(StructureImpl.unproxy(p1), StructureImpl.unproxy(p2)).proxy();
    }

    public AndImpl(PredicateImpl predicate1, PredicateImpl predicate2) {
        super(AND_FUNCTOR, predicate1, predicate2);
    }

    private AndImpl(Object[] args) {
        super(args);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected AndImpl struct(Object[] array) {
        return new AndImpl(array);
    }

    @Override
    public AndImpl set(int i, Object... a) {
        return (AndImpl) super.set(i, a);
    }

    @Override
    public String toString(StructureImpl<?> parent) {
        return parent instanceof AndImpl ? predicate1().toString(this) + "\u2227" + predicate2().toString(this) : toString();
    }

    @Override
    public String toString() {
        return PRETTY_NELUMBO ? "(" + predicate1().toString(this) + "\u2227" + predicate2().toString(this) + ")" : super.toString();
    }

}
