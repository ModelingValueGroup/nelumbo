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

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;

public abstract class CompoundPredicate extends Predicate {
    private static final long serialVersionUID = -4926802375244295351L;

    protected CompoundPredicate(Functor functor, Object... predicates) {
        super(functor, predicates);
    }

    protected CompoundPredicate(Object[] args, CompoundPredicate declaration) {
        super(args, declaration);
    }

    @Override
    protected final InferResult resolve(InferContext context) {
        Map<Map<Variable, Object>, Predicate> now, next = Map.of(Entry.of(getBinding(), this));
        Set<Predicate> facts = Set.of(), falsehoods = Set.of();
        boolean completeFacts = true, completeFalsehoods = true;
        Set<Predicate> cycles = Set.of();
        InferContext reduce = context.reduce(true);
        do {
            now = next;
            next = Map.of();
            for (Entry<Map<Variable, Object>, Predicate> entry : now) {
                InferResult result = entry.getValue().infer(reduce);
                if (result.hasStackOverflow()) {
                    return result;
                } else if (result.isFalseCC()) {
                    falsehoods = falsehoods.add(setBinding(entry.getKey()));
                } else if (result.isTrueCC()) {
                    facts = facts.add(setBinding(entry.getKey()));
                } else {
                    Predicate predicate = result.unknown();
                    result = predicate.infer(context);
                    if (result.hasStackOverflow()) {
                        return result;
                    } else {
                        for (Predicate pred : result.facts()) {
                            Map<Variable, Object> binding = entry.getKey().putAll(pred.getBinding());
                            next = next.put(binding, predicate.setBinding(binding).replace(pred, Boolean.TRUE));
                        }
                        for (Predicate pred : result.falsehoods()) {
                            Map<Variable, Object> binding = entry.getKey().putAll(pred.getBinding());
                            next = next.put(binding, predicate.setBinding(binding).replace(pred, Boolean.FALSE));
                        }
                        completeFacts &= result.completeFacts();
                        completeFalsehoods &= result.completeFalsehoods();
                        cycles = cycles.addAll(result.cycles());
                    }
                }
            }
        } while (!next.isEmpty());
        return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

}
