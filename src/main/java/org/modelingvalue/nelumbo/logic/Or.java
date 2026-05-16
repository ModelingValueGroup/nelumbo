//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

package org.modelingvalue.nelumbo.logic;

import java.io.Serial;

import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;

public final class Or extends BinaryPredicate {
    @Serial
    private static final long serialVersionUID = -1732549494864415986L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    @NelumboConstructor
    public Or(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public static Or of(Predicate predicate1, Predicate predicate2) {
        return new Or(NodeInfo.of(FUNCTOR), predicate1, predicate2);
    }

    @Override
    public Or declaration() {
        return (Or) super.declaration();
    }

    @Override
    protected Or set(NodeInfo nodeInfo, Object[] args) {
        return new Or(nodeInfo, args);
    }

    @Override
    public Or set(int i, Object... a) {
        return (Or) super.set(i, a);
    }

    @Override
    protected boolean isTrue(InferResult predResult, int i) {
        return predResult.isTrueCC();
    }

    @Override
    protected boolean isFalse(InferResult predResult, int i) {
        return false;
    }

    @Override
    protected boolean isUnknown(InferResult predResult, int i) {
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

}
