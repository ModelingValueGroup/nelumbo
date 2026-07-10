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

package org.modelingvalue.nelumbo.lang;

import static org.modelingvalue.nelumbo.syntax.TokenType.NAME;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.collections.util.NotMergeableException;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.NelumboMethod;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.logic.And;
import org.modelingvalue.nelumbo.logic.ExistentialQuantifier;
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

public class Functor extends Node implements FunctorOrType {
    @Serial
    private static final long serialVersionUID = -1901047746034698364L;

    public static Functor of(Pattern pattern, Type result, Type local, Class<?> clazz, Integer leftPrecedence)
            throws ParseException {
        return of(List.of(), pattern, result, local, clazz, leftPrecedence);
    }

    public static Functor of(List<AstElement> elements, Pattern pattern, Type result, Type local, Class<?> clazz,
            Integer leftPrecedence) throws ParseException {
        return new Functor(elements, pattern, result, local,
                clazz != null ? NelumboConstructor.Finder.find(clazz, KnowledgeBase.CURRENT.get(), List.of()) : null,
                leftPrecedence,
                clazz != null ? NelumboMethod.Finder.find(clazz, pattern.name(), KnowledgeBase.CURRENT.get(), List.of())
                        : null);
    }

    private String        name;
    private List<Type>    argTypes;
    private Set<Variable> typeVariables;
    private ParseState    start;
    private ParseState    startPre;
    private ParseState    startPost;

    private Functor(List<AstElement> elements, Object... args) {
        super(NodeInfo.of(Type.FUNCTOR, elements), args);
    }

