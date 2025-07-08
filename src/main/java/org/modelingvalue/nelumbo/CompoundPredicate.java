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
