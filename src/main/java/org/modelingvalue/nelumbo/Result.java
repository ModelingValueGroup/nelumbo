package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.impl.InferResult;
import org.modelingvalue.nelumbo.impl.PredicateImpl;
import org.modelingvalue.nelumbo.impl.StructureImpl;

public final class Result {

    private final InferResult inferResult;

    public Result(InferResult inferResult) {
        this.inferResult = inferResult;
    }

    public Result(Set<Predicate> facts, boolean completeFacts, Set<Predicate> falsehoods, boolean completeFalsehoods) {
        this.inferResult = InferResult.of(unproxy(facts), completeFacts, unproxy(falsehoods), completeFalsehoods, Set.of());
    }

    private static Set<PredicateImpl<?>> unproxy(Set<Predicate> set) {
        return set.replaceAll(StructureImpl::unproxy);
    }

    public Set<Predicate> facts() {
        return inferResult.facts().replaceAll(PredicateImpl::proxyWithVariables);
    }

    public Set<Predicate> falsehoods() {
        return inferResult.falsehoods().replaceAll(PredicateImpl::proxyWithVariables);
    }

    public boolean completeFacts() {
        return inferResult.completeFacts();
    }

    public boolean completeFalsehoods() {
        return inferResult.completeFalsehoods();
    }

    @Override
    public int hashCode() {
        return inferResult.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Result)) {
            return false;
        } else {
            Result other = (Result) obj;
            return inferResult.equals(other.inferResult);
        }
    }

    @Override
    public String toString() {
        return inferResult.toString();
    }
}
