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

package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.nelumbo.Logic;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Predicate;

public final class NotImpl extends PredicateImpl {
    private static final long                   serialVersionUID  = -4543178470298951866L;

    private static final FunctorImpl<Predicate> NOT_FUNCTOR       = FunctorImpl.<Predicate, Predicate> of(Logic::not);
    private static final Functor<Predicate>     NOT_FUNCTOR_PROXY = NOT_FUNCTOR.proxy();

    public NotImpl(Predicate pred) {
        super(NOT_FUNCTOR_PROXY, pred);
    }

    private NotImpl(PredicateImpl pred) {
        super(NOT_FUNCTOR, pred);
    }

    private NotImpl(Object[] args, NotImpl declaration) {
        super(args, declaration);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected NotImpl struct(Object[] array) {
        return new NotImpl(array, declaration());
    }

    @Override
    public NotImpl declaration() {
        return (NotImpl) super.declaration();
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl predicate() {
        return (PredicateImpl) get(1);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected InferResult infer(InferContext context) {
        PredicateImpl predicate = predicate();
        InferResult predResult = predicate.infer(context);
        if (predResult.hasStackOverflow()) {
            return predResult;
        } else if (context.reduce()) {
            if (predResult.facts().isEmpty()) {
                return BooleanImpl.TRUE_CONCLUSION;
            } else if (predResult.falsehoods().isEmpty()) {
                return BooleanImpl.FALSE_CONCLUSION;
            } else {
                return set(1, predResult.facts().get(0)).unknown();
            }
        } else {
            if (predResult.isUnknown()) {
                return InferResult.EMPTY;
            } else {
                return predResult.not();
            }
        }
    }

    @Override
    public NotImpl set(int i, Object... a) {
        return (NotImpl) super.set(i, a);
    }

    @Override
    public String toString() {
        return PRETTY_NELUMBO ? "\u00AC(" + predicate() + ")" : super.toString();
    }

    @Override
    protected PredicateImpl setDeclaration(PredicateImpl to) {
        throw new UnsupportedOperationException();
    }
}
