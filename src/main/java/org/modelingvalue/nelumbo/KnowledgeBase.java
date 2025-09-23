//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo;

import static org.modelingvalue.nelumbo.patterns.Pattern.*;
import static org.modelingvalue.nelumbo.syntax.TokenType.ENDOFFILE;
import static org.modelingvalue.nelumbo.syntax.TokenType.NEWLINE;

import java.io.PrintStream;
import java.io.Serial;
import java.lang.reflect.Constructor;
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
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;
import org.modelingvalue.nelumbo.patterns.SequencePattern;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

@SuppressWarnings("DuplicatedCode")
public final class KnowledgeBase {

    private static final boolean                                                        TRACE_NELUMBO        = java.lang.Boolean.getBoolean("TRACE_NELUMBO");
    public static final Context<KnowledgeBase>                                          CURRENT              = Context.of();
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
    public static final KnowledgeBase                                                   BASE                 = new KnowledgeBase(null).initBase();

    public static void registerFunctorSetter(Class<? extends Node> clazz, Consumer<Functor> setter) {
        FUNCTOR_REGISTRATION.updateAndGet(map -> map.put(clazz, setter));
    }

    private static class Inference extends Struct2Impl<Predicate, InferResult> {
        @Serial
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
        @Serial
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

    private Functor eqFunctor;

    public Functor eqFunctor() {
        if (eqFunctor == null) {
            eqFunctor = null; // TODO
        }
        return eqFunctor;
    }

    private void addType(Type type) {
        register(Functor.of(t(type.toString()), Type.TYPE(), (t, a) -> {
            return type.setAstElements(t);
        }));
    }

    private static <E> List<List<E>> split(int start, List<E> list, java.util.function.Predicate<E> separator) {
        List<List<E>> split = List.of();
        int prev = start;
        for (int i = start; i < list.size(); i++) {
            E e = list.get(i);
            if (separator.test(e)) {
                split = split.add(list.sublist(prev, i));
                prev = i + 1;
            }
        }
        if (prev < list.size()) {
            split = split.add(list.sublist(prev, list.size()));
        }
        return split;
    }

    private Pattern pattern(List<AstElement> elements) {
        List<Pattern> patterns = List.of();
        for (AstElement e : elements) {
            if (!e.isMeta()) {
                if (e instanceof Token t) {
                    String text = t.text();
                    if (t.type() == TokenType.STRING) {
                        text = text.substring(1, text.length() - 1);
                    }
                    patterns = patterns.add(t(text));
                } else if (e instanceof Type t) {
                    TokenType tt = t.tokenType();
                    if (tt != null) {
                        patterns = patterns.add(t(tt));
                    } else {
                        patterns = patterns.add(n(t, null));
                    }
                } else {
                    Pattern pattern = (Pattern) e;
                    if (pattern instanceof SequencePattern sp) {
                        patterns = patterns.addAll(sp.elements());
                    } else {
                        patterns = patterns.add(pattern);
                    }
                }
            }
        }
        return patterns.size() > 1 ? s(patterns.toArray(i -> new Pattern[i])) : patterns.first();
    }

