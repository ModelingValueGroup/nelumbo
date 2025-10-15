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

public final class And extends BinaryPredicate {
    @Serial
    private static final long serialVersionUID = -7248491569810098948L;

    private static Functor    FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(And.class, f -> FUNCTOR = f);
    }

    public And(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1]);
    }

    private And(Object[] args, And declaration) {
        super(args, declaration);
    }

    public static And of(Node predicate1, Node predicate2) {
        return new And(FUNCTOR, List.of(), new Object[]{predicate1, predicate2});
    }

    @Override
    public And declaration() {
        return (And) super.declaration();
    }

    @Override
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
    protected InferResult add(InferResult[] predResult) {
        return predResult[0].addAnd(predResult[1]);
    }

}
