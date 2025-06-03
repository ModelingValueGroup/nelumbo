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

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.QualifiedSet;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.struct.impl.Struct2Impl;
import org.modelingvalue.collections.util.Context;
import org.modelingvalue.collections.util.ContextPool;
import org.modelingvalue.collections.util.ContextThread;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.Rule;
import org.modelingvalue.nelumbo.Result;

@SuppressWarnings("rawtypes")
public final class KnowledgeBaseImpl implements KnowledgeBase {

    public static final Context<KnowledgeBaseImpl>                          CURRENT             = Context.of();

    private static final ContextPool                                        POOL                = ContextThread.createPool();
    @SuppressWarnings("rawtypes")
    private static final QualifiedSet<RelationImpl, Inference>              EMPTY_MEMOIZ        = QualifiedSet.of(Inference::premise);
    private static final int                                                MAX_LOGIC_MEMOIZ    = Integer.getInteger("MAX_LOGIC_MEMOIZ", 512);
    private static final int                                                MAX_LOGIC_MEMOIZ_D4 = KnowledgeBaseImpl.MAX_LOGIC_MEMOIZ / 4;
    private static final int                                                INITIAL_USAGE_COUNT = Integer.getInteger("INITIAL_USAGE_COUNT", 4);
    @SuppressWarnings("unchecked")
    private static final BiFunction<Set<RuleImpl>, RuleImpl, Set<RuleImpl>> ADD_RULE            = (l, e) -> {
                                                                                                    if (l == null) {
                                                                                                        return Set.of(e);
                                                                                                    } else {
                                                                                                        for (int i = 0; i < l.size(); i++) {
                                                                                                            RuleImpl r = l.get(i);
                                                                                                            if (r.consequence().equals(e.consequence()) &&   //
                                                                                                                    r.condition().contains(e.condition())) {
                                                                                                                return l;
                                                                                                            }
                                                                                                        }
                                                                                                        return l.add(e);
                                                                                                    }
                                                                                                };

    @SuppressWarnings("rawtypes")
    private static class Inference extends Struct2Impl<RelationImpl, InferResult> {
        private static final long serialVersionUID = 1531759272582548244L;

        public int                count            = INITIAL_USAGE_COUNT;

        public Inference(RelationImpl predicate, InferResult result) {
            super(predicate, result);
        }

        public RelationImpl premise() {
            return get0();
        }

        public InferResult result() {
            return get1();
        }

        protected boolean keep() {
            return count-- > 0;
        }
    }

    private static final class LogicTask extends ForkJoinTask<KnowledgeBaseImpl> {
        private static final long       serialVersionUID = -1375078574164947441L;

        private final Runnable          runnable;
        private final KnowledgeBaseImpl knowledgebase;

        public LogicTask(Runnable runnable, KnowledgeBaseImpl init) {
            this.runnable = runnable;
            this.knowledgebase = new KnowledgeBaseImpl(init);
        }

        @Override
        public KnowledgeBaseImpl getRawResult() {
            return knowledgebase;
        }

        @Override
        protected void setRawResult(KnowledgeBaseImpl knowledgebase) {
        }

        @Override
        protected boolean exec() {
            CURRENT.run(knowledgebase, runnable);
            knowledgebase.stopped = true;
            return true;
        }

    }

