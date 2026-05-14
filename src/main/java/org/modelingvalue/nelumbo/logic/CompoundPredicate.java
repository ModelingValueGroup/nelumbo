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
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;

public abstract class CompoundPredicate extends Predicate {
    @Serial
    private static final long serialVersionUID = -4926802375244295351L;

    protected CompoundPredicate(Functor functor, List<AstElement> elements, Node declaration, Object... predicates) {
        super(functor, elements, declaration, predicates);
    }

    protected CompoundPredicate(Type type, List<AstElement> elements, Node declaration, Object... predicates) {
        super(type, elements, declaration, predicates);
    }

    protected CompoundPredicate(Object[] args, Node functorOrType, List<AstElement> elements,
            CompoundPredicate declaration) {
        super(args, functorOrType, elements, declaration);
    }

    @Override
    public InferResult resolve(InferContext context) {
        Map<Map<Variable, Object>, Predicate> now, next = Map.of(Entry.of(getBinding(), this));
        Set<Predicate> facts = Set.of(), falsehoods = Set.of(), cycles = Set.of();
        boolean completeFacts = true, completeFalsehoods = true;
        InferContext deep = context.toDeep(); // Resolve variables shallow (bind)
        InferContext shallow = context.toShallow(); // Resolve variables deep (bind)
        InferContext reduce = context.toReduce(); // Do not resolve variables but perform logic and simplify (rewrite)
        do {
            now = next;
            next = Map.of();
            for (Entry<Map<Variable, Object>, Predicate> entry : now) {
                Map<Variable, Object> binding = entry.getKey();
                Predicate predicate = entry.getValue();
                InferContext resolve = shallow;
                InferResult result = predicate.infer(reduce);
                if (result.hasStackOverflow()) {
                    return result;
                } else if (result.isFalseCC()) {
                    falsehoods = falsehoods.add(setBinding(binding));
                } else if (result.isTrueCC()) {
                    facts = facts.add(setBinding(binding));
                } else {
                    predicate = result.predicate();
                    resolve = deep;
                }
                result = predicate.infer(resolve);
                if (result.hasStackOverflow()) {
                    return result;
                } else if (!result.unresolvable()) {
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
                    completeFacts &= result.completeFacts();
                    completeFalsehoods &= result.completeFalsehoods();
                    cycles = cycles.addAll(result.cycles());
                }
            }
        } while (!next.isEmpty());
        if (facts.isEmpty() && completeFacts && falsehoods.isEmpty() && completeFalsehoods) {
            completeFacts = false;
            completeFalsehoods = false;
        }
        return InferResult.of(this, facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

}
