package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.RuleModifier;

public class CollectingRuleImpl extends RuleImpl {
    private static final long serialVersionUID = -2048323475525053868L;

    @SuppressWarnings("rawtypes")
    private VariableImpl      total;
    @SuppressWarnings("rawtypes")
    private VariableImpl      iterator;
    @SuppressWarnings("rawtypes")
    private StructureImpl     init;

    public CollectingRuleImpl(Relation consequence, Predicate condition, Relation collector, RuleModifier[] modifiers) {
        super(modifiers, consequence, condition, collector);
        init();
    }

    private CollectingRuleImpl(Object[] args) {
        super(args);
        init();
    }

    @Override
    protected CollectingRuleImpl struct(Object[] array) {
        return new CollectingRuleImpl(array);
    }

    @SuppressWarnings("rawtypes")
    private void init() {
        Map<VariableImpl, Object> consVars = consequence().variables();
        Map<VariableImpl, Object> condVars = condition().variables();
        Map<VariableImpl, Object> collVars = collector().variables();
        Map<VariableImpl, Object> totalVars = collVars.retainAllKey(consVars).removeAllKey(condVars);
        if (totalVars.size() != 1) {
            throw new IllegalArgumentException("Collecting Rules shoud have exactly one shared variable (total) in the consequence and the collector only (not used in the condition), " + totalVars.size() + " found in " + this);
        }
        total = totalVars.get(0).getKey();
        Map<VariableImpl, Object> iteratorVars = collVars.retainAllKey(condVars).removeAllKey(consVars);
        if (iteratorVars.size() != 1) {
            throw new IllegalArgumentException("Collecting Rules shoud have exactly one shared variable (iterator) in the condition and the collector only (not used in the consequence), " + iteratorVars.size() + " found in " + this);
        }
        iterator = iteratorVars.get(0).getKey();
        Map<StructureImpl, Object> initStrcs = collector().structures();
        if (initStrcs.size() != 1) {
            throw new IllegalArgumentException("Collecting Rules shoud have exactly one constant in the collector, " + initStrcs.size() + " found in " + collector());
        }
        init = initStrcs.get(0).getKey();
    }

    @Override
    public CollectingRuleImpl set(int i, Object... a) {
        return (CollectingRuleImpl) super.set(i, a);
    }

    @SuppressWarnings("rawtypes")
    public final RelationImpl collector() {
        return (RelationImpl) get(3);
    }

    @Override
    protected String collectorString() {
        return "\u03BB" + collector();
    }

    @Override
    protected InferResult collect(InferResult condResult, PredicateImpl<?> consequence, InferContext context) {
        RelationImpl collector = collector();
        Set<Object> now, next = Set.of(init);
        Set<RelationImpl> cycles = condResult.cycles();
        for (PredicateImpl<?> condFact : condResult.facts()) {
            now = next;
            next = Set.of();
            for (Object accum : now) {
                PredicateImpl<?> coll = collector.replace(init, accum).set(iterator, condFact.get(iterator));
                InferResult result = coll.infer(context);
                if (result.hasStackOverflow()) {
                    return result;
                }
                for (PredicateImpl<?> collFact : result.facts()) {
                    next = next.add(collFact.get(total));
                }
                cycles = cycles.addAll(result.cycles());
            }
        }
        return InferResult.of(next.replaceAll(a -> consequence.set(total, a)), Set.of(), cycles);
    }
}
