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

package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;

public final class Relation extends Predicate {
    private static final long serialVersionUID   = 1032898038061287135L;
    public static final Type  TYPE               = new Type(Relation.class);

    static final int          MAX_LOGIC_DEPTH    = Integer.getInteger("MAX_LOGIC_DEPTH", 32);
    private static final int  MAX_LOGIC_DEPTH_D2 = MAX_LOGIC_DEPTH / 2;

    private final InferResult cycleResult        = InferResult.cycle(Set.of(), Set.of(), this);

    public Relation(Functor functor, Object... args) {
        super(functor, args);
    }

    private Relation(Object[] args, Relation declaration) {
        super(args, declaration);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Relation struct(Object[] array, Predicate declaration) {
        return new Relation(array, (Relation) declaration);
    }

    @Override
    public Relation declaration() {
        return (Relation) super.declaration();
    }

    @Override
    public Relation set(int i, Object... a) {
        return (Relation) super.set(i, a);
    }

    @Override
    public Relation set(int[] idx, Object val) {
        return (Relation) super.set(idx, val);
    }

    @Override
    protected final Relation set(Variable var, Object val) {
        return (Relation) super.set(var, val);
    }

    @Override
    protected Relation clearDeclaration() {
        return (Relation) super.clearDeclaration();
    }

    @Override
    protected Predicate castFrom(Predicate from) {
        Object[] array = from.toArray();
        array[0] = functor();
        return new Relation(array, declaration());
    }

    @Override
    protected InferResult resolve(InferContext context) {
        return infer(context);
    }

    @Override
    protected final InferResult infer(InferContext context) {
        int nrOfUnbound = nrOfUnbound();
        if (nrOfUnbound > 0 && context.reduce()) {
            return unknown();
        }
        prefix(context);
        return result(infer(nrOfUnbound, context), context);
    }

    protected InferResult infer(int nrOfUnbound, InferContext context) {
        Functor functor = functor();
        if (nrOfUnbound > 1 || (nrOfUnbound == 1 && functor.args().size() == 1)) {
            return unknown();
        }
        KnowledgeBase knowledgebase = context.knowledgebase();
        if (knowledgebase.getRules(this).isEmpty()) {
            return knowledgebase.getFacts(this);
        } else {
            InferResult result = knowledgebase.getMemoiz(this);
            if (result != null) {
                return result;
            }
            result = context.getCycleResult(this);
            if (result != null) {
                return context.reduce() ? unknown() : result;
            }
            List<Relation> stack = context.stack();
            if (stack.size() >= MAX_LOGIC_DEPTH) {
                return InferResult.overflow(stack.append(this));
            }
            if (context.trace()) {
                System.err.println();
            }
            result = fixpoint(context.pushOnStack(this));
            if (stack.size() >= MAX_LOGIC_DEPTH_D2) {
                List<Relation> overflow = result.stackOverflow();
                if (overflow != null) {
                    if (stack.size() == MAX_LOGIC_DEPTH_D2) {
                        result = flatten(result, overflow, context);
                    }
                    prefix(context);
                    return result;
                }
            }
            knowledgebase.memoization(this, result);
            prefix(context);
            return result;
        }
    }

    private void prefix(InferContext context) {
        if (context.trace()) {
            System.err.print(context.prefix() + "    " + toString());
        }
    }

    private InferResult result(InferResult result, InferContext context) {
        if (context.trace()) {
            System.err.println("\u2192" + result);
        }
        return result;
    }

    private static InferResult flatten(InferResult result, List<Relation> overflow, InferContext context) {
        int stackSize = context.stack().size();
        List<Relation> todo = overflow.sublist(stackSize, overflow.size());
        while (todo.size() > 0) {
            Relation predicate = todo.last();
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

    private InferResult fixpoint(InferContext context) {
        InferResult previousResult = null, cycleResult = this.cycleResult, nextResult;
        do {
            nextResult = inferRules(context.putCycleResult(this, cycleResult));
            if (nextResult.hasStackOverflow()) {
                return nextResult;
            }
            if (nextResult.hasCycleWith(this)) {
                if (!nextResult.equals(previousResult)) {
                    previousResult = nextResult;
                    cycleResult = InferResult.cycle(nextResult.facts(), nextResult.falsehoods(), this);
                    context.knowledgebase().memoization(this, cycleResult);
                    continue;
                } else {
                    return InferResult.of(nextResult.facts(), nextResult.completeFacts(), //
                            nextResult.falsehoods(), nextResult.completeFalsehoods(), //
                            nextResult.cycles().remove(this));
                }
            }
            return nextResult;
        } while (true);
    }

    private InferResult inferRules(InferContext context) {
        KnowledgeBase knowledgebase = context.knowledgebase();
        InferResult result = knowledgebase.getFacts(this), ruleResult;
        if (result.isTrueCC()) {
            return result;
        }
        Set<Rule> rules = knowledgebase.getRules(this);
        for (Rule rule : REVERSE_NELUMBO ? rules.reverse() : RANDOM_NELUMBO ? rules.random() : rules) {
            ruleResult = rule.imply(this, context);
            if (ruleResult != null) {
                if (ruleResult.hasStackOverflow()) {
                    return ruleResult;
                } else if (ruleResult.isTrueCC()) {
                    return ruleResult;
                } else if (ruleResult.hasCycleWith(this)) {
                    result = result.or(ruleResult.complete());
                } else {
                    result = result.or(ruleResult);
                }
            }
        }
        return result;
    }

    @Override
    protected final Relation setType(int i, Type type) {
        return (Relation) super.setType(i, type);
    }

    @Override
    protected final Relation setTyped(int i, Structure typed) {
        return (Relation) super.setTyped(i, typed);
    }

    @Override
    protected final Relation signature(int depth) {
        return (Relation) super.signature(depth);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final Set<Relation> generalize(boolean full) {
        return (Set<Relation>) super.generalize(full);
    }

}
