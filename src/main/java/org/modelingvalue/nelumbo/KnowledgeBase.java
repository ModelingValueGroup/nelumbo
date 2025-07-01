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

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import org.modelingvalue.collections.util.Quadruple;
import org.modelingvalue.collections.util.Triple;
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

    private static Node constant(String name) {
        return KnowledgeBase.CURRENT.get().getConstant(name);
    }

    private static Node createNode(Token token, Constructor<? extends Node> constructor, Functor functor, Object... args) throws ParseException {
        if (constructor != null) {
            try {
                return constructor.newInstance(functor, args);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ParseException(e.getClass().getSimpleName() + ": " + e.getMessage(), token);
            }
        }
        return Relation.TYPE.isAssignableFrom(functor.resultType()) ? new Relation(functor, args) : new Node(functor, args);
    }

    @SuppressWarnings("unchecked")
    private KnowledgeBase initBase() {
        CURRENT.run(this, () -> {
            for (TokenType type : TokenType.values()) {
                new Type(type);
            }

            Type RELATION = Relation.TYPE;

            Type TYPE_NAME = new Type("TypeName", Node.TYPE);
            Type VAR_NAME = new Type("VarName", Node.TYPE);
            Type SIGNATURE = new Type("Signature", Node.TYPE);
            Type RESULT = new Type("Result", Node.ROOT);
            Type NATIVE = new Type("Native", Node.TYPE);
            Type PRECEDENCE = new Type("Precedence", Node.TYPE);

            register(ParenParselet.INSTANCE);

            register(AtomicParselet.of(TokenType.TYPE, t -> {
                return type(t);
            }));
            register(AtomicParselet.of(TokenType.NAME, t -> {
                return node(t);
            }));

            // Types
            register(AtomicParselet.of(Node.ROOT, TokenType.TYPE, "::", t -> {
                String name = t.text();
                name = name.substring(1, name.length() - 1);
                return new Terminal(TYPE_NAME, name);
            }));
            register(InfixParselet.of(Node.ROOT, TYPE_NAME, "::", Type.TYPE().list(), 10, (l, t, r) -> {
                String name = l.getVal(1);
                return new Type(name, ((ListNode) r).elements());
            }));

            // Functors
            register(InfixParselet.of(Node.ROOT, Type.TYPE(), "::=", SIGNATURE.list(), 10, (l, t, r) -> {
                ListNode list = new ListNode(Functor.TYPE);
                for (Node s : ((ListNode) r).elements()) {
                    list = new ListNode(list, createFunctor((Type) l, t, s));
                }
                return list;
            }));
            register(CallWithArgs.of(SIGNATURE, TokenType.NAME, (t, l) -> {
                return new Node(SIGNATURE, t.text(), l);
            }, Type.TYPE().list()));
            register(AtomicParselet.of(SIGNATURE, TokenType.NAME, t -> {
                return new Node(SIGNATURE, t.text());
            }));
            register(AtomicParselet.of(SIGNATURE, TokenType.TYPE, t -> {
                Type type = type(t);
                return type.tokenType() != null ? new Node(SIGNATURE, type) : type;
            }));
            register(PrefixParselet.of(SIGNATURE, TokenType.OPERATOR, TokenType.TYPE, Type.TYPE(), 50, (t, r) -> {
                return new Node(SIGNATURE, t.text(), r);
            }));
            register(PrefixParselet.of(SIGNATURE, TokenType.NAME, TokenType.TYPE, Type.TYPE(), 50, (t, r) -> {
                return new Node(SIGNATURE, t.text(), r);
            }));
            register(PostfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.OPERATOR, 50, (l, t) -> {
                return new Node(SIGNATURE, l, t.text());
            }));
            register(PostfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.NAME, 50, (l, t) -> {
                return new Node(SIGNATURE, l, t.text());
            }));
            register(InfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.OPERATOR, TokenType.TYPE, Type.TYPE(), 50, (l, t, r) -> {
                return new Node(SIGNATURE, l, t.text(), r);
            }));
            register(InfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.NAME, TokenType.TYPE, Type.TYPE(), 50, (l, t, r) -> {
                return new Node(SIGNATURE, l, t.text(), r);
            }));
            register(InfixParselet.of(SIGNATURE, "#", TokenType.NUMBER, PRECEDENCE, 50, (l, t, r) -> {
                return new Node(SIGNATURE, l, r.get(1));
            }));
            register(AtomicParselet.of(PRECEDENCE, TokenType.NUMBER, t -> {
                return new Node(PRECEDENCE, Integer.parseInt(t.text()));
            }));
            register(InfixParselet.of(SIGNATURE, "@", TokenType.QNAME, NATIVE, 50, (l, t, r) -> {
                return new Node(SIGNATURE, l, r.get(1));
            }));
            register(AtomicParselet.of(NATIVE, TokenType.QNAME, t -> {
                try {
                    return new Node(NATIVE, Class.forName(t.text()).getConstructor(Functor.class, Object[].class));
                } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
                    throw new ParseException(e.getClass().getSimpleName() + ": " + e.getMessage(), t);
                }
            }));

            // Variables
            register(PrefixParselet.of(Node.ROOT, TokenType.TYPE, TokenType.NAME, VAR_NAME.list(), 10, (t, l) -> {
                ListNode list = new ListNode(Variable.TYPE);
                Type type = type(t);
                for (Node v : ((ListNode) l).elements()) {
                    list = new ListNode(list, new Variable(type, (String) v.get(1)));
                }
                return list;
            }));
            register(AtomicParselet.of(VAR_NAME, TokenType.NAME, t -> {
                String name = t.text();
                return new Terminal(VAR_NAME, name);
            }));

            // Rules
            register(InfixParselet.of(Node.ROOT, RELATION, "<==", Predicate.TYPE.list(), 10, (l, t, r) -> {
                ListNode list = new ListNode(Rule.TYPE);
                for (Node s : ((ListNode) r).elements()) {
                    list = new ListNode(list, new Rule((Relation) l, (Predicate) s));
                }
                return list;
            }));

            // Queries
            register(PrefixParselet.of(Node.ROOT, "?", Predicate.TYPE, 10, (t, r) -> {
                InferResult result = ((Predicate) r).infer();
                System.err.println(r + " " + result);
                return new Node(RESULT, r, result);
            }));

            try {
                Parser.parseLogic(KnowledgeBase.class);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        });
        return this;
    }

    private static Type type(Token t) throws ParseException {
        String name = t.text();
        name = name.substring(1, name.length() - 1);
        boolean many = false;
        if (name.endsWith("*") || name.endsWith("+")) {
            many = true;
            name = name.substring(0, name.length() - 1);
        }
        Type type = type(name);
        if (type != null) {
            return many ? type.list() : type;
        }
        throw new ParseException("Could not find type " + t.text(), t);
    }

    private static Node node(Token t) throws ParseException {
        String name = t.text();
        Node node = constant(name);
        if (node != null) {
            return node;
        }
        node = var(name);
        if (node != null) {
            return node;
        }
        throw new ParseException("Could not find variable nor constant " + name, t);
    }

    @SuppressWarnings("unchecked")
    private Node createFunctor(Type type, Token token, Node sig) throws ParseException {
        KnowledgeBase current = KnowledgeBase.CURRENT.get();
        Constructor<? extends Node> constructor = sig.length() == 3 && sig.get(1) instanceof Node && sig.get(2) instanceof Constructor ? //
                (Constructor<? extends Node>) sig.get(2) : null;
        if (constructor != null) {
            sig = (Node) sig.get(1);
        }
        Integer precedence = sig.length() == 3 && sig.get(1) instanceof Node && sig.get(2) instanceof Integer ? //
                (Integer) sig.get(2) : null;
        if (precedence != null) {
            sig = (Node) sig.get(1);
        }
        if (sig.length() == 2 && sig.get(1) instanceof Type && ((Type) sig.get(1)).tokenType() != null) {
            if (precedence != null) {
                throw new ParseException("Precedence should not be defined " + sig, token);
            }
            TokenType tokenType = ((Type) sig.get(1)).tokenType();
            Functor functor = new Functor(type, constructor.getDeclaringClass().getSimpleName(), new Type(String.class));
            current.register(AtomicParselet.of(tokenType, (tt) -> createNode(token, constructor, functor, tt.text())));
            return functor;
        } else if (sig.length() == 2 && sig.get(1) instanceof String) {
            // Constant
            if (precedence != null) {
                throw new ParseException("Precedence should not be defined " + sig, token);
            }
            String name = (String) sig.get(1);
            if (constructor != null) {
                Functor functor = new Functor(type, constructor.getDeclaringClass().getSimpleName(), new Type(String.class));
                current.addConstant(name, createNode(token, constructor, functor, name));
            }
            return new Constant(type, name);
        } else if (sig.length() == 3 && sig.get(1) instanceof String && sig.get(2) instanceof List) {
            // CallWithArgs
            if (precedence != null) {
                throw new ParseException("Precedence should not be defined " + sig, token);
            }
            String name = (String) sig.get(1);
            Functor functor = new Functor(type, name, (List<Type>) sig.get(2));
            current.register(CallWithArgs.of(name, (tt, ll) -> createNode(token, constructor, functor, ll.toArray()), //
                    functor.args().toArray(i -> new Type[i])));
            return functor;
        } else if (sig.length() == 3 && sig.get(1) instanceof Type && sig.get(2) instanceof String) {
            // PostfixOperator
            if (precedence == null) {
                throw new ParseException("No precedence defined " + sig, token);
            }
            String oper = (String) sig.get(2);
            Functor functor = new Functor(type, oper, n -> n.toString(1) + oper, precedence, (Type) sig.get(1));
            current.register(PostfixParselet.of((Type) sig.get(1), oper, precedence, (ll, tt) -> createNode(token, constructor, functor, ll)));
            return functor;
        } else if (sig.length() == 3 && sig.get(1) instanceof String && sig.get(2) instanceof Type) {
            // PrefixOperator
            if (precedence == null) {
                throw new ParseException("No precedence defined " + sig, token);
            }
            String oper = (String) sig.get(1);
            Functor functor = new Functor(type, oper, n -> oper + n.toString(1), precedence, (Type) sig.get(2));
            current.register(PrefixParselet.of(oper, (Type) sig.get(2), precedence, (tt, rr) -> createNode(token, constructor, functor, rr)));
            return functor;
        } else if (sig.length() == 4 && sig.get(1) instanceof Type && sig.get(2) instanceof String && sig.get(3) instanceof Type) {
            // InfixOperator
            if (precedence == null) {
                throw new ParseException("No precedence defined " + sig, token);
            }
            String oper = (String) sig.get(2);
            Functor functor = new Functor(type, oper, n -> n.toString(1) + oper + n.toString(2), precedence, (Type) sig.get(1), (Type) sig.get(3));
            current.register(InfixParselet.of((Type) sig.get(1), oper, (Type) sig.get(3), precedence, (ll, tt, rr) -> createNode(token, constructor, functor, ll, rr)));
            return functor;
        } else {
            throw new ParseException("Invalid signature " + sig, token);
        }
    }

    private final AtomicReference<Map<String, Type>>                   types;
    private final AtomicReference<Set<Functor>>                        functors;
    private final AtomicReference<Map<String, Node>>                   constants;
    private final AtomicReference<Map<String, Variable>>               variables;
    private final AtomicReference<Map<Relation, InferResult>>          facts;
    private final AtomicReference<Map<Relation, Set<Rule>>>            rules;

    private final AtomicReference<Map<Object, AtomicParselet>>         prefixParselets;
    private final AtomicReference<Map<Object, PostfixParselet>>        postfixParselets;
    private final AtomicReference<Map<Object, List<CallWithArgs>>>     callsWithArgs;

    private final AtomicReference<Set<String>>                         allOperators;

    private final AtomicInteger                                        depth;
    private final AtomicReference<QualifiedSet<Relation, Inference>[]> memoization;
    private final InferContext                                         context;
    private boolean                                                    stopped;

    @SuppressWarnings("unchecked")
    public KnowledgeBase(KnowledgeBase init) {
        types = new AtomicReference<>(init != null ? init.types.get() : Map.of());
        functors = new AtomicReference<>(init != null ? init.functors.get() : Set.of());
        variables = new AtomicReference<>(Map.of());
        constants = new AtomicReference<>(init != null ? init.constants.get() : Map.of());
        facts = new AtomicReference<>(init != null ? init.facts.get() : Map.of());
        rules = new AtomicReference<>(init != null ? init.rules.get() : Map.of());

        prefixParselets = new AtomicReference<>(init != null ? init.prefixParselets.get() : Map.of());
        postfixParselets = new AtomicReference<>(init != null ? init.postfixParselets.get() : Map.of());
        callsWithArgs = new AtomicReference<>(init != null ? init.callsWithArgs.get() : Map.of());

        allOperators = new AtomicReference<>(init != null ? init.allOperators.get() : Set.of());

        context = InferContext.of(KnowledgeBase.this, List.of(), Map.of(), false, TRACE_NELUMBO);
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

    public Map<String, Node> constants() {
        return constants.get();
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

    public final void addConstant(String name, Node constant) {
        constants.updateAndGet(map -> map.put(name, constant));
    }

    public final Node getConstant(String name) {
        return constants.get().get(name);
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

    public void print(PrintStream stream) {
        for (Entry<String, Type> e : types()) {
            Type type = e.getValue();
            String supers = type.supers().toString();
            stream.println(type + " :: " + supers.substring(4, supers.length() - 1));
        }
        for (Functor e : functors()) {
            stream.println(e.resultType() + " ::= " + e);
        }
        for (Entry<String, Node> e : constants()) {
            Node con = e.getValue();
            stream.println(con.type() + " ::= " + e.getValue());
        }
        for (Entry<String, Variable> e : variables()) {
            Variable var = e.getValue();
            stream.println(var.type() + " " + var.name());
        }
        Set<Rule> rules = rules().flatMap(Entry::getValue).asSet();
        for (Rule r : rules) {
            stream.println(r);
        }
        for (Entry<Relation, InferResult> e : facts()) {
            if (e.getValue().isTrueCC()) {
                stream.println(e.getKey());
            }
        }
    }

    public void register(AtomicParselet parselet) {
        Type expected = parselet.expected();
        Object key;
        if (expected != null) {
            key = parselet.key2() != null ? Triple.of(parselet.expected(), parselet.key1(), parselet.key2()) : Pair.of(parselet.expected(), parselet.key1());
        } else {
            key = parselet.key2() != null ? Pair.of(parselet.key1(), parselet.key2()) : parselet.key1();
        }
        if (prefixParselets.get().containsKey(key)) {
            throw new IllegalArgumentException();
        }
        prefixParselets.updateAndGet(map -> map.put(key, parselet));
        if (parselet.oper1() != null) {
            allOperators.updateAndGet(set -> set.add(parselet.oper1()));
        }
    }

    public void register(PostfixParselet parselet) {
        Type expected = parselet.expected();
        Type left = parselet.left();
        Object key;
        if (expected != null) {
            key = parselet.key2() != null ? Quadruple.of(expected, left, parselet.key1(), parselet.key2()) : Triple.of(expected, left, parselet.key1());
        } else {
            key = parselet.key2() != null ? Triple.of(left, parselet.key1(), parselet.key2()) : Pair.of(left, parselet.key1());
        }
        if (postfixParselets.get().containsKey(key)) {
            throw new IllegalArgumentException();
        }
        postfixParselets.updateAndGet(map -> map.put(key, parselet));
        if (parselet.oper1() != null) {
            allOperators.updateAndGet(set -> set.add(parselet.oper1()));
        }
    }

    public void register(CallWithArgs call) {
        callsWithArgs.updateAndGet(map -> map.compute(call.key(), (k, v) -> {
            if (v == null) {
                if (call.expected() != null) {
                    if (call.name() != null) {
                        register(new CallWithArgsParselet(call.expected(), call.name()));
                    } else {
                        register(new CallWithArgsParselet(call.expected(), call.type()));
                    }
                } else {
                    if (call.name() != null) {
                        register(new CallWithArgsParselet(call.name()));
                    } else {
                        register(new CallWithArgsParselet(call.type()));
                    }
                }
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

    public List<CallWithArgs> callsWithArgs(Token token) {
        List<CallWithArgs> list = callsWithArgs.get().get(token.text());
        if (list != null) {
            return list;
        }
        return callsWithArgs.get().get(token.type());
    }

    public AtomicParselet prefix(Type expected, Token token1, Token token2) {
        if (token2 != null) {
            Triple<Type, Object, Object> triple = Triple.of(expected, token1.text(), token2.text());
            AtomicParselet prefix = prefixParselets.get().get(triple);
            if (prefix != null) {
                return prefix;
            }
            triple = Triple.of(expected, token1.text(), token2.type());
            prefix = prefixParselets.get().get(triple);
            if (prefix != null) {
                return prefix;
            }
            triple = Triple.of(expected, token1.type(), token2.text());
            prefix = prefixParselets.get().get(triple);
            if (prefix != null) {
                return prefix;
            }
            triple = Triple.of(expected, token1.type(), token2.type());
            prefix = prefixParselets.get().get(triple);
            if (prefix != null) {
                return prefix;
            }
            Pair<Object, Object> pair = Pair.of(token1.text(), token2.text());
            prefix = prefixParselets.get().get(pair);
            if (prefix != null) {
                return prefix;
            }
            pair = Pair.of(token1.text(), token2.type());
            prefix = prefixParselets.get().get(pair);
            if (prefix != null) {
                return prefix;
            }
            pair = Pair.of(token1.type(), token2.text());
            prefix = prefixParselets.get().get(pair);
            if (prefix != null) {
                return prefix;
            }
            pair = Pair.of(token1.type(), token2.type());
            prefix = prefixParselets.get().get(pair);
            if (prefix != null) {
                return prefix;
            }
        }
        Pair<Object, Object> pair = Pair.of(expected, token1.text());
        AtomicParselet prefix = prefixParselets.get().get(pair);
        if (prefix != null) {
            return prefix;
        }
        pair = Pair.of(expected, token1.type());
        prefix = prefixParselets.get().get(pair);
        if (prefix != null) {
            return prefix;
        }
        prefix = prefixParselets.get().get(token1.text());
        if (prefix != null) {
            return prefix;
        }
        return prefixParselets.get().get(token1.type());
    }

    public PostfixParselet postfix(Type expected, Type left, Token token1, Token token2) {
        if (token2 != null) {
            Quadruple<Type, Type, Object, Object> quadruple = Quadruple.of(expected, left, token1.text(), token2.text());
            PostfixParselet postfix = postfixParselets.get().get(quadruple);
            if (postfix != null) {
                return postfix;
            }
            quadruple = Quadruple.of(expected, left, token1.text(), token2.type());
            postfix = postfixParselets.get().get(quadruple);
            if (postfix != null) {
                return postfix;
            }
            quadruple = Quadruple.of(expected, left, token1.type(), token2.text());
            postfix = postfixParselets.get().get(quadruple);
            if (postfix != null) {
                return postfix;
            }
            quadruple = Quadruple.of(expected, left, token1.type(), token2.type());
            postfix = postfixParselets.get().get(quadruple);
            if (postfix != null) {
                return postfix;
            }
            Triple<Type, Object, Object> triple = Triple.of(left, token1.text(), token2.text());
            postfix = postfixParselets.get().get(triple);
            if (postfix != null) {
                return postfix;
            }
            triple = Triple.of(left, token1.text(), token2.type());
            postfix = postfixParselets.get().get(triple);
            if (postfix != null) {
                return postfix;
            }
            triple = Triple.of(left, token1.type(), token2.text());
            postfix = postfixParselets.get().get(triple);
            if (postfix != null) {
                return postfix;
            }
            triple = Triple.of(left, token1.type(), token2.type());
            postfix = postfixParselets.get().get(triple);
            if (postfix != null) {
                return postfix;
            }
        }
        Triple<Type, Object, Object> triple = Triple.of(expected, left, token1.text());
        PostfixParselet postfix = postfixParselets.get().get(triple);
        if (postfix != null) {
            return postfix;
        }
        triple = Triple.of(expected, left, token1.type());
        postfix = postfixParselets.get().get(triple);
        if (postfix != null) {
            return postfix;
        }
        Pair<Type, Object> pair = Pair.of(left, token1.text());
        postfix = postfixParselets.get().get(pair);
        if (postfix != null) {
            return postfix;
        }
        pair = Pair.of(left, token1.type());
        return postfixParselets.get().get(pair);
    }

    public boolean isOperator(String oper) {
        return allOperators.get().contains(oper);
    }

}