    public static final KnowledgeBaseImpl run(Runnable runnable, KnowledgeBaseImpl init) {
        return POOL.invoke(new LogicTask(runnable, init));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Set<Class> generalizations(Class type, Class top) {
        Set<Class> result = Set.of();
        for (java.lang.reflect.Type g : type.getGenericInterfaces()) {
            while (g instanceof ParameterizedType) {
                g = ((ParameterizedType) g).getRawType();
            }
            if (g instanceof Class && top.isAssignableFrom((Class) g)) {
                result = result.add((Class) g);
            }
        }
        return result;
    }

    private final AtomicReference<Map<RelationImpl, InferResult>>          facts;
    private final AtomicReference<Map<RelationImpl, Set<RuleImpl>>>        rules;
    private final AtomicInteger                                            depth;
    private final AtomicReference<QualifiedSet<RelationImpl, Inference>[]> memoization;
    private final InferContext                                             context = InferContext.of(KnowledgeBaseImpl.this, List.of(), Map.of(), false, StructureImpl.TRACE_NELUMBO);
    private boolean                                                        stopped;

    @SuppressWarnings("unchecked")
    public KnowledgeBaseImpl(KnowledgeBaseImpl init) {
        facts = new AtomicReference<>(init != null ? init.facts.get() : Map.of());
        rules = new AtomicReference<>(init != null ? init.rules.get() : Map.of());
        memoization = new AtomicReference<>(init != null ? init.memoization.get() : new QualifiedSet[]{EMPTY_MEMOIZ, EMPTY_MEMOIZ, EMPTY_MEMOIZ});
        depth = new AtomicInteger(0);
    }

    public InferResult getFacts(RelationImpl relation) {
        InferResult result = facts.get().get(relation);
        return result != null ? result.cast(relation) : relation.isFullyBound() ? relation.falsehoodCC() : InferResult.factsCI(Set.of());
    }

    public Set<RuleImpl> getRules(RelationImpl relation) {
        return doGetRules(relation.signature(depth()));
    }

    private Set<RuleImpl> doGetRules(RelationImpl signature) {
        Set<RuleImpl> result = rules.get().get(signature);
        if (result != null) {
            return result;
        }
        result = Set.of();
        Set<RelationImpl> post = signature.generalize(true);
        while (result.isEmpty() && !post.isEmpty()) {
            for (RelationImpl rel : post) {
                result = result.addAll(doGetRules(rel));
            }
            if (result.isEmpty()) {
                Set<RelationImpl> pre = post;
                post = Set.of();
                for (RelationImpl rel : pre) {
                    post = post.addAll(rel.generalize(true));
                }
            }
        }
        Set<RuleImpl> finalRsult = result;
        rules.updateAndGet(m -> m.put(signature, finalRsult));
        return result;
    }

    public InferResult getMemoiz(RelationImpl relation) {
        for (QualifiedSet<RelationImpl, Inference> m : memoization.get()) {
            Inference memoiz = m.get(relation);
            if (memoiz != null) {
                memoiz.count++;
                return memoiz.result().cast(relation);
            }
        }
        return null;
    }

    @Override
    public Map<Relation, Set<Rule>> rules() {
        return rules.get().replaceAll(e -> {
            Relation k = e.getKey().proxy();
            Set<Rule> v = e.getValue().replaceAll(RuleImpl::proxy);
            return Entry.of(k, v);
        });
    }

    @Override
    public Map<Relation, Result> facts() {
        return facts.get().replaceAll(e -> {
            Relation k = e.getKey().proxy();
            Result v = new Result(e.getValue());
            return Entry.of(k, v);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void memoization(RelationImpl relation, InferResult result) {
        boolean known = result.cycles().isEmpty() && result.isComplete();
        FunctorImpl<Relation, ?> functor = relation.functor();
        if (functor.factual()) {
            facts.updateAndGet(map -> {
                if (known) {
                    map = map.put(relation, result);
                }
                for (PredicateImpl fact : result.facts()) {
                    if (fact instanceof RelationImpl) {
                        map = map.put((RelationImpl) fact, fact.factCC());
                    }
                }
                for (PredicateImpl falsehood : result.falsehoods()) {
                    if (falsehood instanceof RelationImpl) {
                        map = map.put((RelationImpl) falsehood, falsehood.falsehoodCC());
                    }
                }
                return map;
            });
        } else if (!functor.derived()) {
            QualifiedSet<RelationImpl, Inference>[] mem = memoization.updateAndGet(array -> {
                array = array.clone();
                if (known) {
                    array[0] = array[0].put(new Inference(relation, result));
                }
                for (PredicateImpl fact : result.facts()) {
                    if (fact instanceof RelationImpl) {
                        array[0] = array[0].put(new Inference((RelationImpl) fact, fact.factCC()));
                    }
                }
                for (PredicateImpl falsehood : result.falsehoods()) {
                    if (falsehood instanceof RelationImpl) {
                        array[0] = array[0].put(new Inference((RelationImpl) falsehood, falsehood.falsehoodCC()));
                    }
                }
                if (array[0].size() >= MAX_LOGIC_MEMOIZ_D4) {
                    array[2] = array[2].putAll(array[1]);
                    array[1] = array[0];
                    array[0] = EMPTY_MEMOIZ;
                }
                return array;
            });
            if (mem[2].size() > MAX_LOGIC_MEMOIZ) {
                POOL.execute(this::cleanup);
            }
        }
    }

    private void cleanup() {
        QualifiedSet<RelationImpl, Inference>[] mem = memoization.get();
        while (mem[2].size() > MAX_LOGIC_MEMOIZ) {
            for (int i = 0; i < mem[2].size(); i++) {
                if (stopped) {
                    return;
                }
                Inference m = mem[2].get(i);
                if (!m.keep()) {
                    mem = memoization.updateAndGet(array -> {
                        array = array.clone();
                        array[2] = array[2].removeKey(m.premise());
                        return array;
                    });
                    i--;
                }
            }
        }
    }

    public void addRule(RuleImpl ruleImpl) {
        RelationImpl signature = ruleImpl.consequence().signature(Integer.MAX_VALUE);
        rules.updateAndGet(m -> addRule(ruleImpl, signature, m));
        int signDepth = signature.depth();
        depth.accumulateAndGet(signDepth, Math::max);
    }

    private static Map<RelationImpl, Set<RuleImpl>> addRule(RuleImpl ruleImpl, RelationImpl signature, Map<RelationImpl, Set<RuleImpl>> map) {
        map = map.put(signature, ADD_RULE.apply(map.get(signature), ruleImpl));
        for (RelationImpl gen : signature.generalize(false)) {
            map = addRule(ruleImpl, gen, map);
        }
        return map;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void addFact(RelationImpl fact) {
        FunctorImpl<Relation, ?> functor = fact.functor();
        if (functor.logicLambda() != null) {
            throw new IllegalArgumentException("No facts of a functor with a logic lambda allowed. " + this);
        }
        if (!getRules(fact.signature(depth())).isEmpty()) {
            throw new IllegalArgumentException("No facts of a functor with rules allowed. " + this);
        }
        List<Class> args = functor.args();
        facts.updateAndGet(map -> {
            map = map.put(fact, fact.factCC());
            for (int i = 1; i < fact.length(); i++) {
                map = addFact(map, fact, fact.setType(i, fact.getType(i)), i, args.get(i - 1));
            }
            return map;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<RelationImpl, InferResult> addFact(Map<RelationImpl, InferResult> map, RelationImpl fact, RelationImpl relation, int i, Class cls) {
        Class type = relation.getType(i);
        if (cls.isAssignableFrom(type)) {
            InferResult pre = map.get(relation);
            map = map.put(relation, InferResult.factsCI(pre != null ? pre.facts().add(fact) : fact.singleton()));
            if (!cls.equals(type)) {
                for (Class gen : generalizations(type, cls)) {
                    map = addFact(map, fact, relation.setType(i, (Class) gen), i, cls);
                }
            }
        }
        return map;
    }

    public InferContext context() {
        return context;
    }

    public int depth() {
        return depth.get();
    }
}
