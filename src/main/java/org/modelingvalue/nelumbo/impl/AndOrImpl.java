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

    @SuppressWarnings("unchecked")
    @Override
    public InferResult resolve(InferContext context) {
        Set<PredicateImpl> now, next = singleton(), bound, facts = Set.of(), falsehoods = Set.of(), cycles = Set.of();
        InferContext reduce = context.reduceExpand(true, false), expand = context.reduceExpand(false, true);
        PredicateImpl reduced;
        do {
            now = next;
            next = Set.of();
            if (now.size() > 3) {
                System.err.println();
            }
            for (PredicateImpl predicate : now) {
                InferResult predResult = predicate.infer(reduce);
                if (predResult.hasStackOverflow()) {
                    return predResult;
                } else if (predResult.facts().isEmpty()) {
                    falsehoods = falsehoods.add(InferResult.bind(predicate, this, this));
                } else if (predResult.falsehoods().isEmpty()) {
                    facts = facts.add(InferResult.bind(predicate, this, this));
                } else {
                    assert (predResult.facts().equals(predResult.falsehoods()));
                    reduced = predResult.facts().get(0);
                    predResult = reduced.infer(expand);
                    if (predResult.hasStackOverflow()) {
                        return predResult;
                    } else if (predResult.facts().isEmpty()) {
                        falsehoods = falsehoods.add(InferResult.bind(predicate, this, this));
                    } else if (predResult.falsehoods().isEmpty()) {
                        facts = facts.add(InferResult.bind(predicate, this, this));
                    } else {
                        bound = predResult.facts().addAll(predResult.falsehoods()).remove(reduced);
                        if (!bound.isEmpty()) {
                            bound = InferResult.bind(bound.retainAll(PredicateImpl::isFullyBound), this, reduced);
                            if (!bound.isEmpty()) {
                                next = next.addAll(bound);
                                if (!predResult.facts().removeAll(PredicateImpl::isFullyBound).isEmpty()) {
                                    facts = facts.add(this);
                                }
                                if (!predResult.falsehoods().removeAll(PredicateImpl::isFullyBound).isEmpty()) {
                                    falsehoods = falsehoods.add(this);
                                }
                                cycles = cycles.addAll(predResult.cycles());
                            }
                        } else {
                            facts = facts.addAll(InferResult.bind(predResult.facts(), this, this));
                            falsehoods = falsehoods.addAll(InferResult.bind(predResult.falsehoods(), this, this));
                            cycles = cycles.addAll(predResult.cycles());
                            continue;
                        }
                    }
                }
            }
        } while (!next.isEmpty());
        return InferResult.of(facts, falsehoods, cycles);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public final InferResult infer(InferContext context) {
        PredicateImpl[] predicate = new PredicateImpl[2];
        InferResult[] predResult = new InferResult[2];
        for (int i : order()) {
            predicate[i] = predicate(i);
            predResult[i] = predicate[i].infer(context);
            if (predResult[i].hasStackOverflow()) {
                return predResult[i];
            } else if (this instanceof AndImpl && predResult[i].facts().isEmpty()) {
                return BooleanImpl.FALSE_CONCLUSION;
            } else if (this instanceof OrImpl && predResult[i].falsehoods().isEmpty()) {
                return BooleanImpl.TRUE_CONCLUSION;
            } else if (!predResult[i].hasOnly(predicate[i])) {
                return predResult[i];
            }
        }
        if (this instanceof AndImpl && predResult[0].falsehoods().isEmpty() && predResult[1].falsehoods().isEmpty()) {
            return BooleanImpl.TRUE_CONCLUSION;
        } else if (this instanceof OrImpl && predResult[0].facts().isEmpty() && predResult[1].facts().isEmpty()) {
            return BooleanImpl.FALSE_CONCLUSION;
        } else if (this instanceof AndImpl && predResult[0].falsehoods().isEmpty()) {
            return predResult[1];
        } else if (this instanceof OrImpl && predResult[0].facts().isEmpty()) {
            return predResult[1];
        } else if (this instanceof AndImpl && predResult[1].falsehoods().isEmpty()) {
            return predResult[0];
        } else if (this instanceof OrImpl && predResult[1].facts().isEmpty()) {
            return predResult[0];
        }
        return InferResult.of(singleton(), singleton(), predResult[0].cycles().addAll(predResult[1].cycles()));
    }

    private int[] order() {
        if (RANDOM_NELUMBO) {
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
