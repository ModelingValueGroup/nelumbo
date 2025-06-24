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

import java.text.ParseException;
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
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.syntax.*;

@SuppressWarnings("rawtypes")
public final class KnowledgeBase {

    protected static final boolean                              TRACE_NELUMBO       = java.lang.Boolean.getBoolean("TRACE_NELUMBO");

    public static final Context<KnowledgeBase>                  CURRENT             = Context.of();

    private static final ContextPool                            POOL                = ContextThread.createPool();
    @SuppressWarnings("rawtypes")
    private static final QualifiedSet<Relation, Inference>      EMPTY_MEMOIZ        = QualifiedSet.of(Inference::premise);
    private static final int                                    MAX_LOGIC_MEMOIZ    = Integer.getInteger("MAX_LOGIC_MEMOIZ", 512);
    private static final int                                    MAX_LOGIC_MEMOIZ_D4 = KnowledgeBase.MAX_LOGIC_MEMOIZ / 4;
    private static final int                                    INITIAL_USAGE_COUNT = Integer.getInteger("INITIAL_USAGE_COUNT", 4);
    @SuppressWarnings("unchecked")
    private static final BiFunction<Set<Rule>, Rule, Set<Rule>> ADD_RULE            = (l, e) -> {
                                                                                        if (l == null) {
                                                                                            return Set.of(e);
                                                                                        } else {
                                                                                            for (int i = 0; i < l.size(); i++) {
                                                                                                Rule r = l.get(i);
                                                                                                if (r.consequence().equals(e.consequence()) &&   //
                                                                                                        r.condition().contains(e.condition())) {
                                                                                                    return l;
                                                                                                }
                                                                                            }
                                                                                            return l.add(e);
                                                                                        }
                                                                                    };

    public static final KnowledgeBase                           BASE                = new KnowledgeBase(null).initBase();

    @SuppressWarnings("rawtypes")
    private static class Inference extends Struct2Impl<Relation, InferResult> {
        private static final long serialVersionUID = 1531759272582548244L;

        public int                count            = INITIAL_USAGE_COUNT;

        public Inference(Relation predicate, InferResult result) {
            super(predicate, result);
        }

        public Relation premise() {
            return get0();
        }

        public InferResult result() {
            return get1();
        }

        protected boolean keep() {
            return count-- > 0;
        }
    }

    private static final class LogicTask extends ForkJoinTask<KnowledgeBase> {
        private static final long   serialVersionUID = -1375078574164947441L;

        private final Runnable      runnable;
        private final KnowledgeBase knowledgebase;

        public LogicTask(Runnable runnable, KnowledgeBase init) {
            this.runnable = runnable;
            this.knowledgebase = new KnowledgeBase(init);
        }

        @Override
        public KnowledgeBase getRawResult() {
            return knowledgebase;
        }

        @Override
        protected void setRawResult(KnowledgeBase knowledgebase) {
        }

        @Override
        protected boolean exec() {
            CURRENT.run(knowledgebase, runnable);
            knowledgebase.stopped = true;
            return true;
        }

    }

    public static final KnowledgeBase run(Runnable runnable) {
        return run(runnable, BASE);
    }

