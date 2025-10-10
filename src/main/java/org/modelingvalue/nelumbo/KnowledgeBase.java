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
import org.modelingvalue.nelumbo.patterns.SequencePattern;
import org.modelingvalue.nelumbo.syntax.*;

@SuppressWarnings("DuplicatedCode")
public final class KnowledgeBase implements ParseExceptionHandler {

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
    private static final Pattern                                                        ALT_NO_COMMA         = a(PATTERNS_NO_COMMA.toArray(i -> new Pattern[i]));
    private static final List<Pattern>                                                  PATTERNS             = PATTERNS_NO_COMMA.prepend(t(TokenType.COMMA));
    private static final Pattern                                                        ALTERNATIVES         = a(PATTERNS.toArray(i -> new Pattern[i]));
    private static final Pattern                                                        SEQUENCE             = s(ALTERNATIVES, r(ALTERNATIVES));
    private static final Pattern                                                        SEQ_NO_COMMA         = s(ALT_NO_COMMA, r(ALT_NO_COMMA),                                                                          //
            r(s(t("#"), t(TokenType.NUMBER))),                                                                                                                                                                           //
            o(s(t("@"), t(TokenType.NAME), r(s(t("."), t(TokenType.NAME))))));
    private static final Pattern                                                        CONDITION            = n(Type.PREDICATE, 0);

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

    private Functor addType(Type type, boolean predefined) throws ParseException {
        return register(Functor.of(t(type.toString()), Type.TYPE(), false, (elements, args, functor) -> {
            Type result = type.setAstElements(elements);
            if (!predefined) {
                result = result.setFunctor(functor);
            }
            return result;
        }));
    }

