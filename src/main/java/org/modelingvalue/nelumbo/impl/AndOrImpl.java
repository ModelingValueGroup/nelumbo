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
        PredicateImpl pred1Decl = ((AndOrImpl) declaration).predicate1(), predicate1;
        PredicateImpl pred2Decl = ((AndOrImpl) declaration).predicate2(), predicate2;
        Set<AndOrImpl> now, next = (Set) singleton();
        Set<PredicateImpl> facts = Set.of(), falsehoods = Set.of(), cycles = Set.of();
        InferResult pred1Result, pred2Result;
        do {
            now = next;
            next = Set.of();
            for (AndOrImpl andOr : now) {
                // Predicate 1
                predicate1 = andOr.predicate1();
                pred1Result = predicate1.infer(pred1Decl, context);
                if (pred1Result.hasStackOverflow()) {
                    return pred1Result;
                }
                next = next.addAll((Set) InferResult.bind(pred1Result.facts().remove(predicate1), pred1Decl, andOr, declaration));
                next = next.addAll((Set) InferResult.bind(pred1Result.falsehoods().remove(predicate1), pred1Decl, andOr, declaration));
                if (next.isEmpty()) {
                    if (this instanceof AndImpl && pred1Result.facts().isEmpty()) {
                        falsehoods = falsehoods.add(andOr);
                        cycles = cycles.addAll(pred1Result.cycles());
                        continue;
                    } else if (this instanceof OrImpl && pred1Result.falsehoods().isEmpty()) {
                        facts = facts.add(andOr);
                        cycles = cycles.addAll(pred1Result.cycles());
                        continue;
                    }
                } else {
                    if (this instanceof AndImpl) {
                        falsehoods = falsehoods.add(andOr);
                    } else if (this instanceof OrImpl) {
                        facts = facts.add(andOr);
                    }
                    cycles = cycles.addAll(pred1Result.cycles());
                    continue;
                }
                // Predicate 2
                predicate2 = andOr.predicate2();
                pred2Result = predicate2.infer(pred2Decl, context);
                if (pred2Result.hasStackOverflow()) {
                    return pred2Result;
                }
                if (andOr.equals(this) && pred1Result == predicate1.incomplete() && pred2Result == predicate2.incomplete()) {
                    return incomplete();
                }
                next = next.addAll((Set) InferResult.bind(pred2Result.facts().remove(predicate2), pred2Decl, andOr, declaration));
                next = next.addAll((Set) InferResult.bind(pred2Result.falsehoods().remove(predicate2), pred2Decl, andOr, declaration));
                if (next.isEmpty()) {
                    if (this instanceof AndImpl && pred2Result.facts().isEmpty()) {
                        falsehoods = falsehoods.add(andOr);
                        cycles = cycles.addAll(pred2Result.cycles());
                        continue;
                    } else if (this instanceof OrImpl && pred2Result.falsehoods().isEmpty()) {
                        facts = facts.add(andOr);
                        cycles = cycles.addAll(pred2Result.cycles());
                        continue;
                    }
                } else {
                    if (this instanceof AndImpl) {
                        falsehoods = falsehoods.add(andOr);
                    } else if (this instanceof OrImpl) {
                        facts = facts.add(andOr);
                    }
                    cycles = cycles.addAll(pred2Result.cycles());
                    continue;
                }
                cycles = cycles.addAll(pred1Result.cycles()).addAll(pred2Result.cycles());
                if (this instanceof AndImpl) {
                    facts = facts.add(andOr);
                    if (!pred1Result.falsehoods().isEmpty() || !pred2Result.falsehoods().isEmpty()) {
                        falsehoods = falsehoods.add(andOr);
                    }
                } else if (this instanceof OrImpl) {
                    if (!pred1Result.facts().isEmpty() || !pred2Result.facts().isEmpty()) {
                        facts = facts.add(andOr);
                    }
                    falsehoods = falsehoods.add(andOr);
                }
            }
            if (next.isEmpty()) {
                return InferResult.of(facts, falsehoods, cycles);
            }
        } while (true);

    }
}
