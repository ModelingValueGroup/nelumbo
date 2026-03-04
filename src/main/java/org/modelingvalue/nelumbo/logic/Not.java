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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class Not extends CompoundPredicate {
    @Serial
    private static final long serialVersionUID = -4543178470298951866L;

    private static Functor    FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(Not.class, f -> FUNCTOR = f);
    }

    @NelumboConstructor
    public Not(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0]);
    }

    private Not(Object[] args, List<AstElement> elements, Not declaration) {
        super(args, elements, declaration);
    }

    public static Not of(Predicate predicate) {
        return new Not(FUNCTOR, List.of(), new Object[]{predicate});
    }

    @Override
    protected Not struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new Not(array, elements, (Not) declaration);
    }

    @Override
    public Not set(int i, Object... a) {
        return (Not) super.set(i, a);
    }

    @Override
    public Not declaration() {
        return (Not) super.declaration();
    }

    public final Predicate predicate() {
        return predicate(0);
    }

    @Override
    protected InferResult infer(InferContext context) {
        Predicate predicate = predicate();
        InferResult predResult = predicate.infer(context);
        if (predResult.hasStackOverflow()) {
            return predResult;
        } else if (context.reduce()) {
            if (predResult.isFalseCC()) {
                return NBoolean.TRUE.result();
            } else if (predResult.isTrueCC()) {
                return NBoolean.FALSE.result();
            } else {
                return setPredicates(0, predResult.predicate()).unknown();
            }
        } else if (!predResult.unresolvable()) {
            return predResult.flipComplete();
        } else {
            return InferResult.UNRESOLVABLE;
        }
    }

}
