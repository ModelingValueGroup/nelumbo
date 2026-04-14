//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.logic;

import java.io.Serial;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableSet;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;

public abstract class CompoundPredicate extends Predicate {
    @Serial
    private static final long serialVersionUID = -4926802375244295351L;

    protected CompoundPredicate(Functor functor, List<AstElement> elements, Object... predicates) {
        super(functor, elements, predicates);
    }

    protected CompoundPredicate(Type type, List<AstElement> elements, Object... predicates) {
        super(type, elements, predicates);
    }

    protected CompoundPredicate(Object[] args, List<AstElement> elements, CompoundPredicate declaration) {
        super(args, elements, declaration);
    }

    @Override
    public InferResult resolve(InferContext context) {
        Map<Map<Variable, Object>, Predicate> now, next = Map.of(Entry.of(getBinding(), this));
        MutableSet<Predicate> facts = MutableSet.of(Set.of()), falsehoods = MutableSet.of(Set.of()),
                cycles = MutableSet.of(Set.of());
        boolean[] completeFacts = new boolean[] { true }, completeFalsehoods = new boolean[] { true };
        InferContext deep = context.toDeep(); // Resolve variables shallow
        InferContext shallow = context.toShallow(); // resolve variables deep
        InferContext reduce = context.toReduce(); // Do not resolve variables and simplify predicate
        do {
            now = next;
            next = Map.of();
            for (Entry<Map<Variable, Object>, Predicate> entry : now) {
                Map<Variable, Object> binding = entry.getKey();
                Predicate predicate = entry.getValue();
                InferResult result = predicate.infer(shallow);
                if (result.hasStackOverflow()) {
                    return result;
                }
                if (!result.unresolvable()) {
                    next = applyBindings(result, binding, predicate, next, cycles, completeFacts, completeFalsehoods);
                }
                result = predicate.infer(reduce);
                if (result.hasStackOverflow()) {
                    return result;
                } else if (result.isFalseCC()) {
                    falsehoods.add(setBinding(binding));
                } else if (result.isTrueCC()) {
                    facts.add(setBinding(binding));
                } else {
                    predicate = result.predicate();
                    result = predicate.infer(deep);
                    if (result.hasStackOverflow()) {
                        return result;
                    }
                    if (!result.unresolvable()) {
                        next = applyBindings(result, binding, predicate, next, cycles, completeFacts,
                                completeFalsehoods);

                    }
                }
            }
        } while (!next.isEmpty());
        if (completeFacts[0] && completeFalsehoods[0] && facts.isEmpty() && falsehoods.isEmpty())

        {
            completeFacts[0] = false;
            completeFalsehoods[0] = false;
        }
        return InferResult.of(this, facts.get(), completeFacts[0], falsehoods.get(), completeFalsehoods[0],
                cycles.get());
    }

    private static Map<Map<Variable, Object>, Predicate> applyBindings(InferResult result,
            Map<Variable, Object> binding, Predicate predicate, Map<Map<Variable, Object>, Predicate> next,
            MutableSet<Predicate> cycles, boolean[] completeFacts, boolean[] completeFalsehoods) {
        for (Predicate pred : result.allFacts()) {
            Map<Variable, Object> b = pred.getBinding();
            if (!b.isEmpty()) {
                b = binding.putAll(b);
                next = next.put(b, predicate.setBinding(b).replace(pred, NBoolean.TRUE));
            }
        }
        for (Predicate pred : result.allFalsehoods()) {
            Map<Variable, Object> b = pred.getBinding();
            if (!b.isEmpty()) {
                b = binding.putAll(b);
                next = next.put(b, predicate.setBinding(b).replace(pred, NBoolean.FALSE));
            }
        }
        completeFacts[0] &= result.completeFacts();
        completeFalsehoods[0] &= result.completeFalsehoods();
        if (!result.cycles().isEmpty()) {
            cycles.set(result.cycles()::addAll);
        }
        return next;
    }

}
