package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.collections.Map;
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
    @SuppressWarnings("rawtypes")
    public InferResult addFact(InferResult result, PredicateImpl<?> incomplete, Map<VariableImpl, Object> binding) {
        return super.addFact(result, incomplete, binding.put(total, binding.get(iterator)));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public InferResult addFalsehood(InferResult result, PredicateImpl<?> incomplete, Map<VariableImpl, Object> binding) {
        return super.addFalsehood(result, incomplete, binding.put(total, binding.get(iterator)));
    }

    @Override
    public InferResult collect(InferResult result, PredicateImpl<?> incomplete, InferContext context) {
        PredicateImpl<?> resultFact = incomplete.set(total, init);
        for (PredicateImpl<?> fact : result.facts()) {
            Object pre = resultFact.get(total);
            PredicateImpl<?> coll = collector().replace(init, pre).set(iterator, fact.get(total));
            PredicateImpl<?> res = coll.infer(context).facts().get(0);
            resultFact = resultFact.replace(pre, res.get(total));
        }
        return InferResult.of(resultFact.singleton(), result.falsehoods(), result.cycles());
    }

}
