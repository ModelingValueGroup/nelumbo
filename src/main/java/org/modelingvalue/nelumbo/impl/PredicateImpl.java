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

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.LogicLambda;
import org.modelingvalue.nelumbo.Logic.Predicate;

public class PredicateImpl extends StructureImpl<Predicate> {
    private static final long serialVersionUID   = -1605559565948158856L;

    static final int          MAX_LOGIC_DEPTH    = Integer.getInteger("MAX_LOGIC_DEPTH", 32);
    private static final int  MAX_LOGIC_DEPTH_D2 = MAX_LOGIC_DEPTH / 2;

    private final InferResult incomplete;

    public PredicateImpl(Functor<Predicate> functor, Object... args) {
        super(functor, args);
        incomplete = InferResult.incomplete(this);
    }

    public PredicateImpl(FunctorImpl<Predicate> functor, Object... args) {
        super(functor, args);
        incomplete = InferResult.incomplete(this);
    }

    protected PredicateImpl(Object[] args) {
        super(args);
        incomplete = InferResult.incomplete(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected PredicateImpl struct(Object[] array) {
        return new PredicateImpl(array);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public PredicateImpl setBinding(StructureImpl<Predicate> pred, Map<VariableImpl, Object> vars) {
        return (PredicateImpl) super.setBinding(pred, vars);
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
        if (TRACE_NELUMBO) {
            System.err.println(context.prefix() + toString(null));
        }
        InferResult result = setBinding(this, variables()).infer(this, context);
        if (TRACE_NELUMBO) {
            System.err.println(context.prefix() + toString(null) + "\u2192" + result.setVariableNames(this));
        }
        return result;
    }

    public InferResult infer(PredicateImpl declaration, InferContext context) {
        FunctorImpl<Predicate> functor = functor();
        LogicLambda logic = functor.logicLambda();
        if (logic != null) {
            return logic.apply((PredicateImpl) this, context);
        }
        int nrOfUnbound = nrOfUnbound();
        if (nrOfUnbound > 1 || (nrOfUnbound == 1 && functor.args().size() == 1)) {
            return incomplete();
        }
        KnowledgeBaseImpl knowledgebase = context.knowledgebase();
        InferResult result;
        if (knowledgebase.getRules(this) != null) {
            result = knowledgebase.getMemoiz(this);
            if (result != null) {
                return result;
            }
            if (context.shallow()) {
                return incomplete();
            }
            result = context.cycleResult().get(this);
            if (result != null) {
                return result;
            }
            List<PredicateImpl> stack = context.stack();
            if (stack.size() >= MAX_LOGIC_DEPTH) {
                return InferResult.overflow(stack.append(this));
            }
            result = fixpoint(context.pushOnStack(this));
            if (stack.size() >= MAX_LOGIC_DEPTH_D2) {
                List<PredicateImpl> overflow = result.stackOverflow();
                if (overflow != null) {
                    if (stack.size() == MAX_LOGIC_DEPTH_D2) {
                        return flatten(result, overflow, context);
                    }
                    return result;
                }
            }
            knowledgebase.memoization(this, result, context);
        } else {
            result = knowledgebase.getFacts(this);
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
                context.knowledgebase().memoization(predicate, result, context);
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
                    continue;
                } else {
                    return InferResult.of(nextResult.facts().remove(this), nextResult.falsehoods().remove(this), nextResult.cycles().remove(this));
                }
            }
            return nextResult;
        } while (true);
    }

    @SuppressWarnings("rawtypes")
    private InferResult inferRules(InferContext context) {
        KnowledgeBaseImpl knowledgebase = context.knowledgebase();
        List<RuleImpl> rules = knowledgebase.getRules(this);
        InferResult result = knowledgebase.getFacts(this), ruleResult;
        if (result.falsehoods().isEmpty()) {
            return result;
        }
        for (RuleImpl rule : rules) {
            ruleResult = rule.infer(this, context);
            if (ruleResult != null) {
                if (ruleResult.hasStackOverflow()) {
                    return ruleResult;
                } else if (ruleResult.falsehoods().isEmpty()) {
                    return ruleResult;
                } else {
                    result = result.add(ruleResult);
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

    @SuppressWarnings("rawtypes")
    protected PredicateImpl setVariableNames(PredicateImpl declaration) {
        Map<VariableImpl, Object> vars = declaration.getBinding(this, Map.of());
        vars = vars.replaceAll(e -> e.getValue() instanceof Class ? Entry.of(e.getKey(), e.getKey()) : e);
        return declaration.setBinding(this, vars);
    }

    public InferResult incomplete() {
        return incomplete;
    }

    public Set<PredicateImpl> singleton() {
        return incomplete.facts();
    }
}
