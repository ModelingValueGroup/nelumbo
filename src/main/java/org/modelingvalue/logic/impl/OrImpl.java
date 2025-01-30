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

import org.modelingvalue.logic.Logic.Predicate;

public final class OrImpl extends AndOrImpl {
    private static final long                   serialVersionUID = -1732549494864415986L;

    private static final FunctorImpl<Predicate> OR_FUNCTOR       = FunctorImpl.<Predicate, Predicate, Predicate> of(OrImpl::or);

    private static Predicate or(Predicate predicate1, Predicate predicate2) {
        return new OrImpl(StructureImpl.unproxy(predicate1), StructureImpl.unproxy(predicate2)).proxy();
    }

    public OrImpl(PredicateImpl predicate1, PredicateImpl predicate2) {
        super(OR_FUNCTOR, predicate1, predicate2);
    }

    private OrImpl(Object[] args) {
        super(args);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected OrImpl struct(Object[] array) {
        return new OrImpl(array);
    }

    @Override
    public OrImpl set(int i, Object... a) {
        return (OrImpl) super.set(i, a);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean contains(PredicateImpl cond) {
        return super.contains(cond) || predicate1().contains(cond) || predicate2().contains(cond);
    }

    @Override
    protected InferResult flip(InferResult result) {
        return result.not();
    }

    @Override
    public String toString(StructureImpl<?> parent) {
        return parent instanceof OrImpl ? predicate1().toString(this) + " | " + predicate2().toString(this) + ")" : toString();
    }

    @Override
    public String toString() {
        return PRETTY_NELUMBO ? "(" + predicate1().toString(this) + " | " + predicate2().toString(this) + ")" : super.toString();
    }
}
