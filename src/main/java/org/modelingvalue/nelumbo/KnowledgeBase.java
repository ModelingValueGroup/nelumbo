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
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.modelingvalue.collections.Collection;
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
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Import;
import org.modelingvalue.nelumbo.lang.Namespace;
import org.modelingvalue.nelumbo.lang.PatternPart;
import org.modelingvalue.nelumbo.lang.Transform;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.logic.InconsistencyException;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.logic.Rule;
import org.modelingvalue.nelumbo.patterns.AlternationPattern;
import org.modelingvalue.nelumbo.patterns.NodeTypePattern;
import org.modelingvalue.nelumbo.patterns.OptionalPattern;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.PatternPartPattern;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;
import org.modelingvalue.nelumbo.patterns.SequencePattern;
import org.modelingvalue.nelumbo.patterns.TokenTextPattern;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseExceptionHandler;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class KnowledgeBase implements ParseExceptionHandler {

    public static final boolean TRACE_SYNTATIC = Boolean.getBoolean("TRACE_SYNTATIC");
    //
    // Defaults of the evaluation settings of BASE (read once, at class load). Every
    // other knowledge base copies the settings of the one it is created from; change
    // them per knowledge base with the setters below.
    private static final boolean TRACE_NELUMBO       = Boolean.getBoolean("TRACE_NELUMBO");
    private static final boolean RANDOM_NELUMBO      = Boolean.getBoolean("RANDOM_NELUMBO");
    private static final boolean REVERSE_NELUMBO     = Boolean.getBoolean("REVERSE_NELUMBO");
    private static final int     MAX_LOGIC_DEPTH     = Integer.getInteger("MAX_LOGIC_DEPTH", 64);
    private static final int     MAX_LOGIC_MEMOIZ    = Integer.getInteger("MAX_LOGIC_MEMOIZ", 10000);
    private static final int     INITIAL_USAGE_COUNT = Integer.getInteger("INITIAL_USAGE_COUNT", 4);
    //
    private static final Context<KnowledgeBase> CURRENT = Context.of();
    //
    private static final ContextPool                        POOL         = ContextThread.createPool()
            .setWorkerThreadName("nelumbo");
    private static final QualifiedSet<Predicate, Inference> EMPTY_MEMOIZ = QualifiedSet.of(Inference::premise);
    //
    public static final KnowledgeBase BASE = new KnowledgeBase(null).initBase();

    private static class Inference extends Struct2Impl<Predicate, InferResult> {
        @Serial
        private static final long serialVersionUID = 1531759272582548244L;

        public int count;

        public Inference(Predicate predicate, InferResult result, int count) {
            super(predicate, result);
            this.count = count;
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

    public KnowledgeBase invoke(Runnable r) {
        return POOL.invoke(new LogicTask(Collection.sequential(r), this));
    }

    public static KnowledgeBase current() {
        return CURRENT.get();
    }

    public void run(Runnable r) {
        CURRENT.run(this, Collection.sequential(r));
    }

    public <V> V get(Supplier<V> s) {
        return CURRENT.get(this, () -> Collection.getSequential(s));
    }

    public Functor addType(Type type, ParseContext ctx) throws ParseException {
        Variable var = type.variable();
        Pattern pattern;
        if (var != null) {
            pattern = t(List.of(type), var);
        } else if (type.hasArguments()) {
            pattern = c(List.of(type), t(type.rawName()), t("<"), r(n(Type.TYPE, Integer.MAX_VALUE), true, t(",")),
                    t(">"));
        } else {
            pattern = t(List.of(type), type.rawName());
        }
        if (type.supersDeclaration().contains(Type.NATIVE)) {
            Type nat = parseContext().type(type.name());
            if (nat == null) {
                addException(new ParseException("Native type " + type.name() + " is not defined in bootstrap.", type));
            } else {
                type = nat.setAstElements(type.astElements());
            }
        }
        Functor functor = Functor.of(List.of(type), pattern, Type.TYPE, var != null ? Type.NAMESPACE : null, Type.class,
                null, false);
        functor.init(this, ctx, bootstrapping);
        return functor;
    }

    public Functor addVariable(Variable var, ParseContext ctx) throws ParseException {
        if (Type.TYPE.equals(var.type())) {
            return addType(new Type(var), ctx);
        } else {
            Type type = var.type().toVariable();
            Functor functor = Functor.of(List.of(var), t(List.of(var), var), type,
                    worldScopedVariables ? null : Type.NAMESPACE, Variable.class, null, false);
            functor.init(this, ctx, bootstrapping);
            return functor;
        }
    }

    private Functor addPattern(TokenType tokenType, ParseContext ctx) throws ParseException {
        Functor functor = Functor.of(t(tokenType), Type.PATTERN, null, TokenTextPattern.class, null);
        functor.init(this, ctx, bootstrapping);
        return functor;
    }

    private KnowledgeBase initBase() {
        CURRENT.run(this, () -> {
            try {

                Pattern ROOTS = r(s(a(n(Type.ROOT.toList()), n(Type.ROOT)), t(NEWLINE)), false, null);
                List<TokenType> PATTERN_TOKEN_TYPE_LIST = List.of(NAME, OPERATOR, SEMICOLON, SINGLEQUOTE, COMMA,
                        STRING);
                Pattern PATTERNS = r(n(Type.PATTERN, 100), true, null);
                Pattern QNAME = c(r(t(NAME), true, t(".")));

                Functor.of(s(t(BEGINOFFILE), ROOTS, t(ENDOFFILE)), Type.NAMESPACE, null, Namespace.class, null)
                        .init(this, parseContext, bootstrapping);

                Functor.of(s(k("import"), r(QNAME, true, t(","))), Type.ROOT, null, Import.class, 0).init(this,
                        parseContext, bootstrapping);

                Functor.of(
                        s(c(t(NAME), o(s(t("<"), r(n(Type.TYPE, 100), true, t(",")), t(">")))), t("::"),
                                r(n(Type.TYPE, 100), true, t(",")), o(s(t("#"), t(NAME)))),
                        Type.ROOT, null, Type.class, 0).init(this, parseContext, bootstrapping);

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

                Functor.of(s(c(t("<"), t("("), t(">")), r(PATTERNS, true, c(t("<"), t("|"), t(">"))),
                        c(t("<"), t(")"), t(">"))), Type.PATTERN, null, AlternationPattern.class, null)
                        .init(this, parseContext, bootstrapping);

                Functor.of(
                        s(c(t("<"), t("("), t(">")), PATTERNS, o(s(c(t("<"), t(","), t(">")), PATTERNS)),
                                c(t("<"), t(")"), a(t("*"), t("+")), t(">"))),
                        Type.PATTERN, null, RepetitionPattern.class, null).init(this, parseContext, bootstrapping);

                Functor.of(s(c(t("<"), t("("), t(">")), PATTERNS, c(t("<"), t(")"), t("?"), t(">"))), Type.PATTERN,
                        null, OptionalPattern.class, null).init(this, parseContext, bootstrapping);

                Functor.of(s(t(LEFT), PATTERNS, t(RIGHT)), Type.PATTERN, null, SequencePattern.class, null).init(this,
                        parseContext, bootstrapping);

                Functor.of(s(c(t("<"), t("["), t(">")), PATTERNS, c(t("<"), t("]"), t(">"))), Type.PATTERN, null,
                        SequencePattern.class, null).init(this, parseContext, bootstrapping);

                Functor.of(s(t("<"), o(a(k("visible"), k("hidden"))), o(k("one")), n(Type.TYPE, 100),
                        o(s(t("#"), t(NUMBER))), t(">")), Type.PATTERN, null, NodeTypePattern.class, null)
                        .init(this, parseContext, bootstrapping);

                Functor.of(s(t("<"), n(Type.PATTERN_PART, 100), t(">")), Type.PATTERN, null, PatternPartPattern.class,
                        null).init(this, parseContext, bootstrapping);

                Functor.of(
                        s(o(k("private")), n(Type.TYPE, 100), t("::="),
                                r(s(PATTERNS, o(s(t("#"), t(NUMBER))), o(s(t("@"), QNAME))), true, t(","))),
                        Type.ROOT, null, Functor.class, 0).init(this, parseContext, bootstrapping);

                Functor.of(s(k("pattern"), t(TokenType.NAME), t("::="), PATTERNS), Type.PATTERN_PART, null,
                        PatternPart.class, 0).init(this, parseContext, bootstrapping);

            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        });
        return this;

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

    private final AtomicReference<Set<Type>>                   types      = new AtomicReference<>();
    private final AtomicReference<Set<Functor>>                functors   = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, InferResult>> facts      = new AtomicReference<>();
    private final AtomicReference<Set<Rule>>                   rules      = new AtomicReference<>();
    private final AtomicReference<Set<Transform>>              transforms = new AtomicReference<>();
    //
    private final MutableMap<String, Map<Type, ParseState>> prePatterns     = MutableMap.concurrent(Map.of());
    private final MutableMap<String, Map<Type, ParseState>> postPatterns    = MutableMap.concurrent(Map.of());
    private final MutableMap<String, Map<Type, Variable>>   hiddenVariables = MutableMap.concurrent(Map.of());
    //
    private final AtomicReference<Set<String>> imported = new AtomicReference<>();
    //
    private final AtomicReference<MatchState<Rule>>      ruleSignatures      = new AtomicReference<>();
    private final AtomicReference<MatchState<Transform>> transformSignatures = new AtomicReference<>();
    //
    private final MutableMap<Pair<Node, Map<Variable, Type>>, Node> actualized    = MutableMap.concurrent(Map.of());
    private final MutableMap<Pair<Type, Type>, Type>                superSubTypes = MutableMap.concurrent(Map.of());
    //
    private final AtomicReference<QualifiedSet<Predicate, Inference>[]> memoization = new AtomicReference<>();
    private final ParseContext                                          parseContext;
    private final KnowledgeBase                                         init;

    private boolean               stopped;
    private ParseExceptionHandler exceptionHandler;
    private long                  deadlineNanos;
    private boolean               worldScopedVariables;
    // evaluation settings (trace lives in context)
    private InferContext          context;
    private boolean               randomOrder;
    private boolean               reverseOrder;
    private int                   maxLogicDepth;
    private int                   maxLogicMemoiz;
    private int                   initialUsageCount;

    public KnowledgeBase(KnowledgeBase init) {
        this.init = init;
        this.deadlineNanos = init != null ? init.deadlineNanos : 0;
        this.worldScopedVariables = init != null && init.worldScopedVariables;
        this.randomOrder = init != null ? init.randomOrder : RANDOM_NELUMBO;
        this.reverseOrder = init != null ? init.reverseOrder : REVERSE_NELUMBO;
        this.maxLogicDepth = init != null ? init.maxLogicDepth : MAX_LOGIC_DEPTH;
        this.maxLogicMemoiz = init != null ? init.maxLogicMemoiz : MAX_LOGIC_MEMOIZ;
        this.initialUsageCount = init != null ? init.initialUsageCount : INITIAL_USAGE_COUNT;
        context = InferContext.of(KnowledgeBase.this, List.of(), false, false,
                init != null ? init.context.trace() : TRACE_NELUMBO, null);
        parseContext = ParseContext.of(Type.DEFAULT_GROUP, Integer.MIN_VALUE, prePatterns, postPatterns,
                hiddenVariables);
        init();
    }

    // ---- evaluation settings: defaults from the system properties of the same
    // ---- name (see the constants above), inherited by child knowledge bases

    /** Rules in random order (RANDOM_NELUMBO); also randomly flips binary operands. */
    public void setRandomOrder(boolean randomOrder) {
        this.randomOrder = randomOrder;
    }

    public boolean isRandomOrder() {
        return randomOrder;
    }

    /** Rules in reverse order (REVERSE_NELUMBO); also flips binary operands. Wins over random. */
    public void setReverseOrder(boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    public boolean isReverseOrder() {
        return reverseOrder;
    }

    /** Inference stack depth after which a predicate reports an overflow (MAX_LOGIC_DEPTH). */
    public void setMaxLogicDepth(int maxLogicDepth) {
        this.maxLogicDepth = maxLogicDepth;
    }

    public int getMaxLogicDepth() {
        return maxLogicDepth;
    }

    /** Number of memoized inferences kept before cleanup starts (MAX_LOGIC_MEMOIZ). */
    public void setMaxLogicMemoiz(int maxLogicMemoiz) {
        this.maxLogicMemoiz = maxLogicMemoiz;
    }

    public int getMaxLogicMemoiz() {
        return maxLogicMemoiz;
    }

    /** Cleanup rounds a memoized inference survives (INITIAL_USAGE_COUNT); every hit adds one. */
    public void setInitialUsageCount(int initialUsageCount) {
        this.initialUsageCount = initialUsageCount;
    }

    public int getInitialUsageCount() {
        return initialUsageCount;
    }

    /** Inference tracing (TRACE_NELUMBO). */
    public void setTrace(boolean trace) {
        context = context.trace(trace);
    }

    public boolean isTrace() {
        return context.trace();
    }

    /**
     * Sets an absolute {@link System#nanoTime()} deadline for inference run in this
     * knowledge base (and the child knowledge bases it spawns via
     * {@link #invoke(Runnable)}). 0 disables the deadline. When set, inference
     * checks the clock and throws {@link NelumboTimeoutException} once it is past.
     */
    public void setDeadlineNanos(long deadlineNanos) {
        this.deadlineNanos = deadlineNanos;
    }

    public boolean isPastDeadline() {
        return deadlineNanos != 0 && System.nanoTime() - deadlineNanos >= 0;
    }

    /**
     * When set, variable declarations register world-scoped instead of
     * namespace-scoped, so they persist in this knowledge base (and the children it
     * spawns) beyond the declaring document. Used by REPL-style sessions; inherited
     * by child knowledge bases.
     */
    public void setWorldScopedVariables(boolean worldScopedVariables) {
        this.worldScopedVariables = worldScopedVariables;
    }

    @SuppressWarnings("unchecked")
    public void init() {
        types.set(init != null ? init.types.get() : Set.of());
        functors.set(init != null ? init.functors.get() : Set.of());
        facts.set(init != null ? init.facts.get() : Map.of());
        rules.set(init != null ? init.rules.get() : Set.of());
        transforms.set(init != null ? init.transforms.get() : Set.of());
        prePatterns.set(m -> init != null ? init.prePatterns.get() : Map.of());
        postPatterns.set(m -> init != null ? init.postPatterns.get() : Map.of());
        hiddenVariables.set(m -> init != null ? init.hiddenVariables.get() : Map.of());
        ruleSignatures.set(init != null ? init.ruleSignatures.get() : MatchState.EMPTY);
        transformSignatures.set(init != null ? init.transformSignatures.get() : MatchState.EMPTY);
        imported.set(init != null ? init.imported.get() : Set.of());
        actualized.set(m -> init != null ? init.actualized.get() : Map.of());
        superSubTypes.set(m -> init != null ? init.superSubTypes.get() : Map.of());
        resetMemoization();
    }

    public void merge(KnowledgeBase kb, AstElement element) throws ParseException {
        try {
            types.updateAndGet(s -> s.addAll(kb.types.get()));
            functors.updateAndGet(s -> s.addAll(kb.functors.get()));
            facts.updateAndGet(s -> s.addAll(kb.facts.get(), KnowledgeBase::mergeFacts));
            rules.updateAndGet(s -> s.addAll(kb.rules.get()));
            transforms.updateAndGet(s -> s.addAll(kb.transforms.get()));
            prePatterns.set(s -> s.addAll(kb.prePatterns.get()));
            postPatterns.set(s -> s.addAll(kb.postPatterns.get()));
            hiddenVariables.set(s -> s.addAll(kb.hiddenVariables.get()));
            ruleSignatures.updateAndGet(s -> kb.ruleSignatures.get().merge(s));
            transformSignatures.updateAndGet(s -> kb.transformSignatures.get().merge(s));
            imported.updateAndGet(s -> s.addAll(kb.imported.get()));
            actualized.set(s -> s.addAll(kb.actualized.get()));
            superSubTypes.set(s -> s.addAll(kb.superSubTypes.get()));
            resetMemoization();
        } catch (Exception exc) {
            addException(new ParseException(exc.getMessage(), element));
        }
    }

    // facts of two knowledge bases under the same index key (e.g. two modules
    // adding facts to one fact type)
    private static InferResult mergeFacts(InferResult a, InferResult b) {
        return a.facts().equals(b.facts()) ? a : InferResult.factsCI(a.predicate(), a.facts().addAll(b.facts()));
    }

    @SuppressWarnings("unchecked")
    private void resetMemoization() {
        memoization.set(init != null ? init.memoization.get()
                : new QualifiedSet[] { EMPTY_MEMOIZ, EMPTY_MEMOIZ, EMPTY_MEMOIZ });
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

    public Set<Type> types() {
        return types.get();
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
        QualifiedSet<Predicate, Inference>[] mem = memoization.updateAndGet(array -> {
            array = array.clone();
            array[0] = array[0].put(new Inference(predicate, result, initialUsageCount));
            for (Predicate fact : result.facts()) {
                array[0] = array[0].put(new Inference(fact, fact.factCC(), initialUsageCount));
            }
            for (Predicate falsehood : result.falsehoods()) {
                array[0] = array[0].put(new Inference(falsehood, falsehood.falsehoodCC(), initialUsageCount));
            }
            if (array[0].size() >= maxLogicMemoiz / 4) {
                array[2] = array[2].putAll(array[1]);
                array[1] = array[0];
                array[0] = EMPTY_MEMOIZ;
            }
            return array;
        });
        if (mem[2].size() > maxLogicMemoiz) {
            POOL.execute(this::cleanup);
        }
    }

    private void cleanup() {
        QualifiedSet<Predicate, Inference>[] mem = memoization.get();
        while (mem[2].size() > maxLogicMemoiz) {
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
        return ruleSignatures.get().match(predicate, true, this);
    }

    public Transform addTransform(Transform transform) {
        transforms.updateAndGet(s -> s.add(transform));
        Node source = transform.source();
        assert Type.ROOT.isAssignableFrom(source.type());
        MatchState<Transform> state = source.state(new MatchState<>(transform));
        transformSignatures.updateAndGet(state::merge);
        return transform;
    }

    public Set<Transform> getTransforms(Node root) {
        return transformSignatures.get().match(root, false, this);
    }

    public void addFact(Predicate fact) {
        List<int[]> args = fact.paths(n -> n.type().isLiteral());
        facts.updateAndGet(map -> {
            map = map.put(fact, fact.factCC());
            for (int i = 0; i < args.size(); i++) {
                map = addFact(map, fact, fact, args, 1, i);
            }
            return map;
        });
        resetMemoization();
    }

    private static Map<Predicate, InferResult> addFact(Map<Predicate, InferResult> map, Predicate fact, Predicate pred,
            List<int[]> args, int count, int outer) {
        if (args.size() > count) {
            int[] idx = args.get(outer);
            pred = pred.set(idx, Variable.DUMMY);
            InferResult result = map.get(pred);
            Set<Predicate> facts = result != null ? result.facts().add(fact) : fact.singleton();
            if (count == 1 && pred.unique() != null) {
                result = InferResult.factsCI(pred, facts);
                if (facts.size() > 1) {
                    throw new InconsistencyException(result);
                }
            } else {
                result = InferResult.factsII(pred, facts);
            }
            map = map.put(pred, result);
            for (int i = outer + 1; i < args.size(); i++) {
                map = addFact(map, fact, pred, args, count + 1, i);
            }
        }
        return map;
    }

    public InferResult getFacts(Predicate predicate, InferContext context) {
        List<int[]> vars = predicate.paths(n -> n instanceof Variable);
        Predicate pred = predicate;
        for (int[] i : vars) {
            pred = pred.set(i, Variable.DUMMY);
        }
        InferResult result = facts.get().get(pred);
        if (result != null) {
            result = result.cast(predicate);
            Set<Predicate> facts = result.facts();
            Set<Predicate> falsehoods = result.falsehoods();
            for (Predicate fact : facts) {
                for (int[] i : vars) {
                    Variable var = (Variable) predicate.getIdx(i);
                    if (fact.getIdx(i) instanceof Node node && !var.type().isAssignableFrom(node.type())) {
                        facts = facts.remove(fact);
                        break;
                    }
                }
            }
            return InferResult.of(predicate, facts, result.completeFacts(), falsehoods, result.completeFalsehoods(),
                    Set.of(), Map.of());
        }
        if (vars.isEmpty()) {
            for (int i : predicate.functor().unique()) {
                pred = predicate.set(i, Variable.DUMMY);
                result = facts.get().get(pred);
                if (result != null) {
                    return predicate.falsehoodCC();
                }
            }
        }
        return predicate.unknown();
    }

    public void print(PrintStream stream, boolean withTokens) {
        Node.runBaseString(() -> {
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
        });
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

    public void register(Type type) {
        types.accumulateAndGet(Set.of(type), Set::addAll);
    }

    public Type getType(Type type) {
        return types.get().get(type);
    }

    public Type isSuperSubType(Pair<Type, Type> superSubType) {
        return superSubTypes.get().get(superSubType);
    }

    public void register(Pair<Type, Type> superSubType, Type is) {
        superSubTypes.put(superSubType, is);
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <E extends Node> E actualize(E in, Map<Variable, Type> typeArgs,
            Function<Pair<E, Map<Variable, Type>>, E> function) {
        return (E) actualized.computeIfAbsent(Pair.of(in, typeArgs), (Function) function);
    }

}
