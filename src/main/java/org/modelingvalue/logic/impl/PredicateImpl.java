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

package org.modelingvalue.logic.impl;

import java.util.Objects;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.logic.Logic.Functor;
import org.modelingvalue.logic.Logic.LogicLambda;
import org.modelingvalue.logic.Logic.Predicate;

public class PredicateImpl extends StructureImpl<Predicate> {
    private static final long serialVersionUID   = -1605559565948158856L;

    static final int          MAX_LOGIC_DEPTH    = Integer.getInteger("MAX_LOGIC_DEPTH", 32);
    private static final int  MAX_LOGIC_DEPTH_D2 = MAX_LOGIC_DEPTH / 2;

    public PredicateImpl(Functor<Predicate> functor, Object... args) {
        super(functor, args);
    }

    public PredicateImpl(FunctorImpl<Predicate> functor, Object... args) {
        super(functor, args);
    }

    protected PredicateImpl(Object[] args) {
        super(args);
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

    public InferResult infer(PredicateImpl declaration, InferContext context) {
        FunctorImpl<Predicate> functor = functor();
        LogicLambda logic = functor.logic();
        if (logic != null) {
            return logic.apply((PredicateImpl) this, context);
        }
        int nrOfUnbound = nrOfUnbound();
        if (nrOfUnbound > 1 || (nrOfUnbound == 1 && functor.args().size() == 1)) {
            return InferResult.of(context.stack(this));
        }
        KnowledgeBaseImpl knowledgebase = context.knowledgebase();
        List<RuleImpl> rules = knowledgebase.getRules(this);
        InferResult result;
        if (rules != null) {
            result = context.cycleConclusion().get(this);
            if (result != null) {
                return result;
            }
            result = knowledgebase.getMemoiz(this);
            if (result != null) {
                return result;
            }
            List<PredicateImpl> stack = context.stack();
            if (stack.size() >= MAX_LOGIC_DEPTH || stack.lastIndexOf(this) >= 0) {
                return InferResult.of(stack.append(this));
            }
            result = fixpoint(rules, context.pushOnStack(this));
            if (stack.size() >= MAX_LOGIC_DEPTH_D2) {
                List<PredicateImpl> overflow = result.stackOverflow();
                if (overflow != null) {
                    if (stack.size() == MAX_LOGIC_DEPTH_D2) {
                        return flatten(result, overflow, context);
                    }
                    return result;
                }
            }
            knowledgebase.memoization(this, result);
        } else {
            result = knowledgebase.getFacts(this);
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static InferResult flatten(InferResult result, List<PredicateImpl> overflow, InferContext context) {
        int stackSize = context.stack().size();
        List<PredicateImpl> todo = overflow.sublist(stackSize, overflow.size());
        KnowledgeBaseImpl knowledgebase = context.knowledgebase();
        while (todo.size() > 0) {
            PredicateImpl predicate = todo.last();
            result = predicate.fixpoint(knowledgebase.getRules(predicate), context.pushOnStack(predicate));
            overflow = result.stackOverflow();
            if (overflow != null) {
                todo = todo.appendList(overflow.sublist(stackSize, overflow.size()));
            } else {
                knowledgebase.memoization(predicate, result);
                todo = todo.removeLast();
            }
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private InferResult fixpoint(List<RuleImpl> rules, InferContext context) {
        InferResult result = InferResult.EMPTY, ruleResult, facts = context.knowledgebase().getFacts(this);
        Set<PredicateImpl> addedFacts = Set.of(), addedFalsehoods = Set.of();
        boolean cycle = false;
        do {
            ruleResult = inferRules(rules, addedFacts.isEmpty() && addedFalsehoods.isEmpty() ? context : context.putCycleConclusion(this, addedFacts, addedFalsehoods), facts);
            if (ruleResult.hasStackOverflow()) {
                return ruleResult;
            }
            addedFacts = ruleResult.facts().removeAll(result.facts());
            addedFalsehoods = ruleResult.falsehoods().removeAll(result.falsehoods());
            cycle |= result == InferResult.EMPTY && !(addedFacts.isEmpty() && addedFalsehoods.isEmpty()) && ruleResult.hasCycleWith(this);
            if (cycle && result == InferResult.EMPTY) {
                result = InferResult.of(addedFacts, addedFalsehoods);
            } else {
                result = result.add(ruleResult);
            }
        } while (cycle && !(addedFacts.isEmpty() && addedFalsehoods.isEmpty()));
        return result;
    }

    @SuppressWarnings("rawtypes")
    private InferResult inferRules(List<RuleImpl> rules, InferContext context, InferResult result) {
        InferResult ruleResult;
        for (RuleImpl rule : rules) {
            ruleResult = rule.infer(this, context);
            if (ruleResult.hasStackOverflow()) {
                return ruleResult;
            } else {
                result = result.add(ruleResult);
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

    @SuppressWarnings("unchecked")
    public static <P extends Predicate> PredicateImpl of(FunctorImpl<P> functor, Object... args) {
        return new PredicateImpl((FunctorImpl<Predicate>) functor, args);
    }
}
