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

import static org.modelingvalue.nelumbo.patterns.Pattern.*;
import static org.modelingvalue.nelumbo.syntax.TokenType.*;

import java.io.PrintStream;
import java.io.Serial;
import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;
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
import org.modelingvalue.nelumbo.logic.And;
import org.modelingvalue.nelumbo.logic.ExistentialQuantifier;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.logic.When;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.SequencePattern;
import org.modelingvalue.nelumbo.patterns.TokenTextPattern;
import org.modelingvalue.nelumbo.syntax.*;

@SuppressWarnings("DuplicatedCode")
public final class KnowledgeBase implements ParseExceptionHandler {

    private static final boolean                                                        TRACE_NELUMBO        = java.lang.Boolean.getBoolean("TRACE_NELUMBO");
    public static final boolean                                                         TRACE_SYNTATIC       = java.lang.Boolean.getBoolean("TRACE_SYNTATIC");
    //
    public static final Context<KnowledgeBase>                                          CURRENT              = Context.of();
    //
    private static final ContextPool                                                    POOL                 = ContextThread.createPool().setWorkerThreadName("nelumbo");
    private static final QualifiedSet<Predicate, Inference>                             EMPTY_MEMOIZ         = QualifiedSet.of(Inference::premise);
    private static final int                                                            MAX_LOGIC_MEMOIZ     = Integer.getInteger("MAX_LOGIC_MEMOIZ", 512);
    private static final int                                                            MAX_LOGIC_MEMOIZ_D4  = KnowledgeBase.MAX_LOGIC_MEMOIZ / 4;
    private static final int                                                            INITIAL_USAGE_COUNT  = Integer.getInteger("INITIAL_USAGE_COUNT", 4);
    private static final AtomicReference<Map<Class<? extends Node>, Consumer<Functor>>> FUNCTOR_REGISTRATION = new AtomicReference<>(Map.of());
    //
    private static final Pattern                                                        ROOTS                = r(s(a(n(Type.ROOT.list(), null), n(Type.ROOT, null)), t(NEWLINE)), false, null);
    private static final List<TokenType>                                                TOKEN_TYPES          = List.of(NAME, OPERATOR, STRING, SEMICOLON, SINGLEQUOTE);
    private static final List<Pattern>                                                  PATTERNS_NO_COMMA    = TOKEN_TYPES.map(Pattern::t).asList().add(n(Type.PATTERN, Integer.MAX_VALUE));
    private static final Pattern                                                        ALT_NO_COMMA         = a(PATTERNS_NO_COMMA.toArray(Pattern[]::new));
    private static final List<Pattern>                                                  PATTERNS             = PATTERNS_NO_COMMA.prepend(t(COMMA));
    private static final Pattern                                                        ALTERNATIVES         = a(PATTERNS.toArray(Pattern[]::new));
    private static final Pattern                                                        SEQUENCE             = r(ALTERNATIVES, true, null);
    private static final Pattern                                                        SEQ_NO_COMMA         = s(r(ALT_NO_COMMA, true, null),                                                  //
            r(s(t("#"), t(NUMBER)), false, null),                                                                                                                                              //
            o(s(t("@"), r(t(NAME), true, t(".")))));
    private static final Pattern                                                        CONDITION            = s(n(Type.PREDICATE, 0), o(s(t("if"), n(Type.PREDICATE, 0))));
    private static final Pattern                                                        SINGLE               = s(n(Type.VARIABLE, 100), t("="), n(Type.NODE, 100));
    private static final Pattern                                                        BINDING              = s(t("("), r(SINGLE, false, t(",")), t(")"));
    private static final Pattern                                                        ALTERNATIVE          = a(t(".."), BINDING);
    private static final Pattern                                                        PREDICTION           = r(ALTERNATIVE, false, t(","));
    //
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

    private Functor addType(Type type, boolean predefined) throws ParseException {
        Variable var = type.variable();
        Pattern pattern = var != null ? t(List.of(type), var) : t(List.of(type), type.toString());
        return Functor.of(List.of(type), pattern, //
                Type.TYPE, false, (elements, args, functor) -> {
                    Type result = ((Type) functor.astElements().first()).setAstElements(elements);
                    if (!predefined) {
                        result = result.setFunctor(functor);
                    }
                    return result;
                }).init(this);
    }