    @SuppressWarnings({"unchecked", "CodeBlock2Expr"})
    private KnowledgeBase initBase() {
        CURRENT.run(this, () -> {
            for (Type type : Type.predefined()) {
                addType(type);
            }

            for (TokenType tokenType : TokenType.values()) {
                if (!tokenType.skip()) {
                    addType(new Type(tokenType));
                }
            }

            register(Functor.of(s(r(n(Type.ROOT.list(), null)), t(ENDOFFILE)), //
                    Type.ROOT.list().list(Type.TOP_GROUP), (t, a) -> {
                        ListNode roots = new ListNode(List.of(), Type.ROOT.list());
                        for (Object e : a) {
                            roots = new ListNode(List.of(), roots, (ListNode) e);
                        }
                        return roots.setAstElements(roots.astElements().add(t.last()));
                    }));

            List<TokenType> tokenTypes = List.of(TokenType.NAME, TokenType.OPERATOR, TokenType.STRING, //
                    TokenType.SEMICOLON, TokenType.SINGLEQUOTE, TokenType.NEWLINE, TokenType.ENDOFFILE);
            List<Pattern> tokenTypePatternsNoComma = tokenTypes.map(tt -> (Pattern) t(tt)).asList();
            List<Pattern> elementPatternsNoComma = tokenTypePatternsNoComma.add(n(Type.PATTERN, Integer.MAX_VALUE)).add(n(Type.TYPE(), Integer.MAX_VALUE));
            RepetitionPattern patternRepetitionNoComma = r(a(elementPatternsNoComma.toArray(i -> new Pattern[i])));
            List<Pattern> elementPatterns = elementPatternsNoComma.prepend(t(TokenType.COMMA));
            RepetitionPattern patternRepetition = r(a(elementPatterns.toArray(i -> new Pattern[i])));

            register(Functor.of(s(t("<(>"), patternRepetition, r(s(t("<|>"), patternRepetition)), t("<)>")), //
                    Type.PATTERN, (t1, a1) -> {
                        List<Pattern> options = List.of();
                        List<AstElement> option = null;
                        for (AstElement e : t1) {
                            if (e instanceof Token t && t.type() == TokenType.META_OPERATOR) {
                                if (option != null) {
                                    options = options.add(pattern(option));
                                }
                                option = List.of();
                            } else {
                                option = option.add(e);
                            }
                        }
                        return a(t1, options.toArray(i -> new Pattern[i]));
                    }));

            register(Functor.of(s(t("{"), patternRepetition, t("}")), //
                    Type.PATTERN, (t1, a1) -> s(t1, pattern(t1))));

            register(Functor.of(s(t("("), patternRepetition, t(")")), //
                    Type.PATTERN, (t1, a1) -> s(t1, pattern(t1))));

            register(Functor.of(s(t("["), patternRepetition, t("]")), //
                    Type.PATTERN, (t1, a1) -> s(t1, pattern(t1))));

            register(Functor.of(s(t("<{>"), patternRepetition, t("<}>")), //
                    Type.PATTERN, (t1, a1) -> r(t1, pattern(t1))));

            register(Functor.of(s(t("<[>"), patternRepetition, t("<]>")), //
                    Type.PATTERN, (t1, a1) -> o(t1, pattern(t1))));

            SequencePattern patternSequence = s(patternRepetitionNoComma, r(s(t("#"), a(t(TokenType.NUMBER)))), o(s(t("@"), t(TokenType.QNAME))));

            register(Functor.of(s(n(Type.TYPE(), null), t("::="), patternSequence, r(s(t(","), patternSequence)), t(NEWLINE)), Type.FUNCTOR, (t0, a1) -> {
                Type type = (Type) t0.get(0);
                ListNode roots = new ListNode(List.of(t0.get(0), t0.get(1)), Type.ROOT);
                for (List<AstElement> t1 : split(2, t0, e -> e instanceof Token t && (t.text().equals(",") || t.type() == NEWLINE))) {
                    List<List<AstElement>> split = split(0, t1, e -> e instanceof Token t && (t.text().equals("@") || t.text().equals("#")));
                    Pattern pattern = pattern(split.first());
                    Constructor<?> constructor = null;
                    List<Integer> precedence = List.of();
                    for (List<AstElement> t2 : split.skip(1)) {
                        Token t = (Token) t2.first();
                        if (t.type() == TokenType.NUMBER) {
                            precedence = precedence.add(Integer.parseInt(t.text()));
                        } else if (t.type() == TokenType.QNAME) {
                            try {
                                constructor = Class.forName(t.text()).getConstructor(Functor.class, List.class, Object[].class);
                            } catch (NoSuchMethodException | SecurityException | ClassNotFoundException e1) {
                                throw new ParseException(e1, "Exception during constructor of new Node", t);
                            }
                        }
                    }
                    if (!precedence.isEmpty()) {
                        pattern = pattern.setPresedence(precedence, new int[]{0});
                    }
                    t1 = t1.append(t1.last().lastToken().next());
                    Functor functor = new Functor(t1, pattern, type, constructor);
                    roots = new ListNode(List.of(), roots, functor);
                }
                return roots;
            }));

            register(Functor.of(s(t(TokenType.TYPE), t("::"), n(Type.TYPE(), null), r(s(t(","), n(Type.TYPE(), null))), o(s(t("#"), t(TokenType.NAME))), t(NEWLINE)), //
                    Type.TYPE(), (t, a) -> {
                        String name = (String) a[0];
                        name = name.substring(1, name.length() - 1);
                        Set<Type> supers = Set.of();
                        for (int i = 1; i < a.length && a[i] instanceof Type; i++) {
                            supers = supers.add((Type) a[i]);
                        }
                        String group = a[a.length - 1] instanceof String ? (String) a[a.length - 1] : Type.DEFAULT_GROUP;
                        Type type = new Type(t, name, supers, group);
                        CURRENT.get().addType(type);
                        return new ListNode(List.of(), Type.ROOT, type);
                    }));

            //            register(Functor.of(s(t("("), n(Type.__, null), t(")")), Type.__, (t, a) -> {
            //                return (Node) a[0];
            //            }));

            //            // Functors
            //            register(InfixParselet.of(Type.ROOT, Type.TYPE(), "::=", SIGNATURE.list(), 10, (l, t, r) -> {
            //                ListNode list = new ListNode(Token.EMPTY, Type.FUNCTOR);
            //                KnowledgeBase current = KnowledgeBase.CURRENT.get();
            //                for (Node rr : ((ListNode) r).elements()) {
            //                    list = new ListNode(Token.EMPTY, list, current.createFunctor((Type) l, Token.concat(l, t, rr), rr));
            //                }
            //                return list;
            //            }));
            //            register(CallWithArgs.of(SIGNATURE, TokenType.NAME, (t, l) -> {
            //                return new Node(SIGNATURE, Token.concat(t, l), t.text(), l);
            //            }, Type.TYPE().list()));
            //            register(AtomicParselet.of(SIGNATURE, TokenType.NAME, t -> {
            //                return new Node(SIGNATURE, t.singleton(), t.text());
            //            }));
            //            register(AtomicParselet.of(SIGNATURE, TokenType.TYPE, t -> {
            //                Type type = type(t);
            //                return type.tokenType() != null ? new Node(SIGNATURE, t.singleton(), type) : type.setTokens(t.singleton());
            //            }));
            //            register(PrefixParselet.of(SIGNATURE, TokenType.OPERATOR, TokenType.TYPE, Type.TYPE(), 50, (t, r) -> {
            //                return new Node(SIGNATURE, Token.concat(t, r), t.text(), r);
            //            }));
            //            register(PrefixParselet.of(SIGNATURE, TokenType.NAME, TokenType.TYPE, Type.TYPE(), 50, (t, r) -> {
            //                return new Node(SIGNATURE, Token.concat(t, r), t.text(), r);
            //            }));
            //            register(PostfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.OPERATOR, 50, (l, t) -> {
            //                return new Node(SIGNATURE, Token.concat(l, t), l, t.text());
            //            }));
            //            register(PostfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.NAME, 50, (l, t) -> {
            //                return new Node(SIGNATURE, Token.concat(l, t), l, t.text());
            //            }));
            //            register(InfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.OPERATOR, TokenType.TYPE, Type.TYPE(), 50, (l, t, r) -> {
            //                return new Node(SIGNATURE, Token.concat(l, t, r), l, t.text(), r);
            //            }));
            //            register(InfixParselet.of(SIGNATURE, Type.TYPE(), TokenType.NAME, TokenType.TYPE, Type.TYPE(), 50, (l, t, r) -> {
            //                return new Node(SIGNATURE, Token.concat(l, t, r), l, t.text(), r);
            //            }));
            //            register(InfixParselet.of(SIGNATURE, "#", TokenType.NUMBER, PRECEDENCE, 50, (l, t, r) -> {
            //                return new Node(SIGNATURE, Token.concat(l, t, r), l, r.get(0));
            //            }));
            //            register(AtomicParselet.of(PRECEDENCE, TokenType.NUMBER, t -> {
            //                return new Node(PRECEDENCE, t.singleton(), Integer.parseInt(t.text()));
            //            }));
            //            register(InfixParselet.of(SIGNATURE, "@", TokenType.QNAME, NATIVE, 50, (l, t, r) -> {
            //                return new Node(SIGNATURE, Token.concat(l, t, r), l, r.get(0));
            //            }));
            //            register(AtomicParselet.of(NATIVE, TokenType.QNAME, t -> {
            //                try {
            //                    return new Node(NATIVE, t.singleton(), Class.forName(t.text()).getConstructor(Functor.class, Token[].class, Object[].class));
            //                } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
            //                    throw new ParseException(e, "Exception during constructor of new Node", t);
            //                }
            //            }));
            //
            //            // Variables
            //            register(PrefixParselet.of(Type.ROOT, TokenType.TYPE, TokenType.NAME, VAR_NAME.list(), 10, (t, l) -> {
            //                ListNode list = new ListNode(Token.EMPTY, Type.VARIABLE);
            //                Type type = type(t);
            //                KnowledgeBase current = KnowledgeBase.CURRENT.get();
            //                for (Node v : ((ListNode) l).elements()) {
            //                    Variable var = new Variable(v.tokens(), type, (String) v.get(0));
            //                    current.addVar(var);
            //                    list = new ListNode(Token.EMPTY, list, var);
            //                }
            //                return list;
            //            }));
            //            register(AtomicParselet.of(VAR_NAME, TokenType.NAME, t -> {
            //                String name = t.text();
            //                return new Terminal(VAR_NAME, t.singleton(), name);
            //            }));
            //
            //            // Rules
            //            register(InfixParselet.of(Type.ROOT, Type.PREDICATE, "<==", Type.PREDICATE.list(), 10, (l, t, r) -> {
            //                return rule(l, Token.concat(l, t), (ListNode) r, false);
            //            }));
            //            register(InfixParselet.of(Type.ROOT, Type.PREDICATE, "<==>", Type.PREDICATE.list(), 10, (l, t, r) -> {
            //                return rule(l, Token.concat(l, t), (ListNode) r, true);
            //            }));
            //
            //            // Expectations
            //            register(InfixParselet.of(Type.ROOT, Type.RESULT, "[", Type.PREDICATE.list(), 8, (l, t, r) -> {
            //                return new Node(FACTS, Token.concat(l, t, r), l, ((ListNode) r).elements());
            //            }));
            //            register(InfixParselet.of(Type.ROOT, FACTS, "[", Type.PREDICATE.list(), 8, (l, t, r) -> {
            //                return new Node(FALSEHOODS, Token.concat(l, t, r), l.get(0), l.get(1), ((ListNode) r).elements());
            //            }));
            //            register(PostfixParselet.of(Type.ROOT, FACTS, "]", 8, (l, t) -> {
            //                return l.setTokens(Token.concat(l, t));
            //            }));
            //            register(PostfixParselet.of(Type.ROOT, FALSEHOODS, "]", 8, (l, t) -> {
            //                Set<Predicate> facts = ((List<Predicate>) l.get(1)).asSet();
            //                boolean completeFacts = true;
            //                if (facts.contains(INCOMPLETE)) {
            //                    completeFacts = false;
            //                    facts = facts.remove(INCOMPLETE);
            //                }
            //                Set<Predicate> falsehoods = ((List<Predicate>) l.get(2)).asSet();
            //                boolean completeFalsehoods = true;
            //                if (falsehoods.contains(INCOMPLETE)) {
            //                    completeFalsehoods = false;
            //                    falsehoods = falsehoods.remove(INCOMPLETE);
            //                }
            //                InferResult expected = InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, Set.of());
            //                InferResult result = l.getVal(0, 1);
            //                if (!result.equals(expected) && !result.toString().equals(expected.toString())) {
            //                    throw new ParseException("Expected result " + expected + ", found " + result, Token.concat(l, t));
            //                }
            //                return l.getVal(0);
            //            }));
            //            register(AtomicParselet.of(Type.PREDICATE, "..", t -> {
            //                return INCOMPLETE;
            //            }));
            //
            //            // Queries
            //            register(PrefixParselet.of(Type.ROOT, "?", Type.PREDICATE, 10, (t, r) -> {
            //                Predicate predicate = (Predicate) r;
            //                predicate = predicate.setVariables(Predicate.literals(predicate.variables()));
            //                InferResult result = predicate.infer();
            //                return new Node(Type.RESULT, Token.concat(t, r), predicate, result);
            //            }));

            //            try {
            //                Parser.parse(KnowledgeBase.class);
            //            } catch (ParseException e) {
            //                throw new IllegalArgumentException(e);
            //            }
        });
        return this;
    }

