//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class Or extends BinaryPredicate {
    @Serial
    private static final long serialVersionUID = -1732549494864415986L;

    private static Functor    FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(Or.class, f -> FUNCTOR = f);
    }

    public Or(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1]);
    }

    private Or(Object[] args, Or declaration) {
        super(args, declaration);
    }

    public static Or of(Node predicate1, Node predicate2) {
        return new Or(FUNCTOR, List.of(), new Object[]{predicate1, predicate2});
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
    protected InferResult add(InferResult[] predResult) {
        return predResult[0].addOr(predResult[1]);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean contains(Predicate cond) {
        return super.contains(cond) || predicate1().contains(cond) || predicate2().contains(cond);
    }
}
