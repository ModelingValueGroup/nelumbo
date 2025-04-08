package org.modelingvalue.nelumbo.impl;

public interface ResultCollector {

    default InferResult addFact(InferResult result, PredicateImpl<?> fact) {
        return result.addFact(fact);
    }

    default InferResult addFalsehood(InferResult result, PredicateImpl<?> falsehood) {
        return result.addFalsehood(falsehood);
    }

    default InferResult addIncompleteFact(InferResult preResult, InferResult postResult, PredicateImpl<?> incomplete) {
        return postResult.addFact(incomplete);
    }

    default InferResult addIncompleteFalsehood(InferResult preResult, InferResult postResult, PredicateImpl<?> incomplete) {
        return postResult.addFalsehood(incomplete);
    }

}
