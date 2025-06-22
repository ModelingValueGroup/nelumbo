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

import org.modelingvalue.collections.List;

public final class And extends BinaryPredicate {
    private static final long    serialVersionUID = -7248491569810098948L;

    private static final Functor FUNCTOR          = new Functor(Predicate.TYPE, "And", List.of(Predicate.TYPE, Predicate.TYPE));

    public And(Structure predicate1, Structure predicate2) {
        super(FUNCTOR, predicate1, predicate2);
    }

    private And(Object[] args, And declaration) {
        super(args, declaration);
    }

    @Override
    public And declaration() {
        return (And) super.declaration();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected And struct(Object[] array, Predicate declaration) {
        return new And(array, (And) declaration);
    }

    @Override
    public And set(int i, Object... a) {
        return (And) super.set(i, a);
    }

    @Override
    protected boolean isTrue(InferResult predResult) {
        return false;
    }

    @Override
    protected boolean isFalse(InferResult predResult) {
        return predResult.isFalseCC();
    }

    @Override
    protected boolean isTrue(InferResult[] predResult) {
        return predResult[0].isTrueCC() && predResult[1].isTrueCC();
    }

    @Override
    protected boolean isFalse(InferResult[] predResult) {
        return false;
    }

    @Override
    protected boolean isLeft(InferResult[] predResult) {
        return predResult[1].isTrueCC();
    }

    @Override
    protected boolean isRight(InferResult[] predResult) {
        return predResult[0].isTrueCC();
    }

    @Override
    public String toString() {
        return "(" + predicate1() + "&" + predicate2() + ")";
    }

}
