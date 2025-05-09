package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic;
import org.modelingvalue.nelumbo.Logic.Collect;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Logic.Relation;

public class CollectImpl extends PredicateImpl<Collect> {
    private static final long                 serialVersionUID    = -3084545514049410749L;

    private static final FunctorImpl<Collect> COLL_FUNCTOR        = FunctorImpl.<Collect, Predicate, Relation> of(Logic::coll);
    private static final Functor<Collect>     COLL_FUNCTORR_PROXY = COLL_FUNCTOR.proxy();

    private final VariableImpl<?>             resultVar;
    private final VariableImpl<?>             iteratorVar;
    private final StructureImpl<?>            identityCons;
    private final int[]                       identityIdx;
    private final PredicateImpl<?>            identityPred;

    @SuppressWarnings("rawtypes")
    public CollectImpl(Predicate condition, Predicate collector) {
        super(COLL_FUNCTORR_PROXY, condition, collector);
        Map<VariableImpl, Object> condVars = condition().variables();
        Map<VariableImpl, Object> collVars = collector().variables();
        // result
        Map<VariableImpl, Object> resultVars = collVars.removeAllKey(condVars);
        if (resultVars.size() != 1) {
            throw new IllegalArgumentException("Collect shoud have exactly one (result) variable in the collector (that is not used in the condition), " + resultVars.size() + " found in " + this);
        }
        resultVar = resultVars.get(0).getKey();
        // iterator
        Map<VariableImpl, Object> iteratorVars = collVars.retainAllKey(condVars);
        if (iteratorVars.size() != 1) {
            throw new IllegalArgumentException("Collect shoud have exactly one shared (iterator) variable in the condition and the collector, " + iteratorVars.size() + " found in " + this);
        }
        iteratorVar = iteratorVars.get(0).getKey();
        // identity
        Map<StructureImpl, int[]> collCons = collector().constants();
        if (collCons.size() != 1) {
            throw new IllegalArgumentException("Collect shoud have exactly one (identity) constant in the collector, " + collCons.size() + " found in " + collector());
        }
        identityCons = collCons.get(0).getKey();
        identityIdx = collCons.get(0).getValue();
        identityPred = collector().set(iteratorVar, identityCons).set(resultVar, identityCons);
        InferResult result = identityPred.infer();
        if (!result.equals(identityPred.factCC())) {
            throw new IllegalArgumentException("The (identity) constant in the collector of is not an identity, hence " + identityPred + " is not true");
        }
    }

    private CollectImpl(Object[] args, CollectImpl declaration) {
        super(args, declaration);
        resultVar = declaration.resultVar;
        iteratorVar = declaration.iteratorVar;
        identityCons = declaration.identityCons;
        identityIdx = declaration.identityIdx;
        identityPred = declaration.identityPred;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected CollectImpl struct(Object[] array, PredicateImpl declaration) {
        return new CollectImpl(array, (CollectImpl) declaration);
    }

    @Override
    public CollectImpl declaration() {
        return (CollectImpl) super.declaration();
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl<?> condition() {
        return (PredicateImpl) get(1);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl<?> collector() {
        return (PredicateImpl<?>) get(2);
    }

    @Override
    protected InferResult infer(InferContext context) {
        if (context.reduce()) {
            if (get(resultVar) instanceof Class) {
                return unknown();
            } else {
                return BooleanImpl.TRUE_CONCLUSION;
            }
        } else {
            InferResult condResult = condition().resolve(context);
            if (condResult.hasStackOverflow()) {
                return condResult;
            } else {
                return collect(condResult, context);
            }
        }
    }

    private InferResult collect(InferResult condResult, InferContext context) {
        PredicateImpl<?> collector = collector(), condColl;
        Set<PredicateImpl<?>> prev, next = Set.of(identityPred);
        Set<RelationImpl> cycles = condResult.cycles();
        for (PredicateImpl<?> condFact : condResult.facts()) {
            prev = next;
            next = Set.of();
            condColl = collector.set(iteratorVar, condFact.get(iteratorVar));
            for (PredicateImpl<?> prevFact : prev) {
                PredicateImpl<?> coll = condColl.set(identityIdx, prevFact.get(resultVar));
                InferResult inferResult = coll.resolve(context);
                if (inferResult.hasStackOverflow()) {
                    return inferResult;
                }
                next = next.addAll(inferResult.facts());
                cycles = cycles.addAll(inferResult.cycles());
            }
        }
        return InferResult.of(next.replaceAll(f -> identityPred.set(resultVar, f.get(resultVar))), cycles.isEmpty(), Set.of(), false, cycles);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<VariableImpl, Object> variables() {
        return super.variables();
    }

    @Override
    public CollectImpl set(int i, Object... a) {
        return (CollectImpl) super.set(i, a);
    }

    @Override
    public String toString() {
        return PRETTY_NELUMBO ? "(" + condition() + "\u03BB" + collector() + ")" : super.toString();
    }

}
