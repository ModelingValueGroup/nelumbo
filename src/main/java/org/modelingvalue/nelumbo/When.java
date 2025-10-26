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

public final class When extends BinaryPredicate {
    @Serial
    private static final long serialVersionUID = 9105566742523301113L;

    private When(Node when, Node predicate) {
        super(Type.PREDICATE, List.of(), when, predicate);
    }

    private When(Object[] args, When declaration) {
        super(args, declaration);
    }

    public static When of(Node when, Node predicate) {
        return new When(when, predicate);
    }

    @Override
    public When declaration() {
        return (When) super.declaration();
    }

    @Override
    protected When struct(Object[] array, Predicate declaration) {
        return new When(array, (When) declaration);
    }

    @Override
    public When set(int i, Object... a) {
        return (When) super.set(i, a);
    }

    @Override
    protected boolean isTrue(InferResult predResult, int i) {
        return false;
    }

    @Override
    protected boolean isFalse(InferResult predResult, int i) {
        return false;
    }

    @Override
    protected boolean isUnknown(InferResult predResult, int i) {
        return i == 0 && predResult.isFalseCC();
    }

    @Override
    protected boolean isTrue(InferResult[] predResult) {
        return predResult[1].isTrueCC();
    }

    @Override
    protected boolean isFalse(InferResult[] predResult) {
        return predResult[1].isFalseCC();
    }

    @Override
    protected boolean isLeft(InferResult[] predResult) {
        return false;
    }

    @Override
    protected boolean isRight(InferResult[] predResult) {
        return predResult[0].isTrueCC();
    }

    @Override
    protected InferResult add(InferResult[] predResult) {
        return predResult[0].addAnd(predResult[1]);
    }

    @Override
    protected void order(Predicate[] predicate) {
        // Do not change order
    }

    @Override
    public String toString() {
        return predicate2() + " if " + predicate1();
    }
}
