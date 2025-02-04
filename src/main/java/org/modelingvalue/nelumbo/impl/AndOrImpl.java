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

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Predicate;

public abstract class AndOrImpl extends PredicateImpl {
    private static final long serialVersionUID = -928776822979604743L;

    protected AndOrImpl(FunctorImpl<Predicate> functor, PredicateImpl predicate1, PredicateImpl predicate2) {
        super(functor, predicate1, predicate2);
    }

    protected AndOrImpl(Object[] args) {
        super(args);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl predicate1() {
        return (PredicateImpl) get(1);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl predicate2() {
        return (PredicateImpl) get(2);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public final InferResult infer(PredicateImpl declaration, InferContext context) {
        PredicateImpl[] predDecl = new PredicateImpl[]{((AndOrImpl) declaration).predicate1(), ((AndOrImpl) declaration).predicate2()};
        PredicateImpl predicate;
        InferResult[] predResult = new InferResult[2];
        Set<AndOrImpl> now, next = (Set) singleton(), andOrAll;
        Set<PredicateImpl> facts = Set.of(), falsehoods = Set.of(), cycles = Set.of(), predAll;
        do {
            now = next;
            next = Set.of();
            andor:
            for (AndOrImpl andOr : now) {
                for (int i = 0; i < 2; i++) {
                    predicate = i == 0 ? andOr.predicate1() : andOr.predicate2();
                    predResult[i] = predicate.infer(predDecl[i], context);
                    if (predResult[i].hasStackOverflow()) {
                        return predResult[i];
                    }
                    predAll = predResult[i].facts().addAll(predResult[i].falsehoods()).remove(predicate);
                    if (predAll.isEmpty()) {
                        if (this instanceof AndImpl && predResult[i].facts().isEmpty()) {
                            falsehoods = falsehoods.add(andOr);
                            continue andor;
                        } else if (this instanceof OrImpl && predResult[i].falsehoods().isEmpty()) {
                            facts = facts.add(andOr);
                            continue andor;
                        }
                    } else {
                        andOrAll = (Set) InferResult.bind(predAll, predDecl[i], andOr, declaration).removeAll(now);
                        if (!andOrAll.isEmpty()) {
                            next = next.addAll(andOrAll);
                            cycles = cycles.addAll(predResult[i].cycles());
                            if (this instanceof AndImpl && !predResult[i].falsehoods().isEmpty()) {
                                falsehoods = falsehoods.add(andOr);
                            } else if (this instanceof OrImpl && !predResult[i].facts().isEmpty()) {
                                facts = facts.add(andOr);
                            }
                        }
                        continue andor;
                    }
                }
                cycles = cycles.addAll(predResult[0].cycles()).addAll(predResult[1].cycles());
                if (this instanceof AndImpl) {
                    facts = facts.add(andOr);
                    if (!predResult[0].falsehoods().isEmpty() || !predResult[1].falsehoods().isEmpty()) {
                        falsehoods = falsehoods.add(andOr);
                    }
                } else if (this instanceof OrImpl) {
                    falsehoods = falsehoods.add(andOr);
                    if (!predResult[0].facts().isEmpty() || !predResult[1].facts().isEmpty()) {
                        facts = facts.add(andOr);
                    }
                }
            }
            if (next.isEmpty()) {
                return InferResult.of(facts, falsehoods, cycles);
            }
        } while (true);

    }
}
