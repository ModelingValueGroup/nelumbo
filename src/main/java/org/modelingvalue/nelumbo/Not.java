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

public final class Not extends CompoundPredicate {
    @Serial
    private static final long serialVersionUID = -4543178470298951866L;

    private static Functor    FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(Not.class, f -> FUNCTOR = f);
    }

    public Not(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0]);
    }

    private Not(Object[] args, Not declaration) {
        super(args, declaration);
    }

    public static Not of(Node predicate) {
        return new Not(FUNCTOR, List.of(), new Object[]{predicate});
    }

    @Override
    protected Not struct(Object[] array, Predicate declaration) {
        return new Not(array, (Not) declaration);
    }

    @Override
    public Not declaration() {
        return (Not) super.declaration();
    }

    public final Predicate predicate() {
        Predicate p = getVal(0);
        return p != null ? p : Boolean.UNKNOWN;
    }

    @Override
    protected InferResult infer(InferContext context) {
        Predicate predicate = predicate();
        InferResult predResult = predicate.infer(context);
        if (predResult.hasStackOverflow()) {
            return predResult;
        } else if (context.reduce()) {
            if (predResult.isFalseCC()) {
                return Boolean.TRUE.result();
            } else if (predResult.isTrueCC()) {
                return Boolean.FALSE.result();
            } else {
                return set(0, predResult.unknown()).unknown();
            }
        } else {
            return predResult.flipComplete();
        }
    }

    @Override
    public Not set(int i, Object... a) {
        return (Not) super.set(i, a);
    }

}