    public static final KnowledgeBase run(Runnable runnable, KnowledgeBase init) {
        return POOL.invoke(new LogicTask(runnable, init));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Set<Type> generalizations(Type type, Type top) {
        Set<Type> result = Set.of();
        for (Type g : type.supers()) {
            if (top.isAssignableFrom(g)) {
                result = result.add(g);
            }
        }
        return result;
    }

    private static Type type(String name) {
        return KnowledgeBase.CURRENT.get().getType(name);
    }

    private static Variable var(String name) {
        return KnowledgeBase.CURRENT.get().getVar(name);
    }

    private static Node createNode(Functor functor, Object[] values) {
        return Relation.TYPE.isAssignableFrom(functor.resultType()) ? new Relation(functor, values) : new Node(functor, values);
    }

    public void register(TokenType token, PrefixParselet parselet) {
        if (prefix1Parselets.get().containsKey(token)) {
            throw new IllegalArgumentException();
        }
        prefix1Parselets.updateAndGet(map -> map.put(token, parselet));
    }

    public void register(TokenType token, String next, PrefixParselet parselet) {
        Pair<TokenType, String> pair = Pair.of(token, next);
        if (prefix2Parselets.get().containsKey(pair)) {
            throw new IllegalArgumentException();
        }
        prefix2Parselets.updateAndGet(map -> map.put(pair, parselet));
    }

    public void register(Type desired, TokenType token, PrefixParselet parselet) {
        Pair<Type, TokenType> pair = Pair.of(desired, token);
        if (prefix3Parselets.get().containsKey(pair)) {
            throw new IllegalArgumentException();
        }
        prefix3Parselets.updateAndGet(map -> map.put(pair, parselet));
    }

    public void register(TokenType token, InfixParselet parselet) {
        if (infixParselets.get().containsKey(token)) {
            throw new IllegalArgumentException();
        }
        infixParselets.updateAndGet(map -> map.put(token, parselet));
    }

    public PrefixParselet prefix(Type desired, TokenType token) {
        Pair<Type, TokenType> pair = Pair.of(desired, token);
        return prefix3Parselets.get().get(pair);
    }

    public PrefixParselet prefix(TokenType token, String next) {
        Pair<TokenType, String> pair = Pair.of(token, next);
        return prefix2Parselets.get().get(pair);
    }

    public PrefixParselet prefix(TokenType token) {
        return prefix1Parselets.get().get(token);
    }

    public InfixParselet infix(TokenType token) {
        return infixParselets.get().get(token);
    }

    public void register(UnaryOperator operator) {
        if (unaryOperators.get().containsKey(operator.oper())) {
            throw new IllegalArgumentException();
        }
        unaryOperators.updateAndGet(map -> map.put(operator.oper(), operator));
    }

    public UnaryOperator unaryOperator(String oper) {
        return unaryOperators.get().get(oper);
    }

    public void register(BinaryOperator operator) {
        Pair<Type, String> pair = Pair.of(operator.left(), operator.oper());
        if (binaryOperators.get().containsKey(pair)) {
            throw new IllegalArgumentException();
        }
        binaryOperators.updateAndGet(map -> map.put(pair, operator));
    }

    public BinaryOperator binaryOperator(Type left, String oper) {
        Pair<Type, String> pair = Pair.of(left, oper);
        return binaryOperators.get().get(pair);
    }

    public void register(CallWithArgs call) {
        callsWithArgs.updateAndGet(map -> map.compute(call.name(), (k, v) -> {
            if (v == null) {
                return List.of(call);
            } else {
                for (int i = 0; i < v.size(); i++) {
                    if (v.get(i).isAssignableFrom(call)) {
                        return v.insert(i, call);
                    }
                }
                return v.append(call);
            }
        }));
    }

    public List<CallWithArgs> callsWithArgs(String name) {
        return callsWithArgs.get().get(name);
    }

    @SuppressWarnings("unchecked")
    private KnowledgeBase initBase() {
        CURRENT.run(this, () -> {
            Type RELATION = Relation.TYPE;

            Type TYPE_NAME = new Type("TypeName");
            Type VAR_NAME = new Type("VarName");
            Type SIGNATURE = new Type("Signature");
            Type FUNCTOR_SET = new Type("FunctorSet");
            Type VAR_SET = new Type("VarSet");

            Type FUNCTION = new Type("Function");
            Type LITERAL = new Type("Literal");

            register(TokenType.OPERATOR, UnaryOperatorParselet.INSTANCE);
            register(TokenType.OPERATOR, BinaryOperatorParselet.INSTANCE);
            // register(TokenType.NAME, UnaryOperatorParselet.INSTANCE);
            register(TokenType.NAME, BinaryOperatorParselet.INSTANCE);
            register(TokenType.NAME, "(", CallWithArgsParselet.INSTANCE);
            register(TokenType.LPAREN, ParenParselet.INSTANCE);
            register(TokenType.TYPE, "::", AtomicParselet.of(t -> {
                String name = t.text();
                name = name.substring(1, name.length() - 1);
                return new Terminal(TYPE_NAME, name);
            }));
            register(TokenType.TYPE, AtomicParselet.of(t -> {
                String name = t.text();
                name = name.substring(1, name.length() - 1);
                Type type = type(name);
                if (type != null) {
                    return type;
                }
                throw new ParseException("Could not find type " + t.text() + " at position " + t.position() + ".", t.position());
            }));
            register(TokenType.NAME, AtomicParselet.of(t -> {
                String name = t.text();
                Variable var = var(name);
                if (var != null) {
                    return var;
                }
                throw new ParseException("Could not find variable " + t.text() + " at position " + t.position() + ".", t.position());
            }));
            register(BinaryOperator.of(TYPE_NAME, "::", Type.TYPE().list(), 10, (t, l, r) -> {
                return new Type((String) l.get(1), ((ListNode) r).elements());
            }));
            register(CallWithArgs.of((t, l) -> {
                return new Node(SIGNATURE, t.text(), l);
            }, Type.TYPE().list()));
            register(BinaryOperator.of(Type.TYPE(), "::=", SIGNATURE.list(), 10, (t, l, r) -> {
                Set<Functor> set = Set.of();
                KnowledgeBase current = KnowledgeBase.CURRENT.get();
                for (Node s : ((ListNode) r).elements()) {
                    Functor functor = new Functor((Type) l, (String) s.get(1), (List<Type>) s.get(2));
                    current.register(CallWithArgs.of(functor.name(), (tt, ll) -> createNode(functor, ll.toArray()), //
                            functor.args().toArray(i -> new Type[i])));
                    set = set.add(functor);
                }
                return new Node(FUNCTOR_SET, set);
            }));
            register(VAR_NAME, TokenType.NAME, AtomicParselet.of(t -> {
                String name = t.text();
                return new Terminal(VAR_NAME, name);
            }));
            register(BinaryOperator.of(Type.TYPE(), ":", VAR_NAME.list(), 10, (t, l, r) -> {
                Set<Variable> set = Set.of();
                for (Node v : ((ListNode) r).elements()) {
                    set = set.add(new Variable((Type) l, (String) v.get(1)));
                }
                return new Node(VAR_SET, set);
            }));
            register(BinaryOperator.of(RELATION, "<==", Predicate.TYPE, 10, (t, l, r) -> {
                return new Rule((Relation) l, (Predicate) r);
            }));
            register(UnaryOperator.of("!", Predicate.TYPE, 10, (t, r) -> {
                return new Not((Predicate) r);
            }));
            register(BinaryOperator.of(Predicate.TYPE, "&", Predicate.TYPE, 20, (t, l, r) -> {
                return new And((Predicate) l, (Predicate) r);
            }));
            register(BinaryOperator.of(Predicate.TYPE, "|", Predicate.TYPE, 20, (t, l, r) -> {
                return new Or((Predicate) l, (Predicate) r);
            }));
            register(BinaryOperator.of(Node.TYPE, "=", Node.TYPE, 30, (t, l, r) -> {
                return new Equal((Node) l, (Node) r);
            }));
        });
        return this;
    }

    private final AtomicReference<Map<String, Type>>                            types;
    private final AtomicReference<Set<Functor>>                                 functors;
    private final AtomicReference<Map<String, Variable>>                        variables;
    private final AtomicReference<Map<Relation, InferResult>>                   facts;
    private final AtomicReference<Map<Relation, Set<Rule>>>                     rules;

    private final AtomicReference<Map<Pair<Type, TokenType>, PrefixParselet>>   prefix3Parselets;
    private final AtomicReference<Map<Pair<TokenType, String>, PrefixParselet>> prefix2Parselets;
    private final AtomicReference<Map<TokenType, PrefixParselet>>               prefix1Parselets;
    private final AtomicReference<Map<TokenType, InfixParselet>>                infixParselets;

    private final AtomicReference<Map<String, UnaryOperator>>                   unaryOperators;
    private final AtomicReference<Map<Pair<Type, String>, BinaryOperator>>      binaryOperators;
    private final AtomicReference<Map<String, List<CallWithArgs>>>              callsWithArgs;

    private final AtomicInteger                                                 depth;
    private final AtomicReference<QualifiedSet<Relation, Inference>[]>          memoization;
    private final InferContext                                                  context = InferContext.of(KnowledgeBase.this, List.of(), Map.of(), false, TRACE_NELUMBO);
    private boolean                                                             stopped;

    @SuppressWarnings("unchecked")
    public KnowledgeBase(KnowledgeBase init) {
        types = new AtomicReference<>(init != null ? init.types.get() : Map.of());
        functors = new AtomicReference<>(init != null ? init.functors.get() : Set.of());
        variables = new AtomicReference<>(Map.of());
        facts = new AtomicReference<>(init != null ? init.facts.get() : Map.of());
        rules = new AtomicReference<>(init != null ? init.rules.get() : Map.of());

        prefix3Parselets = new AtomicReference<>(init != null ? init.prefix3Parselets.get() : Map.of());
        prefix2Parselets = new AtomicReference<>(init != null ? init.prefix2Parselets.get() : Map.of());
        prefix1Parselets = new AtomicReference<>(init != null ? init.prefix1Parselets.get() : Map.of());
        infixParselets = new AtomicReference<>(init != null ? init.infixParselets.get() : Map.of());

        unaryOperators = new AtomicReference<>(init != null ? init.unaryOperators.get() : Map.of());
        binaryOperators = new AtomicReference<>(init != null ? init.binaryOperators.get() : Map.of());
        callsWithArgs = new AtomicReference<>(init != null ? init.callsWithArgs.get() : Map.of());

        memoization = new AtomicReference<>(init != null ? init.memoization.get() : new QualifiedSet[]{EMPTY_MEMOIZ, EMPTY_MEMOIZ, EMPTY_MEMOIZ});
        depth = new AtomicInteger(0);
    }

    public InferResult getFacts(Relation relation) {
        InferResult result = facts.get().get(relation);
        return result != null ? result.cast(relation) : relation.isFullyBound() ? relation.falsehoodCC() : InferResult.factsCI(Set.of());
    }

    public Set<Rule> getRules(Relation relation) {
        return doGetRules(relation.signature(depth()));
    }

    private Set<Rule> doGetRules(Relation signature) {
        Set<Rule> result = rules.get().get(signature);
        if (result != null) {
            return result;
        }
        result = Set.of();
        Set<Relation> post = signature.generalize(true);
        while (result.isEmpty() && !post.isEmpty()) {
            for (Relation rel : post) {
                result = result.addAll(doGetRules(rel));
            }
            if (result.isEmpty()) {
                Set<Relation> pre = post;
                post = Set.of();
                for (Relation rel : pre) {
                    post = post.addAll(rel.generalize(true));
                }
            }
        }
        Set<Rule> finalRsult = result;
        rules.updateAndGet(m -> m.put(signature, finalRsult));
        return result;
    }

    public InferResult getMemoiz(Relation relation) {
        for (QualifiedSet<Relation, Inference> m : memoization.get()) {
            Inference memoiz = m.get(relation);
            if (memoiz != null) {
                memoiz.count++;
                return memoiz.result().cast(relation);
            }
        }
        return null;
    }

    public Map<String, Variable> variables() {
        return variables.get();
    }

    public Set<Functor> functors() {
        return functors.get();
    }

    public Map<String, Type> types() {
        return types.get();
    }

    public Map<Relation, Set<Rule>> rules() {
        return rules.get();
    }

    public Map<Relation, InferResult> facts() {
        return facts.get();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void memoization(Relation relation, InferResult result) {
        boolean known = result.cycles().isEmpty() && result.isComplete();
        QualifiedSet<Relation, Inference>[] mem = memoization.updateAndGet(array -> {
            array = array.clone();
            if (known) {
                array[0] = array[0].put(new Inference(relation, result));
            }
            for (Predicate fact : result.facts()) {
                if (fact instanceof Relation) {
                    array[0] = array[0].put(new Inference((Relation) fact, fact.factCC()));
                }
            }
            for (Predicate falsehood : result.falsehoods()) {
                if (falsehood instanceof Relation) {
                    array[0] = array[0].put(new Inference((Relation) falsehood, falsehood.falsehoodCC()));
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

    private void cleanup() {
        QualifiedSet<Relation, Inference>[] mem = memoization.get();
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

    public void addRule(Rule ruleImpl) {
        Relation signature = ruleImpl.consequence().signature(Integer.MAX_VALUE);
        rules.updateAndGet(m -> addRule(ruleImpl, signature, m));
        int signDepth = signature.depth();
        depth.accumulateAndGet(signDepth, Math::max);
    }

    private static Map<Relation, Set<Rule>> addRule(Rule ruleImpl, Relation signature, Map<Relation, Set<Rule>> map) {
        map = map.put(signature, ADD_RULE.apply(map.get(signature), ruleImpl));
        for (Relation gen : signature.generalize(false)) {
            map = addRule(ruleImpl, gen, map);
        }
        return map;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void addFact(Relation fact) {
        Functor functor = fact.functor();
        List<Type> args = functor.args();
        facts.updateAndGet(map -> {
            map = map.put(fact, fact.factCC());
            for (int i = 1; i < fact.length(); i++) {
                map = addFact(map, fact, fact.setType(i, fact.getType(i)), i, args.get(i - 1));
            }
            return map;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<Relation, InferResult> addFact(Map<Relation, InferResult> map, Relation fact, Relation relation, int i, Type cls) {
        Type type = relation.getType(i);
        if (cls.isAssignableFrom(type)) {
            InferResult pre = map.get(relation);
            map = map.put(relation, InferResult.factsCI(pre != null ? pre.facts().add(fact) : fact.singleton()));
            if (!cls.equals(type)) {
                for (Type gen : generalizations(type, cls)) {
                    map = addFact(map, fact, relation.setType(i, gen), i, cls);
                }
            }
        }
        return map;
    }

    public final void addType(Type type) {
        types.updateAndGet(map -> map.put(type.name(), type));
    }

    public final Type getType(String name) {
        return types.get().get(name);
    }

    public final void addVar(Variable var) {
        variables.updateAndGet(map -> map.put(var.name(), var));
    }

    public final Variable getVar(String name) {
        return variables.get().get(name);
    }

    public final void addFunctor(Functor functor) {
        functors.updateAndGet(set -> set.add(functor));
    }

    public InferContext context() {
        return context;
    }

    public int depth() {
        return depth.get();
    }

    public void print() {
        for (Entry<String, Type> e : types()) {
            Type type = e.getValue();
            String supers = type.supers().toString();
            System.err.println(type + " :: " + supers.substring(4, supers.length() - 1));
        }
        for (Functor e : functors()) {
            System.err.println(e.resultType() + " ::= " + e);
        }
        for (Entry<String, Variable> e : variables()) {
            Variable var = e.getValue();
            System.err.println(var.type() + " : " + var.name());
        }
        for (Entry<Relation, InferResult> e : facts()) {
            System.err.println(e.getKey() + " " + e.getValue());
        }
        for (Entry<Relation, Set<Rule>> e : rules()) {
            Set<Rule> rules = e.getValue();
            String type = e.getKey().toString() + " : ";
            for (int i = 0; i < rules.size(); i++) {
                if (i == 1) {
                    type = " ".repeat(type.length());
                }
                System.err.println(type + rules.get(i) + (i < rules.size() - 1 ? "," : ""));
            }
        }
    }

}
