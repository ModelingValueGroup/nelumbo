//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

import static org.modelingvalue.nelumbo.ConstructionReason.bootstrapping;
import static org.modelingvalue.nelumbo.patterns.Pattern.*;
import static org.modelingvalue.nelumbo.syntax.TokenType.*;

import java.io.PrintStream;
import java.io.Serial;
import java.util.Optional;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.QualifiedSet;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.struct.impl.Struct2Impl;
import org.modelingvalue.collections.util.Context;
import org.modelingvalue.collections.util.ContextPool;
import org.modelingvalue.collections.util.ContextThread;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Import;
import org.modelingvalue.nelumbo.lang.Namespace;
import org.modelingvalue.nelumbo.lang.Transform;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.logic.And;
import org.modelingvalue.nelumbo.logic.ExistentialQuantifier;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.NIs;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.logic.Rule;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.SequencePattern;
import org.modelingvalue.nelumbo.patterns.TokenTextPattern;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseExceptionHandler;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class KnowledgeBase implements ParseExceptionHandler {

    private static final boolean TRACE_NELUMBO  = Boolean.getBoolean("TRACE_NELUMBO");
    public static final boolean  TRACE_SYNTATIC = Boolean.getBoolean("TRACE_SYNTATIC");
    //
    public static final Context<KnowledgeBase> CURRENT = Context.of();
    //
    private static final ContextPool                        POOL                = ContextThread.createPool()
            .setWorkerThreadName("nelumbo");
    private static final QualifiedSet<Predicate, Inference> EMPTY_MEMOIZ        = QualifiedSet.of(Inference::premise);
    private static final int                                MAX_LOGIC_MEMOIZ    = Integer.getInteger("MAX_LOGIC_MEMOIZ",
            10000);
    private static final int                                MAX_LOGIC_MEMOIZ_D4 = KnowledgeBase.MAX_LOGIC_MEMOIZ / 4;
    private static final int                                INITIAL_USAGE_COUNT = Integer
            .getInteger("INITIAL_USAGE_COUNT", 4);
    //
    private static final Pattern ROOTS = r(s(a(n(Type.ROOT.list()), n(Type.ROOT)), t(NEWLINE)), false, null);
    //
    private static final List<TokenType> PATTERN_TOKEN_TYPE_LIST = List.of(NAME, OPERATOR, SEMICOLON, SINGLEQUOTE,
            COMMA);
    //
    private static final Pattern PATTERNS = r(n(Type.PATTERN, 100), true, null);
    //
    public static final KnowledgeBase BASE = new KnowledgeBase(null).initBase();

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

    public static Set<Type> generalizations(Type type, Type top) {
        Set<Type> result = Set.of();
        for (Type g : type.supers()) {
            if (top.isAssignableFrom(g)) {
                result = result.add(g);
            }
        }
        return result;
    }

    public KnowledgeBase run(Runnable runnable) {
        return POOL.invoke(new LogicTask(runnable, this));
    }

    public Functor addType(Type type, ParseContext ctx) throws ParseException {
        Variable var = type.variable();
        Pattern pattern;
        if (var != null) {
            pattern = t(List.of(type), var);
        } else if (type.isCollection()) {
            pattern = s(List.of(type), t(type.rawName()), t("<"), n(Type.TYPE, Integer.MAX_VALUE), t(">"));
        } else {
            pattern = t(List.of(type), type.rawName());
        }
        Functor functor = Functor.of(List.of(type), pattern, Type.TYPE, null, Type.class, null);
        if (type.supers().contains(Type.NATIVE)) {
            if (parseContext().type(type.name()) == null) {
                addException(new ParseException("Native type " + type.name() + " is not defined in bootstrap.", type));
            }
        } else {
            functor.init(this, ctx, bootstrapping);
        }
        return functor;
    }

    public Functor addVariable(Variable var, ParseContext ctx) throws ParseException {
        Type literal = var.type().toLiteral();
        for (Pair<Functor, Transform> pair : literalTransforms.get().getOrDefault(literal, Set.of())) {
            pair.b().transform(pair.a().pattern(), t(List.of(var), var), null, this, ctx);
        }
        if (Type.TYPE.equals(var.type())) {
            return addType(new Type(var), ctx);
        } else {
            Type type = Type.BOOLEAN.isAssignableFrom(var.type()) ? var.type() : var.type().toVariable();
            Functor functor = Functor.of(List.of(var), t(List.of(var), var), type, Type.NAMESPACE, Variable.class,
                    null);
            functor.init(this, ctx, bootstrapping);
            return functor;
        }
    }

    private Functor addPattern(TokenType tokenType, ParseContext ctx) throws ParseException {
        Functor variable = Functor.of(t(tokenType), //
                Type.PATTERN, null, (elements, args, functor, pc) -> {
                    if (args[0] instanceof Variable var) {
                        return t(elements, var);
                    } else {
                        return t(elements, (String) args[0]);
                    }
                }, null);
        variable.init(this, ctx, bootstrapping);
        return variable;
    }

    public Pattern pattern(List<AstElement> elements) {
        List<Pattern> patterns = List.of();
        for (int i = 0; i < elements.size(); i++) {
            AstElement e = elements.get(i);
            if (e instanceof Token t) {
                Pattern tokenPattern = t(List.of(t), t.text());
                patterns = patterns.add(tokenPattern);
                elements = elements.replace(i, tokenPattern);
            } else {
                Pattern pattern = (Pattern) e;
                if (pattern instanceof SequencePattern sp) {
                    patterns = patterns.addAll(sp.elements());
                    elements = elements.removeIndex(i);
                    elements = elements.insertList(i, sp.astElements());
                    i = i - 1 + sp.astElements().size();
                } else {
                    patterns = patterns.add(pattern);
                }
            }
        }
        return patterns.size() > 1 ? s(elements, patterns.toArray(Pattern[]::new)) : patterns.first();
    }

    @SuppressWarnings({ "unchecked" })
    private KnowledgeBase initBase() {
        CURRENT.run(this, () -> {
            try {

                Functor.of(s(t(BEGINOFFILE), ROOTS, t(ENDOFFILE)), Type.NAMESPACE, null, Namespace.class, null)
                        .init(this, parseContext, bootstrapping);

                Functor.of(s(k("import"), r(r(t(NAME), true, t(".")), true, t(","))), Type.ROOT, null, Import.class,
                        null).init(this, parseContext, bootstrapping);

                Functor.of(
                        s(t(NAME), o(s(t("<"), n(Type.TYPE, 100), t(">"))), t("::"), r(n(Type.TYPE, 100), true, t(",")),
                                o(s(t("#"), t(NAME)))), //
                        Type.ROOT, null, Type.class, null).init(this, parseContext, bootstrapping);

                for (Type type : Type.predefined()) {
                    addType(type, parseContext);
                }

                for (TokenType tokenType : TokenType.values()) {
                    if (!tokenType.isNotMatched() && !tokenType.isSkip() && tokenType != TokenType.ERROR) {
                        addType(new Type(tokenType), parseContext);
                    }
                }

                for (TokenType tokenType : PATTERN_TOKEN_TYPE_LIST) {
                    addPattern(tokenType, parseContext);
                }

                Functor.of(t(STRING), //
                        Type.PATTERN, null, (elements, args, functor, pc) -> {
                            String text = (String) args[0];
                            text = text.substring(1, text.length() - 1);
                            return TokenType.of(text) == TokenType.NAME ? k(elements, text) : t(elements, text);
                        }, null).init(this, parseContext, bootstrapping);

                Functor.of(
                        s(t("<"), t("("), t(">"), r(PATTERNS, true, s(t("<"), t("|"), t(">"))), t("<"), t(")"), t(">")), //
                        Type.PATTERN, null, (elements, args, functor, pc) -> {
                            List<AstElement> options = List.of();
                            List<AstElement> list = List.of();
                            for (int i = 3; i < elements.size() - 2; i++) {
                                AstElement e = elements.get(i);
                                if (e instanceof Token t && t.text().startsWith("<")) {
                                    if (!list.isEmpty()) {
                                        options = options.add(pattern(list));
                                        list = List.of();
                                    }
                                    i += 2;
                                } else {
                                    list = list.add(e);
                                }
                            }
                            return a(elements, options.toArray(Pattern[]::new));
                        }, null).init(this, parseContext, bootstrapping);

                Functor.of(
                        s(t("<"), t("("), t(">"), PATTERNS, o(s(t("<"), t(","), t(">"), PATTERNS)), t("<"), t(")"),
                                a(t("*"), t("+")), t(">")), //
                        Type.PATTERN, null, (elements, args, functor, pc) -> {
                            Pattern repeated = null, separator = null;
                            List<AstElement> list = List.of();
                            for (int i = 3; i < elements.size() - 3; i++) {
                                AstElement e = elements.get(i);
                                if (e instanceof Token t && t.text().startsWith("<")) {
                                    if (!list.isEmpty()) {
                                        if (repeated == null) {
                                            repeated = pattern(list);
                                            list = List.of();
                                        } else {
                                            separator = pattern(list);
                                        }
                                    }
                                    i += 2;
                                } else {
                                    list = list.add(e);
                                }
                            }
                            boolean mandatory = args[2].equals("+");
                            return r(elements, repeated, mandatory, separator);
                        }, null).init(this, parseContext, bootstrapping);

                Functor.of(s(t("<"), t("("), t(">"), PATTERNS, t("<"), t(")"), t("?"), t(">")), //
                        Type.PATTERN, null, (elements, args, functor, pc) -> {
                            Pattern optional = pattern(elements.sublist(3, elements.size() - 4));
                            return o(elements, optional);
                        }, null).init(this, parseContext, bootstrapping);

                Functor.of(s(t(LEFT), PATTERNS, t(RIGHT)), //
                        Type.PATTERN, null, (elements, args, functor, pc) -> {
                            return s(elements, pattern(elements));
                        }, null).init(this, parseContext, bootstrapping);

                Functor.of(
                        s(t("<"), o(a(k("visible"), k("hidden"))), n(Type.TYPE, Integer.MAX_VALUE),
                                o(s(t("#"), t(NUMBER))), t(">")), //
                        Type.PATTERN, null, (elements, args, functor, pc) -> {
                            Boolean visible = null;
                            Optional<String> v = (Optional<String>) args[0];
                            if (v.isPresent()) {
                                visible = v.get().equals("visible");
                            }
                            Type type = (Type) args[1];
                            Integer precedence = null;
                            Optional<String> p = (Optional<String>) args[2];
                            if (p.isPresent()) {
                                precedence = Integer.parseInt(p.get());
                            }
                            TokenType tt = type.tokenType();
                            return tt != null ? t(elements, tt) : n(elements, type, precedence, visible);
                        }, null).init(this, parseContext, bootstrapping);

                Functor.of(s(t("<"), n(Type.VARIABLE, Integer.MAX_VALUE), o(s(t("#"), t(NUMBER))), t(">")), //
                        Type.PATTERN, null, (elements, args, functor, pc) -> {
                            Variable var = (Variable) args[0];
                            return t(elements, var);
                        }, null).init(this, parseContext, bootstrapping);

                Functor.of(
                        s(o(k("private")), n(Type.TYPE, 100), t("::="),
                                r(s(PATTERNS, o(s(t("#"), t(NUMBER))), o(s(t("@"), r(t(NAME), true, t("."))))), true,
                                        t(","))), //
                        Type.ROOT.list(), null, Functor.class, null).init(this, parseContext, bootstrapping);

            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        });
        return this;

    }

    public NList createFunctor(Type type, NList roots, List<AstElement> ast, Class<?> clazz, Pattern pattern,
            Type local, Integer prec, ParseContext ctx) throws ParseException {
        boolean toLiteral = false, function = false;
        List<Type> args = pattern.argTypes(List.of());
        Type e = type.isCollection() ? type.element() : null;
        if (!Type.ROOT.isAssignableFrom(type) && !Type.NAMESPACE.isAssignableFrom(type)
                && args.noneMatch(t -> Type.OBJECT.isAssignableFrom(t) && !t.equals(e))) {
            type = type.toLiteral();
        } else if (type.variable() == null) {
            if (!Type.TYPE.isAssignableFrom(type) && !Type.BOOLEAN.isAssignableFrom(type)
                    && !Type.ROOT.isAssignableFrom(type) && !Type.NAMESPACE.isAssignableFrom(type)) {
                type = type.toFunction();
                function = true;
            }
            if (!Type.TYPE.isAssignableFrom(type) && !Type.ROOT.isAssignableFrom(type)
                    && !Type.NAMESPACE.isAssignableFrom(type) && !Type.COLLECTION.isAssignableFrom(type) //
                    && args.noneMatch(t -> Type.OBJECT.equals(t.element()) //
                            || Type.BOOLEAN.isAssignableFrom(t.element()) //
                            || Type.VARIABLE.isAssignableFrom(t.element()) //
                            || Type.LITERAL.isAssignableFrom(t.element()))) {
                toLiteral = true;
            }
        }
        Type nodType = toLiteral && Type.FACT_TYPE.isAssignableFrom(type) ? Type.BOOLEAN : type;
        Functor nodFunctor = Functor.of(ast.prepend(pattern), pattern, nodType, local, toLiteral ? null : clazz, prec);
        nodFunctor.init(this, ctx, ConstructionReason.transforming);
        roots = new NList(List.of(), roots, nodFunctor);
        if (pattern instanceof TokenTextPattern && clazz != null) {
            nodFunctor.construct(List.of(), new Object[0], this, ctx).init(this, ctx, ConstructionReason.parsing);
        }
        if (toLiteral) {
            Pattern litPattern = pattern.setTypes(Type::toLiteral);
            Functor litFunctor = Functor.of(List.of(), litPattern, type, local, clazz, prec);
            litFunctor.init(this, ctx, ConstructionReason.transforming);
            roots = new NList(List.of(), roots, litFunctor);
            addLiteral(nodFunctor, litFunctor);
            // Implied Rule
            Object[] nodVars = new Object[args.size()];
            Object[] litVars = new Object[args.size()];
            List<Type> litArgs = args.replaceAll(Type::toLiteral);
            for (int v = 0; v < args.size(); v++) {
                nodVars[v] = new Variable(List.of(), false, args.get(v), "n" + (v + 1));
                litVars[v] = new Variable(List.of(), false, litArgs.get(v), "l" + (v + 1));
            }
            Node nodNode = nodFunctor.construct(List.of(), nodVars, this, ctx);
            Node litNode = litFunctor.construct(List.of(), litVars, this, ctx);
            Variable rigthVar = function ? new Variable(List.of(), false, type.nonFunction(), "r") : null;
            Predicate nodCons = function ? new NIs(List.of(), nodNode, rigthVar) : (Predicate) nodNode;
            Predicate litCond = function ? new NIs(List.of(), litNode, rigthVar) : (Predicate) litNode;
            for (int c = args.size() - 1; c >= 0; c--) {
                Predicate eq = new NIs(List.of(), (Variable) nodVars[c], (Variable) litVars[c]);
                litCond = And.of(eq, litCond);
            }
            List<Variable> localVars = List.of();
            for (int v = 0; v < args.size(); v++) {
                localVars = localVars.add((Variable) litVars[v]);
            }
            ExistentialQuantifier exists = new ExistentialQuantifier(List.of(), localVars, litCond);
            Rule rule = new Rule(List.of(), nodCons, exists);
            roots = new NList(List.of(), roots, rule);
        }
        return roots;
    }

    public void addLiteral(Functor nodFunctor, Functor litFunctor) {
        literalFunctors.updateAndGet(m -> m.put(nodFunctor, litFunctor));
    }

    private final static AtomicReference<Map<String, KnowledgeBase>> IMPORT_MAP       = new AtomicReference<>(Map.of());
    private final static AtomicReference<List<ImportResolver>>       IMPORT_RESOLVERS = new AtomicReference<>(
            List.of(new ResourceImportResolver()));

    /**
     * Registers an import resolver. Resolvers added later have higher priority.
     *
     * @param resolver the resolver to register
     */
    public static void registerResolver(ImportResolver resolver) {
        // Add at front for priority (newer resolvers checked first)
        IMPORT_RESOLVERS.updateAndGet(l -> l.insert(0, resolver));
    }

    /**
     * Unregisters an import resolver.
     *
     * @param resolver the resolver to unregister
     */
    public static void unregisterResolver(ImportResolver resolver) {
        IMPORT_RESOLVERS.updateAndGet(l -> l.remove(resolver));
    }

    private final AtomicReference<Set<Functor>>                             functors          = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, InferResult>>              facts             = new AtomicReference<>();
    private final AtomicReference<Set<Rule>>                                rules             = new AtomicReference<>();
    private final AtomicReference<Set<Transform>>                           transforms        = new AtomicReference<>();
    private final AtomicReference<Map<Type, Set<Pair<Functor, Transform>>>> literalTransforms = new AtomicReference<>();
    //
    private final MutableMap<String, Map<Type, ParseState>> prePatterns     = MutableMap.concurrent(Map.of());
    private final MutableMap<String, Map<Type, ParseState>> postPatterns    = MutableMap.concurrent(Map.of());
    private final MutableMap<String, Map<Type, Variable>>   hiddenVariables = MutableMap.concurrent(Map.of());
    //
    private final AtomicReference<Map<Functor, Functor>> literalFunctors = new AtomicReference<>();
    //
    private final AtomicReference<Set<String>> imported = new AtomicReference<>();
    //
    private final AtomicReference<MatchState<Rule>>      ruleSignatures      = new AtomicReference<>();
    private final AtomicReference<MatchState<Transform>> transformSignatures = new AtomicReference<>();
    //
    private final AtomicReference<QualifiedSet<Predicate, Inference>[]> memoization = new AtomicReference<>();
    private final InferContext                                          context;
    private final ParseContext                                          parseContext;
    private final KnowledgeBase                                         init;

    private boolean               stopped;
    private ParseExceptionHandler exceptionHandler;

    public KnowledgeBase(KnowledgeBase init) {
        this.init = init;
        context = InferContext.of(KnowledgeBase.this, List.of(), Map.of(), false, false, TRACE_NELUMBO);
        parseContext = ParseContext.of(prePatterns, postPatterns, hiddenVariables);
        init();
    }

    @SuppressWarnings("unchecked")
    public void init() {
        functors.set(init != null ? init.functors.get() : Set.of());
        facts.set(init != null ? init.facts.get() : Map.of());
        rules.set(init != null ? init.rules.get() : Set.of());
        transforms.set(init != null ? init.transforms.get() : Set.of());
        literalTransforms.set(init != null ? init.literalTransforms.get() : Map.of());
        prePatterns.set(m -> init != null ? init.prePatterns.get() : Map.of());
        postPatterns.set(m -> init != null ? init.postPatterns.get() : Map.of());
        hiddenVariables.set(m -> init != null ? init.hiddenVariables.get() : Map.of());
        literalFunctors.set(init != null ? init.literalFunctors.get() : Map.of());
        ruleSignatures.set(init != null ? init.ruleSignatures.get() : MatchState.EMPTY);
        transformSignatures.set(init != null ? init.transformSignatures.get() : MatchState.EMPTY);
        imported.set(init != null ? init.imported.get() : Set.of());
        resetMemoization();
    }

    public void merge(KnowledgeBase kb, AstElement element) throws ParseException {
        try {
            functors.updateAndGet(s -> s.addAll(kb.functors.get()));
            facts.updateAndGet(s -> s.addAll(kb.facts.get()));
            rules.updateAndGet(s -> s.addAll(kb.rules.get()));
            transforms.updateAndGet(s -> s.addAll(kb.transforms.get()));
            literalTransforms.updateAndGet(s -> s.addAll(kb.literalTransforms.get()));
            prePatterns.set(s -> s.addAll(kb.prePatterns.get()));
            postPatterns.set(s -> s.addAll(kb.postPatterns.get()));
            hiddenVariables.set(s -> s.addAll(kb.hiddenVariables.get()));
            literalFunctors.updateAndGet(s -> s.addAll(kb.literalFunctors.get()));
            ruleSignatures.updateAndGet(s -> kb.ruleSignatures.get().merge(s));
            transformSignatures.updateAndGet(s -> kb.transformSignatures.get().merge(s));
            imported.updateAndGet(s -> s.addAll(kb.imported.get()));
            resetMemoization();
        } catch (Exception exc) {
            addException(new ParseException(exc.getMessage(), element));
        }
    }

    @SuppressWarnings("unchecked")
    private void resetMemoization() {
        memoization.set(init != null ? init.memoization.get()
                : new QualifiedSet[] { EMPTY_MEMOIZ, EMPTY_MEMOIZ, EMPTY_MEMOIZ });
    }

    public void setExceptionHandler(ParseExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public Functor literal(Functor functor) {
        return literalFunctors.get().get(functor);
    }

    @Override
    public void addException(ParseException exception) throws ParseException {
        if (exceptionHandler != null) {
            exceptionHandler.addException(exception);
        } else {
            throw exception;
        }
    }

    @Override
    public List<ParseException> exceptions() {
        return exceptionHandler.exceptions();
    }

    public void endParsing() {
        exceptionHandler = null;
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

    public Set<Rule> rules() {
        return rules.get();
    }

    public Map<Predicate, InferResult> facts() {
        return facts.get();
    }

    public Set<Transform> transforms() {
        return transforms.get();
    }

    public void memoization(Predicate predicate, InferResult result) {
        boolean known = result.cycles().isEmpty(); // && result.isComplete();
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

    public Rule addRule(Rule rule) {
        rules.updateAndGet(s -> s.add(rule));
        MatchState<Rule> state = rule.consequence().state(new MatchState<>(rule));
        ruleSignatures.updateAndGet(state::merge);
        resetMemoization();
        return rule;
    }

    public Set<Rule> getRules(Predicate predicate) {
        return ruleSignatures.get().match(predicate);
    }

    public Transform addTransform(Transform transform) {
        transforms.updateAndGet(s -> s.add(transform));
        Node source = transform.source();
        assert Type.ROOT.isAssignableFrom(source.type());
        MatchState<Transform> state = source.state(new MatchState<>(transform));
        transformSignatures.updateAndGet(state::merge);
        for (Functor functor : transform.literals()) {
            Type literal = functor.resultType();
            literalTransforms.updateAndGet(m -> m.put(literal, m.getOrDefault(literal, Set.of()).//
                    add(Pair.of(functor, transform))));
        }
        return transform;
    }

    public Set<Transform> getTransforms(Node root) {
        return transformSignatures.get().match(root);
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
        resetMemoization();
    }

    private static Map<Predicate, InferResult> addFact(Map<Predicate, InferResult> map, Predicate fact,
            Predicate predicate, int i, Type cls) {
        Type type = predicate.getType(i);
        if (cls.isAssignableFrom(type)) {
            InferResult pre = map.get(predicate);
            map = map.put(predicate,
                    InferResult.factsCI(predicate, pre != null ? pre.facts().add(fact) : fact.singleton()));
            if (!cls.equals(type)) {
                for (Type gen : generalizations(type, cls)) {
                    map = addFact(map, fact, predicate.setType(i, gen), i, cls);
                }
            }
        }
        return map;
    }

    public InferResult getFacts(Predicate predicate, InferContext context) {
        InferResult result = facts.get().get(predicate);
        if (result != null) {
            result = result.cast(predicate);
        } else {
            result = predicate.isFullyBound() ? predicate.falsehoodCC() : InferResult.factsCI(predicate, Set.of());
        }
        if (context.trace()) {
            System.out.println(context.prefix() + "  " + predicate + " " + result);
        }
        return result;
    }

    public void print(PrintStream stream, boolean withTokens) {
        System.out.printf("    %s%-96s%s%n", U.colorCode(46), "functors", U.colorCode(0));
        for (Functor e : functors()) {
            stream.printf("        %-20s ::= %s%n", e.resultType(), e);
            if (withTokens) {
                for (Token token : e.tokens()) {
                    stream.println("            " + token);
                }
            }
        }
        System.out.printf("    %s%-96s%s%n", U.colorCode(46), "rules", U.colorCode(0));
        for (Rule r : rules()) {
            stream.println("        " + r);
            if (withTokens) {
                for (Token token : r.tokens()) {
                    stream.println("            " + token);
                }
            }
        }
        System.out.printf("    %s%-96s%s%n", U.colorCode(46), "facts", U.colorCode(0));
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

    public InferContext context() {
        return context;
    }

    public ParseContext parseContext() {
        return parseContext;
    }

    public Set<String> imported() {
        return imported.get();
    }

    public void doImport(String name, Import imp) throws ParseException {
        if (!imported.get().contains(name)) {
            KnowledgeBase kb = knowledgeBase(name, imp);
            if (kb != null) {
                merge(kb, imp);
                imported.updateAndGet(s -> s.add(name));
            }
        }
    }

    public static Map<String, KnowledgeBase> importMap() {
        return IMPORT_MAP.get();
    }

    public void register(Functor functor) {
        if (functor.local() == null) {
            functors.accumulateAndGet(Set.of(functor), Set::addAll);
        }
    }

    public KnowledgeBase knowledgeBase(String name, Import imp) throws ParseException {
        // Check cache first
        KnowledgeBase kb = IMPORT_MAP.get().get(name);
        if (kb != null) {
            return kb;
        }
        // Try each resolver in order
        for (ImportResolver resolver : IMPORT_RESOLVERS.get()) {
            if (!resolver.canHandle(name)) {
                continue;
            }

            ImportResolver.ImportResult result = resolver.resolve(name, imp);
            if (result != null && result.knowledgeBase() != null) {
                // Cache if result is cacheable
                if (result.cacheable()) {
                    IMPORT_MAP.updateAndGet(m -> m.put(name, result.knowledgeBase()));
                }
                return result.knowledgeBase();
            }
        }
        addException(new ParseException("Cannot resolve import: " + name, imp));
        return null;
    }

}
