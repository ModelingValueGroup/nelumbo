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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ForkJoinTask;
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
import org.modelingvalue.logic.KnowledgeBase;
import org.modelingvalue.logic.Logic.Predicate;
import org.modelingvalue.logic.Logic.Relation;
import org.modelingvalue.logic.Logic.Rule;
import org.modelingvalue.logic.Logic.Structure;

@SuppressWarnings("rawtypes")
public final class KnowledgeBaseImpl implements KnowledgeBase {

    public static final Context<KnowledgeBaseImpl>                            CURRENT             = Context.of();

    private static final ContextPool                                          POOL                = ContextThread.createPool();
    @SuppressWarnings("rawtypes")
    private static final AtomicReference<Map<Class, Set<Class>>>              SPECIALIZATIONS     = new AtomicReference<>(Map.of());
    @SuppressWarnings("rawtypes")
    private static final QualifiedSet<PredicateImpl, Inference>               EMPTY_MEMOIZ        = QualifiedSet.of(Inference::premise);
    private static final int                                                  MAX_LOGIC_MEMOIZ    = Integer.getInteger("MAX_LOGIC_MEMOIZ", 512);
    private static final int                                                  MAX_LOGIC_MEMOIZ_D4 = KnowledgeBaseImpl.MAX_LOGIC_MEMOIZ / 4;
    private static final int                                                  INITIAL_USAGE_COUNT = Integer.getInteger("INITIAL_USAGE_COUNT", 4);
    @SuppressWarnings("unchecked")
    private static final BiFunction<List<RuleImpl>, RuleImpl, List<RuleImpl>> ADD_RULE            = (l, e) -> {
                                                                                                      if (l == null) {
                                                                                                          return List.of(e);
                                                                                                      } else {
                                                                                                          int p = e.rulePrio();
                                                                                                          for (int i = 0; i < l.size(); i++) {
                                                                                                              RuleImpl r = l.get(i);
                                                                                                              if (r.equals(e)) {
                                                                                                                  return l;
                                                                                                              } else if (r.consequence().equals(e.consequence()) &&   //
                                                                                                                      r.condition().contains(e.condition())) {
                                                                                                                  return l;
                                                                                                              } else if (r.rulePrio() > p) {
                                                                                                                  return l.insert(i, e);
                                                                                                              }
                                                                                                          }
                                                                                                          return l.append(e);
                                                                                                      }
                                                                                                  };

    @SuppressWarnings("rawtypes")
    private static class Inference extends Struct2Impl<PredicateImpl, InferResult> {
        private static final long serialVersionUID = 1531759272582548244L;

        public int                count            = INITIAL_USAGE_COUNT;

        public Inference(PredicateImpl predicate, InferResult result) {
            super(predicate, result);
        }

