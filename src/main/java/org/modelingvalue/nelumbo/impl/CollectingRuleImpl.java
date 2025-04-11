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
        Map<VariableImpl, Object> totalVars = collVars.removeAllKey(condVars);
        Map<VariableImpl, Object> iteratorVars = collVars.removeAllKey(consVars);
        Map<StructureImpl, Object> initStrcs = collector().structures();
        // TODO: Checks!
        total = totalVars.get(0).getKey();
        iterator = iteratorVars.get(0).getKey();
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
