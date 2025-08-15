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
import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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
import org.modelingvalue.nelumbo.syntax.AtomicParselet;
import org.modelingvalue.nelumbo.syntax.CallWithArgs;
import org.modelingvalue.nelumbo.syntax.CallWithArgsParselet;
import org.modelingvalue.nelumbo.syntax.InfixParselet;
import org.modelingvalue.nelumbo.syntax.ParenParselet;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.PostfixParselet;
import org.modelingvalue.nelumbo.syntax.PrefixParselet;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

@SuppressWarnings("DuplicatedCode")
public final class KnowledgeBase {

    private static final boolean                                                        TRACE_NELUMBO        = java.lang.Boolean.getBoolean("TRACE_NELUMBO");
    public static final  Context<KnowledgeBase>                                         CURRENT              = Context.of();
    private static final ContextPool                                                    POOL                 = ContextThread.createPool();
    private static final QualifiedSet<Predicate, Inference>                             EMPTY_MEMOIZ         = QualifiedSet.of(Inference::premise);
    private static final int                                                            MAX_LOGIC_MEMOIZ     = Integer.getInteger("MAX_LOGIC_MEMOIZ", 512);
    private static final int                                                            MAX_LOGIC_MEMOIZ_D4  = KnowledgeBase.MAX_LOGIC_MEMOIZ / 4;
    private static final int                                                            INITIAL_USAGE_COUNT  = Integer.getInteger("INITIAL_USAGE_COUNT", 4);
    private static final BiFunction<Set<Rule>, Rule, Set<Rule>>                         ADD_RULE             = (l, e) -> {
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
    private static final AtomicReference<Map<Class<? extends Node>, Consumer<Functor>>> FUNCTOR_REGISTRATION = new AtomicReference<>(Map.of());
    public static final  KnowledgeBase                                                  BASE                 = new KnowledgeBase(null).initBase();

    public static void registerFunctorSetter(Class<? extends Node> clazz, Consumer<Functor> setter) {
        FUNCTOR_REGISTRATION.updateAndGet(map -> map.put(clazz, setter));
    }

    private static class Inference extends Struct2Impl<Predicate, InferResult> {
        @Serial
        private static final long serialVersionUID = 1531759272582548244L;

        public int count = INITIAL_USAGE_COUNT;

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
        @Serial
        private static final long serialVersionUID = -1375078574164947441L;

        private final Runnable      runnable;
        private final KnowledgeBase knowledgebase;

        public LogicTask(Runnable runnable, KnowledgeBase init) {
            this.runnable      = runnable;
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

    public static KnowledgeBase run(Runnable runnable) {
        return run(runnable, BASE);
    }

    public static KnowledgeBase run(Runnable runnable, KnowledgeBase init) {
        return POOL.invoke(new LogicTask(runnable, init));
    }

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

    private static Node createNode(boolean predicate, Token[] tokens, Constructor<? extends Node> constructor, Functor functor, Object... args) throws ParseException {
        if (constructor != null) {
            try {
                return constructor.newInstance(functor, tokens, args);
            } catch (InstantiationException |
                     IllegalAccessException |
                     IllegalArgumentException |
                     InvocationTargetException e) {
                throw new ParseException(e.getClass().getSimpleName() + ": " + e.getMessage(), tokens);
            }
        }
        return predicate ? new Predicate(functor, tokens, args) : new Node(functor, tokens, args);
    }

    private Functor eqFunctor;

    public Functor eqFunctor() {
        if (eqFunctor == null) {
            eqFunctor = functors().get(new Functor(Token.EMPTY, Type.PREDICATE, "=", null, 30, Type.NODE, Type.NODE));
        }
        return eqFunctor;
    }

    @SuppressWarnings({"unchecked", "CodeBlock2Expr"})
    private KnowledgeBase initBase() {
        CURRENT.run(this, () -> {
            for (Type predefined : Type.predefined()) {
                addType(predefined);
            }
            for (TokenType type : TokenType.values()) {
                addType(new Type(type));
            }

            Type TYPE_NAME  = new Type("TypeName", Type.NODE);
            Type VAR_NAME   = new Type("VarName", Type.NODE);
            Type SIGNATURE  = new Type("Signature", Type.NODE);
            Type NATIVE     = new Type("Native", Type.NODE);
            Type PRECEDENCE = new Type("Precedence", Type.NODE);
            Type FACTS      = new Type("Facts", Type.NODE);
            Type FALSEHOODS = new Type("Falsehoods", Type.NODE);
            Node INCOMPLETE = new Predicate(Type.PREDICATE, Token.EMPTY, "..");

            register(ParenParselet.INSTANCE);

            register(AtomicParselet.of(TokenType.TYPE, t -> type(t).setTokens(t.singleton())));
            register(AtomicParselet.of(TokenType.NAME, t -> variable(t).setTokens(t.singleton())));

            // Types
            register(AtomicParselet.of(Type.ROOT, TokenType.TYPE, "::", t -> {
                String name = t.text();
                name = name.substring(1, name.length() - 1);
                return new Terminal(TYPE_NAME, t.singleton(), name);
            }));
            register(InfixParselet.of(Type.ROOT, TYPE_NAME, "::", Type.TYPE().list(), 10, (l, t, r) -> {
                String name = l.getVal(0);
                Type   type = new Type(t.prepend(l.tokens()), name, ((ListNode) r).elements());
                KnowledgeBase.CURRENT.get().addType(type);
                return type;
            }));

            // Functors
            register(InfixParselet.of(Type.ROOT, Type.TYPE(), "::=", SIGNATURE.list(), 10, (l, t, r) -> {
                ListNode      list    = new ListNode(Token.EMPTY, Type.FUNCTOR);
                KnowledgeBase current = KnowledgeBase.CURRENT.get();
                for (Node s : ((ListNode) r).elements()) {
                    list = new ListNode(Token.EMPTY, list, current.createFunctor((Type) l, s.tokens(), s));
                }
                return list;
            }));
            register(CallWithArgs.of(SIGNATURE, TokenType.NAME, (t, l) -> {
                return new Node(SIGNATURE, t.singleton(), t.text(), l);
            }, Type.TYPE().list()));
            register(AtomicParselet.of(SIGNATURE, TokenType.NAME, t -> {
                return new Node(SIGNATURE, t.singleton(), t.text());
            }));
            register(AtomicParselet.of(SIGNATURE, TokenType.TYPE, t -> {
                Type type = type(t);
                return type.tokenType() != null ? new Node(SIGNATURE, t.singleton(), type) : type;
            }));
            register(PrefixParselet.of(SIGNATURE, TokenType.OPERATOR, TokenType.TYPE, Type.TYPE(), 50, (t, r) -> {
                return new Node(SIGNATURE, t.singleton(), t.text(), r);
            }));
            register(PrefixParselet.of(SIGNATURE, TokenType.NAME, TokenType.TYPE, Type.TYPE(), 50, (t, r) -> {
                return new Node(SIGNATURE, t.singleton(), t.text(), r);
            }));
            register(PostfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.OPERATOR, 50, (l, t) -> {
                return new Node(SIGNATURE, t.singleton(), l, t.text());
            }));
            register(PostfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.NAME, 50, (l, t) -> {
                return new Node(SIGNATURE, t.singleton(), l, t.text());
            }));
            register(InfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.OPERATOR, TokenType.TYPE, Type.TYPE(), 50, (l, t, r) -> {
                return new Node(SIGNATURE, t.singleton(), l, t.text(), r);
            }));
            register(InfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.NAME, TokenType.TYPE, Type.TYPE(), 50, (l, t, r) -> {
                return new Node(SIGNATURE, t.singleton(), l, t.text(), r);
            }));
            register(InfixParselet.of(SIGNATURE, "#", TokenType.NUMBER, PRECEDENCE, 50, (l, t, r) -> {
                return new Node(SIGNATURE, t.append(r.tokens()), l, r.get(0));
            }));
            register(AtomicParselet.of(PRECEDENCE, TokenType.NUMBER, t -> {
                return new Node(PRECEDENCE, t.singleton(), Integer.parseInt(t.text()));
            }));
            register(InfixParselet.of(SIGNATURE, "@", TokenType.QNAME, NATIVE, 50, (l, t, r) -> {
                return new Node(SIGNATURE, t.append(r.tokens()), l, r.get(0));
            }));
            register(AtomicParselet.of(NATIVE, TokenType.QNAME, t -> {
                try {
                    return new Node(NATIVE, t.singleton(), Class.forName(t.text()).getConstructor(Functor.class, Token[].class, Object[].class));
                } catch (ClassNotFoundException |
                         NoSuchMethodException |
                         SecurityException e) {
                    throw new ParseException(e.getClass().getSimpleName() + ": " + e.getMessage(), t);
                }
            }));

            // Variables
            register(PrefixParselet.of(Type.ROOT, TokenType.TYPE, TokenType.NAME, VAR_NAME.list(), 10, (t, l) -> {
                ListNode      list    = new ListNode(Token.EMPTY, Type.VARIABLE);
                Type          type    = type(t);
                KnowledgeBase current = KnowledgeBase.CURRENT.get();
                for (Node v : ((ListNode) l).elements()) {
                    Variable var = new Variable(v.tokens(), type, (String) v.get(0));
                    current.addVar(var);
                    list = new ListNode(Token.EMPTY, list, var);
                }
                return list;
            }));
            register(AtomicParselet.of(VAR_NAME, TokenType.NAME, t -> {
                String name = t.text();
                return new Terminal(VAR_NAME, t.singleton(), name);
            }));

            // Rules
            register(InfixParselet.of(Type.ROOT, Type.PREDICATE, "<==", Type.PREDICATE.list(), 10, (l, t, r) -> {
                return rule(l, t, r, false);
            }));
            register(InfixParselet.of(Type.ROOT, Type.PREDICATE, "<==>", Type.PREDICATE.list(), 10, (l, t, r) -> {
                return rule(l, t, r, true);
            }));

            // Expectations
            register(InfixParselet.of(Type.ROOT, Type.RESULT, "[", Type.PREDICATE.list(), 8, (l, t, r) -> {
                return new Node(FACTS, t.singleton(), l, ((ListNode) r).elements());
            }));
            register(InfixParselet.of(Type.ROOT, FACTS, "[", Type.PREDICATE.list(), 8, (l, t, r) -> {
                return new Node(FALSEHOODS, l.tokens(), l.get(0), l.get(1), ((ListNode) r).elements());
            }));
            register(PostfixParselet.of(Type.ROOT, FACTS, "]", 8, (l, t) -> {
                return l.setTokens(t.prepend(l.tokens()));
            }));
            register(PostfixParselet.of(Type.ROOT, FALSEHOODS, "]", 8, (l, t) -> {
                Set<Predicate> facts         = ((List<Predicate>) l.get(1)).asSet();
                boolean        completeFacts = true;
                if (facts.contains(INCOMPLETE)) {
                    completeFacts = false;
                    facts         = facts.remove(INCOMPLETE);
                }
                Set<Predicate> falsehoods         = ((List<Predicate>) l.get(2)).asSet();
                boolean        completeFalsehoods = true;
                if (falsehoods.contains(INCOMPLETE)) {
                    completeFalsehoods = false;
                    falsehoods         = falsehoods.remove(INCOMPLETE);
                }
                InferResult expected = InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, Set.of());
                InferResult result   = l.getVal(0, 1);
                if (!result.equals(expected) && !result.toString().equals(expected.toString())) {
                    throw new ParseException("Expected result " + expected + ", found " + result, t.prepend(l.tokens()));
                }
                return l.getVal(0);
            }));
            register(AtomicParselet.of(Type.PREDICATE, "..", t -> {
                return INCOMPLETE;
            }));

            // Queries
            register(PrefixParselet.of(Type.ROOT, "?", Type.PREDICATE, 10, (t, r) -> {
                Predicate predicate = (Predicate) r;
                predicate = predicate.setVariables(Predicate.literals(predicate.variables()));
                InferResult result = predicate.infer();
                return new Node(Type.RESULT, t.prepend(r.tokens()), predicate, result);
            }));

            try {
                Parser.parse(KnowledgeBase.class);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        });
        return this;
    }

    @SuppressWarnings("unused")
    private static Node rule(Node l, Token t, Node r, boolean symmetric) throws ParseException {
        ListNode              list     = new ListNode(Token.EMPTY, Type.RULE);
        KnowledgeBase         current  = KnowledgeBase.CURRENT.get();
        Predicate             cons     = (Predicate) l;
        Map<Variable, Object> consVars = cons.variables();
        for (Node c : ((ListNode) r).elements()) {
            Predicate             cond     = (Predicate) c;
            Map<Variable, Object> condVars = cond.variables();
            Functor               rel      = current.relations.get().get(cons.functor());
            Map<Variable, Object> local    = condVars.removeAllKey(consVars);
            if (symmetric && !local.isEmpty()) {
                throw new ParseException("No local variables allowed in condition of a symmetric rule. Found: " + local.get(0).getKey().name(), local.get(0).getKey().tokens());
            }
            if (rel != null) {
                Map<Variable, Object> vars = Predicate.literals(condVars.putAll(consVars));
                cons = cons.setFunctor(rel).setVariables(vars);
                cond = cond.setVariables(vars);
            } else if (!local.isEmpty()) {
                cond = cond.setVariables(Predicate.literals(local));
            }
            Rule rule = new Rule(c.tokens(), cons, cond, symmetric);
            current.addRule(rule);
            list = new ListNode(Token.EMPTY, list, rule);
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

    // NB: ConstantValue because Intellij concludes that rel is always false, but it is not !!
    @SuppressWarnings({"unchecked", "ConstantValue", "DataFlowIssue"})
    private Node createFunctor(Type type, Token[] tokens, Node sig) throws ParseException {
        Constructor<? extends Node> constructor = sig.length() == 2 && sig.get(0) instanceof Node && sig.get(1) instanceof Constructor ? //
                                                  (Constructor<? extends Node>) sig.get(1) : null;
        if (constructor != null) {
            sig = (Node) sig.get(0);
        }
        Integer precedence = sig.length() == 2 && sig.get(0) instanceof Node && sig.get(1) instanceof Integer ? //
                             (Integer) sig.get(1) : null;
        if (precedence != null) {
            sig = (Node) sig.get(0);
        }
        boolean relation  = Type.RELATION.isAssignableFrom(type);
        boolean predicate = relation || Type.PREDICATE.isAssignableFrom(type);
        if (sig.length() == 1 && sig.get(0) instanceof Type && ((Type) sig.get(0)).tokenType() != null) {
            // Literal
            if (precedence != null) {
                throw new ParseException("Precedence should not be defined " + sig, tokens);
            }
            type = type.literal();
            TokenType tokenType   = ((Type) sig.get(0)).tokenType();
            String    functorName = constructor != null ? constructor.getDeclaringClass().getSimpleName() : type.literal().name();
            Functor   functor     = new Functor(tokens, type, functorName, Type.STRING);
            addFunctor(functor, tokens, constructor);
            register(AtomicParselet.of(tokenType, tt -> createNode(predicate, tt.singleton(), constructor, functor, tt.text())));
            return functor;
        } else if (sig.length() == 1 && sig.get(0) instanceof String name) {
            // Constant
            if (precedence != null) {
                throw new ParseException("Precedence should not be defined " + sig, tokens);
            }
            type = type.literal();
            String  functorName = constructor != null ? constructor.getDeclaringClass().getSimpleName() : type.literal().name();
            Functor functor     = new Functor(tokens, type, functorName, n -> n.toString(0), 100, Type.STRING);
            addFunctor(functor, tokens, constructor);
            Node node = createNode(predicate, tokens, constructor, functor, name);
            register(AtomicParselet.of(name, tt -> node.setTokens(tt.singleton())));
            return functor;
        } else if (sig.length() == 2 && sig.get(0) instanceof String name && sig.get(1) instanceof List) {
            // CallWithArgs
            if (precedence != null) {
                throw new ParseException("Precedence should not be defined " + sig, tokens);
            }
            if (!predicate) {
                type = type.function();
            }
            List<Type> args    = (List<Type>) sig.get(1);
            boolean    rel     = relation && !args.isEmpty() && args.noneMatch(Type::isLiteral);
            Functor    functor = new Functor(tokens, rel ? Type.PREDICATE : type, name, args);
            addFunctor(functor, tokens, rel ? null : constructor);
            register(CallWithArgs.of(name, //
                                     (tt, ll) -> createNode(predicate, tt.prepend(ll.last().tokens()), rel ? null : constructor, functor, ll.toArray()), //
                                     args.toArray(Type[]::new) //
                                    ));
            if (rel) {
                List<Type> litArgs    = args.replaceAll(Type::literal);
                Functor    relFunctor = new Functor(tokens, type, name, litArgs);
                relations.updateAndGet(map -> map.put(functor, relFunctor));
                addFunctor(relFunctor, tokens, constructor);
                register(CallWithArgs.of(name, (tt, ll) -> createNode(predicate, tt.prepend(ll.last().tokens()), constructor, relFunctor, ll.toArray()), //
                                         litArgs.toArray(Type[]::new)));
                Object[] nodVars = new Variable[args.size()];
                Object[] litVars = new Variable[args.size()];
                for (int i = 0; i < args.size(); i++) {
                    nodVars[i] = new Variable(args.get(i).tokens(), args.get(i), "n" + (i + 1));
                    litVars[i] = new Variable(litArgs.get(i).tokens(), litArgs.get(i), "l" + (i + 1));
                }
                Predicate conclusion = new Predicate(functor, tokens, nodVars);
                Predicate condition  = (Predicate) createNode(true, tokens, constructor, relFunctor, litVars);
                for (int i = 0; i < args.size(); i++) {
                    Predicate eq = new Predicate(eqFunctor(), tokens, nodVars[i], litVars[i]);
                    condition = And.of(eq, condition);
                }
                addRule(new Rule(tokens, conclusion, condition, false));
            }
            return functor;
        } else if (sig.length() == 2 && sig.get(0) instanceof Type pre && sig.get(1) instanceof String oper) {
            // PostfixOperator
            if (precedence == null) {
                throw new ParseException("No precedence defined " + sig, tokens);
            }
            if (!predicate) {
                type = type.function();
            }
            boolean rel     = relation && !pre.isLiteral();
            Functor functor = new Functor(tokens, rel ? Type.PREDICATE : type, oper, n -> n.toString(0) + oper, precedence, pre);
            addFunctor(functor, tokens, rel ? null : constructor);
            register(PostfixParselet.of(pre, oper, precedence, (ll, tt) -> createNode(predicate, tt.prepend(ll.tokens()), rel ? null : constructor, functor, ll)));
            if (rel) {
                Type    litPre     = pre.literal();
                Functor relFunctor = new Functor(tokens, type, oper, n -> n.toString(0) + oper, precedence, litPre);
                relations.updateAndGet(map -> map.put(functor, relFunctor));
                addFunctor(relFunctor, tokens, constructor);
                //noinspection ConstantValue (predicate is always true)
                register(PostfixParselet.of(litPre, oper, precedence, (ll, tt) -> createNode(predicate, tt.prepend(ll.tokens()), constructor, relFunctor, ll)));
                Variable  nodVar     = new Variable(pre.tokens(), pre, "n");
                Variable  litVar     = new Variable(litPre.tokens(), litPre, "l");
                Predicate conclusion = new Predicate(functor, tokens, nodVar);
                Predicate condition  = (Predicate) createNode(true, tokens, constructor, relFunctor, litVar);
                Predicate eq         = new Predicate(eqFunctor(), tokens, nodVar, litVar);
                condition = And.of(eq, condition);
                addRule(new Rule(tokens, conclusion, condition, false));
            }
            return functor;
        } else if (sig.length() == 2 && sig.get(0) instanceof String oper && sig.get(1) instanceof Type post) {
            // PrefixOperator
            if (precedence == null) {
                throw new ParseException("No precedence defined " + sig, tokens);
            }
            if (!predicate) {
                type = type.function();
            }
            boolean rel     = relation && !post.isLiteral();
            Functor functor = new Functor(tokens, rel ? Type.PREDICATE : type, oper, n -> oper + n.toString(0), precedence, post);
            addFunctor(functor, tokens, rel ? null : constructor);
            register(PrefixParselet.of(oper, post, precedence, (tt, rr) -> createNode(predicate, tt.append(rr.tokens()), rel ? null : constructor, functor, rr)));
            if (rel) {
                Type    litPost    = post.literal();
                Functor relFunctor = new Functor(tokens, type, oper, n -> oper + n.toString(0), precedence, litPost);
                relations.updateAndGet(map -> map.put(functor, relFunctor));
                addFunctor(relFunctor, tokens, constructor);
                //noinspection ConstantValue (predicate is always true)
                register(PrefixParselet.of(oper, litPost, precedence, (tt, rr) -> createNode(predicate, tt.append(rr.tokens()), constructor, relFunctor, rr)));
                Variable  nodVar     = new Variable(post.tokens(), post, "n");
                Variable  litVar     = new Variable(litPost.tokens(), litPost, "l");
                Predicate conclusion = new Predicate(functor, tokens, nodVar);
                Predicate condition  = (Predicate) createNode(true, tokens, constructor, relFunctor, litVar);
                Predicate eq         = new Predicate(eqFunctor(), tokens, nodVar, litVar);
                condition = And.of(eq, condition);
                addRule(new Rule(tokens, conclusion, condition, false));
            }
            return functor;
        } else if (sig.length() == 3 && sig.get(0) instanceof Type pre && sig.get(1) instanceof String oper && sig.get(2) instanceof Type post) {
            // InfixOperator
            if (precedence == null) {
                throw new ParseException("No precedence defined " + sig, tokens);
            }
            if (!predicate) {
                type = type.function();
            }
            boolean rel     = relation && !pre.isLiteral() && !post.isLiteral();
            Functor functor = new Functor(tokens, rel ? Type.PREDICATE : type, oper, n -> n.toString(0) + oper + n.toString(1), precedence, pre, post);
            addFunctor(functor, tokens, rel ? null : constructor);
            register(InfixParselet.of(pre, oper, post, precedence, (ll, tt, rr) -> createNode(predicate, Token.concat(ll.tokens(), tt.append(rr.tokens())), rel ? null : constructor, functor, ll, rr)));
            if (rel) {
                Type    litPre     = pre.literal();
                Type    litPost    = post.literal();
                Functor relFunctor = new Functor(tokens, type, oper, n -> n.toString(0) + oper + n.toString(1), precedence, litPre, litPost);
                relations.updateAndGet(map -> map.put(functor, relFunctor));
                addFunctor(relFunctor, tokens, constructor);
                //noinspection ConstantValue (predicate is always true)
                register(InfixParselet.of(litPre, oper, litPost, precedence, (ll, tt, rr) -> createNode(predicate, Token.concat(ll.tokens(), tt.append(rr.tokens())), constructor, relFunctor, ll, rr)));
                Variable  nodVar0    = new Variable(pre.tokens(), pre, "n1");
                Variable  litVar0    = new Variable(litPre.tokens(), litPre, "l1");
                Variable  nodVar1    = new Variable(post.tokens(), post, "n2");
                Variable  litVar1    = new Variable(litPost.tokens(), litPost, "l2");
                Predicate conclusion = new Predicate(functor, tokens, nodVar0, nodVar1);
                Predicate condition  = (Predicate) createNode(true, tokens, constructor, relFunctor, litVar0, litVar1);
                Predicate eq0        = new Predicate(eqFunctor(), tokens, nodVar0, litVar0);
                Predicate eq1        = new Predicate(eqFunctor(), tokens, nodVar1, litVar1);
                condition = And.of(eq0, And.of(eq1, condition));
                addRule(new Rule(tokens, conclusion, condition, false));
            }
            return functor;
        } else {
            throw new ParseException("Invalid signature " + sig, tokens);
        }
    }

    private final AtomicReference<Map<String, Type>>                    types            = new AtomicReference<>();
    private final AtomicReference<Set<Functor>>                         functors         = new AtomicReference<>();
    private final AtomicReference<Map<String, Variable>>                variables        = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, InferResult>>          facts            = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, Set<Rule>>>            rules            = new AtomicReference<>();
    //
    private final AtomicReference<Map<Object, AtomicParselet>>          prefixParselets  = new AtomicReference<>();
    private final AtomicReference<Map<Object, PostfixParselet>>         postfixParselets = new AtomicReference<>();
    private final AtomicReference<Map<Object, List<CallWithArgs>>>      callsWithArgs    = new AtomicReference<>();
    //
    private final AtomicReference<Set<String>>                          allOperators     = new AtomicReference<>();
    private final AtomicReference<Map<Functor, Functor>>                relations        = new AtomicReference<>();
    //
    private final AtomicInteger                                         depth            = new AtomicInteger();
    private final AtomicReference<QualifiedSet<Predicate, Inference>[]> memoization      = new AtomicReference<>();
    private final InferContext                                          context;
    private final KnowledgeBase                                         init;
    private       boolean                                               stopped;

    public KnowledgeBase(KnowledgeBase init) {
        this.init = init;
        context   = InferContext.of(KnowledgeBase.this, List.of(), Map.of(), false, TRACE_NELUMBO);
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
                System.out.println(context.prefix() + "  " + predicate + " " + result);
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

    public InferResult getMemoiz(Predicate predicate) {
        for (QualifiedSet<Predicate, Inference> m : memoization.get()) {
            Inference memoiz = m.get(predicate);
            if (memoiz != null) {
                memoiz.count++;
                return memoiz.result().cast(predicate);
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

    void memoization(Predicate predicate, InferResult result) {
        boolean known = result.cycles().isEmpty() && result.isComplete();
        QualifiedSet<Predicate, Inference>[] mem = memoization.updateAndGet(array -> {
            array = array.clone();
            if (known) {
                array[0] = array[0].put(new Inference(predicate, result));
            }
            for (Predicate fact : result.facts()) {
                array[0] = array[0].put(new Inference(fact, fact.factCC()));
            }
            for (Predicate falsehood : result.falsehoods()) {
                array[0] = array[0].put(new Inference(falsehood, falsehood.falsehoodCC()));
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
                        array    = array.clone();
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

    public void addFact(Predicate fact) {
        Functor    functor = fact.functor();
        List<Type> args    = functor.args();
        facts.updateAndGet(map -> {
            map = map.put(fact, fact.factCC());
            for (int i = 0; i < fact.length(); i++) {
                map = addFact(map, fact, fact.setType(i, fact.getType(i)), i, args.get(i));
            }
            return map;
        });
    }

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

    public void addType(Type type) {
        types.updateAndGet(map -> map.put(type.name(), type));
    }

    public Type getType(String name) {
        return types.get().get(name);
    }

    public void addVar(Variable var) {
        variables.updateAndGet(map -> map.put(var.name(), var));
    }

    public Variable getVar(String name) {
        return variables.get().get(name);
    }

    public void addFunctor(Functor functor, @SuppressWarnings("unused") Token[] tokens, Constructor<? extends Node> constructor) {
        if (constructor != null && !FUNCTOR_REGISTRATION.get().isEmpty()) {
            Class<? extends Node> cls    = constructor.getDeclaringClass();
            Consumer<Functor>     setter = FUNCTOR_REGISTRATION.get().get(cls);
            if (setter != null) {
                setter.accept(functor);
                FUNCTOR_REGISTRATION.updateAndGet(map -> map.remove(cls));
            }
        }
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
            Type   type   = e.getValue();
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
        Type   expected = parselet.expected();
        Object k1       = parselet.key1();
        Object k2       = parselet.key2();
        String o1       = parselet.oper1();
        Object key;
        if (expected != null && k2 != null) {
            key = Triple.of(expected, k1, k2);
        } else if (expected != null) {
            key = Pair.of(expected, k1);
        } else if (k2 != null) {
            key = Pair.of(k1, k2);
        } else {
            key = k1;
        }
        if (prefixParselets.get().containsKey(key)) {
            throw new IllegalArgumentException("prefixParselet already registered " + key);
        }
        prefixParselets.updateAndGet(map -> map.put(key, parselet));
        if (o1 != null) {
            allOperators.updateAndGet(set -> set.add(o1));
        }
    }

    public void register(PostfixParselet parselet) {
        Type   expected = parselet.expected();
        Type   left     = parselet.left();
        Object k1       = parselet.key1();
        Object k2       = parselet.key2();
        String o1       = parselet.oper1();
        Object key;
        if (expected != null && k2 != null) {
            key = Quadruple.of(expected, left, k1, k2);
        } else if (expected != null) {
            key = Triple.of(expected, left, k1);
        } else if (k2 != null) {
            key = Triple.of(left, k1, k2);
        } else {
            key = Pair.of(left, k1);
        }
        if (postfixParselets.get().containsKey(key)) {
            throw new IllegalArgumentException("postfixParselet already registered " + key);
        }
        postfixParselets.updateAndGet(map -> map.put(key, parselet));
        if (o1 != null) {
            allOperators.updateAndGet(set -> set.add(o1));
        }
    }

    public void register(CallWithArgs call) {
        String    name = call.name();
        TokenType type = call.type();
        Type      exp  = call.expected();
        Object    k    = call.key();
        Object    key  = exp != null ? Pair.of(exp, k) : k;
        callsWithArgs.updateAndGet(map -> map.compute(key, (__, v) -> {
            if (v == null) {
                if (exp != null) {
                    if (name != null) {
                        register(new CallWithArgsParselet(exp, name));
                    } else {
                        register(new CallWithArgsParselet(exp, type));
                    }
                } else {
                    if (name != null) {
                        register(new CallWithArgsParselet(name));
                    } else {
                        register(new CallWithArgsParselet(type));
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
        String                          text   = token.text();
        TokenType                       type   = token.type();
        Map<Object, List<CallWithArgs>> swaMap = callsWithArgs.get();
        List<CallWithArgs>              list;

        list = swaMap.get(Pair.of(expected, text));
        if (list != null) {
            return list;
        }
        list = swaMap.get(Pair.of(expected, type));
        if (list != null) {
            return list;
        }
        list = swaMap.get(text);
        if (list != null) {
            return list;
        }
        return swaMap.get(type);
    }

    public AtomicParselet prefix(Type expected, Token token1, Token token2) {
        assert token1 != null;

        Map<Object, AtomicParselet> ppMap = prefixParselets.get();
        AtomicParselet              pp;

        String    text1 = token1.text();
        TokenType type1 = token1.type();
        if (token2 != null) {
            String    text2 = token2.text();
            TokenType type2 = token2.type();

            pp = ppMap.get(Triple.of(expected, text1, text2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Triple.of(expected, text1, type2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Triple.of(expected, type1, text2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Triple.of(expected, type1, type2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Pair.of(text1, text2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Pair.of(text1, type2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Pair.of(type1, text2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Pair.of(type1, type2));
            if (pp != null) {
                return pp;
            }
        }
        pp = ppMap.get(Pair.of(expected, text1));
        if (pp != null) {
            return pp;
        }
        pp = ppMap.get(Pair.of(expected, type1));
        if (pp != null) {
            return pp;
        }
        pp = ppMap.get(text1);
        if (pp != null) {
            return pp;
        }
        return ppMap.get(type1);
    }

    public PostfixParselet postfix(Type expected, Type left, Token token1, Token token2) {
        assert token1 != null;

        PostfixParselet              pp;
        Map<Object, PostfixParselet> ppMap = postfixParselets.get();

        String    text1 = token1.text();
        TokenType type1 = token1.type();
        if (token2 != null) {
            String    text2 = token2.text();
            TokenType type2 = token2.type();

            pp = ppMap.get(Quadruple.of(expected, left, text1, text2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Quadruple.of(expected, left, text1, type2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Quadruple.of(expected, left, type1, text2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Quadruple.of(expected, left, type1, type2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Triple.of(left, text1, text2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Triple.of(left, text1, type2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Triple.of(left, type1, text2));
            if (pp != null) {
                return pp;
            }
            pp = ppMap.get(Triple.of(left, type1, type2));
            if (pp != null) {
                return pp;
            }
        }
        pp = ppMap.get(Triple.of(expected, left, text1));
        if (pp != null) {
            return pp;
        }
        pp = ppMap.get(Triple.of(expected, left, type1));
        if (pp != null) {
            return pp;
        }
        pp = ppMap.get(Pair.of(left, text1));
        if (pp != null) {
            return pp;
        }
        return ppMap.get(Pair.of(left, type1));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isOperator(String oper) {
        return allOperators.get().contains(oper);
    }

}