        public PredicateImpl premise() {
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

    @SuppressWarnings("rawtypes")
    public static <F extends Structure> void updateSpecializations(Class type) {
        if (!SPECIALIZATIONS.get().containsKey(type)) {
            SPECIALIZATIONS.updateAndGet(m -> addToSpecializations(m, type));
        }
    }

    @SuppressWarnings("rawtypes")
    private static Map<Class, Set<Class>> addToSpecializations(Map<Class, Set<Class>> specs, Class type) {
        if (!specs.containsKey(type)) {
            specs = specs.put(type, Set.of());
            for (java.lang.reflect.Type g : type.getGenericInterfaces()) {
                while (g instanceof ParameterizedType) {
                    g = ((ParameterizedType) g).getRawType();
                }
                if (g instanceof Class && !g.equals(Structure.class)) {
                    specs = addToSpecializations(specs, (Class) g);
                    specs = specs.put((Class) g, specs.get((Class) g).add(type));
                }
            }
        }
        return specs;
    }

    private final AtomicReference<Map<PredicateImpl, InferResult>>          facts;
    private final AtomicReference<Map<PredicateImpl, List<RuleImpl>>>       rules;
    private final AtomicReference<QualifiedSet<PredicateImpl, Inference>[]> memoization;
    private final InferContext                                              context = InferContext.of(KnowledgeBaseImpl.this, List.of(), Map.of());
    private boolean                                                         stopped;

    @SuppressWarnings("unchecked")
    public KnowledgeBaseImpl(KnowledgeBaseImpl init) {
        facts = new AtomicReference<>(init != null ? init.facts.get() : Map.of());
        rules = new AtomicReference<>(init != null ? init.rules.get() : Map.of());
        memoization = new AtomicReference<>(init != null ? init.memoization.get() : new QualifiedSet[]{EMPTY_MEMOIZ, EMPTY_MEMOIZ, EMPTY_MEMOIZ});
    }

    public InferResult getFacts(PredicateImpl pred) {
        InferResult result = facts.get().get(pred);
        return result != null ? result : InferResult.trueFalse(Set.of(pred), Set.of(pred));
    }

    public List<RuleImpl> getRules(PredicateImpl pred) {
        return rules.get().get(pred.signature());
    }

    public InferResult getMemoiz(PredicateImpl pred) {
        for (QualifiedSet<PredicateImpl, Inference> m : memoization.get()) {
            Inference memoiz = m.get(pred);
            if (memoiz != null) {
                memoiz.count++;
                return memoiz.result();
            }
        }
        return null;
    }

    @Override
    public Map<Relation, List<Rule>> rules() {
        return rules.get().replaceAll(e -> {
            Relation k = (Relation) e.getKey().proxy();
            List<Rule> v = e.getValue().replaceAll(RuleImpl::proxy);
            return Entry.of(k, v);
        });
    }

    @Override
    public Map<Relation, Set<Relation>> facts() {
        return facts.get().replaceAll(e -> {
            Relation k = (Relation) e.getKey().proxy();
            Set<Relation> v = e.getValue().facts().replaceAll(p -> (Relation) p.proxy());
            return Entry.of(k, v);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void memoization(PredicateImpl predicate, InferResult result) {
        FunctorImpl<Predicate> functor = predicate.functor();
        if (functor.factual()) {
            facts.updateAndGet(map -> {
                map = map.put(predicate, result);
                for (PredicateImpl fact : result.facts()) {
                    if (fact.isFullyBound()) {
                        map = map.put(fact, InferResult.trueFalse(Set.of(fact), Set.of()));
                    }
                }
                for (PredicateImpl falsehood : result.falsehoods()) {
                    if (falsehood.isFullyBound()) {
                        map = map.put(falsehood, InferResult.trueFalse(Set.of(), Set.of(falsehood)));
                    }
                }
                return map;
            });
        } else if (!functor.derived()) {
            QualifiedSet<PredicateImpl, Inference>[] mem = memoization.updateAndGet(array -> {
                array = array.clone();
                array[0] = array[0].put(new Inference(predicate, result));
                for (PredicateImpl fact : result.facts()) {
                    if (fact.isFullyBound()) {
                        array[0] = array[0].put(new Inference(fact, InferResult.trueFalse(Set.of(fact), Set.of())));
                    }
                }
                for (PredicateImpl falsehood : result.falsehoods()) {
                    if (falsehood.isFullyBound()) {
                        array[0] = array[0].put(new Inference(falsehood, InferResult.trueFalse(Set.of(), Set.of(falsehood))));
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
        QualifiedSet<PredicateImpl, Inference>[] mem = memoization.get();
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
        Map<Class, Set<Class>> specs = SPECIALIZATIONS.get();
        PredicateImpl consequence = ruleImpl.consequence();
        rules.updateAndGet(m -> addRule(consequence.signature(), ruleImpl, m, specs));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<PredicateImpl, List<RuleImpl>> addRule(PredicateImpl signature, RuleImpl ruleImpl, Map<PredicateImpl, List<RuleImpl>> rules, Map<Class, Set<Class>> specs) {
        rules = rules.put(signature, ADD_RULE.apply(rules.get(signature), ruleImpl));
        for (int i = 1; i < signature.length(); i++) {
            Object v = signature.get(i);
            if (v instanceof Class) {
                for (Class g : specs.get((Class) v)) {
                    PredicateImpl p = signature.set(i, g);
                    rules = addRule(p, ruleImpl, rules, specs);
                }
            }
        }
        return rules;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void addFact(PredicateImpl fact) {
        FunctorImpl<Predicate> functor = fact.functor();
        if (functor.logic() != null) {
            throw new IllegalArgumentException("No facts of a functor with a logic lambda allowed. " + this);
        }
        if (getRules(fact.signature()) != null) {
            throw new IllegalArgumentException("No facts of a functor with rules allowed. " + this);
        }
        facts.updateAndGet(map -> {
            List<Class> args = functor.args();
            map = map.put(fact, InferResult.trueFalse(Set.of(fact), Set.of()));
            for (int i = 1; i < fact.length(); i++) {
                map = addFact(map, fact, fact.set(i, fact.getType(i)), i, args.get(i - 1));
            }
            return map;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<PredicateImpl, InferResult> addFact(Map<PredicateImpl, InferResult> map, PredicateImpl fact, PredicateImpl predicate, int i, Class cls) {
        Class type = predicate.getType(i);
        if (cls.isAssignableFrom(type)) {
            InferResult pre = map.get(predicate);
            map = map.put(predicate, InferResult.trueFalse(pre != null ? pre.facts().add(fact) : Set.of(fact), Set.of(predicate)));
            if (!cls.equals(type)) {
                for (Type gen : type.getGenericInterfaces()) {
                    while (gen instanceof ParameterizedType) {
                        gen = ((ParameterizedType) gen).getRawType();
                    }
                    if (gen instanceof Class) {
                        map = addFact(map, fact, predicate.set(i, gen), i, cls);
                    }
                }
            }
        }
        return map;
    }

    public InferContext context() {
        return context;
    }
}
