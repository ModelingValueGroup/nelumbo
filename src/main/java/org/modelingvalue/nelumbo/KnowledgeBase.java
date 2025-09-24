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
import org.modelingvalue.nelumbo.syntax.PatternMergeException;
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
                                                                                                                         if (r.consequence().equals(e.consequence()) &&                                                  //
                                                                                                                                 r.condition().contains(e.condition())) {
                                                                                                                             return l;
                                                                                                                         }
                                                                                                                     }
                                                                                                                     return l.add(e);
                                                                                                                 }
                                                                                                             };
    private static final AtomicReference<Map<Class<? extends Node>, Consumer<Functor>>> FUNCTOR_REGISTRATION = new AtomicReference<>(Map.of());

    private static final List<TokenType>                                                TOKEN_TYPES          = List.of(TokenType.NAME, TokenType.OPERATOR, TokenType.STRING, TokenType.SEMICOLON, TokenType.SINGLEQUOTE);
    private static final List<Pattern>                                                  PATTERNS_NO_COMMA    = TOKEN_TYPES.map(tt -> (Pattern) t(tt)).asList().add(n(Type.PATTERN, Integer.MAX_VALUE));
    private static final RepetitionPattern                                              REPETITION_NO_COMMA  = r(a(PATTERNS_NO_COMMA.toArray(i -> new Pattern[i])));
    private static final List<Pattern>                                                  PATTERNS             = PATTERNS_NO_COMMA.prepend(t(TokenType.COMMA));
    private static final RepetitionPattern                                              REPETITION           = r(a(PATTERNS.toArray(i -> new Pattern[i])));
    private static final SequencePattern                                                SEQUENCE             = s(REPETITION_NO_COMMA,                                                                                    //
            r(s(t("#"), a(t(TokenType.NUMBER)))),                                                                                                                                                                        //
            o(s(t("@"), t(TokenType.NAME), r(s(t("."), t(TokenType.NAME))))));

    private static final Pattern                                                        ALTERNATIVE          = a(t(".."), n(Type.PREDICATE, null));
    private static final Pattern                                                        PREDICTION           = o(s(ALTERNATIVE, r(s(t(","), ALTERNATIVE))));

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

    private void addType(Type type) {
        register(Functor.of(t(type.toString()), Type.TYPE(), (t, a) -> {
            return type.setAstElements(t);
        }));
    }

    private void addVariable(Variable var) {
        register(Functor.of(t(var.toString()), var.type(), (t, a) -> {
            return var.setAstElements(t);
        }));
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

            register(Functor.of(s(t("<(>"), REPETITION, r(s(t("<|>"), REPETITION)), t("<)>")), //
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

            register(Functor.of(s(t("{"), REPETITION, t("}")), //
                    Type.PATTERN, (t1, a1) -> s(t1, pattern(t1))));

            register(Functor.of(s(t("("), REPETITION, t(")")), //
                    Type.PATTERN, (t1, a1) -> s(t1, pattern(t1))));

            register(Functor.of(s(t("["), REPETITION, t("]")), //
                    Type.PATTERN, (t1, a1) -> s(t1, pattern(t1))));

            register(Functor.of(s(t("<{>"), REPETITION, t("<}>")), //
                    Type.PATTERN, (t1, a1) -> r(t1, pattern(t1))));

            register(Functor.of(s(t("<[>"), REPETITION, t("<]>")), //
                    Type.PATTERN, (t1, a1) -> o(t1, pattern(t1))));

            register(Functor.of(n(Type.TYPE(), Integer.MAX_VALUE), //
                    Type.PATTERN, (t1, a1) -> {
                        Type type = (Type) a1[0];
                        TokenType tt = type.tokenType();
                        return tt != null ? t(t1, tt) : n(t1, type, null);
                    }));

            register(Functor.of(s(n(Type.TYPE(), null), t("::="), SEQUENCE, r(s(t(","), SEQUENCE)), t(NEWLINE)), //
                    Type.ROOT.list(), (t1, a1) -> {
                        Type type = (Type) t1.get(0);
                        ListNode roots = new ListNode(t1.sublist(0, 2), Type.ROOT);
                        List<AstElement> pttrn = List.of(), ast = List.of();
                        Constructor<?> constructor = null;
                        List<Integer> precedence = List.of();
                        for (int i = 2; i < t1.size(); i++) {
                            AstElement e = t1.get(i);
                            ast = ast.add(e);
                            if (e instanceof Token t) {
                                if ((t.text().equals(",") || t.type() == NEWLINE)) {
                                    Pattern pattern = pattern(pttrn);
                                    if (!precedence.isEmpty()) {
                                        pattern = pattern.setPresedence(precedence, new int[1]);
                                    }
                                    Functor functor = new Functor(ast, pattern, type, constructor);
                                    roots = new ListNode(List.of(), roots, functor);
                                    CURRENT.get().register(functor);
                                    ast = pttrn = List.of();
                                    constructor = null;
                                    precedence = List.of();
                                } else if (t.text().equals("#")) {
                                    t = t.next();
                                    ast = ast.add(t);
                                    i++;
                                    precedence = precedence.add(Integer.parseInt(t.text()));
                                } else if (t.text().equals("@")) {
                                    String qname = "";
                                    t = t.next();
                                    do {
                                        ast = ast.add(t);
                                        i++;
                                        qname += t.text();
                                        t = t.next();
                                    } while (t.text().equals(".") || t.type() == TokenType.NAME);
                                    try {
                                        constructor = Class.forName(qname).getConstructor(Functor.class, List.class, Object[].class);
                                    } catch (NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
                                        throw new ParseException(ex, "Exception during finding class with Node constructor " + qname, t.next());
                                    }
                                } else {
                                    pttrn = pttrn.add(e);
                                }
                            } else {
                                pttrn = pttrn.add(e);
                            }
                        }
                        return roots;
                    }));

            register(Functor.of(s(t(TokenType.TYPE), t("::"), n(Type.TYPE(), null), r(s(t(","), n(Type.TYPE(), null))), o(s(t("#"), t(TokenType.NAME))), t(NEWLINE)), //
                    Type.ROOT.list(), (t, a) -> {
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

            register(Functor.of(s(n(Type.TYPE(), null), t(TokenType.NAME), r(s(t(","), t(TokenType.NAME))), t(NEWLINE)), //
                    Type.ROOT.list(), (t, a) -> {
                        Type type = (Type) t.get(0);
                        ListNode roots = new ListNode(t.sublist(0, 1), Type.ROOT);
                        for (int i = 1; i < t.size() - 1; i++) {
                            Token token = (Token) t.get(i);
                            Token comma = null;
                            if (token.text().equals(",")) {
                                comma = token;
                                token = (Token) t.get(++i);
                            }
                            Variable var = new Variable(List.of(token), type, token.text());
                            roots = new ListNode(List.of(), roots, var);
                            if (comma != null) {
                                roots.setAstElements(roots.astElements().add(comma));
                            }
                            CURRENT.get().addVariable(var);
                        }
                        return roots.setAstElements(roots.astElements().add(t.last()));
                    }));

            register(Functor.of(s(t("("), n(Type.NODE, null), t(")")), //
                    Type.NODE, (t, a) -> {
                        Node node = (Node) a[0];
                        return node.setAstElements(node.astElements().prepend(t.first()).append(t.last()));
                    }));

            register(Functor.of(s(n(Type.PREDICATE, null), t("<=="), n(Type.PREDICATE, 10), r(s(t(","), n(Type.PREDICATE, 10))), t(TokenType.NEWLINE)), //
                    Type.RULE, (t, a) -> CURRENT.get().rules(t, a, false)));

            register(Functor.of(s(n(Type.PREDICATE, null), t("<==>"), n(Type.PREDICATE, 10), r(s(t(","), n(Type.PREDICATE, 10))), t(TokenType.NEWLINE)), //
                    Type.RULE, (t, a) -> CURRENT.get().rules(t, a, true)));

            register(Functor.of(s(t("?"), n(Type.PREDICATE, null), o(s(t("["), PREDICTION, t("]"), t("["), PREDICTION, t("]"))), t(TokenType.NEWLINE)), Type.ROOT.list(), (t, a) -> {
                return new ListNode(t, Type.ROOT, new Node(Type.QUERY, t, a));
            }));

            //            try {
            //                Parser.parse(KnowledgeBase.class);
            //            } catch (ParseException e) {
            //                throw new IllegalArgumentException(e);
            //            }
        });
        return this;

    }

    private ListNode rules(List<AstElement> elements, Object[] args, boolean symmetric) {
        ListNode roots = new ListNode(elements, Type.ROOT);
        return roots;
    }

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
        boolean post = functor.left() != null;
        String group = functor.resultType().group();
        try {
            Patterns patterns = functor.patterns();
            (post ? postPatterns : prePatterns).updateAndGet(m -> m.put(group, patterns.merge(m.get(group))));
        } catch (PatternMergeException pme) {
            //            if (functor.firstToken() != null) {
            //                functor = functor.setError(new ParseException(pme.getMessage(), functor));
            //            } else {
            throw pme;
            //            }
        }
        Constructor<? extends Node> constructor = functor.constructor();
        if (constructor != null && !FUNCTOR_REGISTRATION.get().isEmpty()) {
            Class<? extends Node> cls = constructor.getDeclaringClass();
            Consumer<Functor> setter = FUNCTOR_REGISTRATION.get().get(cls);
            if (setter != null) {
                setter.accept(functor);
                FUNCTOR_REGISTRATION.updateAndGet(map -> map.remove(cls));
            }
        }
        functors.accumulateAndGet(Set.of(functor), Set::addAll);
    }

    public ParseResult preParse(Token token, String group, Node left, Parser parser) throws ParseException {
        Map<String, Patterns> patternsMap = (left != null ? postPatterns : prePatterns).get();
        Patterns patterns = patternsMap.get(group);
        return patterns != null ? preParse(token, left, parser, patterns) : null;
    }

    private ParseResult preParse(Token token, Node left, Parser parser, Patterns patterns) throws ParseException {
        if (left != null) {
            for (Type type : left.type().allsupers()) {
                Patterns found = patterns.map().get(type);
                if (found != null) {
                    ParseResult result = new ParseResult();
                    result.add(left);
                    return found.parse(token, result, parser, true);
                }
            }
            return null;
        }
        return patterns.parse(token, new ParseResult(), parser, true);
    }

}