    @NelumboConstructor
    public Functor(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected Functor set(NodeInfo nodeInfo, Object[] args) {
        return new Functor(nodeInfo, args);
    }

    public Pattern pattern() {
        return (Pattern) get(0);
    }

    @Override
    public Type resultType() {
        return (Type) get(1);
    }

    public Type local() {
        return (Type) get(2);
    }

    @SuppressWarnings("unchecked")
    public Constructor<? extends Node> constructor() {
        Object val = get(3);
        return val instanceof Constructor ? (Constructor<? extends Node>) val : null;
    }

    public Integer leftPrecedence() {
        return (Integer) get(4);
    }

    public Method method() {
        return (Method) get(5);
    }

    @Override
    public Variable variable() {
        return constructedVariable();
    }

    public Variable constructedVariable() {
        List<AstElement> astElements = astElements();
        return astElements.isEmpty() ? null : astElements.first() instanceof Variable v ? v : null;
    }

    public Type constructedType() {
        List<AstElement> astElements = astElements();
        return astElements.isEmpty() ? null : astElements.first() instanceof Type t ? t : null;
    }

    @Override
    public Functor setBinding(Map<Variable, Object> vars) {
        return (Functor) super.setBinding(vars);
    }

    @Override
    public Functor setTypeArgs(Map<Variable, Type> typeArgs) {
        return (Functor) super.setTypeArgs(typeArgs);
    }

    @Override
    public Functor resetDeclaration() {
        return (Functor) super.resetDeclaration();
    }

    @Override
    public Functor set(int i, Object... a) {
        return (Functor) super.set(i, a);
    }

    public String name() {
        if (name == null) {
            name = pattern().name();
        }
        return name;
    }

    public List<Type> argTypes() {
        if (argTypes == null) {
            argTypes = pattern().argTypes(List.of());
        }
        return argTypes;
    }

    public Set<Variable> typeVariables() {
        if (typeVariables == null) {
            typeVariables = argTypes().flatMap(Type::typeVariables).asSet();
        }
        return typeVariables;
    }

    @Override
    public String toString(TokenType[] previous) {
        return resultType() + "::=" + pattern();
    }

    public Node construct(List<AstElement> elements, Object[] args, ParseExceptionHandler handler, ParseContext ctx)
            throws ParseException {
        Constructor<? extends Node> constructor = constructor();
        if (constructor != null) {
            try {
                return constructor.newInstance(NodeInfo.of(this, elements), args);
            } catch (Exception e) {
                handleException(elements, handler, e);
            }
        }
        return Type.BOOLEAN.isAssignableFrom(resultType()) ? new Predicate(NodeInfo.of(this, elements), args)
                : new Node(NodeInfo.of(this, elements), args);
    }

    private void handleException(List<AstElement> elements, ParseExceptionHandler handler, Exception e)
            throws ParseException {
        if (e instanceof ParseException pe) {
            handler.addException(pe);
        } else {
            handler.addException(new ParseException(e, "Exception during Node construction: " + e, elements));
        }
    }

    public ParseState preStart() {
        start();
        return startPre;
    }

    public ParseState postStart() {
        start();
        return startPost;
    }

    public ParseState start() {
        if (start == null) {
            ParseState s = pattern().state(new ParseState(this));
            startPre = s.pre();
            ParseState post = s.post();
            if (post != null) {
                Integer left = leftPrecedence();
                Integer inner = post.leftPrecedence();
                startPost = post.setLeftPrecedence(left != null ? left : inner != null ? inner : Integer.MAX_VALUE);
            }
            start = s;
        }
        return start;
    }

    @Override
    public Functor setFunctorOrType(FunctorOrType functorOrType) {
        return (Functor) super.setFunctorOrType(functorOrType);
    }

    @Override
    public Functor setAstElements(List<AstElement> elements) {
        return (Functor) super.setAstElements(elements);
    }

    public Functor setResultType(Type type) {
        return set(1, type);
    }

    public Object[] args(List<AstElement> elements, Map<Variable, Type> typeArgs) {
        Pattern pattern = pattern();
        MutableList<Object> args = MutableList.of(List.of());
        int i = pattern.args(elements, 0, args, false, this, typeArgs);
        if (i < 0) {
            throw new IllegalArgumentException("Error during argument extraction for " + this + " with elements "
                    + elements + " and typeArgs " + typeArgs);
        }
        return pattern instanceof SequencePattern && args.size() == 1 && args.get(0) instanceof List<?> seq
                ? seq.toArray()
                : args.toArray();
    }

    public String string(List<Object> args, TokenType[] previous) {
        Pattern pattern = pattern();
        if (pattern instanceof SequencePattern && argTypes().size() > 1) {
            args = List.of(args);
        }
        StringBuffer sb = new StringBuffer();
        if (pattern.string(args, 0, sb, previous, false) < 0) {
            return null;
        }
        return sb.toString();
    }

    public Functor literal() {
        return KnowledgeBase.CURRENT.get().literal(this);
    }

    public Pattern declaration(Token token) {
        return pattern().declaration(token);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing) {
            Type local = null;
            int start = 2;
            Object mod = get(0);
            if (mod != null && mod.equals("private")) {
                local = Type.NAMESPACE;
                start += 1;
            }
            List<AstElement> elements = astElements();
            Type type = (Type) elements.get(start - 2);
            NList roots = new NList(elements.sublist(0, start), Type.ROOT);
            List<AstElement> pttrn = List.of(), ast = List.of();
            Class<?> clazz = null;
            Integer precedence = null;
            for (int i = start; i <= elements.size(); i++) {
                AstElement e = i < elements.size() ? elements.get(i) : null;
                if (e == null || e instanceof Token) {
                    Token t = (Token) e;
                    if (t == null || t.text().equals(",")) {
                        Pattern pattern = Pattern.pattern(pttrn);
                        if (precedence != null) {
                            pattern = pattern.setPresedence(precedence);
                        }
                        roots = createFunctor(type, roots, ast, clazz, pattern, local, precedence, knowledgeBase, ctx);
                        if (t != null) {
                            roots = roots.setAstElements(roots.astElements().add(t));
                        }
                        ast = pttrn = List.of();
                        clazz = null;
                        precedence = null;
                    } else if (t.text().equals("#")) {
                        ast = ast.add(t);
                        t = t.next();
                        ast = ast.add(t);
                        i++;
                        precedence = Integer.parseInt(t.text());
                    } else if (t.text().equals("@")) {
                        int s = ast.size();
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
                        clazz = NelumboConstructor.Finder.find(className, knowledgeBase, ast.sublist(s, ast.size()));
                    } else {
                        pttrn = pttrn.add(e);
                    }
                } else {
                    pttrn = pttrn.add(e);
                }
            }
            return roots;
        }
        Constructor<? extends Node> constructor = constructor();
        List<AstElement> elements = astElements();
        if (constructor != null && !elements.isEmpty()) {
            Field field = NelumboFunctorField.Finder.find(constructor.getDeclaringClass(), knowledgeBase, elements);
            if (field != null) {
                try {
                    field.set(null, this);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    knowledgeBase.addException(new ParseException(ex,
                            ex + " during setting FUNCTOR field in " + constructor.getDeclaringClass().getName(),
                            elements));
                }
            }
        }
        Type type = resultType();
        String group = type.group();
        Type local = local();
        if (local != null) {
            ctx.register(knowledgeBase, group, local, this);
        } else {
            knowledgeBase.parseContext().register(knowledgeBase, group, Type.WORLD, this);
        }
        return this;
    }