    //    @SuppressWarnings("unused")
    //    private static Node rule(Node l, List<AstElement> elements, ListNode r, boolean symmetric) throws ParseException {
    //        ListNode list = new ListNode(Token.EMPTY, Type.RULE);
    //        KnowledgeBase current = KnowledgeBase.CURRENT.get();
    //        Predicate cons = (Predicate) l;
    //        Map<Variable, Object> consVars = cons.variables();
    //        for (Node c : r.elements()) {
    //            Predicate cond = (Predicate) c;
    //            Map<Variable, Object> condVars = cond.variables();
    //            Functor rel = current.relations.get().get(cons.functor());
    //            Map<Variable, Object> local = condVars.removeAllKey(consVars);
    //            if (symmetric && !local.isEmpty()) {
    //                throw new ParseException("No local variables allowed in condition of a symmetric rule. Found: " + local.get(0).getKey().name(), local.get(0).getKey().tokens());
    //            }
    //            if (rel != null) {
    //                Map<Variable, Object> vars = Predicate.literals(condVars.putAll(consVars));
    //                cons = cons.setFunctor(rel).setVariables(vars);
    //                cond = cond.setVariables(vars);
    //            } else if (!local.isEmpty()) {
    //                cond = cond.setVariables(Predicate.literals(local));
    //            }
    //            Rule rule = new Rule(Token.concat(tokens, c.tokens()), cons, cond, symmetric);
    //            current.addRule(rule);
    //            list = new ListNode(Token.EMPTY, list, rule);
    //        }
    //        return list;
    //    }
    //

