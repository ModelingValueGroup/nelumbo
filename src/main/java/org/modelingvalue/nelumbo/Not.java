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

import org.modelingvalue.nelumbo.syntax.Token;

public final class Not extends CompoundPredicate {
    private static final long serialVersionUID = -4543178470298951866L;

    // Automatically set in addFcuntor in KnowledgeBase
    private static Functor    FUNCTOR;

    public Not(Functor functor, Token[] tokens, Object[] args) {
        super(functor, tokens, args[0]);
    }

    private Not(Object[] args, int start, Not declaration) {
        super(args, start, declaration);
    }

    public static Not of(Node predicate) {
        return new Not(FUNCTOR, Token.EMPTY, new Object[]{predicate});
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Not struct(Object[] array, int start, Predicate declaration) {
        return new Not(array, start, (Not) declaration);
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
                return set(1, predResult.unknown()).unknown();
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