    private NList createFunctor(Type type, NList roots, List<AstElement> ast, Class<?> clazz, Pattern pattern,
            Type local, Integer prec, KnowledgeBase knowledgeBase, ParseContext ctx) throws ParseException {
        ast = ast.prepend(pattern);
        boolean toLiteral = false, function = false;
        List<Type> args = pattern.argTypes(List.of());
        List<Type> gen = type.hasArguments() ? type.arguments() : List.of();
        if (!Type.ROOT.isAssignableFrom(type) && !Type.NAMESPACE.isAssignableFrom(type)
                && !Type.PATTERN.isAssignableFrom(type) && (Lambda.class.equals(clazz)
                        || args.noneMatch(t -> Type.OBJECT.isAssignableFrom(t) && !gen.contains(t)))) {
            type = type.toLiteral();
        } else if (type.variable() == null) {
            if (!Type.TYPE.isAssignableFrom(type) && !Type.BOOLEAN.isAssignableFrom(type)
                    && !Type.ROOT.isAssignableFrom(type) && !Type.PATTERN.isAssignableFrom(type)
                    && !Type.NAMESPACE.isAssignableFrom(type)) {
                type = type.toFunction();
                function = true;
            }
            if (!Type.TYPE.isAssignableFrom(type) && !Type.ROOT.isAssignableFrom(type)
                    && !Type.NAMESPACE.isAssignableFrom(type) && !Type.PATTERN.isAssignableFrom(type)
                    && !Type.COLLECTION.isAssignableFrom(type) //
                    && args.flatMap(t -> t.hasArguments() ? t.arguments() : List.of(t))
                            .noneMatch(t -> Type.OBJECT.equals(t) //
                                    || Type.BOOLEAN.isAssignableFrom(t) //
                                    || Type.VARIABLE.isAssignableFrom(t) //
                                    || Type.LITERAL.isAssignableFrom(t))) {
                toLiteral = true;
            }
        }
        Type nodType = toLiteral && Type.FACT_TYPE.isAssignableFrom(type) ? Type.BOOLEAN : type;
        Functor nodFunctor = Functor.of(ast, pattern, nodType, local, toLiteral ? null : clazz, prec);
        nodFunctor.init(knowledgeBase, ctx, ConstructionReason.transforming);
        roots = new NList(List.of(), roots, nodFunctor);
        if (pattern instanceof TokenTextPattern && clazz != null) {
            nodFunctor.construct(List.of(), new Object[0], knowledgeBase, ctx).init(knowledgeBase, ctx,
                    ConstructionReason.parsing);
        }
        if (toLiteral) {
            Pattern litPattern = pattern.setTypes(Type::toLiteral);
            Functor litFunctor = Functor.of(ast, litPattern, type, local, clazz, prec);
            litFunctor.init(knowledgeBase, ctx, ConstructionReason.transforming);
            roots = new NList(List.of(), roots, litFunctor);
            knowledgeBase.addLiteral(nodFunctor, litFunctor);
            // Implied Rule
            Object[] nodVars = new Object[args.size()];
            Object[] litVars = new Object[args.size()];
            List<Type> litArgs = args.replaceAll(Type::toLiteral);
            for (int v = 0; v < args.size(); v++) {
                nodVars[v] = new Variable(List.of(), false, args.get(v), "n" + (v + 1));
                litVars[v] = new Variable(List.of(), false, litArgs.get(v), "l" + (v + 1));
            }
            Node nodNode = nodFunctor.construct(List.of(), nodVars, knowledgeBase, ctx);
            Node litNode = litFunctor.construct(List.of(), litVars, knowledgeBase, ctx);
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

    public Functor mostSpecific(Functor other) {
        List<Type> thisTypes = argTypes();
        List<Type> otherTypes = other.argTypes();
        for (int i = 0; i < thisTypes.size() && i < otherTypes.size(); i++) {
            Type thisType = thisTypes.get(i);
            Type otherType = otherTypes.get(i);
            if (!thisType.equals(otherType)) {
                if (thisType.isAssignableFrom(otherType)) {
                    return other;
                } else if (otherType.isAssignableFrom(thisType)) {
                    return this;
                }
            }
        }
        throw new NotMergeableException("Non deterministic pattern merge " + this + " <> " + other);
    }

    public Functor nonBootstrap(Functor functor) {
        return functor.firstToken() != null && firstToken() == null ? functor : this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Functor setBinding(Node declaration, Map<Variable, Object> vars, boolean setFunctorOrType) {
        Functor functor = (Functor) super.setBinding(declaration, vars, setFunctorOrType);
        List<AstElement> from = astElements();
        List to = (List) setBinding(from, from, vars, -1, setFunctorOrType);
        to = to.replaceAll(e -> e instanceof String s ? Pattern.t(s) : e);
        return from.equals(to) ? functor : functor.setAstElements(to);
    }

    @Override
    public Functor declaration() {
        return (Functor) super.declaration();
    }

}
