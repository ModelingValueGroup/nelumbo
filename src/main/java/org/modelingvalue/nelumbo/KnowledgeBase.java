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
    private static final QualifiedSet<Predicate, Inference>     EMPTY_MEMOIZ        = QualifiedSet.of(Inference::premise);
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
    private static class Inference extends Struct2Impl<Predicate, InferResult> {
        private static final long serialVersionUID = 1531759272582548244L;

        public int                count            = INITIAL_USAGE_COUNT;

        public Inference(Predicate predicate, InferResult result) {
            super(predicate, result);
        }

        public Predicate premise() {
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

    private static Node createNode(boolean predicate, Token token, Constructor<? extends Node> constructor, Functor functor, Object... args) throws ParseException {
        if (constructor != null) {
            try {
                return constructor.newInstance(functor, args);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ParseException(e.getClass().getSimpleName() + ": " + e.getMessage(), token);
            }
        }
        return predicate ? new Predicate(functor, args) : new Node(functor, args);
    }

    private Functor eqFunctor;

    public Functor eqFunctor() {
        if (eqFunctor == null) {
            eqFunctor = functors().get(new Functor(Type.PREDICATE, "=", null, 30, Type.NODE, Type.NODE));
        }
        return eqFunctor;
    }

    @SuppressWarnings("unchecked")
    private KnowledgeBase initBase() {
        CURRENT.run(this, () -> {
            for (Type predefined : Type.predefined()) {
                addType(predefined);
            }
            for (TokenType type : TokenType.values()) {
                addType(new Type(type));
            }

            Type TYPE_NAME = new Type("TypeName", Type.NODE);
            Type VAR_NAME = new Type("VarName", Type.NODE);
            Type SIGNATURE = new Type("Signature", Type.NODE);
            Type NATIVE = new Type("Native", Type.NODE);
            Type PRECEDENCE = new Type("Precedence", Type.NODE);
            Type FACTS = new Type("Facts", Type.NODE);
            Type FALSEHOODS = new Type("Falsehoods", Type.NODE);
            Node INCOMPLETE = new Predicate(Type.PREDICATE, "..");

            register(ParenParselet.INSTANCE);

            register(AtomicParselet.of(TokenType.TYPE, t -> {
                return type(t);
            }));
            register(AtomicParselet.of(TokenType.NAME, t -> {
                return variable(t);
            }));

            // Types
            register(AtomicParselet.of(Type.ROOT, TokenType.TYPE, "::", t -> {
                String name = t.text();
                name = name.substring(1, name.length() - 1);
                return new Terminal(TYPE_NAME, name);
            }));
            register(InfixParselet.of(Type.ROOT, TYPE_NAME, "::", Type.TYPE().list(), 10, (l, t, r) -> {
                String name = l.getVal(1);
                Type type = new Type(name, ((ListNode) r).elements());
                KnowledgeBase.CURRENT.get().addType(type);
                return type;
            }));

            // Functors
            register(InfixParselet.of(Type.ROOT, Type.TYPE(), "::=", SIGNATURE.list(), 10, (l, t, r) -> {
                ListNode list = new ListNode(Type.FUNCTOR);
                KnowledgeBase current = KnowledgeBase.CURRENT.get();
                for (Node s : ((ListNode) r).elements()) {
                    list = new ListNode(list, current.createFunctor((Type) l, t, s));
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
            register(PrefixParselet.of(Type.ROOT, TokenType.TYPE, TokenType.NAME, VAR_NAME.list(), 10, (t, l) -> {
                ListNode list = new ListNode(Type.VARIABLE);
                Type type = type(t);
                KnowledgeBase current = KnowledgeBase.CURRENT.get();
                for (Node v : ((ListNode) l).elements()) {
                    Variable var = new Variable(type, (String) v.get(1));
                    current.addVar(var);
                    list = new ListNode(list, var);
                }
                return list;
            }));
            register(AtomicParselet.of(VAR_NAME, TokenType.NAME, t -> {
                String name = t.text();
                return new Terminal(VAR_NAME, name);
            }));

            // Rules
            register(InfixParselet.of(Type.ROOT, Type.PREDICATE, "<==", Type.PREDICATE.list(), 10, (l, t, r) -> {
                return rule(l, r, false);
            }));
            register(InfixParselet.of(Type.ROOT, Type.PREDICATE, "<==>", Type.PREDICATE.list(), 10, (l, t, r) -> {
                return rule(l, r, true);
            }));

            // Expectations
            register(InfixParselet.of(Type.ROOT, Type.RESULT, "[", Type.PREDICATE.list(), 8, (l, t, r) -> {
                return new Node(FACTS, t, l, ((ListNode) r).elements());
            }));
            register(InfixParselet.of(Type.ROOT, FACTS, "[", Type.PREDICATE.list(), 8, (l, t, r) -> {
                return new Node(FALSEHOODS, l.get(1), l.get(2), l.get(3), ((ListNode) r).elements());
            }));
            register(PostfixParselet.of(Type.ROOT, FACTS, "]", 8, (l, t) -> {
                return l;
            }));
            register(PostfixParselet.of(Type.ROOT, FALSEHOODS, "]", 8, (l, t) -> {
                Token pos = l.getVal(1);
                Set<Predicate> facts = ((List<Predicate>) l.get(3)).asSet();
                boolean completeFacts = true;
                if (facts.contains(INCOMPLETE)) {
                    completeFacts = false;
                    facts = facts.remove(INCOMPLETE);
                }
                Set<Predicate> falsehoods = ((List<Predicate>) l.get(4)).asSet();
                boolean completeFalsehoods = true;
                if (falsehoods.contains(INCOMPLETE)) {
                    completeFalsehoods = false;
                    falsehoods = falsehoods.remove(INCOMPLETE);
                }
                InferResult expected = InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, Set.of());
                InferResult result = l.getVal(2, 2);
                if (!result.equals(expected) && !result.toString().equals(expected.toString())) {
                    throw new ParseException("Expected result " + expected + ", found " + result, pos, t);
                }
                return l.getVal(2);
            }));
            register(AtomicParselet.of(Type.PREDICATE, "..", t -> {
                return INCOMPLETE;
            }));

            // Queries
            register(PrefixParselet.of(Type.ROOT, "?", Type.PREDICATE, 10, (t, r) -> {
                Predicate predicate = (Predicate) r;
                predicate = predicate.setVariables(Predicate.literals(predicate.variables()));
                InferResult result = predicate.infer();
                return new Node(Type.RESULT, predicate, result);
            }));

            try {
                Parser.parse(KnowledgeBase.class);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        });
        return this;
    }

    private static Node rule(Node l, Node r, boolean symmetric) {
        ListNode list = new ListNode(Type.RULE);
        KnowledgeBase current = KnowledgeBase.CURRENT.get();
        for (Node s : ((ListNode) r).elements()) {
            Predicate cons = (Predicate) l;
            Predicate cond = (Predicate) s;
            Functor rel = current.relations.get().get(cons.functor());
            if (rel != null) {
                Rule rule = new Rule(cons, cond, symmetric);
                // current.addRule(rule);
                Map<Variable, Object> vars = Predicate.literals(rule.variables());
                cons = cons.setFunctor(rel).setVariables(vars);
                cond = cond.setVariables(vars);
            } else {
                Map<Variable, Object> local = cond.variables().removeAllKey(cons.variables());
                if (!local.isEmpty()) {
                    cond = cond.setVariables(Predicate.literals(local));
                }
            }
            Rule rule = new Rule(cons, cond, symmetric);
            current.addRule(rule);
            list = new ListNode(list, rule);
        }
        return list;
    }

    private static Type type(Token t) throws ParseException {
        String name = t.text();
        name = name.substring(1, name.length() - 1);
        if (name.endsWith("*") || name.endsWith("+")) {
            name = name.substring(0, name.length() - 1);
            Type type = type(name);
            if (type != null) {
                return type.list();
            }
        } else {
            Type type = type(name);
            if (type != null) {
                return type;
            }
        }
        throw new ParseException("Could not find type " + t.text(), t);
    }

    private static Variable variable(Token token) throws ParseException {
        Variable var = var(token.text());
        if (var != null) {
            return var;
        }
        throw new ParseException("Could not find variable " + token.text(), token);
    }

    @SuppressWarnings("unchecked")
    private Node createFunctor(Type type, Token token, Node sig) throws ParseException {
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
        boolean relation = Type.RELATION.isAssignableFrom(type);
        boolean predicate = relation || Type.PREDICATE.isAssignableFrom(type);
        if (sig.length() == 2 && sig.get(1) instanceof Type && ((Type) sig.get(1)).tokenType() != null) {
            // Literal
            if (precedence != null) {
                throw new ParseException("Precedence should not be defined " + sig, token);
            }
            type = type.literal();
            TokenType tokenType = ((Type) sig.get(1)).tokenType();
            String funtorName = constructor != null ? constructor.getDeclaringClass().getSimpleName() : type.literal().name();
            Functor functor = new Functor(type, funtorName, Type.STRING);
            addFunctor(functor);
            register(AtomicParselet.of(tokenType, tt -> createNode(predicate, token, constructor, functor, tt.text())));
            return functor;
        } else if (sig.length() == 2 && sig.get(1) instanceof String) {
            // Constant
            if (precedence != null) {
                throw new ParseException("Precedence should not be defined " + sig, token);
            }
            type = type.literal();
            String name = (String) sig.get(1);
            String funtorName = constructor != null ? constructor.getDeclaringClass().getSimpleName() : type.literal().name();
            Functor functor = new Functor(type, funtorName, n -> n.toString(1), 100, Type.STRING);
            addFunctor(functor);
            Node node = createNode(predicate, token, constructor, functor, name);
            register(AtomicParselet.of(name, tt -> node));
            return functor;
        } else if (sig.length() == 3 && sig.get(1) instanceof String && sig.get(2) instanceof List) {
            // CallWithArgs
            if (precedence != null) {
                throw new ParseException("Precedence should not be defined " + sig, token);
            }
            if (!predicate) {
                type = type.function();
            }
            String name = (String) sig.get(1);
            List<Type> args = (List<Type>) sig.get(2);
            boolean rel = relation && !args.isEmpty() && args.noneMatch(Type::isLiteral);
            Functor functor = new Functor(rel ? Type.PREDICATE : type, name, args);
            addFunctor(functor);
            register(CallWithArgs.of(name, (tt, ll) -> createNode(predicate, token, rel ? null : constructor, functor, ll.toArray()), //
                    args.toArray(i -> new Type[i])));
            if (rel) {
                List<Type> litArgs = args.replaceAll(Type::literal);
                Functor relFunctor = new Functor(type, name, litArgs);
                relations.updateAndGet(map -> map.put(functor, relFunctor));
                addFunctor(relFunctor);
                register(CallWithArgs.of(name, (tt, ll) -> createNode(predicate, token, constructor, relFunctor, ll.toArray()), //
                        litArgs.toArray(i -> new Type[i])));
                Object[] nodVars = new Variable[args.size()];
                Object[] litVars = new Variable[args.size()];
                for (int i = 0; i < args.size(); i++) {
                    nodVars[i] = new Variable(args.get(i), "n" + (i + 1));
                    litVars[i] = new Variable(litArgs.get(i), "l" + (i + 1));
                }
                Predicate conclusion = new Predicate(functor, nodVars);
                Predicate condition = (Predicate) createNode(true, token, constructor, relFunctor, litVars);
                for (int i = 0; i < args.size(); i++) {
                    Predicate eq = new Predicate(eqFunctor(), nodVars[i], litVars[i]);
                    condition = And.of(eq, condition);
                }
                addRule(new Rule(conclusion, condition, false));
            }
            return functor;
        } else if (sig.length() == 3 && sig.get(1) instanceof Type && sig.get(2) instanceof String) {
            // PostfixOperator
            if (precedence == null) {
                throw new ParseException("No precedence defined " + sig, token);
            }
            if (!predicate) {
                type = type.function();
            }
            Type pre = (Type) sig.get(1);
            boolean rel = relation && !pre.isLiteral();
            String oper = (String) sig.get(2);
            Functor functor = new Functor(rel ? Type.PREDICATE : type, oper, n -> n.toString(1) + oper, precedence, pre);
            addFunctor(functor);
            register(PostfixParselet.of(pre, oper, precedence, (ll, tt) -> createNode(predicate, token, rel ? null : constructor, functor, ll)));
            if (rel) {
                Type litPre = pre.literal();
                Functor relFunctor = new Functor(type, oper, n -> n.toString(1) + oper, precedence, litPre);
                relations.updateAndGet(map -> map.put(functor, relFunctor));
                addFunctor(relFunctor);
                register(PostfixParselet.of(litPre, oper, precedence, (ll, tt) -> createNode(predicate, token, constructor, relFunctor, ll)));
                Variable nodVar = new Variable(pre, "n");
                Variable litVar = new Variable(litPre, "l");
                Predicate conclusion = new Predicate(functor, nodVar);
                Predicate condition = (Predicate) createNode(true, token, constructor, relFunctor, litVar);
                Predicate eq = new Predicate(eqFunctor(), nodVar, litVar);
                condition = And.of(eq, condition);
                addRule(new Rule(conclusion, condition, false));
            }
            return functor;
        } else if (sig.length() == 3 && sig.get(1) instanceof String && sig.get(2) instanceof Type) {
            // PrefixOperator
            if (precedence == null) {
                throw new ParseException("No precedence defined " + sig, token);
            }
            if (!predicate) {
                type = type.function();
            }
            String oper = (String) sig.get(1);
            Type post = (Type) sig.get(2);
            boolean rel = relation && !post.isLiteral();
            Functor functor = new Functor(rel ? Type.PREDICATE : type, oper, n -> oper + n.toString(1), precedence, post);
            addFunctor(functor);
            register(PrefixParselet.of(oper, post, precedence, (tt, rr) -> createNode(predicate, token, rel ? null : constructor, functor, rr)));
            if (rel) {
                Type litPost = post.literal();
                Functor relFunctor = new Functor(type, oper, n -> oper + n.toString(1), precedence, litPost);
                relations.updateAndGet(map -> map.put(functor, relFunctor));
                addFunctor(relFunctor);
                register(PrefixParselet.of(oper, litPost, precedence, (tt, rr) -> createNode(predicate, token, constructor, relFunctor, rr)));
                Variable nodVar = new Variable(post, "n");
                Variable litVar = new Variable(litPost, "l");
                Predicate conclusion = new Predicate(functor, nodVar);
                Predicate condition = (Predicate) createNode(true, token, constructor, relFunctor, litVar);
                Predicate eq = new Predicate(eqFunctor(), nodVar, litVar);
                condition = And.of(eq, condition);
                addRule(new Rule(conclusion, condition, false));
            }
            return functor;
        } else if (sig.length() == 4 && sig.get(1) instanceof Type && sig.get(2) instanceof String && sig.get(3) instanceof Type) {
            // InfixOperator
            if (precedence == null) {
                throw new ParseException("No precedence defined " + sig, token);
            }
            if (!predicate) {
                type = type.function();
            }
            Type pre = (Type) sig.get(1);
            String oper = (String) sig.get(2);
            Type post = (Type) sig.get(3);
            boolean rel = relation && !pre.isLiteral() && !post.isLiteral();
            Functor functor = new Functor(rel ? Type.PREDICATE : type, oper, n -> n.toString(1) + oper + n.toString(2), precedence, pre, post);
            addFunctor(functor);
            register(InfixParselet.of(pre, oper, post, precedence, (ll, tt, rr) -> createNode(predicate, token, rel ? null : constructor, functor, ll, rr)));
            if (rel) {
                Type litPre = pre.literal();
                Type litPost = post.literal();
                Functor relFunctor = new Functor(type, oper, n -> n.toString(1) + oper + n.toString(2), precedence, litPre, litPost);
                relations.updateAndGet(map -> map.put(functor, relFunctor));
                addFunctor(relFunctor);
                register(InfixParselet.of(litPre, oper, litPost, precedence, (ll, tt, rr) -> createNode(predicate, token, constructor, relFunctor, ll, rr)));
                Variable nodVar0 = new Variable(pre, "n1");
                Variable litVar0 = new Variable(litPre, "l1");
                Variable nodVar1 = new Variable(post, "n2");
                Variable litVar1 = new Variable(litPost, "l2");
                Predicate conclusion = new Predicate(functor, nodVar0, nodVar1);
                Predicate condition = (Predicate) createNode(true, token, constructor, relFunctor, litVar0, litVar1);
                Predicate eq0 = new Predicate(eqFunctor(), nodVar0, litVar0);
                Predicate eq1 = new Predicate(eqFunctor(), nodVar1, litVar1);
                condition = And.of(eq0, And.of(eq1, condition));
                addRule(new Rule(conclusion, condition, false));
            }
            return functor;
        } else {
            throw new ParseException("Invalid signature " + sig, token);
        }
    }

    private final AtomicReference<Map<String, Type>>                    types            = new AtomicReference<>();
    private final AtomicReference<Set<Functor>>                         functors         = new AtomicReference<>();
    private final AtomicReference<Map<String, Variable>>                variables        = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, InferResult>>          facts            = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, Set<Rule>>>            rules            = new AtomicReference<>();

    private final AtomicReference<Map<Object, AtomicParselet>>          prefixParselets  = new AtomicReference<>();
    private final AtomicReference<Map<Object, PostfixParselet>>         postfixParselets = new AtomicReference<>();
    private final AtomicReference<Map<Object, List<CallWithArgs>>>      callsWithArgs    = new AtomicReference<>();

    private final AtomicReference<Set<String>>                          allOperators     = new AtomicReference<>();
    private final AtomicReference<Map<Functor, Functor>>                relations        = new AtomicReference<>();

    private final AtomicInteger                                         depth            = new AtomicInteger();
    private final AtomicReference<QualifiedSet<Predicate, Inference>[]> memoization      = new AtomicReference<>();
    private final InferContext                                          context;
    private final KnowledgeBase                                         init;
    private boolean                                                     stopped;

    @SuppressWarnings("unchecked")
    public KnowledgeBase(KnowledgeBase init) {
        this.init = init;
        context = InferContext.of(KnowledgeBase.this, List.of(), Map.of(), false, TRACE_NELUMBO);
        init();
    }

    @SuppressWarnings("unchecked")
    public void init() {
        types.set(init != null ? init.types.get() : Map.of());
        functors.set(init != null ? init.functors.get() : Set.of());
        variables.set(Map.of());
        facts.set(init != null ? init.facts.get() : Map.of());
        rules.set(init != null ? init.rules.get() : Map.of());

        prefixParselets.set(init != null ? init.prefixParselets.get() : Map.of());
        postfixParselets.set(init != null ? init.postfixParselets.get() : Map.of());
        callsWithArgs.set(init != null ? init.callsWithArgs.get() : Map.of());

        allOperators.set(init != null ? init.allOperators.get() : Set.of());
        relations.set(init != null ? init.relations.get() : Map.of());

        memoization.set(init != null ? init.memoization.get() : new QualifiedSet[]{EMPTY_MEMOIZ, EMPTY_MEMOIZ, EMPTY_MEMOIZ});
        depth.set(init != null ? init.depth.get() : 0);
    }

    public InferResult getFacts(Predicate predicate, InferContext context) {
        InferResult result = facts.get().get(predicate);
        if (result != null) {
            result = result.cast(predicate);
            if (context.trace()) {
                System.err.println(context.prefix() + "  " + predicate + " " + result);
            }
            return result;
        }
        return predicate.isFullyBound() ? predicate.falsehoodCC() : InferResult.factsCI(Set.of());
    }

    public Set<Rule> getRules(Predicate predicate) {
        return doGetRules(predicate.signature(depth()));
    }

    private Set<Rule> doGetRules(Predicate signature) {
        Set<Rule> result = rules.get().get(signature);
        if (result != null) {
            return result;
        }
        result = Set.of();
        Set<Predicate> post = signature.generalize(true);
        while (result.isEmpty() && !post.isEmpty()) {
            for (Predicate rel : post) {
                result = result.addAll(doGetRules(rel));
            }
            if (result.isEmpty()) {
                Set<Predicate> pre = post;
                post = Set.of();
                for (Predicate rel : pre) {
                    post = post.addAll(rel.generalize(true));
                }
            }
        }
        Set<Rule> finalRsult = result;
        rules.updateAndGet(m -> m.put(signature, finalRsult));
        return result;
    }

    public InferResult getMemoiz(Predicate Predicate) {
        for (QualifiedSet<Predicate, Inference> m : memoization.get()) {
            Inference memoiz = m.get(Predicate);
            if (memoiz != null) {
                memoiz.count++;
                return memoiz.result().cast(Predicate);
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

    public Map<Predicate, Set<Rule>> rules() {
        return rules.get();
    }

    public Map<Predicate, InferResult> facts() {
        return facts.get();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void memoization(Predicate Predicate, InferResult result) {
        boolean known = result.cycles().isEmpty() && result.isComplete();
        QualifiedSet<Predicate, Inference>[] mem = memoization.updateAndGet(array -> {
            array = array.clone();
            if (known) {
                array[0] = array[0].put(new Inference(Predicate, result));
            }
            for (Predicate fact : result.facts()) {
                if (fact instanceof Predicate) {
                    array[0] = array[0].put(new Inference((Predicate) fact, fact.factCC()));
                }
            }
            for (Predicate falsehood : result.falsehoods()) {
                if (falsehood instanceof Predicate) {
                    array[0] = array[0].put(new Inference((Predicate) falsehood, falsehood.falsehoodCC()));
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
        QualifiedSet<Predicate, Inference>[] mem = memoization.get();
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
        Predicate signature = ruleImpl.consequence().signature(Integer.MAX_VALUE);
        rules.updateAndGet(m -> addRule(ruleImpl, signature, m));
        int signDepth = signature.depth();
        depth.accumulateAndGet(signDepth, Math::max);
    }

    private static Map<Predicate, Set<Rule>> addRule(Rule ruleImpl, Predicate signature, Map<Predicate, Set<Rule>> map) {
        map = map.put(signature, ADD_RULE.apply(map.get(signature), ruleImpl));
        for (Predicate gen : signature.generalize(false)) {
            map = addRule(ruleImpl, gen, map);
        }
        return map;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void addFact(Predicate fact) {
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
    private static Map<Predicate, InferResult> addFact(Map<Predicate, InferResult> map, Predicate fact, Predicate predicate, int i, Type cls) {
        Type type = predicate.getType(i);
        if (cls.isAssignableFrom(type)) {
            InferResult pre = map.get(predicate);
            map = map.put(predicate, InferResult.factsCI(pre != null ? pre.facts().add(fact) : fact.singleton()));
            if (!cls.equals(type)) {
                for (Type gen : generalizations(type, cls)) {
                    map = addFact(map, fact, predicate.setType(i, gen), i, cls);
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

    public void print(PrintStream stream) {
        for (Entry<String, Type> e : types()) {
            Type type = e.getValue();
            String supers = type.supers().toString();
            stream.println(type + " :: " + supers.substring(4, supers.length() - 1));
        }
        for (Functor e : functors()) {
            stream.println(e.resultType() + " ::= " + e);
        }
        for (Entry<String, Variable> e : variables()) {
            Variable var = e.getValue();
            stream.println(var.type() + " " + var.name());
        }
        Set<Rule> rules = rules().flatMap(Entry::getValue).asSet();
        for (Rule r : rules) {
            stream.println(r);
        }
        for (Entry<Predicate, InferResult> e : facts()) {
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
        Object key = call.expected() != null ? Pair.of(call.expected(), call.key()) : call.key();
        callsWithArgs.updateAndGet(map -> map.compute(key, (k, v) -> {
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

    public List<CallWithArgs> callsWithArgs(Type expected, Token token) {
        List<CallWithArgs> list = callsWithArgs.get().get(Pair.of(expected, token.text()));
        if (list != null) {
            return list;
        }
        list = callsWithArgs.get().get(Pair.of(expected, token.type()));
        if (list != null) {
            return list;
        }
        list = callsWithArgs.get().get(token.text());
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