    private Functor addVariable(Variable var, boolean predefined) throws ParseException {
        return register(Functor.of(t(var.toString()), var.type(), true, (elements, args, functor) -> {
            Variable result = var.setAstElements(elements);
            if (!predefined) {
                result = result.setFunctor(functor);
            }
            return result;
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

    private static Functor equalsFunctor;
    private static Functor ruleFunctor;

    @SuppressWarnings({"unchecked", "CodeBlock2Expr"})
    private KnowledgeBase initBase() {
        CURRENT.run(this, () -> {
            try {

                for (Type type : Type.predefined()) {
                    addType(type, true);
                }

                for (TokenType tokenType : TokenType.values()) {
                    if (!tokenType.skip()) {
                        addType(new Type(tokenType), true);
                    }
                }

                equalsFunctor = Functor.of(s(n(Type.NODE, 30), t("="), n(Type.NODE, 30)), // 
                        Type.PREDICATE, false);
                register(equalsFunctor);

                register(Functor.of(s(r(a(n(Type.ROOT.list(), null), n(Type.ROOT, null))), t(ENDOFFILE)), //
                        Type.ROOT.list(Type.TOP_GROUP), false, (elements, args, functor) -> {
                            ListNode roots = new ListNode(List.of(), Type.ROOT.list());
                            for (int i = 0; i < args.length; i++) {
                                Node e1 = (Node) args[i];
                                if (e1 instanceof ListNode) {
                                    for (Node e2 : ((ListNode) e1).elements()) {
                                        roots = new ListNode(List.of(), roots, (Node) e2);
                                    }
                                } else {
                                    roots = new ListNode(List.of(), roots, e1);
                                }
                            }
                            return roots.setAstElements(roots.astElements().add(elements.last()));
                        }));

                register(Functor.of(s(t("<(>"), SEQUENCE, r(s(t("<|>"), SEQUENCE)), t("<)>")), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            List<Pattern> options = List.of();
                            List<AstElement> option = null;
                            for (AstElement e : elements) {
                                if (e instanceof Token t && t.type() == TokenType.META_OPERATOR) {
                                    if (option != null) {
                                        options = options.add(pattern(option));
                                    }
                                    option = List.of();
                                } else {
                                    option = option.add(e);
                                }
                            }
                            return a(elements, options.toArray(i -> new Pattern[i]));
                        }));

                register(Functor.of(s(t(TokenType.LEFT), SEQUENCE, t(TokenType.RIGHT)), //
                        Type.PATTERN, false, (elements, args, functor) -> s(elements, pattern(elements))));

                register(Functor.of(s(t("<{>"), SEQUENCE, t("<}>")), //
                        Type.PATTERN, false, (elements, args, functor) -> r(elements, pattern(elements))));

                register(Functor.of(s(t("<[>"), SEQUENCE, t("<]>")), //
                        Type.PATTERN, false, (elements, args, functor) -> o(elements, pattern(elements))));

                register(Functor.of(n(Type.TYPE(), Integer.MAX_VALUE), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            Type type = (Type) args[0];
                            TokenType tt = type.tokenType();
                            return tt != null ? t(elements, tt) : n(elements, type, null);
                        }));

                register(Functor.of(s(n(Type.TYPE(), null), t("::="), SEQ_NO_COMMA, r(s(t(","), SEQ_NO_COMMA)), t(NEWLINE)), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            Type type = (Type) elements.get(0);
                            boolean relation = Type.RELATION.isAssignableFrom(type);
                            boolean predicate = relation || Type.PREDICATE.isAssignableFrom(type);
                            if (!predicate) {
                                type = type.function();
                            }
                            ListNode roots = new ListNode(elements.sublist(0, 2), Type.ROOT);
                            List<AstElement> pttrn = List.of(), ast = List.of();
                            Constructor<?> constructor = null;
                            List<Integer> precedence = List.of();
                            for (int i = 2; i < elements.size(); i++) {
                                AstElement e = elements.get(i);
                                ast = ast.add(e);
                                if (e instanceof Token t) {
                                    if ((t.text().equals(",") || t.type() == NEWLINE)) {
                                        Pattern pattern = pattern(pttrn);
                                        if (!precedence.isEmpty()) {
                                            pattern = pattern.setPresedence(precedence, new int[1]);
                                        }
                                        roots = CURRENT.get().createFunctor(type, relation, roots, ast, constructor, pattern);
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
                                            CURRENT.get().addException(new ParseException(ex, "Exception during finding class with Node constructor " + qname, t.next()));
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
                        Type.FUNCTOR, false, (elements, args, functor) -> {
                            String name = (String) args[0];
                            name = name.substring(1, name.length() - 1);
                            Set<Type> supers = Set.of();
                            for (int i = 1; i < args.length && args[i] instanceof Type; i++) {
                                supers = supers.add((Type) args[i]);
                            }
                            String group = args[args.length - 1] instanceof String ? (String) args[args.length - 1] : Type.DEFAULT_GROUP;
                            Type type = new Type(elements, name, supers, group);
                            return CURRENT.get().addType(type, false).setAstElements(elements);
                        }));

                register(Functor.of(s(n(Type.TYPE(), null), t(TokenType.NAME), r(s(t(","), t(TokenType.NAME))), t(NEWLINE)), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            Type type = (Type) elements.get(0);
                            ListNode roots = new ListNode(elements.sublist(0, 1), Type.ROOT);
                            for (int i = 1; i < elements.size() - 1; i++) {
                                Token token = (Token) elements.get(i);
                                Token comma = null;
                                if (token.text().equals(",")) {
                                    comma = token;
                                    token = (Token) elements.get(++i);
                                }
                                Variable var = new Variable(List.of(token), type, token.text());
                                if (comma != null) {
                                    roots.setAstElements(roots.astElements().add(comma));
                                }
                                Functor varFun = CURRENT.get().addVariable(var, false).setAstElements(List.of(token));
                                roots = new ListNode(List.of(), roots, varFun);
                            }
                            return roots.setAstElements(roots.astElements().add(elements.last()));
                        }));

                ruleFunctor = Functor.of(s(n(Type.PREDICATE, 0), t("<==>"), CONDITION, r(s(t(","), CONDITION)), t(TokenType.NEWLINE)), //
                        Type.ROOT.list(), false, (elements, args, functor) -> CURRENT.get().createRules(functor, elements, args));
                register(ruleFunctor);

                register(Functor.of(s(n(Type.PREDICATE, 0), t("?"), o(s(t("["), PREDICTION, t("]"), t("["), PREDICTION, t("]"))), t(TokenType.NEWLINE)), //
                        Type.QUERY, false, (elements, args, functor) -> new Query(functor, elements, args)));

                register(Functor.of(s(n(Type.PREDICATE, 0), t(TokenType.NEWLINE)), //
                        Type.FACT, false, (elements, args, functor) -> new Fact(functor, elements, args)));

                register(Functor.of(s(t("("), n(Type.NODE, 0), t(")")), //
                        Type.NODE, false, (elements, args, functor) -> {
                            Node node = (Node) args[0];
                            return node.setAstElements(node.astElements().prepend(elements.first()).append(elements.last()));
                        }));

                Parser.parse(KnowledgeBase.class);
            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        });
        return this;

    }

    private ListNode createFunctor(Type type, boolean relation, ListNode roots, List<AstElement> ast, Constructor<?> constructor, Pattern pattern) throws ParseException {
        List<Type> args = pattern.argTypes();
        boolean rel = relation && !args.isEmpty() && args.noneMatch(Type::isLiteral);
        Functor functor = Functor.of(ast, pattern, rel ? Type.PREDICATE : type, false, rel ? null : constructor);
        register(functor);
        roots = new ListNode(List.of(), roots, functor);
        if (rel) {
            List<Type> litArgs = args.replaceAll(Type::literal);
            pattern = pattern.setTypes(Type::literal);
            Functor relFunctor = Functor.of(List.of(), pattern, type, false, constructor);
            relations.updateAndGet(map -> map.put(functor, relFunctor));
            register(relFunctor);
            roots = new ListNode(List.of(), roots, relFunctor);
            // Implied Rule
            Object[] nodVars = new Variable[args.size()];
            Object[] litVars = new Variable[args.size()];
            for (int v = 0; v < args.size(); v++) {
                nodVars[v] = new Variable(List.of(), args.get(v), "n" + (v + 1));
                litVars[v] = new Variable(List.of(), litArgs.get(v), "l" + (v + 1));
            }
            Predicate conclusion = (Predicate) functor.construct(List.of(), nodVars, this);
            Predicate condition = (Predicate) relFunctor.construct(List.of(), litVars, this);
            for (int c = 0; c < args.size(); c++) {
                Predicate eq = new Predicate(equalsFunctor, List.of(), nodVars[c], litVars[c]);
                condition = And.of(eq, condition);
            }
            Rule rule = new Rule(ruleFunctor, List.of(), conclusion, condition);
            roots = new ListNode(List.of(), roots, rule);
        }
        return roots;
    }

    @SuppressWarnings("unchecked")
    private ListNode createRules(Functor funtor, List<AstElement> elements, Object[] args) throws ParseException {
        ListNode roots = new ListNode(elements.sublist(0, 1), Type.ROOT);
        Predicate cons = (Predicate) args[0];
        Map<Variable, Object> consVars = cons.variables();
        List<Predicate> nextList = (List<Predicate>) args[2];
        for (Predicate cond : nextList.prepend((Predicate) args[1])) {
            Map<Variable, Object> condVars = cond.variables();
            Functor rel = relations.get().get(cons.functor());
            Map<Variable, Object> local = condVars.removeAllKey(consVars);
            if (rel != null) {
                Map<Variable, Object> vars = Predicate.literals(condVars.putAll(consVars));
                cons = cons.setFunctor(rel).setVariables(vars);
                cond = cond.setVariables(vars);
            } else if (!local.isEmpty()) {
                cond = cond.setVariables(Predicate.literals(local));
            }
            Rule rule = new Rule(funtor, List.of(cond), cons, cond);
            roots = new ListNode(List.of(), roots, rule);
        }
        return roots.setAstElements(roots.astElements().add(elements.last()));
    }

    private final AtomicReference<Set<Functor>>                         functors          = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, InferResult>>          facts             = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, Set<Rule>>>            rules             = new AtomicReference<>();
    //
    private final AtomicReference<Map<String, ParseState>>              prePatterns       = new AtomicReference<>();
    private final AtomicReference<Map<String, ParseState>>              postPatterns      = new AtomicReference<>();
    //
    private final AtomicReference<Map<String, ParseState>>              localPrePatterns  = new AtomicReference<>();
    private final AtomicReference<Map<String, ParseState>>              localPostPatterns = new AtomicReference<>();
    //
    private final AtomicReference<Map<Functor, Functor>>                relations         = new AtomicReference<>();
    //
    private final AtomicInteger                                         depth             = new AtomicInteger();
    private final AtomicReference<QualifiedSet<Predicate, Inference>[]> memoization       = new AtomicReference<>();
    private final InferContext                                          context;
    private final KnowledgeBase                                         init;

    private boolean                                                     stopped;
    private ParseExceptionHandler                                       exceptionHandler;

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
        localPrePatterns.set(prePatterns.get());
        localPostPatterns.set(postPatterns.get());
        relations.set(init != null ? init.relations.get() : Map.of());
        memoization.set(init != null ? init.memoization.get() : new QualifiedSet[]{EMPTY_MEMOIZ, EMPTY_MEMOIZ, EMPTY_MEMOIZ});
        depth.set(init != null ? init.depth.get() : 0);
        endParsing();
    }

    public void setExceptionHandler(ParseExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void addException(ParseException exception) throws ParseException {
        if (exceptionHandler != null) {
            exceptionHandler.addException(exception);
        } else {
            throw exception;
        }
    }

    public void endParsing() {
        this.exceptionHandler = null;
        localPrePatterns.set(prePatterns.get());
        localPostPatterns.set(postPatterns.get());
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

    public void addRule(Rule rule) {
        Predicate signature = rule.consequence().signature(Integer.MAX_VALUE);
        rules.updateAndGet(m -> addRule(rule, signature, m));
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
        List<Type> args = functor.argTypes();
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

    public Functor register(Functor functor) throws ParseException {
        boolean post = functor.left() != null;
        Type type = functor.resultType();
        String group = type.group();
        try {
            ParseState state = functor.start();
            if (!functor.local()) {
                (post ? postPatterns : prePatterns).updateAndGet(p -> p.put(group, state.merge(p.get(group), false)));
            }
            (post ? localPostPatterns : localPrePatterns).updateAndGet(l -> l.put(group, state.merge(l.get(group), false)));
        } catch (PatternMergeException pme) {
            addException(new ParseException(pme.getMessage(), functor));
        }
        Constructor<? extends Node> constructor = functor.constructor();
        Class<? extends Node> cls = constructor != null ? constructor.getDeclaringClass() : type.clss();
        if (cls != null && !FUNCTOR_REGISTRATION.get().isEmpty()) {
            Consumer<Functor> setter = FUNCTOR_REGISTRATION.get().get(cls);
            if (setter != null) {
                setter.accept(functor);
                FUNCTOR_REGISTRATION.updateAndGet(map -> map.remove(cls));
            }
        }
        functors.accumulateAndGet(Set.of(functor), Set::addAll);
        return functor;
    }

    public PatternResult preParse(Token token, String group, Node left, Parser parser) throws ParseException {
        ParseState state = (left != null ? localPostPatterns : localPrePatterns).get().get(group);
        return state != null ? preParse(token, left, parser, state) : null;
    }

    private PatternResult preParse(Token token, Node left, Parser parser, ParseState state) throws ParseException {
        if (left != null) {
            for (Type sup : left.type().allsupers()) {
                ParseState found = state.transitions().get(sup);
                if (found != null) {
                    PatternResult result = new PatternResult(parser);
                    result.add(left);
                    result = found.parse(token, result, Map.of(), true);
                    if (result != null) {
                        left.setInput(sup);
                        return result;
                    }
                }
            }
            return null;
        }
        return state.parse(token, new PatternResult(parser), Map.of(), true);
    }

}
