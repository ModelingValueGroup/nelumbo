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
        Set<PredicateImpl> now, next = singleton(), facts = Set.of(), falsehoods = Set.of(), cycles = Set.of();
        do {
            now = next;
            next = Set.of();
            for (PredicateImpl predicate : now) {
                InferResult predResult = predicate.infer(context.reduce(true));
                if (predResult.hasStackOverflow()) {
                    return predResult;
                } else if (predResult.hasOnly(predicate)) {
                    predResult = predicate.infer(context);
                    if (predResult.hasStackOverflow()) {
                        return predResult;
                    } else if (predResult.hasOnly(predicate)) {
                        facts = facts.addAll(InferResult.bind(predResult.facts(), predicate.declaration(), this));
                        falsehoods = falsehoods.addAll(InferResult.bind(predResult.falsehoods(), predicate.declaration(), this));
                        cycles = cycles.addAll(predResult.cycles());
                        continue;
                    }
                }
                next = next.addAll(predResult.facts()).addAll(predResult.falsehoods()).removeAll(now);
                if (!next.isEmpty() && !predicate.isFullyBound()) {
                    if (predResult.facts().contains(predicate)) {
                        facts = facts.add(this);
                        cycles = cycles.addAll(predResult.cycles());
                    }
                    if (predResult.falsehoods().contains(predicate)) {
                        falsehoods = falsehoods.add(this);
                        cycles = cycles.addAll(predResult.cycles());
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
                return predResult[i];
            } else if (this instanceof OrImpl && predResult[i].falsehoods().isEmpty()) {
                return predResult[i];
            } else if (!predResult[i].hasOnly(predicate[i])) {
                return predResult[i].bind(predicate[i], this);
            }
        }
        Set<PredicateImpl> facts = Set.of(), falsehoods = Set.of(), cycles;
        if (this instanceof AndImpl) {
            facts = singleton();
            if (!predResult[0].falsehoods().isEmpty() || !predResult[1].falsehoods().isEmpty()) {
                falsehoods = singleton();
            }
        } else if (this instanceof OrImpl) {
            falsehoods = singleton();
            if (!predResult[0].facts().isEmpty() || !predResult[1].facts().isEmpty()) {
                facts = singleton();
            }
        }
        cycles = predResult[0].cycles().addAll(predResult[1].cycles());
        return InferResult.of(facts, falsehoods, cycles);
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