    //
    //    // NB: ConstantValue because Intellij concludes that rel is always false, but it is not !!
    //    @SuppressWarnings({"unchecked", "ConstantValue", "DataFlowIssue"})
    //    private Node createFunctor(Type type, List<AstElement> elements, Node sig) throws ParseException {
    //        Constructor<? extends Node> constructor = sig.length() == 2 && sig.get(0) instanceof Node && sig.get(1) instanceof Constructor ? //
    //                (Constructor<? extends Node>) sig.get(1) : null;
    //        if (constructor != null) {
    //            sig = (Node) sig.get(0);
    //        }
    //        Integer precedence = sig.length() == 2 && sig.get(0) instanceof Node && sig.get(1) instanceof Integer ? //
    //                (Integer) sig.get(1) : null;
    //        if (precedence != null) {
    //            sig = (Node) sig.get(0);
    //        }
    //        boolean relation = Type.RELATION.isAssignableFrom(type);
    //        boolean predicate = relation || Type.PREDICATE.isAssignableFrom(type);
    //        if (sig.length() == 1 && sig.get(0) instanceof Type && ((Type) sig.get(0)).tokenType() != null) {
    //            // Literal
    //            if (precedence != null) {
    //                throw new ParseException("Precedence should not be defined " + sig, tokens);
    //            }
    //            type = type.literal();
    //            TokenType tokenType = ((Type) sig.get(0)).tokenType();
    //            String functorName = constructor != null ? constructor.getDeclaringClass().getSimpleName() : type.literal().name();
    //            Functor functor = new Functor(tokens, type, functorName, Type.STRING);
    //            addFunctor(functor, tokens, constructor);
    //            register(AtomicParselet.of(tokenType, tt -> createNode(predicate, tt.singleton(), constructor, functor, tt.text())));
    //            return functor;
    //        } else if (sig.length() == 1 && sig.get(0) instanceof String name) {
    //            // Constant
    //            if (precedence != null) {
    //                throw new ParseException("Precedence should not be defined " + sig, tokens);
    //            }
    //            type = type.literal();
    //            String functorName = constructor != null ? constructor.getDeclaringClass().getSimpleName() : type.literal().name();
    //            Functor functor = new Functor(tokens, type, functorName, n -> n.toString(0), 100, Type.STRING);
    //            addFunctor(functor, tokens, constructor);
    //            Node node = createNode(predicate, tokens, constructor, functor, name);
    //            register(AtomicParselet.of(name, tt -> node.setTokens(tt.singleton())));
    //            return functor;
    //        } else if (sig.length() == 2 && sig.get(0) instanceof String name && sig.get(1) instanceof List) {
    //            // CallWithArgs
    //            if (precedence != null) {
    //                throw new ParseException("Precedence should not be defined " + sig, tokens);
    //            }
    //            if (!predicate) {
    //                type = type.function();
    //            }
    //            List<Type> args = (List<Type>) sig.get(1);
    //            boolean rel = relation && !args.isEmpty() && args.noneMatch(Type::isLiteral);
    //            Functor functor = new Functor(tokens, rel ? Type.PREDICATE : type, name, args);
    //            addFunctor(functor, tokens, rel ? null : constructor);
    //            register(CallWithArgs.of(name, //
    //                    (tt, ll) -> createNode(predicate, Token.concat(tt, ll), rel ? null : constructor, functor, ll.toArray()), //
    //                    args.toArray(Type[]::new) //
    //            ));
    //            if (rel) {
    //                List<Type> litArgs = args.replaceAll(Type::literal);
    //                Functor relFunctor = new Functor(tokens, type, name, litArgs);
    //                relations.updateAndGet(map -> map.put(functor, relFunctor));
    //                addFunctor(relFunctor, tokens, constructor);
    //                register(CallWithArgs.of(name, (tt, ll) -> createNode(predicate, Token.concat(tt, ll), constructor, relFunctor, ll.toArray()), //
    //                        litArgs.toArray(Type[]::new)));
    //                Object[] nodVars = new Variable[args.size()];
    //                Object[] litVars = new Variable[args.size()];
    //                for (int i = 0; i < args.size(); i++) {
    //                    nodVars[i] = new Variable(args.get(i).tokens(), args.get(i), "n" + (i + 1));
    //                    litVars[i] = new Variable(litArgs.get(i).tokens(), litArgs.get(i), "l" + (i + 1));
    //                }
    //                Predicate conclusion = new Predicate(functor, tokens, nodVars);
    //                Predicate condition = (Predicate) createNode(true, tokens, constructor, relFunctor, litVars);
    //                for (int i = 0; i < args.size(); i++) {
    //                    Predicate eq = new Predicate(eqFunctor(), tokens, nodVars[i], litVars[i]);
    //                    condition = And.of(eq, condition);
    //                }
    //                addRule(new Rule(tokens, conclusion, condition, false));
    //            }
    //            return functor;
    //        } else if (sig.length() == 2 && sig.get(0) instanceof Type pre && sig.get(1) instanceof String oper) {
    //            // PostfixOperator
    //            if (precedence == null) {
    //                throw new ParseException("No precedence defined " + sig, tokens);
    //            }
    //            if (!predicate) {
    //                type = type.function();
    //            }
    //            boolean rel = relation && !pre.isLiteral();
    //            Functor functor = new Functor(tokens, rel ? Type.PREDICATE : type, oper, n -> n.toString(0) + oper, precedence, pre);
    //            addFunctor(functor, tokens, rel ? null : constructor);
    //            register(PostfixParselet.of(pre, oper, precedence, (ll, tt) -> createNode(predicate, Token.concat(ll, tt), rel ? null : constructor, functor, ll)));
    //            if (rel) {
    //                Type litPre = pre.literal();
    //                Functor relFunctor = new Functor(tokens, type, oper, n -> n.toString(0) + oper, precedence, litPre);
    //                relations.updateAndGet(map -> map.put(functor, relFunctor));
    //                addFunctor(relFunctor, tokens, constructor);
    //                //noinspection ConstantValue (predicate is always true)
    //                register(PostfixParselet.of(litPre, oper, precedence, (ll, tt) -> createNode(predicate, Token.concat(ll, tt), constructor, relFunctor, ll)));
    //                Variable nodVar = new Variable(pre.tokens(), pre, "n");
    //                Variable litVar = new Variable(litPre.tokens(), litPre, "l");
    //                Predicate conclusion = new Predicate(functor, tokens, nodVar);
    //                Predicate condition = (Predicate) createNode(true, tokens, constructor, relFunctor, litVar);
    //                Predicate eq = new Predicate(eqFunctor(), tokens, nodVar, litVar);
    //                condition = And.of(eq, condition);
    //                addRule(new Rule(tokens, conclusion, condition, false));
    //            }
    //            return functor;
    //        } else if (sig.length() == 2 && sig.get(0) instanceof String oper && sig.get(1) instanceof Type post) {
    //            // PrefixOperator
    //            if (precedence == null) {
    //                throw new ParseException("No precedence defined " + sig, tokens);
    //            }
    //            if (!predicate) {
    //                type = type.function();
    //            }
    //            boolean rel = relation && !post.isLiteral();
    //            Functor functor = new Functor(tokens, rel ? Type.PREDICATE : type, oper, n -> oper + n.toString(0), precedence, post);
    //            addFunctor(functor, tokens, rel ? null : constructor);
    //            register(PrefixParselet.of(oper, post, precedence, (tt, rr) -> createNode(predicate, Token.concat(tt, rr), rel ? null : constructor, functor, rr)));
    //            if (rel) {
    //                Type litPost = post.literal();
    //                Functor relFunctor = new Functor(tokens, type, oper, n -> oper + n.toString(0), precedence, litPost);
    //                relations.updateAndGet(map -> map.put(functor, relFunctor));
    //                addFunctor(relFunctor, tokens, constructor);
    //                //noinspection ConstantValue (predicate is always true)
    //                register(PrefixParselet.of(oper, litPost, precedence, (tt, rr) -> createNode(predicate, Token.concat(tt, rr), constructor, relFunctor, rr)));
    //                Variable nodVar = new Variable(post.tokens(), post, "n");
    //                Variable litVar = new Variable(litPost.tokens(), litPost, "l");
    //                Predicate conclusion = new Predicate(functor, tokens, nodVar);
    //                Predicate condition = (Predicate) createNode(true, tokens, constructor, relFunctor, litVar);
    //                Predicate eq = new Predicate(eqFunctor(), tokens, nodVar, litVar);
    //                condition = And.of(eq, condition);
    //                addRule(new Rule(tokens, conclusion, condition, false));
    //            }
    //            return functor;
    //        } else if (sig.length() == 3 && sig.get(0) instanceof Type pre && sig.get(1) instanceof String oper && sig.get(2) instanceof Type post) {
    //            // InfixOperator
    //            if (precedence == null) {
    //                throw new ParseException("No precedence defined " + sig, tokens);
    //            }
    //            if (!predicate) {
    //                type = type.function();
    //            }
    //            boolean rel = relation && !pre.isLiteral() && !post.isLiteral();
    //            Functor functor = new Functor(tokens, rel ? Type.PREDICATE : type, oper, n -> n.toString(0) + oper + n.toString(1), precedence, pre, post);
    //            addFunctor(functor, tokens, rel ? null : constructor);
    //            register(InfixParselet.of(pre, oper, post, precedence, (ll, tt, rr) -> createNode(predicate, Token.concat(ll.tokens(), Token.concat(tt, rr)), rel ? null : constructor, functor, ll, rr)));
    //            if (rel) {
    //                Type litPre = pre.literal();
    //                Type litPost = post.literal();
    //                Functor relFunctor = new Functor(tokens, type, oper, n -> n.toString(0) + oper + n.toString(1), precedence, litPre, litPost);
    //                relations.updateAndGet(map -> map.put(functor, relFunctor));
    //                addFunctor(relFunctor, tokens, constructor);
    //                //noinspection ConstantValue (predicate is always true)
    //                register(InfixParselet.of(litPre, oper, litPost, precedence, (ll, tt, rr) -> createNode(predicate, Token.concat(ll.tokens(), Token.concat(tt, rr)), constructor, relFunctor, ll, rr)));
    //                Variable nodVar0 = new Variable(pre.tokens(), pre, "n1");
    //                Variable litVar0 = new Variable(litPre.tokens(), litPre, "l1");
    //                Variable nodVar1 = new Variable(post.tokens(), post, "n2");
    //                Variable litVar1 = new Variable(litPost.tokens(), litPost, "l2");
    //                Predicate conclusion = new Predicate(functor, tokens, nodVar0, nodVar1);
    //                Predicate condition = (Predicate) createNode(true, tokens, constructor, relFunctor, litVar0, litVar1);
    //                Predicate eq0 = new Predicate(eqFunctor(), tokens, nodVar0, litVar0);
    //                Predicate eq1 = new Predicate(eqFunctor(), tokens, nodVar1, litVar1);
    //                condition = And.of(eq0, And.of(eq1, condition));
    //                addRule(new Rule(tokens, conclusion, condition, false));
    //            }
    //            return functor;
    //        } else {
    //            throw new ParseException("Invalid signature " + sig, tokens);
    //        }
    //    }