    private Functor addVariable(Variable var) throws ParseException {
        Type literal = var.type().literal();
        for (Pair<Functor, Transform> pair : literalTransforms.get().getOrDefault(literal, Set.of())) {
            pair.b().rewrite(pair.a().pattern(), t(List.of(var), var), this);
        }
        String string = var.type().equals(Type.TYPE) ? ("<" + var.name() + ">") : var.name();
        return Functor.of(List.of(var), t(List.of(var), string), //
                Type.VARIABLE, true, (elements, args, functor) -> {
                    Variable result = ((Variable) functor.astElements().first()).setAstElements(elements);
                    return result.setFunctor(functor);
                }).init(this);
    }

    private Pattern pattern(List<AstElement> elements) {
        List<Pattern> patterns = List.of();
        for (int i = 0; i < elements.size(); i++) {
            AstElement e = elements.get(i);
            if (!e.isMeta()) {
                if (e instanceof Token t) {
                    String text = t.text();
                    if (t.type() == STRING) {
                        text = text.substring(1, text.length() - 1);
                    }
                    Pattern tokenPattern = t(List.of(t), text);
                    patterns = patterns.add(tokenPattern);
                    elements = elements.replace(i, tokenPattern);
                } else if (e instanceof Variable v) {
                    Pattern variablePattern = v(v);
                    patterns = patterns.add(variablePattern);
                    elements = elements.replace(i, variablePattern);
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
        }
        return patterns.size() > 1 ? s(elements, patterns.toArray(Pattern[]::new)) : patterns.first();
    }

    private static Functor equalsFunctor;
    private static Functor ruleFunctor;

    public static Functor equalsFunctor() {
        return equalsFunctor;
    }

    @SuppressWarnings("unused")
    public static Functor ruleFunctor() {
        return ruleFunctor;
    }

    @SuppressWarnings({"unchecked", "CodeBlock2Expr"})
    private KnowledgeBase initBase() {
        CURRENT.run(this, () -> {
            try {

                for (Type type : Type.predefined()) {
                    addType(type, true);
                }

                for (TokenType tokenType : values()) {
                    if (!tokenType.skip()) {
                        addType(new Type(tokenType), true);
                    }
                }

                equalsFunctor = Functor.of(s(n(Type.NODE, 30), t("="), n(Type.NODE, 30)), // 
                        Type.PREDICATE, false).init(this);

                Functor.of(s(t(BEGINOFFILE), ROOTS, t(ENDOFFILE)), //
                        Type.ROOT.list(Type.TOP_GROUP), false, (elements, args, functor) -> {
                            ListNode roots = new ListNode(List.of(), Type.ROOT.list());
                            for (AstElement e1 : elements) {
                                if (e1 instanceof Node e1n) {
                                    if (e1n instanceof ListNode list) {
                                        for (AstElement e2 : list.astElements()) {
                                            if (e2 instanceof Node e2n) {
                                                roots = new ListNode(List.of(), roots, e2n);
                                            } else {
                                                roots = roots.setAstElements(roots.astElements().add(e2));
                                            }
                                        }
                                    } else {
                                        roots = new ListNode(List.of(), roots, e1n);
                                    }
                                } else {
                                    roots = roots.setAstElements(roots.astElements().add(e1));
                                }
                            }
                            return roots;
                        }).init(this);

                Functor.of(s(t("<(>"), r(SEQUENCE, true, t("<|>")), t("<)>")), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            List<Pattern> options = List.of();
                            List<AstElement> option = null;
                            for (AstElement e : elements) {
                                if (e.isMeta()) {
                                    if (option != null) {
                                        options = options.add(pattern(option));
                                    }
                                    option = List.of();
                                } else {
                                    assert option != null;
                                    option = option.add(e);
                                }
                            }
                            return a(elements, options.toArray(Pattern[]::new));
                        }).init(this);

                Functor.of(s(t("<(>"), SEQUENCE, o(s(t("<,>"), SEQUENCE)), a(t("<)*>"), t("<)+>"))), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            Pattern repeated = null, separator = null;
                            List<AstElement> list = List.of();
                            for (AstElement e : elements) {
                                if (e.isMeta()) {
                                    if (!list.isEmpty()) {
                                        if (repeated == null) {
                                            repeated = pattern(list);
                                            list = List.of();
                                        } else {
                                            separator = pattern(list);
                                        }
                                    }
                                } else {
                                    list = list.add(e);
                                }
                            }
                            boolean mandatory = args[2].equals("<)+>");
                            return r(elements, repeated, mandatory, separator);
                        }).init(this);

                Functor.of(s(t("<(>"), SEQUENCE, t("<)?>")), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            return o(elements, pattern(elements));
                        }).init(this);

                Functor.of(s(t(LEFT), SEQUENCE, t(RIGHT)), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            return s(elements, pattern(elements));
                        }).init(this);

                Functor.of(n(Type.TYPE, Integer.MAX_VALUE), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            Type type = args[0] instanceof Variable var ? new Type(var) : (Type) args[0];
                            TokenType tt = type.tokenType();
                            return tt != null ? t(elements, tt) : n(elements, type, null);
                        }).init(this);

                Functor.of(s(n(Type.TYPE, null), t("::="), r(SEQ_NO_COMMA, true, t(","))), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            AstElement node = elements.get(0);
                            Type type = node instanceof Variable var ? new Type(var) : (Type) node;
                            ListNode roots = new ListNode(elements.sublist(0, 2), Type.ROOT);
                            List<AstElement> pttrn = List.of(), ast = List.of();
                            Constructor<?> constructor = null;
                            List<Integer> precedence = List.of();
                            for (int i = 2; i <= elements.size(); i++) {
                                AstElement e = i < elements.size() ? elements.get(i) : null;
                                if (e != null) {
                                    ast = ast.add(e);
                                }
                                if (e == null || e instanceof Token) {
                                    Token t = (Token) e;
                                    if (t == null || t.text().equals(",")) {
                                        Pattern pattern = pattern(pttrn);
                                        if (!precedence.isEmpty()) {
                                            pattern = pattern.setPresedence(precedence, new int[1]);
                                        }
                                        roots = CURRENT.get().createFunctor(type, roots, ast, constructor, pattern);
                                        ast = pttrn = List.of();
                                        constructor = null;
                                        precedence = List.of();
                                    } else if (t.text().equals("#")) {
                                        t = t.next();
                                        ast = ast.add(t);
                                        i++;
                                        precedence = precedence.add(Integer.parseInt(t.text()));
                                    } else if (t.text().equals("@")) {
                                        StringBuilder qname = new StringBuilder();
                                        t = t.next();
                                        do {
                                            ast = ast.add(t);
                                            i++;
                                            qname.append(t.text());
                                            t = t.next();
                                        } while (t.text().equals(".") || t.type() == NAME);
                                        try {
                                            constructor = Class.forName(qname.toString()).getConstructor(Functor.class, List.class, Object[].class);
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
                            return roots.setAstElements(roots.astElements().add(node).add(elements.last()));
                        }).init(this);

                Functor.of(s(t(TYPE), t("::"), r(n(Type.TYPE, Integer.MAX_VALUE), true, t(",")), o(s(t("#"), t(NAME)))), //
                        Type.FUNCTOR, false, (elements, args, functor) -> {
                            Set<Type> supers = Set.of();
                            for (Type sup : (List<Type>) args[1]) {
                                supers = supers.add(sup);
                            }
                            String group = ((Optional<String>) args[2]).orElse(Type.DEFAULT_GROUP);
                            Type type;
                            if (args[0] instanceof Variable var) {
                                type = new Type(elements, var, group);
                            } else {
                                String name = (String) args[0];
                                name = name.substring(1, name.length() - 1);
                                type = new Type(elements, name, supers, group);
                            }
                            return CURRENT.get().addType(type, false);
                        }).init(this);

                Functor.of(s(t("import"), r(r(t(NAME), true, t(".")), true, t(","))), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            ListNode roots = new ListNode(elements.sublist(0, 1), Type.ROOT);
                            KnowledgeBase kb = CURRENT.get();
                            StringBuffer sb = new StringBuffer();
                            List<AstElement> el = List.of();
                            for (int i = 1; i <= elements.size(); i++) {
                                Token t = i < elements.size() ? (Token) elements.get(i) : null;
                                if (t == null || t.text().equals(",")) {
                                    Import ip = new Import(el, sb.toString());
                                    roots = new ListNode(List.of(), roots, ip);
                                    if (t != null) {
                                        roots = roots.setAstElements(roots.astElements().add(t));
                                    }
                                    el = List.of();
                                    sb = new StringBuffer();
                                    ip.init(kb);
                                } else {
                                    sb.append(t.text());
                                    el = el.add(t);
                                }
                            }
                            return roots;
                        }).init(this);

                Functor.of(s(n(Type.TYPE, null), r(t(NAME), true, t(","))), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            AstElement e = elements.get(0);
                            Type type = e instanceof Variable var ? new Type(var) : (Type) e;
                            ListNode roots = new ListNode(elements.sublist(0, 1), Type.ROOT);
                            for (int i = 1; i < elements.size(); i++) {
                                e = elements.get(i);
                                Token comma = null;
                                if (e instanceof Token t && t.text().equals(",")) {
                                    comma = t;
                                    e = elements.get(++i);
                                }
                                List<AstElement> el = List.of(e);
                                Variable var = e instanceof Variable v ? //
                                        new Variable(el, type, v) : //
                                        new Variable(el, type, ((Token) e).text());
                                if (comma != null) {
                                    roots = roots.setAstElements(roots.astElements().add(comma));
                                }
                                Functor varFun = CURRENT.get().addVariable(var);
                                roots = new ListNode(List.of(), roots, varFun);
                            }
                            return roots.setAstElements(roots.astElements().add(elements.last()));
                        }).init(this);

                ruleFunctor = Functor.of(s(n(Type.PREDICATE, 0), t("<=>"), r(CONDITION, true, t(","))), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            return CURRENT.get().createRules(functor, elements, args);
                        }).init(this);

                Functor.of(s(n(Type.PREDICATE, 0), t("?"), o(s(t("["), PREDICTION, t("]"), t("["), PREDICTION, t("]")))), //
                        Type.QUERY, false, (elements, args, functor) -> {
                            return new Query(functor, elements, args);
                        }).init(this);

                Functor.of(s(n(Type.PREDICATE, 0)), //
                        Type.FACT, false, (elements, args, functor) -> {
                            return new Fact(functor, elements, args);
                        }).init(this);

                Functor.of(s(n(Type.ROOT, null), t("::>"), t("{"), ROOTS, t("}")), //
                        Type.TRANSFORM, false, (elements, args, functor) -> {
                            Node source = (Node) args[0];
                            List<Node> targets = List.of();
                            for (Node arg : (List<Node>) args[1]) {
                                if (arg instanceof ListNode list) {
                                    targets = targets.addAll(list.elements());
                                } else {
                                    targets = targets.add(arg);
                                }
                            }
                            return new Transform(functor, elements, source, targets);
                        }).init(this);

                Functor.of(s(t("("), n(Type.NODE, 0), t(")")), //
                        Type.NODE, false, (elements, args, functor) -> {
                            Node node = (Node) args[0];
                            return node.setAstElements(node.astElements().prepend(elements.first()).append(elements.last()));
                        }).init(this);

            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        });
        return this;

    }

    @SuppressWarnings("ConstantValue")
    private ListNode createFunctor(Type type, ListNode roots, List<AstElement> ast, Constructor<?> constructor, Pattern pattern) throws ParseException {
        boolean toLiteral = false, function = false;
        List<Type> args = pattern.argTypes(List.of());
        if (args.noneMatch(Type.NODE::isAssignableFrom)) {
            if (!Type.PREDICATE.isAssignableFrom(type)) {
                type = type.literal();
            }
        } else {
            if (!Type.PREDICATE.isAssignableFrom(type) && !Type.ROOT.isAssignableFrom(type)) {
                type = type.function();
                function = true;
            }
            if (!Type.ROOT.isAssignableFrom(type) //
                    && !args.allMatch(t -> Type.NODE.equals(t.element())) //
                    && !args.allMatch(t -> Type.PREDICATE.isAssignableFrom(t.element()) || Type.VARIABLE.isAssignableFrom(t.element())) //
                    && args.noneMatch(t -> Type.LITERAL.isAssignableFrom(t.element()))) {
                toLiteral = true;
            }
        }
        Type nodType = toLiteral && Type.RELATION.isAssignableFrom(type) ? Type.PREDICATE : type;
        Functor nodFunctor = Functor.of(ast, pattern, nodType, false, toLiteral ? null : constructor).init(this);
        roots = new ListNode(List.of(), roots, nodFunctor);
        if (pattern instanceof TokenTextPattern && constructor != null) {
            nodFunctor.construct(List.of(), new Object[0], this);
        }
        if (toLiteral) {
            Pattern litPattern = pattern.setTypes(Type::literal);
            Functor litFunctor = Functor.of(ast, litPattern, type, false, constructor);
            register(litFunctor);
            roots = new ListNode(List.of(), roots, litFunctor);
            addLiteral(nodFunctor, litFunctor);
            // Implied Rule
            Variable[] nodVars = new Variable[args.size()];
            Variable[] litVars = new Variable[args.size()];
            List<Type> litArgs = args.replaceAll(Type::literal);
            for (int v = 0; v < args.size(); v++) {
                nodVars[v] = new Variable(List.of(), args.get(v), "n" + (v + 1));
                litVars[v] = new Variable(List.of(), litArgs.get(v), "l" + (v + 1));
            }
            Node nodNode = nodFunctor.construct(List.of(), nodVars, this);
            Node litNode = litFunctor.construct(List.of(), litVars, this);
            Variable rigthVar = function ? new Variable(List.of(), type.nonFunction(), "r") : null;
            Predicate nodCons = function ? new Predicate(equalsFunctor, List.of(), nodNode, rigthVar) : (Predicate) nodNode;
            Predicate litCond = function ? new Predicate(equalsFunctor, List.of(), litNode, rigthVar) : (Predicate) litNode;
            for (int c = args.size() - 1; c >= 0; c--) {
                Predicate eq = new Predicate(equalsFunctor, List.of(), nodVars[c], litVars[c]);
                litCond = And.of(eq, litCond);
            }
            ExistentialQuantifier exists = new ExistentialQuantifier(List.of(), List.of(litVars), litCond);
            Rule rule = new Rule(ruleFunctor, List.of(), nodCons, exists);
            roots = new ListNode(List.of(), roots, rule);
        }
        return roots;
    }

    public void addLiteral(Functor nodFunctor, Functor litFunctor) {
        literalFunctors.updateAndGet(m -> m.put(nodFunctor, litFunctor));
    }

    @SuppressWarnings("unchecked")
    private ListNode createRules(Functor functor, List<AstElement> elements, Object[] args) throws ParseException {
        ListNode roots = new ListNode(elements.sublist(0, 1), Type.ROOT);
        Predicate cons = Predicate.predicate((Node) args[0]);
        Functor consFunctor = cons.functor();
        Functor litFunctor = literalFunctors.get().get(consFunctor);
        if (Type.RELATION.isAssignableFrom((litFunctor != null ? litFunctor : consFunctor).resultType())) {
            addException(new ParseException("Rule consequence " + cons + " must be a Predicate, not a Relation", cons));
        }
        Map<Variable, Object> consVars = cons.getBinding();
        Node node = consFunctor.equals(equalsFunctor) ? (Node) cons.get(0) : cons;
        Map<Variable, Object> nodeVars = node == cons ? consVars : node.getBinding();
        Functor nodeFunctor = node.functor();
        Functor literalFunctor = literalFunctors.get().get(nodeFunctor);
        for (List<Object> condIf : (List<List<Object>>) args[1]) {
            Predicate cond = Predicate.predicate((Node) condIf.get(0));
            Predicate when = Predicate.predicate((Node) ((Optional<Object>) condIf.get(1)).orElse(null));
            Map<Variable, Object> condVars = cond.getBinding();
            Map<Variable, Object> whenVars = when != null ? when.getBinding() : null;
            Map<Variable, Object> nonConsVars = (when != null ? condVars.addAll(whenVars) : condVars).removeAllKey(consVars);
            if (!nonConsVars.isEmpty()) {
                Map<Variable, Object> localVars = nonConsVars.removeAllKey(cond.allLocalVars());
                if (when != null) {
                    localVars = localVars.removeAllKey(when.allLocalVars());
                }
                if (!localVars.isEmpty()) {
                    String message = "Rule has local variables " + nonConsVars.map(e -> e.getKey().toString()).reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) + " in condition";
                    addException(when != null ? new ParseException(message, cond, when) : new ParseException(message, cond));
                }
            }
            if (literalFunctor != null) {
                Map<Variable, Object> litVars = Predicate.literals(nodeVars.putAll(nonConsVars));
                cons = cons.setVariables(litVars);
                cond = cond.setVariables(litVars);
                if (when != null) {
                    when = when.setVariables(litVars);
                }
            } else if (!nonConsVars.isEmpty()) {
                Map<Variable, Object> litVars = Predicate.literals(nonConsVars);
                cond = cond.setVariables(litVars);
                if (when != null) {
                    when = when.setVariables(litVars);
                }
            }
            Rule rule = new Rule(functor, //
                    when != null ? List.of(cond, when) : List.of(cond), //
                    cons, //
                    when != null ? When.of(when, cond) : cond);
            roots = new ListNode(List.of(), roots, rule);
        }
        return roots.setAstElements(roots.astElements().add(elements.last()));
    }

    private final static AtomicReference<Map<String, KnowledgeBase>>        IMPORT_MAP          = new AtomicReference<>(Map.of());

    private final AtomicReference<Set<Functor>>                             functors            = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, InferResult>>              facts               = new AtomicReference<>();
    private final AtomicReference<Set<Rule>>                                rules               = new AtomicReference<>();
    private final AtomicReference<Set<Transform>>                           transforms          = new AtomicReference<>();
    private final AtomicReference<Map<Type, Set<Pair<Functor, Transform>>>> literalTransforms   = new AtomicReference<>();
    //
    private final AtomicReference<Map<String, ParseState>>                  prePatterns         = new AtomicReference<>();
    private final AtomicReference<Map<String, ParseState>>                  postPatterns        = new AtomicReference<>();
    //
    private final AtomicReference<Map<String, ParseState>>                  localPrePatterns    = new AtomicReference<>();
    private final AtomicReference<Map<String, ParseState>>                  localPostPatterns   = new AtomicReference<>();
    //
    private final AtomicReference<Map<Functor, Functor>>                    literalFunctors     = new AtomicReference<>();
    //
    private final AtomicReference<Set<String>>                              imported            = new AtomicReference<>();
    //
    private final AtomicReference<MatchState<Rule>>                         ruleSignatures      = new AtomicReference<>();
    private final AtomicReference<MatchState<Transform>>                    transformSignatures = new AtomicReference<>();
    //
    private final AtomicReference<QualifiedSet<Predicate, Inference>[]>     memoization         = new AtomicReference<>();
    private final InferContext                                              context;
    private final KnowledgeBase                                             init;

    private boolean                                                         stopped;
    private ParseExceptionHandler                                           exceptionHandler;

    public KnowledgeBase(KnowledgeBase init) {
        this.init = init;
        context = InferContext.of(KnowledgeBase.this, List.of(), Map.of(), false, false, TRACE_NELUMBO);
        init();
    }

    @SuppressWarnings("unchecked")
    public void init() {
        functors.set(init != null ? init.functors.get() : Set.of());
        facts.set(init != null ? init.facts.get() : Map.of());
        rules.set(init != null ? init.rules.get() : Set.of());
        transforms.set(init != null ? init.transforms.get() : Set.of());
        literalTransforms.set(init != null ? init.literalTransforms.get() : Map.of());
        prePatterns.set(init != null ? init.prePatterns.get() : Map.of());
        postPatterns.set(init != null ? init.postPatterns.get() : Map.of());
        localPrePatterns.set(prePatterns.get());
        localPostPatterns.set(postPatterns.get());
        literalFunctors.set(init != null ? init.literalFunctors.get() : Map.of());
        ruleSignatures.set(init != null ? init.ruleSignatures.get() : MatchState.EMPTY);
        transformSignatures.set(init != null ? init.transformSignatures.get() : MatchState.EMPTY);
        imported.set(init != null ? init.imported.get() : Set.of());
        resetMemoization();
        endParsing(false);
    }

    public void merge(KnowledgeBase kb, AstElement element) throws ParseException {
        try {
            functors.updateAndGet(s -> s.addAll(kb.functors.get()));
            facts.updateAndGet(s -> s.addAll(kb.facts.get()));
            rules.updateAndGet(s -> s.addAll(kb.rules.get()));
            transforms.updateAndGet(s -> s.addAll(kb.transforms.get()));
            literalTransforms.updateAndGet(s -> s.addAll(kb.literalTransforms.get()));
            prePatterns.updateAndGet(s -> s.addAll(kb.prePatterns.get()));
            postPatterns.updateAndGet(s -> s.addAll(kb.postPatterns.get()));
            localPrePatterns.updateAndGet(s -> s.addAll(kb.prePatterns.get()));
            localPostPatterns.updateAndGet(s -> s.addAll(kb.postPatterns.get()));
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
        memoization.set(init != null ? init.memoization.get() : new QualifiedSet[]{EMPTY_MEMOIZ, EMPTY_MEMOIZ, EMPTY_MEMOIZ});
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

    public void endParsing(boolean mutiple) {
        this.exceptionHandler = null;
        if (!mutiple) {
            localPrePatterns.set(prePatterns.get());
            localPostPatterns.set(postPatterns.get());
        }
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

    public void memoization(Predicate predicate, InferResult result) {
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

    @SuppressWarnings("UnusedReturnValue")
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

    @SuppressWarnings("UnusedReturnValue")
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
        assert Type.ROOT.isAssignableFrom(root.type());
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

    public InferResult getFacts(Predicate predicate, InferContext context) {
        InferResult result = facts.get().get(predicate);
        if (result != null) {
            result = result.cast(predicate);
        } else {
            result = predicate.isFullyBound() ? predicate.falsehoodCC() : InferResult.factsCI(Set.of());
        }
        if (context.trace()) {
            System.out.println(context.prefix() + "  " + predicate + " " + result);
        }
        return result;
    }

    public Functor register(Functor functor) throws ParseException {
        boolean post = functor.left() != null;
        Type type = functor.resultType();
        String group = Type.VARIABLE.isAssignableFrom(type) ? //
                functor.construct(List.of(), new Object[0], this).type().group() : //
                type.group();
        try {
            ParseState state = functor.start();
            if (!functor.local()) {
                (post ? postPatterns : prePatterns).updateAndGet(p -> p.put(group, state.merge(p.get(group))));
            }
            (post ? localPostPatterns : localPrePatterns).updateAndGet(l -> l.put(group, state.merge(l.get(group))));
        } catch (PatternMergeException pme) {
            addException(new ParseException(pme.getMessage(), functor));
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
        return functor;
    }

    public Variable variable(Token token, ParseContext ctx, Parser parser) throws ParseException {
        ParseState state = localPrePatterns.get().get(ctx.group());
        if (state != null) {
            ParseState found = state.transitions().get(token.text());
            if (found == null && token.type() == TokenType.TYPE) {
                found = state.transitions().get(token.text().substring(1, token.text().length() - 1));
            }
            if (found != null && found.functor() != null && found.functor().resultType() == Type.VARIABLE) {
                return (Variable) found.functor().construct(List.of(token), new Object[0], parser);
            }
        }
        return null;
    }

    public PatternResult preParse(Token token, ParseContext ctx, Node left, Parser parser) throws ParseException {
        ParseState state = (left != null ? localPostPatterns : localPrePatterns).get().get(ctx.group());
        return state != null ? preParse(token, left, parser, state, ctx) : null;
    }

    private PatternResult preParse(Token token, Node left, Parser parser, ParseState state, ParseContext ctx) throws ParseException {
        if (left != null) {
            for (Type sup : left.type().allSupers()) {
                ParseState found = state.transitions().get(sup);
                if (found != null) {
                    PatternResult result = new PatternResult(parser, ctx);
                    result.left(left);
                    return found.parse(token, result, Map.of(), true) ? result : null;
                }
            }
            return null;
        }
        PatternResult result = new PatternResult(parser, ctx);
        return state.parse(token, result, Map.of(), true) ? result : null;
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

    public Set<String> imported() {
        return imported.get();
    }

    public void doImport(String name, Import imp) throws ParseException {
        if (!imported.get().contains(name)) {
            merge(knowledgeBase(name), imp);
            imported.updateAndGet(s -> s.add(name));
        }
    }

    public static Map<String, KnowledgeBase> importMap() {
        return IMPORT_MAP.get();
    }

    public static KnowledgeBase knowledgeBase(String name) throws ParseException {
        KnowledgeBase kb = IMPORT_MAP.get().get(name);
        if (kb == null) {
            String path = "/" + name.replace('.', '/') + ".nl";
            ParseException[] exc = new ParseException[1];
            KnowledgeBase nw = BASE.run(() -> {
                try {
                    Parser.parse(KnowledgeBase.class, path);
                } catch (ParseException e) {
                    exc[0] = e;
                }
            });
            if (exc[0] != null) {
                throw exc[0];
            } else {
                IMPORT_MAP.updateAndGet(m -> m.put(name, nw));
            }
            kb = nw;
        }
        return kb;
    }

}
