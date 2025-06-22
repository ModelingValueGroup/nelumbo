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

public final class Or extends BinaryPredicate {
    private static final long    serialVersionUID = -1732549494864415986L;

    private static final Functor FUNCTOR          = new Functor(Predicate.TYPE, "Or", List.of(Predicate.TYPE, Predicate.TYPE));

    public Or(Structure predicate1, Structure predicate2) {
        super(FUNCTOR, predicate1, predicate2);
    }

    private Or(Object[] args, Or declaration) {
        super(args, declaration);
    }

    @Override
    public Or declaration() {
        return (Or) super.declaration();
    }

    @Override
    protected Or struct(Object[] array, Predicate declaration) {
        return new Or(array, (Or) declaration);
    }

    @Override
    public Or set(int i, Object... a) {
        return (Or) super.set(i, a);
    }

    @Override
    protected boolean isTrue(InferResult predResult) {
        return predResult.isTrueCC();
    }

    @Override
    protected boolean isFalse(InferResult predResult) {
        return false;
    }

    @Override
    protected boolean isTrue(InferResult[] predResult) {
        return false;
    }

    @Override
    protected boolean isFalse(InferResult[] predResult) {
        return predResult[0].isFalseCC() && predResult[1].isFalseCC();
    }

    @Override
    protected boolean isLeft(InferResult[] predResult) {
        return predResult[1].isFalseCC();
    }

    @Override
    protected boolean isRight(InferResult[] predResult) {
        return predResult[0].isFalseCC();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean contains(Predicate cond) {
        return super.contains(cond) || predicate1().contains(cond) || predicate2().contains(cond);
    }

    @Override
    public String toString() {
        return "(" + predicate1() + "|" + predicate2() + ")";
    }
}
