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

import static org.modelingvalue.nelumbo.patterns.Pattern.n;
import static org.modelingvalue.nelumbo.patterns.Pattern.s;
import static org.modelingvalue.nelumbo.patterns.Pattern.t;

import java.io.Serial;

import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class And extends BinaryPredicate {
    @Serial
    private static final long serialVersionUID = -7248491569810098948L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    static {
        try {
            FUNCTOR = Functor.of(s(n(Type.BOOLEAN), t("&"), n(Type.BOOLEAN)), Type.BOOLEAN, null, And.class, 22);
        } catch (ParseException e) {
            throw new IllegalStateException("Cannot create functor for NIs", e);
        }
    }

    @NelumboConstructor
    public And(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public static And of(Predicate predicate1, Predicate predicate2) {
        return new And(NodeInfo.of(FUNCTOR), predicate1, predicate2);
    }

    @Override
    public And declaration() {
        return (And) super.declaration();
    }

    @Override
    protected boolean isTrue(InferResult predResult, int i) {
        return false;
    }

    @Override
    protected boolean isFalse(InferResult predResult, int i) {
        return predResult.isFalseCC();
    }

    @Override
    protected boolean isUnknown(InferResult predResult, int i) {
        return false;
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

}
