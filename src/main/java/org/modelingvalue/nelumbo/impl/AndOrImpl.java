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

import java.util.concurrent.ThreadLocalRandom;

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Predicate;

public abstract class AndOrImpl extends PredicateImpl {
    private static final long  serialVersionUID = -928776822979604743L;

    private static final int[] ZERO_ONE         = new int[]{0, 1};
    private static final int[] ONE_ZERO         = new int[]{1, 0};

    protected AndOrImpl(FunctorImpl<Predicate> functor, PredicateImpl predicate1, PredicateImpl predicate2) {
        super(functor, predicate1, predicate2);
    }

    protected AndOrImpl(Object[] args, AndOrImpl declaration) {
        super(args, declaration);
    }

    @Override
    public AndOrImpl declaration() {
        return (AndOrImpl) super.declaration();
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl predicate1() {
        return (PredicateImpl) get(1);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl predicate2() {
        return (PredicateImpl) get(2);
    }

    private PredicateImpl predicate(int i) {
        return (PredicateImpl) get(i + 1);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public final InferResult infer(InferContext context) {
        PredicateImpl predicate;
        InferResult[] predResult = new InferResult[2];
        Set<AndOrImpl> now, next = (Set) singleton(), andOrAll;
        Set<PredicateImpl> facts = Set.of(), falsehoods = Set.of(), cycles = Set.of(), predAll;
        do {
            now = next;
            next = Set.of();
            andor:
            for (AndOrImpl andOr : now) {
                for (int i : andOr.order()) {
                    predicate = andOr.predicate(i);
                    predResult[i] = predicate.infer(context);
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
                        andOrAll = (Set) InferResult.bind(predAll, predicate, andOr).removeAll(now);
                        if (!andOrAll.isEmpty()) {
                            next = next.addAll(andOrAll);
                            cycles = cycles.addAll(predResult[i].cycles());
                            if (this instanceof AndImpl && !predResult[i].falsehoods().isEmpty() && !isFullyBound()) {
                                falsehoods = falsehoods.add(this);
                            } else if (this instanceof OrImpl && !predResult[i].facts().isEmpty() && !isFullyBound()) {
                                facts = facts.add(this);
                            }
                        }
                        continue andor;
                    }
                }
                cycles = cycles.addAll(predResult[0].cycles()).addAll(predResult[1].cycles());
                if (this instanceof AndImpl) {
                    facts = facts.add(andOr);
                    if (!predResult[0].falsehoods().isEmpty() || !predResult[1].falsehoods().isEmpty()) {
                        falsehoods = falsehoods.add(this);
                    }
                } else if (this instanceof OrImpl) {
                    falsehoods = falsehoods.add(andOr);
                    if (!predResult[0].facts().isEmpty() || !predResult[1].facts().isEmpty()) {
                        facts = facts.add(this);
                    }
                }
            }
            if (next.isEmpty()) {
                return InferResult.of(facts, falsehoods, cycles);
            }
        } while (true);

    }

    private int[] order() {
        int unbound1 = predicate1().nrOfUnbound(), unbound2 = predicate2().nrOfUnbound();
        if (unbound2 < unbound1) {
            return ONE_ZERO;
        } else if (RANDOM_NELUMBO && unbound1 == unbound2) {
            return ThreadLocalRandom.current().nextBoolean() ? ONE_ZERO : ZERO_ONE;
        } else {
            return ZERO_ONE;
        }
    }

    @Override
    protected PredicateImpl setDeclaration(PredicateImpl to) {
        throw new UnsupportedOperationException();
    }
}
