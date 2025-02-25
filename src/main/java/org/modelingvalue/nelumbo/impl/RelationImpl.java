package org.modelingvalue.nelumbo.impl;

import java.util.Objects;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.LogicLambda;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Logic.Relation;

public class RelationImpl extends PredicateImpl {
    private static final long serialVersionUID   = 1032898038061287135L;

    static final int          MAX_LOGIC_DEPTH    = Integer.getInteger("MAX_LOGIC_DEPTH", 32);
    private static final int  MAX_LOGIC_DEPTH_D2 = MAX_LOGIC_DEPTH / 2;

    public RelationImpl(Functor<Relation> functor, Object... args) {
        super(functor, args);
    }

    private RelationImpl(Object[] args, RelationImpl declaration) {
        super(args, declaration);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RelationImpl struct(Object[] array, PredicateImpl declaration) {
        return new RelationImpl(array, (RelationImpl) declaration);
    }

    @Override
    public RelationImpl declaration() {
        return (RelationImpl) super.declaration();
    }

    @Override
    public RelationImpl set(int i, Object... a) {
        return (RelationImpl) super.set(i, a);
    }

    @Override
    protected PredicateImpl castFrom(PredicateImpl from) {
        Object[] array = from.toArray();
        array[0] = functor();
        return new RelationImpl(array, declaration());
    }

    @Override
    protected InferResult infer(InferContext context) {
        int nrOfUnbound = nrOfUnbound();
        if (nrOfUnbound > 0 && context.reduce()) {
            return unknown();
        }
        prefix(context);
        FunctorImpl<Predicate> functor = functor();
        LogicLambda logic = functor.logicLambda();
        if (logic != null) {
            return result(logic.apply((PredicateImpl) this, context), context);
        }
        if (nrOfUnbound > 1 || (nrOfUnbound == 1 && functor.args().size() == 1)) {
            return result(unknown(), context);
        }
        KnowledgeBaseImpl knowledgebase = context.knowledgebase();
        if (knowledgebase.getRules(this) != null) {
            InferResult result = knowledgebase.getMemoiz(this);
            if (result != null) {
                return result(result, context);
            }
            result = context.getCycleResult(this);
            if (result != null) {
                return result(context.reduce() ? unknown() : result, context);
            }
            List<RelationImpl> stack = context.stack();
            if (stack.size() >= MAX_LOGIC_DEPTH) {
                return result(InferResult.overflow(stack.append(this)), context);
            }
            if (context.trace()) {
                System.err.println();
            }
            result = fixpoint(context.pushOnStack(this));
            if (stack.size() >= MAX_LOGIC_DEPTH_D2) {
                List<RelationImpl> overflow = result.stackOverflow();
                if (overflow != null) {
                    if (stack.size() == MAX_LOGIC_DEPTH_D2) {
                        result = flatten(result, overflow, context);
                    }
                    prefix(context);
                    return result(result, context);
                }
            }
            knowledgebase.memoization(this, result);
            prefix(context);
            return result(result, context);
        } else {
            return result(knowledgebase.getFacts(this), context);
        }
    }

    private void prefix(InferContext context) {
        if (context.trace()) {
            System.err.print(context.prefix() + "  " + toString(null));
        }
    }

    private InferResult result(InferResult result, InferContext context) {
        if (context.trace()) {
            System.err.println("\u2192" + result);
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static InferResult flatten(InferResult result, List<RelationImpl> overflow, InferContext context) {
        int stackSize = context.stack().size();
        List<RelationImpl> todo = overflow.sublist(stackSize, overflow.size());
        while (todo.size() > 0) {
            RelationImpl predicate = todo.last();
            result = predicate.fixpoint(context.pushOnStack(predicate));
            overflow = result.stackOverflow();
            if (overflow != null) {
                todo = todo.appendList(overflow.sublist(stackSize, overflow.size()));
            } else {
                context.knowledgebase().memoization(predicate, result);
                todo = todo.removeLast();
            }
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private InferResult fixpoint(InferContext context) {
        InferResult previousResult = InferResult.EMPTY, cycleResult = InferResult.cycle(this), nextResult;
        do {
            nextResult = inferRules(context.putCycleResult(this, cycleResult));
            if (nextResult.hasStackOverflow()) {
                return nextResult;
            }
            if (nextResult.hasCycleWith(this)) {
                if (!nextResult.equals(previousResult) && !nextResult.equals(cycleResult)) {
                    previousResult = nextResult;
                    cycleResult = InferResult.of(nextResult.facts().add(this), nextResult.falsehoods().add(this), (Set) singleton());
                    context.knowledgebase().memoization(this, cycleResult);
                    continue;
                } else {
                    return InferResult.of(uncycle(nextResult.facts()), uncycle(nextResult.falsehoods()), nextResult.cycles().remove(this));
                }
            }
            return nextResult;
        } while (true);
    }

    private Set<PredicateImpl> uncycle(Set<PredicateImpl> set) {
        return set.equals(singleton()) && !isFullyBound() ? set : set.remove(this);
    }

    @SuppressWarnings("rawtypes")
    private InferResult inferRules(InferContext context) {
        KnowledgeBaseImpl knowledgebase = context.knowledgebase();
        InferResult result = knowledgebase.getFacts(this), ruleResult;
        if (result.falsehoods().isEmpty()) {
            return result;
        }
        List<RuleImpl> rules = knowledgebase.getRules(this);
        for (RuleImpl rule : REVERSE_NELUMBO ? rules.reverse() : RANDOM_NELUMBO ? rules.random() : rules) {
            ruleResult = rule.imply(this, context);
            if (ruleResult != null) {
                if (ruleResult.hasStackOverflow()) {
                    return ruleResult;
                } else if (ruleResult.falsehoods().isEmpty()) {
                    return ruleResult;
                } else {
                    result = result.or(ruleResult);
                }
            }
        }
        return result;
    }

    @Override
    protected InferResult resolve(PredicateImpl consequence, InferContext context) {
        InferResult result = infer(context);
        return result.bind(this, consequence);
    }

    @SuppressWarnings("rawtypes")
    protected final RelationImpl signature() {
        Object[] array = null;
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            Object s = typeOf(v);
            if (!Objects.equals(s, v)) {
                if (array == null) {
                    array = toArray();
                }
                array[i] = s;
            }
        }
        return array != null ? struct(array, declaration()) : this;
    }

}
