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
        PredicateImpl pred1 = predicate1();
        PredicateImpl pred1Decl = ((AndOrImpl) declaration).predicate1();
        InferResult pred1Result = pred1.infer(pred1Decl, context);
        if (pred1Result.hasStackOverflow()) {
            return pred1Result;
        }
        PredicateImpl pred2 = predicate2();
        PredicateImpl pred2Decl = ((AndOrImpl) declaration).predicate2();
        InferResult pred2Result = pred2.infer(pred2Decl, context);
        if (pred2Result.hasStackOverflow()) {
            return pred2Result;
        }
        pred1Result = flip(pred1Result.bind(pred1Decl, this, declaration));
        pred2Result = flip(pred2Result.bind(pred2Decl, this, declaration));
        Set<PredicateImpl> andFacts = pred1Result.facts().retainAll(pred2Result.facts());
        Set<PredicateImpl> exOrFacts = pred1Result.facts().addAll(pred2Result.facts()).removeAll(andFacts);
        Set<PredicateImpl> falsehoods = pred1Result.falsehoods().addAll(pred2Result.falsehoods()).addAll(exOrFacts);
        Set<PredicateImpl> cycles = pred1Result.cycles().addAll(pred2Result.cycles());
        return flip(InferResult.of(andFacts, falsehoods, cycles));
    }

    protected abstract InferResult flip(InferResult result);
}
