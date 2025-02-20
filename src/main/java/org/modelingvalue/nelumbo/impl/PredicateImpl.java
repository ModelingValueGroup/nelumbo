//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.impl;

import java.util.Objects;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.LogicLambda;
import org.modelingvalue.nelumbo.Logic.Predicate;

public class PredicateImpl extends StructureImpl<Predicate> {

    private static final long        serialVersionUID   = -1605559565948158856L;

    static final int                 MAX_LOGIC_DEPTH    = Integer.getInteger("MAX_LOGIC_DEPTH", 32);
    private static final int         MAX_LOGIC_DEPTH_D2 = MAX_LOGIC_DEPTH / 2;

    private final Set<PredicateImpl> singleton          = Set.of(this);

    public PredicateImpl(Functor<Predicate> functor, Object... args) {
        super(functor, args);
    }

    public PredicateImpl(FunctorImpl<Predicate> functor, Object... args) {
        super(functor, args);
    }

    protected PredicateImpl(Object[] args, PredicateImpl declaration) {
        super(args, declaration);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected PredicateImpl struct(Object[] array) {
        return new PredicateImpl(array, declaration());
    }

    protected PredicateImpl setDeclaration(PredicateImpl to) {
        return new PredicateImpl(toArray(), to.declaration());
    }

    @Override
    public PredicateImpl declaration() {
        return (PredicateImpl) super.declaration();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public PredicateImpl setBinding(Map<VariableImpl, Object> vars) {
        return (PredicateImpl) super.setBinding(vars);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl signature() {
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
        return array != null ? struct(array) : this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class getType(int i) {
        Object v = get(i);
        return v instanceof Class ? (Class) v : v instanceof StructureImpl ? ((StructureImpl) v).type() : null;
    }

    public InferResult infer() {
        InferContext context = KnowledgeBaseImpl.CURRENT.get().context();
        if (context.trace()) {
            System.err.println(context.prefix() + toString(null));
        }
        PredicateImpl conditon = setBinding(variables());
        InferResult result = conditon.resolve(context);
        if (context.trace()) {
            System.err.println(context.prefix() + toString(null) + "\u2192" + result);
        }
        return result;
    }

    protected InferResult expand(InferContext context) {
        throw new UnsupportedOperationException();
    }

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
            List<PredicateImpl> stack = context.stack();
            if (stack.size() >= MAX_LOGIC_DEPTH) {
                return result(InferResult.overflow(stack.append(this)), context);
            }
            if (context.trace()) {
                System.err.println();
            }
            result = fixpoint(context.pushOnStack(this));
            if (stack.size() >= MAX_LOGIC_DEPTH_D2) {
                List<PredicateImpl> overflow = result.stackOverflow();
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
    private static InferResult flatten(InferResult result, List<PredicateImpl> overflow, InferContext context) {
        int stackSize = context.stack().size();
        List<PredicateImpl> todo = overflow.sublist(stackSize, overflow.size());
        while (todo.size() > 0) {
            PredicateImpl predicate = todo.last();
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
                    cycleResult = InferResult.of(nextResult.facts().add(this), nextResult.falsehoods().add(this), singleton());
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

    @SuppressWarnings("rawtypes")
    public boolean contains(PredicateImpl cond) {
        return equals(cond);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public PredicateImpl set(int i, Object... a) {
        return (PredicateImpl) super.set(i, a);
    }

    public final InferResult unknown() {
        return InferResult.unknowns(singleton);
    }

    public final InferResult fact() {
        return InferResult.facts(singleton);
    }

    public final InferResult falsehood() {
        return InferResult.falsehoods(singleton);
    }

    public final Set<PredicateImpl> singleton() {
        return singleton;
    }

    @SuppressWarnings("rawtypes")
    protected final InferResult resolve(InferContext context) {
        Set<PredicateImpl> now, next = singleton(), bindings, facts = Set.of(), falsehoods = Set.of(), cycles = Set.of();
        InferContext reduce = context.reduceExpand(true, false), expand = context.reduceExpand(false, true);
        PredicateImpl reduced;
        InferResult predResult, reducedResult, bindResult;
        do {
            now = next;
            next = Set.of();
            for (PredicateImpl predicate : now) {
                predResult = predicate.infer(reduce);
                if (predResult.hasStackOverflow()) {
                    return predResult;
                } else if (predResult.facts().isEmpty()) {
                    falsehoods = falsehoods.add(predicate);
                } else if (predResult.falsehoods().isEmpty()) {
                    facts = facts.add(predicate);
                } else {
                    reduced = predResult.facts().get(0);
                    reducedResult = reduced.infer(expand);
                    if (reducedResult.hasStackOverflow()) {
                        return reducedResult;
                    } else {
                        bindResult = reducedResult.bind(null, predicate);
                        bindings = bindResult.facts().addAll(bindResult.falsehoods()).removeAll(now);
                        next = next.addAll(bindings);
                        cycles = cycles.addAll(bindResult.cycles());
                        if (!bindResult.facts().allMatch(bindings::contains)) {
                            facts = facts.add(this);
                        }
                        if (!bindResult.falsehoods().allMatch(bindings::contains)) {
                            falsehoods = falsehoods.add(this);
                        }
                    }
                }
            }
        } while (!next.isEmpty());
        predResult = InferResult.of(facts, falsehoods, cycles);
        if (context.trace()) {
            System.err.println(context.prefix() + toString(null) + "\u2192" + predResult);
        }
        return predResult;
    }

}
