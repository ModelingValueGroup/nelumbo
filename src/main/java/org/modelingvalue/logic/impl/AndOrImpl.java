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

package org.modelingvalue.logic.impl;

import org.modelingvalue.collections.Set;
import org.modelingvalue.logic.Logic.Predicate;

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
        PredicateImpl pred1Decl = ((AndOrImpl) declaration).predicate1();
        PredicateImpl pred2Decl = ((AndOrImpl) declaration).predicate2();
        Set<AndOrImpl> now, next = Set.of(this);
        Set<PredicateImpl> facts = Set.of(), falsehoods = null, cycles = Set.of();
        InferResult pred1Result, pred2Result, andOr1Result, andOr2Result;
        do {
            now = next;
            next = Set.of();
            for (AndOrImpl andOr : now) {
                InferContext ctx = context;
                do {
                    if (!context.deep() && !context.shallow()) {
                        ctx = ctx == context ? context.deepShallow(false, true) : context.deepShallow(true, false);
                    }
                    ctx = context.deepShallow(true, false);
                    // Predicate 1
                    pred1Result = andOr.predicate1().infer(pred1Decl, ctx);
                    if (pred1Result.hasStackOverflow()) {
                        return pred1Result;
                    }
                    andOr1Result = flip(pred1Result).bind(pred1Decl, andOr, declaration);
                    if (andOr1Result.facts().isEmpty()) {
                        andOr2Result = andOr1Result;
                    } else {
                        // Predicate 2
                        pred2Result = andOr.predicate2().infer(pred2Decl, ctx);
                        if (pred2Result.hasStackOverflow()) {
                            return pred2Result;
                        }
                        andOr2Result = flip(pred2Result).bind(pred2Decl, andOr, declaration);
                        if (andOr2Result.facts().isEmpty()) {
                            andOr1Result = andOr2Result;
                        }
                    }
                } while (ctx != context && ctx.shallow());
                // Combine
                cycles = cycles.addAll(andOr1Result.cycles()).addAll(andOr2Result.cycles());
                if (falsehoods == null) {
                    if (andOr1Result.falsehoods().contains(andOr) || andOr2Result.falsehoods().contains(andOr)) {
                        falsehoods = Set.of(andOr);
                    } else {
                        falsehoods = andOr1Result.falsehoods().addAll(andOr2Result.falsehoods());
                    }
                }
                facts = facts.addAll(andOr1Result.facts().retainAll(andOr2Result.facts()));
                next = next.addAll((Set) andOr1Result.facts()).addAll((Set) andOr2Result.facts()).removeAll(now).removeAll(facts);
            }
            if (next.isEmpty()) {
                return flip(InferResult.of(facts, falsehoods, cycles));
            }
        } while (true);

    }

    protected abstract InferResult flip(InferResult result);
}
