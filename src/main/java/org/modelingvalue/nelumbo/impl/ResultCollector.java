package org.modelingvalue.nelumbo.impl;

public interface ResultCollector {

    default InferResult addFact(InferResult result, PredicateImpl<?> fact) {
        return result.addFact(fact);
    }

    default InferResult addFalsehood(InferResult result, PredicateImpl<?> falsehood) {
        return result.addFalsehood(falsehood);
    }

    default InferResult addIncompleteFact(InferResult preResult, InferResult postResult, PredicateImpl<?> incomplete) {
        return preResult.facts().isEmpty() && !preResult.falsehoods().contains(incomplete) ? postResult.addFact(incomplete) : postResult;
    }

    default InferResult addIncompleteFalsehood(InferResult preResult, InferResult postResult, PredicateImpl<?> incomplete) {
        return !preResult.facts().contains(incomplete) ? postResult.addFalsehood(incomplete) : postResult;
    }

}
