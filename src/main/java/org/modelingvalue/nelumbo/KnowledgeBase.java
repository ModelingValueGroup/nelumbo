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
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.struct.impl.Struct2Impl;
import org.modelingvalue.collections.util.Context;
import org.modelingvalue.collections.util.ContextPool;
import org.modelingvalue.collections.util.ContextThread;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.logic.And;
import org.modelingvalue.nelumbo.logic.BooleanVariable;
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

    private static final boolean                                                        TRACE_NELUMBO        = Boolean.getBoolean("TRACE_NELUMBO");
    public static final boolean                                                         TRACE_SYNTATIC       = Boolean.getBoolean("TRACE_SYNTATIC");
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
    private static final Pattern                                                        ROOTS                = r(s(a(n(Type.ROOT.list(), Integer.MIN_VALUE), n(Type.ROOT, Integer.MIN_VALUE)), t(NEWLINE)), false, null);
    private static final List<TokenType>                                                TOKEN_TYPES          = List.of(NAME, OPERATOR, STRING, SEMICOLON, SINGLEQUOTE);
    private static final List<Pattern>                                                  PATTERNS_NO_COMMA    = TOKEN_TYPES.map(Pattern::t).asList().add(n(Type.PATTERN, Integer.MAX_VALUE));
    private static final Pattern                                                        ALT_NO_COMMA         = a(PATTERNS_NO_COMMA.toArray(Pattern[]::new));
    private static final List<Pattern>                                                  PATTERNS             = PATTERNS_NO_COMMA.prepend(t(COMMA));
    private static final Pattern                                                        ALTERNATIVES         = a(PATTERNS.toArray(Pattern[]::new));
    private static final Pattern                                                        SEQUENCE             = r(ALTERNATIVES, true, null);
    private static final Pattern                                                        SEQ_NO_COMMA         = s(r(ALT_NO_COMMA, true, null),                                                                            //
            o(s(t("#"), t(NUMBER))),                                                                                                                                                                                     //
            o(s(t("@"), r(t(NAME), true, t(".")))));
    private static final Pattern                                                        CONDITION            = s(n(Type.BOOLEAN, 0), o(s(k("if"), n(Type.BOOLEAN, 0))));
    private static final Pattern                                                        SINGLE               = s(n(Type.VARIABLE, 100), t("="), n(Type.OBJECT, 100));
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

    private Functor addType(Type type) throws ParseException {
        Variable var = type.variable();
        Pattern pattern;
        if (var != null) {
            pattern = t(List.of(type), var);
        } else if (type.isCollection()) {
            pattern = s(List.of(type), t(type.rawName()), t("<"), n(Type.TYPE, Integer.MAX_VALUE), t(">"));
        } else {
            pattern = t(List.of(type), type.rawName());
        }
        return Functor.of(List.of(type), pattern, //
                Type.TYPE, false, (elements, args, functor) -> {
                    Type result = ((Type) functor.astElements().first()).setAstElements(elements);
                    if (result.isCollection() && args[0] instanceof Type elem) {
                        result = result.setElement(elem);
                    }
                    return result.setFunctor(functor);
                }, null).init(this);
    }

    private Functor addVariable(Variable var) throws ParseException {
        Type literal = var.type().literal();
        for (Pair<Functor, Transform> pair : literalTransforms.get().getOrDefault(literal, Set.of())) {
            pair.b().rewrite(pair.a().pattern(), t(List.of(var), var), this);
        }
        if (Type.TYPE.equals(var.type())) {
            return addType(new Type(var));
        } else if (Type.BOOLEAN.isAssignableFrom(var.type())) {
            return Functor.of(List.of(var), t(List.of(var), var), //
                    var.type(), true, (elements, args, functor) -> {
                        Variable result = ((Variable) functor.astElements().first()).setAstElements(elements);
                        return new BooleanVariable(functor, elements, result);
                    }, null).init(this);
        } else {
            return Functor.of(List.of(var), t(List.of(var), var), //
                    Type.VARIABLE, true, (elements, args, functor) -> {
                        Variable result = ((Variable) functor.astElements().first()).setAstElements(elements);
                        return result.setFunctor(functor);
                    }, null).init(this);
        }
    }

    private Pattern pattern(List<AstElement> elements) {
        List<Pattern> patterns = List.of();
        for (int i = 0; i < elements.size(); i++) {
            AstElement e = elements.get(i);
            if (e instanceof Token t) {
                String text = t.text();
                if (t.type() == STRING) {
                    text = text.substring(1, text.length() - 1);
                }
                Pattern tokenPattern = t.type() == STRING && TokenType.of(text) == TokenType.NAME ? //
                        k(List.of(t), text) : t(List.of(t), text);
                patterns = patterns.add(tokenPattern);
                elements = elements.replace(i, tokenPattern);
            } else if (e instanceof Variable v) {
                Pattern variablePattern = v(List.of(v), v);
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
                    addType(type);
                }

                for (TokenType tokenType : TokenType.values()) {
                    if (!tokenType.isNotMatched() && !tokenType.isSkip()) {
                        addType(new Type(tokenType));
                    }
                }

                equalsFunctor = Functor.of(s(n(Type.OBJECT, 30), t("="), n(Type.OBJECT, 30)), //
                        Type.BOOLEAN, false, null).init(this);

                Functor.of(s(t(BEGINOFFILE), ROOTS, t(ENDOFFILE)), //
                        Type.ROOT.list(Type.TOP_GROUP), false, (elements, args, functor) -> {
                            List<Node> roots = List.of();
                            for (Object arg : args) {
                                roots = roots.add((Node) arg);
                            }
                            return new NList(Type.ROOT.list(), elements, roots);
                        }, null).init(this);

                Functor.of(s(t("<"), t("("), t(">"), r(SEQUENCE, true, s(t("<"), t("|"), t(">"))), t("<"), t(")"), t(">")), //
                        Type.PATTERN, false, (elements, args, functor) -> {
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
                        }, null).init(this);

                Functor.of(s(t("<"), t("("), t(">"), SEQUENCE, o(s(t("<"), t(","), t(">"), SEQUENCE)), t("<"), t(")"), a(t("*"), t("+")), t(">")), //
                        Type.PATTERN, false, (elements, args, functor) -> {
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
                        }, null).init(this);

                Functor.of(s(t("<"), t("("), t(">"), SEQUENCE, t("<"), t(")"), t("?"), t(">")), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            return o(elements, pattern(elements));
                        }, null).init(this);

                Functor.of(s(t(LEFT), SEQUENCE, t(RIGHT)), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            return s(elements, pattern(elements));
                        }, null).init(this);

                Functor.of(s(t("<"), n(Type.TYPE, Integer.MAX_VALUE), o(s(t("#"), t(NUMBER))), t(">")), //
                        Type.PATTERN, false, (elements, args, functor) -> {
                            Type type = (Type) args[0];
                            Integer precedence = null;
                            Optional<String> o = (Optional<String>) args[1];
                            if (o.isPresent()) {
                                precedence = Integer.parseInt(o.get());
                            }
                            TokenType tt = type.tokenType();
                            return tt != null ? t(elements, tt) : n(elements, type, precedence);
                        }, null).init(this);

                Functor.of(s(o(k("private")), n(Type.TYPE, Integer.MAX_VALUE), t("::="), r(SEQ_NO_COMMA, true, t(","))), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            boolean local = ((Optional<String>) args[0]).isPresent();
                            Type type = (Type) elements.get(local ? 1 : 0);
                            int start = local ? 3 : 2;
                            NList roots = new NList(elements.sublist(0, start), Type.ROOT);
                            List<AstElement> pttrn = List.of(), ast = List.of();
                            Constructor<?> constructor = null;
                            Integer precedence = null;
                            for (int i = start; i <= elements.size(); i++) {
                                AstElement e = i < elements.size() ? elements.get(i) : null;
                                if (e == null || e instanceof Token) {
                                    Token t = (Token) e;
                                    if (t == null || t.text().equals(",")) {
                                        Pattern pattern = pattern(pttrn);
                                        if (precedence != null) {
                                            pattern = pattern.setPresedence(precedence);
                                        }
                                        roots = CURRENT.get().createFunctor(type, roots, ast, constructor, pattern, local, precedence);
                                        if (t != null) {
                                            roots = roots.setAstElements(roots.astElements().add(t));
                                        }
                                        ast = pttrn = List.of();
                                        constructor = null;
                                        precedence = null;
                                    } else if (t.text().equals("#")) {
                                        ast = ast.add(t);
                                        t = t.next();
                                        ast = ast.add(t);
                                        i++;
                                        precedence = Integer.parseInt(t.text());
                                    } else if (t.text().equals("@")) {
                                        ast = ast.add(t);
                                        StringBuilder qname = new StringBuilder();
                                        t = t.next();
                                        do {
                                            ast = ast.add(t);
                                            i++;
                                            qname.append(t.text());
                                            t = t.next();
                                        } while (t.text().equals(".") || t.type() == NAME);
                                        String className = qname.toString();
                                        try {
                                            constructor = NelumboConstructor.Finder.find(className);
                                        } catch (SecurityException | ClassNotFoundException | NoSuchMethodException ex) {
                                            CURRENT.get().addException(new ParseException(ex, ex + " during finding class with Node constructor " + className, t.next()));
                                        }
                                    } else {
                                        pttrn = pttrn.add(e);
                                    }
                                } else {
                                    pttrn = pttrn.add(e);
                                }
                            }
                            return roots;
                        }, null).init(this);

                Functor.of(s(t(NAME), o(s(t("<"), n(Type.TYPE, Integer.MAX_VALUE), t(">"))), t("::"), r(n(Type.TYPE, Integer.MAX_VALUE), true, t(",")), o(s(t("#"), t(NAME)))), //
                        Type.FUNCTOR, false, (elements, args, functor) -> {
                            KnowledgeBase kb = CURRENT.get();
                            Set<Type> supers = Set.of();
                            for (Type sup : (List<Type>) args[2]) {
                                supers = supers.add(sup);
                            }
                            String group = ((Optional<String>) args[3]).orElse(Type.DEFAULT_GROUP);
                            Type type;
                            String name = (String) args[0];
                            Type arg = ((Optional<Type>) args[1]).orElse(null);
                            if (arg != null) {
                                Variable var = arg.variable();
                                if (var == null || !Type.TYPE.equals(var.type())) {
                                    kb.addException(new ParseException("Type argument " + arg + " must be a Variable of type <Type>", arg));
                                }
                                type = new Type(elements, name, supers, group, arg);
                            } else {
                                type = new Type(elements, name, supers, group);
                            }
                            return kb.addType(type);
                        }, null).init(this);

                Functor.of(s(k("import"), r(r(t(NAME), true, t(".")), true, t(","))), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            NList roots = new NList(elements.sublist(0, 1), Type.ROOT);
                            KnowledgeBase kb = CURRENT.get();
                            StringBuilder sb = new StringBuilder();
                            List<AstElement> el = List.of();
                            for (int i = 1; i <= elements.size(); i++) {
                                Token t = i < elements.size() ? (Token) elements.get(i) : null;
                                if (t == null || t.text().equals(",")) {
                                    Import ip = new Import(el, sb.toString());
                                    roots = new NList(List.of(), roots, ip);
                                    if (t != null) {
                                        roots = roots.setAstElements(roots.astElements().add(t));
                                    }
                                    el = List.of();
                                    sb = new StringBuilder();
                                    ip.init(kb);
                                } else {
                                    sb.append(t.text());
                                    el = el.add(t);
                                }
                            }
                            return roots;
                        }, null).init(this);

                Functor.of(s(n(Type.TYPE, Integer.MAX_VALUE), r(t(NAME), true, t(","))), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            KnowledgeBase kb = CURRENT.get();
                            Type type = (Type) elements.get(0);
                            NList roots = new NList(List.of(type), Type.ROOT);
                            for (int i = 1; i < elements.size(); i++) {
                                AstElement e = elements.get(i);
                                if (e instanceof Token t && t.text().equals(",")) {
                                    roots = roots.setAstElements(roots.astElements().add(t));
                                    e = elements.get(++i);
                                }
                                Variable var = e instanceof Variable v ? //
                                        new Variable(List.of(e), type, v) : //
                                        new Variable(List.of(e), type, ((Token) e).text());
                                Functor varFun = kb.addVariable(var);
                                roots = new NList(List.of(), roots, varFun);
                            }
                            return roots;
                        }, 0).init(this);

                ruleFunctor = Functor.of(s(n(Type.BOOLEAN, 0), t("<=>"), r(CONDITION, true, t(","))), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            return CURRENT.get().createRules(functor, elements, args);
                        }, null).init(this);

                Functor.of(s(n(Type.BOOLEAN, 0), t("?"), o(s(t("["), PREDICTION, t("]"), t("["), PREDICTION, t("]")))), //
                        Type.QUERY, false, (elements, args, functor) -> {
                            return new Query(functor, elements, args);
                        }, null).init(this);

                Functor.of(s(k("fact"), r(n(Type.BOOLEAN, 0), true, t(","))), //
                        Type.ROOT.list(), false, (elements, args, functor) -> {
                            NList roots = new NList(elements.sublist(0, 1), Type.ROOT);
                            for (Object arg : args) {
                                Predicate pred = (Predicate) arg;
                                Fact fact = new Fact(functor, List.of(pred), pred);
                                roots = new NList(List.of(), roots, fact);
                            }
                            return roots;
                        }, null).init(this);

                Functor.of(s(n(Type.ROOT, 0), t("::>"), t("{"), ROOTS, t("}")), //
                        Type.TRANSFORM, false, (elements, args, functor) -> {
                            Node source = (Node) args[0];
                            List<Node> targets = List.of();
                            for (Node arg : (List<Node>) args[1]) {
                                targets = targets.add(arg);
                            }
                            return new Transform(functor, elements, source, targets);
                        }, null).init(this);

                Functor.of(s(t("("), n(Type.OBJECT, 0), t(")")), //
                        Type.OBJECT, false, (elements, args, functor) -> {
                            Node node = (Node) args[0];
                            return node.setAstElements(elements);
                        }, null).init(this);

            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        });
        return this;

    }

    @SuppressWarnings("ConstantValue")
    private NList createFunctor(Type type, NList roots, List<AstElement> ast, Constructor<?> constructor, Pattern pattern, boolean local, Integer prec) throws ParseException {
        boolean toLiteral = false, function = false;
        List<Type> args = pattern.argTypes(List.of());
        Type e = type.isCollection() ? type.element() : null;
        if (args.noneMatch(t -> Type.OBJECT.isAssignableFrom(t) && !t.equals(e))) {
            type = type.literal();
        } else {
            if (!Type.BOOLEAN.isAssignableFrom(type) && !Type.ROOT.isAssignableFrom(type)) {
                type = type.function();
                function = true;
            }
            if (!Type.ROOT.isAssignableFrom(type) && !Type.COLLECTION.isAssignableFrom(type) //
                    && args.noneMatch(t -> Type.OBJECT.equals(t.element()) // 
                            || Type.BOOLEAN.isAssignableFrom(t.element()) //
                            || Type.VARIABLE.isAssignableFrom(t.element()) //
                            || Type.LITERAL.isAssignableFrom(t.element()))) {
                toLiteral = true;
            }
        }
        Type nodType = toLiteral && Type.FACT_TYPE.isAssignableFrom(type) ? Type.BOOLEAN : type;
        Functor nodFunctor = Functor.of(ast.prepend(pattern), pattern, nodType, local, toLiteral ? null : constructor, prec).init(this);
        roots = new NList(List.of(), roots, nodFunctor);
        if (pattern instanceof TokenTextPattern && constructor != null) {
            nodFunctor.construct(List.of(), new Object[0], this);
        }
        if (toLiteral) {
            Pattern litPattern = pattern.setTypes(Type::literal);
            Functor litFunctor = Functor.of(List.of(), litPattern, type, local, constructor, prec).init(this);
            roots = new NList(List.of(), roots, litFunctor);
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
            roots = new NList(List.of(), roots, rule);
        }
        return roots;
    }

    public void addLiteral(Functor nodFunctor, Functor litFunctor) {
        literalFunctors.updateAndGet(m -> m.put(nodFunctor, litFunctor));
    }

    @SuppressWarnings("unchecked")
    private NList createRules(Functor functor, List<AstElement> elements, Object[] args) throws ParseException {
        Predicate p = (Predicate) args[0];
        Functor consFunctor = p.functor();
        Functor litFunctor = literalFunctors.get().get(consFunctor);
        if (Type.FACT_TYPE.isAssignableFrom((litFunctor != null ? litFunctor : consFunctor).resultType())) {
            addException(new ParseException("Rule consequence " + p + " must be a Predicate, not a FactType", p));
        }
        NList roots = new NList(elements.sublist(0, 2), Type.ROOT);
        Node l = consFunctor.equals(equalsFunctor) ? (Node) p.get(0) : p;
        Predicate cons = (Predicate) p.replace(e -> e != p && e instanceof BooleanVariable v ? v.variable() : e).resetDeclaration();
        Map<Variable, Object> consVars = cons.getBinding();
        Map<Variable, Object> nodeVars = l == cons ? consVars : l.getBinding();
        Functor nodeFunctor = l.functor();
        Functor literalFunctor = nodeFunctor != null ? literalFunctors.get().get(nodeFunctor) : null;
        int i = 0;
        for (List<Object> condIf : (List<List<Object>>) args[1]) {
            Predicate cond = (Predicate) condIf.get(0);
            Predicate when = (Predicate) ((Optional<Object>) condIf.get(1)).orElse(null);
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
            roots = new NList(List.of(), roots, rule);
            for (i++; i < elements.size(); i++) {
                if (elements.get(i) instanceof Token t && t.text().equals(",")) {
                    roots = roots.setAstElements(roots.astElements().add(t));
                    break;
                }
            }
        }
        return roots;
    }

    private final static AtomicReference<Map<String, KnowledgeBase>> IMPORT_MAP       = new AtomicReference<>(Map.of());
    private final static AtomicReference<List<ImportResolver>>       IMPORT_RESOLVERS = new AtomicReference<>(List.of(new ResourceImportResolver()));

    /**
     * Registers an import resolver. Resolvers added later have higher priority.
     *
     * @param resolver
     *            the resolver to register
     */
    public static void registerResolver(ImportResolver resolver) {
        // Add at front for priority (newer resolvers checked first)
        IMPORT_RESOLVERS.updateAndGet(l -> l.insert(0, resolver));
    }

    /**
     * Unregisters an import resolver.
     *
     * @param resolver
     *            the resolver to unregister
     */
    public static void unregisterResolver(ImportResolver resolver) {
        IMPORT_RESOLVERS.updateAndGet(l -> l.remove(resolver));
    }

    private final AtomicReference<Set<Functor>>                             functors            = new AtomicReference<>();
    private final AtomicReference<Map<Predicate, InferResult>>              facts               = new AtomicReference<>();
    private final AtomicReference<Set<Rule>>                                rules               = new AtomicReference<>();
    private final AtomicReference<Set<Transform>>                           transforms          = new AtomicReference<>();
    private final AtomicReference<Map<Type, Set<Pair<Functor, Transform>>>> literalTransforms   = new AtomicReference<>();
    //
    private final MutableMap<String, ParseState>                            prePatterns         = MutableMap.concurrent(Map.of());
    private final MutableMap<String, ParseState>                            postPatterns        = MutableMap.concurrent(Map.of());
    //
    private final MutableMap<String, ParseState>                            localPrePatterns    = MutableMap.concurrent(Map.of());
    private final MutableMap<String, ParseState>                            localPostPatterns   = MutableMap.concurrent(Map.of());
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
        if (init != null) {
            prePatterns.set(m -> init.prePatterns.get());
            postPatterns.set(m -> init.postPatterns.get());
            localPrePatterns.set(m -> init.prePatterns.get());
            localPostPatterns.set(m -> init.postPatterns.get());
        }
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
            prePatterns.set(s -> s.addAll(kb.prePatterns.get()));
            postPatterns.set(s -> s.addAll(kb.postPatterns.get()));
            localPrePatterns.set(s -> s.addAll(kb.prePatterns.get()));
            localPostPatterns.set(s -> s.addAll(kb.postPatterns.get()));
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
            localPrePatterns.set(m -> prePatterns.get());
            localPostPatterns.set(m -> postPatterns.get());
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

    public Set<Transform> transforms() {
        return transforms.get();
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
        Type type = functor.resultType();
        String group = Type.VARIABLE.isAssignableFrom(type) ? //
                functor.construct(List.of(), new Object[0], this).type().group() : //
                type.group();
        boolean local = functor.local();
        try {
            ParseState pre = functor.preStart();
            ParseState post = functor.postStart();
            if (!local) {
                if (pre != null) {
                    prePatterns.set(p -> p.put(group, pre.merge(p.get(group))));
                }
                if (post != null) {
                    postPatterns.set(p -> p.put(group, post.merge(p.get(group))));
                }
            }
            if (pre != null) {
                localPrePatterns.set(p -> p.put(group, pre.merge(p.get(group))));
            }
            if (post != null) {
                localPostPatterns.set(p -> p.put(group, post.merge(p.get(group))));
            }
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
        if (!local) {
            functors.accumulateAndGet(Set.of(functor), Set::addAll);
        }
        return functor;
    }

    public ParseState groupState(String group) {
        return localPrePatterns.get().get(group);
    }

    public Variable variable(Token token, String group, Parser parser) throws ParseException {
        ParseState state = groupState(group);
        ParseState found = state != null ? state.tokenTexts().get(token.text()) : null;
        if (found != null && found.functor() != null && found.functor().resultType() == Type.VARIABLE) {
            return (Variable) found.functor().construct(List.of(token), new Object[0], parser);
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
                ParseState found = state.nodeTypes().get(sup);
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

    public MutableMap<String, ParseState> preStates() {
        return prePatterns;
    }

    @SuppressWarnings("unused")
    public Set<String> imported() {
        return imported.get();
    }

    public void doImport(String name, Import imp) throws ParseException {
        if (!imported.get().contains(name)) {
            merge(knowledgeBase(name, imp), imp);
            imported.updateAndGet(s -> s.add(name));
        }
    }

    @SuppressWarnings("unused")
    public static Map<String, KnowledgeBase> importMap() {
        return IMPORT_MAP.get();
    }

    public static KnowledgeBase knowledgeBase(String name, Import imp) throws ParseException {
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

        throw new ParseException("Cannot resolve import: " + name, imp);
    }

}
