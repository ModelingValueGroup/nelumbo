package org.modelingvalue.nelumbo.impl;

public interface ResultCollector {

    default InferResult addFact(InferResult result, PredicateImpl<?> fact, PredicateImpl<?> incomplete) {
        return result.addFact(fact);
    }

    default InferResult addFalsehood(InferResult result, PredicateImpl<?> falsehood, PredicateImpl<?> incomplete) {
        return result.addFalsehood(falsehood);
    }

    default InferResult addIncompleteFact(InferResult result, PredicateImpl<?> incomplete) {
        return result.addFact(incomplete);
    }

    default InferResult addIncompleteFalsehood(InferResult result, PredicateImpl<?> incomplete) {
        return result.addFalsehood(incomplete);
    }

}
