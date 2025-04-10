package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.collections.Map;

public interface ResultCollector {

    @SuppressWarnings("rawtypes")
    default InferResult addFact(InferResult result, PredicateImpl<?> incomplete, Map<VariableImpl, Object> binding) {
        PredicateImpl<?> fact = incomplete.setBinding(binding);
        return result.addFact(fact);
    }

    @SuppressWarnings("rawtypes")
    default InferResult addFalsehood(InferResult result, PredicateImpl<?> incomplete, Map<VariableImpl, Object> binding) {
        PredicateImpl<?> falsehood = incomplete.setBinding(binding);
        return result.addFalsehood(falsehood);
    }

    default InferResult collect(InferResult postResult, PredicateImpl<?> incomplete, InferContext context) {
        return postResult;
    }

    default InferResult addIncompleteFact(InferResult preResult, InferResult postResult, PredicateImpl<?> incomplete) {
        return preResult.facts().isEmpty() && !preResult.falsehoods().contains(incomplete) ? postResult.addFact(incomplete) : postResult;
    }

    default InferResult addIncompleteFalsehood(InferResult preResult, InferResult postResult, PredicateImpl<?> incomplete) {
        return !preResult.facts().contains(incomplete) ? postResult.addFalsehood(incomplete) : postResult;
    }

}
