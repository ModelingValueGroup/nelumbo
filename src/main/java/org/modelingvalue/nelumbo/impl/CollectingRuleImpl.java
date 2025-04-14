package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.RuleModifier;

public class CollectingRuleImpl extends RuleImpl {
    private static final long serialVersionUID = -2048323475525053868L;

    @SuppressWarnings("rawtypes")
    private VariableImpl      result;
    @SuppressWarnings("rawtypes")
    private VariableImpl      iterator;
    @SuppressWarnings("rawtypes")
    private StructureImpl<?>  identity;
    private PredicateImpl<?>  identityFact;

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
        Map<VariableImpl, Object> resultVars = collVars.retainAllKey(consVars).removeAllKey(condVars);
        if (resultVars.size() != 1) {
            throw new IllegalArgumentException("Collecting Rules shoud have exactly one shared (result) variable in the consequence and the collector (that is not used in the condition), " + resultVars.size() + " found in " + this);
        }
        result = resultVars.get(0).getKey();
        Map<VariableImpl, Object> iteratorVars = collVars.retainAllKey(condVars).removeAllKey(consVars);
        if (iteratorVars.size() != 1) {
            throw new IllegalArgumentException("Collecting Rules shoud have exactly one shared (iterator) variable in the condition and the collector (that is not used in the consequence), " + iteratorVars.size() + " found in " + this);
        }
        iterator = iteratorVars.get(0).getKey();
        Map<StructureImpl, Object> identityStrcs = collector().structures();
        if (identityStrcs.size() != 1) {
            throw new IllegalArgumentException("Collecting Rules shoud have exactly one (identity) constant in the collector, " + identityStrcs.size() + " found in " + collector());
        }
        identity = identityStrcs.get(0).getKey();
        identityFact = collector().set(iterator, identity).set(result, identity);
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
        RelationImpl collector = collector(), condColl;
        Set<PredicateImpl<?>> prev, next = Set.of(identityFact), facts, falsehoods;
        Set<RelationImpl> cycles = condResult.cycles();
        for (PredicateImpl<?> condFact : condResult.facts()) {
            prev = next;
            next = Set.of();
            condColl = collector.set(iterator, condFact.get(iterator));
            for (PredicateImpl<?> prevFact : prev) {
                PredicateImpl<?> coll = condColl.replace(identity, prevFact.get(result));
                InferResult inferResult = coll.infer(context);
                if (inferResult.hasStackOverflow()) {
                    return inferResult;
                }
                next = next.addAll(inferResult.facts());
                cycles = cycles.addAll(inferResult.cycles());
            }
        }
        facts = next.replaceAll(f -> consequence.set(result, f.get(result)));
        next = Set.of(identityFact);
        for (PredicateImpl<?> condFalsehood : condResult.falsehoods()) {
            prev = next;
            next = Set.of();
            condColl = collector.set(iterator, condFalsehood.get(iterator));
            for (PredicateImpl<?> prevFalsehood : prev) {
                PredicateImpl<?> coll = condColl.replace(identity, prevFalsehood.get(result));
                InferResult inferResult = coll.infer(context);
                if (inferResult.hasStackOverflow()) {
                    return inferResult;
                }
                next = next.addAll(inferResult.facts());
                cycles = cycles.addAll(inferResult.cycles());
            }
        }
        falsehoods = next.replaceAll(f -> consequence.set(result, f.get(result)));
        return InferResult.of(facts, falsehoods.removeAll(facts), cycles);
    }
}