    private final AtomicReference<Set<Functor>>                         functors     = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, InferResult>>          facts        = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, Set<Rule>>>            rules        = new AtomicReference<>();
    //
    private final AtomicReference<Map<String, Patterns>>                prePatterns  = new AtomicReference<>();
    private final AtomicReference<Map<String, Patterns>>                postPatterns = new AtomicReference<>();
    //
    private final AtomicReference<Map<Functor, Functor>>                relations    = new AtomicReference<>();
    //
    private final AtomicInteger                                         depth        = new AtomicInteger();
    private final AtomicReference<QualifiedSet<Predicate, Inference>[]> memoization  = new AtomicReference<>();
    private final InferContext                                          context;
    private final KnowledgeBase                                         init;
    private boolean                                                     stopped;
    private boolean                                                     noInfer;

    public KnowledgeBase(KnowledgeBase init) {
        this.init = init;
        context = InferContext.of(KnowledgeBase.this, List.of(), Map.of(), false, TRACE_NELUMBO);
        init();
    }

    @SuppressWarnings("unchecked")
    public void init() {
        functors.set(init != null ? init.functors.get() : Set.of());
        facts.set(init != null ? init.facts.get() : Map.of());
        rules.set(init != null ? init.rules.get() : Map.of());
        prePatterns.set(init != null ? init.prePatterns.get() : Map.of());
        postPatterns.set(init != null ? init.postPatterns.get() : Map.of());
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

    public Set<Functor> functors() {
        return functors.get();
    }

    public Map<Predicate, Set<Rule>> rules() {
        return rules.get();
    }

    public Map<Predicate, InferResult> facts() {
        return facts.get();
    }

    public boolean noInfer() {
        return noInfer;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean noInfer(boolean b) {
        boolean old = noInfer;
        noInfer = b;
        return old;
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

    public void addFact(Predicate fact) {
        Functor functor = fact.functor();
        List<Type> args = functor.args();
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

    public void addFunctor(Functor functor, @SuppressWarnings("unused") Token[] tokens, Constructor<? extends Node> constructor) {
        if (constructor != null && !FUNCTOR_REGISTRATION.get().isEmpty()) {
            Class<? extends Node> cls = constructor.getDeclaringClass();
            Consumer<Functor> setter = FUNCTOR_REGISTRATION.get().get(cls);
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

    public void print(PrintStream stream, boolean withTokens) {
        System.out.printf("    %s%-96s%s%n", U.Colors.code(46), "functors", U.Colors.code(0));
        for (Functor e : functors()) {
            stream.printf("        %-20s ::= %s%n", e.resultType(), e);
            if (withTokens) {
                for (Token token : e.tokens()) {
                    stream.println("            " + token);
                }
            }
        }
        System.out.printf("    %s%-96s%s%n", U.Colors.code(46), "rules", U.Colors.code(0));
        Set<Rule> rules = rules().flatMap(Entry::getValue).asSet();
        for (Rule r : rules) {
            stream.println("        " + r);
            if (withTokens) {
                for (Token token : r.tokens()) {
                    stream.println("            " + token);
                }
            }
        }
        System.out.printf("    %s%-96s%s%n", U.Colors.code(46), "facts", U.Colors.code(0));
        for (Entry<Predicate, InferResult> e : facts()) {
            if (e.getValue().isTrueCC()) {
                stream.println("        " + e.getKey());
                if (withTokens) {
                    for (Token token : e.getKey().tokens()) {
                        stream.println("            " + token);
                    }
                }
            }
        }
    }

    public void register(Functor functor) {
        boolean post = functor.leftType() != null;
        String group = functor.resultType().group();
        Patterns patterns = functor.patterns();
        (post ? postPatterns : prePatterns).updateAndGet(m -> m.put(group, patterns.merge(m.get(group))));
    }

    public ParseResult preParse(Token token, String group, Node left, Parser parser) throws ParseException {
        Map<String, Patterns> patternsMap = (left != null ? postPatterns : prePatterns).get();
        Patterns patterns = patternsMap.get(group);
        return patterns != null ? preParse(token, left, parser, patterns) : null;
    }

    private ParseResult preParse(Token token, Node left, Parser parser, Patterns patterns) throws ParseException {
        if (left != null) {
            for (Type type : left.type().allsupers()) {
                Patterns found = patterns.get(type);
                if (found != null) {
                    ParseResult result = new ParseResult();
                    result.add(left);
                    return found.preParse(token, result, parser);
                }
            }
            return null;
        }
        return patterns.preParse(token, new ParseResult(), parser);
    }

}
