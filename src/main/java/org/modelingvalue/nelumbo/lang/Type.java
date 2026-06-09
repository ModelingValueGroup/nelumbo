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

import java.io.Serial;
import java.util.Optional;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class Type extends Node implements FunctorOrType {
    @Serial
    private static final long serialVersionUID = -4583279157841144493L;
    //
    public static final String DEFAULT_GROUP = "_";
    public static final String PATTERN_GROUP = "PATTERN";
    //
    private static final Object EQUALS_TYPE = new Object() {
        @Override
        public String toString() {
            return "Type";
        }

        @Override
        public int hashCode() {
            return 0;
        }
    };
    //
    public static final Type $OBJECT = new Type(Object.class);
    public static final Type $STRING = new Type(String.class, $OBJECT);
    //
    public static final Type  NATIVE       = new Type("NATIVE");
    public static final Type  OBJECT       = new Type("Object", $OBJECT);
    public static final Type  TYPE         = new Type("Type", OBJECT);
    public static final Type  WORLD        = new Type("World", OBJECT);
    public static final Type  NAMESPACE    = new Type("Namespace", OBJECT);
    public static final Type  FUNCTION     = new Type("Function", OBJECT);
    public static final Type  LITERAL      = new Type("Literal", OBJECT);
    public static final Type  ROOT         = new Type("Root", OBJECT);
    public static final Type  BOOLEAN      = new Type("Boolean", OBJECT);
    public static final Type  FACT_TYPE    = new Type("FactType", BOOLEAN);
    public static final Type  VARIABLE     = new Type("Variable", OBJECT);
    public static final Type  FUNCTOR      = new Type("Functor", ROOT);
    public static final Type  PATTERN_PART = new Type("PatternPart", ROOT);
    public static final Type  PATTERN      = new Type("Pattern", PATTERN_GROUP, OBJECT);
    private static final Type TYPE_ARG_VAR = new Type(new Variable(List.of(), false, TYPE, "E"));
    public static final Type  COLLECTION   = new Type("Collection", OBJECT, TYPE_ARG_VAR, DEFAULT_GROUP);
    public static final Type  SET          = new Type("Set", COLLECTION, TYPE_ARG_VAR, DEFAULT_GROUP);
    public static final Type  LIST         = new Type("List", COLLECTION, TYPE_ARG_VAR, DEFAULT_GROUP);

    public static List<Type> predefined() {
        return List.of(//
                NATIVE, //
                OBJECT, //
                TYPE, //
                WORLD, //
                NAMESPACE, //
                FUNCTION, //
                LITERAL, //
                ROOT, //
                BOOLEAN, //
                FACT_TYPE, //
                VARIABLE, //
                FUNCTOR, //
                PATTERN_PART, //
                PATTERN, //
                COLLECTION, //
                SET, //
                LIST);
    }

    private Type       list;
    private Type       set;
    private Type       literal;
    private Type       variable;
    private Type       function;
    private List<Type> allSupers;

    @Override
    public FunctorOrType functorOrType() {
        return TYPE;
    }

    @Override
    protected Object typeForEquals() {
        return EQUALS_TYPE;
    }

    @NelumboConstructor
    public Type(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public Type(Class<?> clss, Type... supers) {
        super(NodeInfo.of(TYPE), clss, supers.length == 0 ? Set.of() : Set.of(supers), group(supers));
    }

    public Type(String name, String group, Type... supers) {
        super(NodeInfo.of(TYPE), name, supers.length == 0 ? Set.of(OBJECT) : Set.of(supers), group);
    }

    public Type(String name, Type... supers) {
        super(NodeInfo.of(TYPE), name, supers.length == 0 ? Set.of(OBJECT) : Set.of(supers), group(supers));
    }

    public Type(TokenType type) {
        super(NodeInfo.of(TYPE), type, Set.of(), DEFAULT_GROUP);
    }

    public Type(Variable var) {
        this(List.of(var), var, DEFAULT_GROUP);
    }

    public Type(List<AstElement> elements, Variable var, String group) {
        super(NodeInfo.of(TYPE, elements), var, Set.of(OBJECT), group);
        assert Type.TYPE.equals(var.type());
    }

    public Type(List<AstElement> elements, String name, Collection<Type> supers, String group) {
        super(NodeInfo.of(TYPE, elements), name, supers.asSet(), group);
    }

    public Type(List<AstElement> elements, String name, Collection<Type> supers, String group, Type element) {
        super(NodeInfo.of(TYPE, elements), name, supers.asSet(), group, element);
    }

    private Type(String name, Type sup, Type element, String group) {
        this(List.of(), name, Set.of(sup), group, element);
    }

    public Type(Type super1, Type super2) {
        super(NodeInfo.of(TYPE), Set.of(super1, super2), Set.of(super1, super2) //
                .addAll(super1.supers().remove(OBJECT).replaceAll(s1 -> new Type(s1, super2))) //
                .addAll(super2.supers().remove(OBJECT).replaceAll(s2 -> new Type(super1, s2))) //
                , super1.group());
    }

    private static Object group(Type... supers) {
        return supers.length > 0 ? supers[0].group() : DEFAULT_GROUP;
    }

    @Override
    public Type setFunctorOrType(FunctorOrType functorOrType) {
        return (Type) super.setFunctorOrType(functorOrType);
    }

    @Override
    public Type setAstElements(List<AstElement> elements) {
        return (Type) super.setAstElements(elements);
    }

    public Type argument() {
        if (hasArgument()) {
            if (isMany()) {
                return many().filter(Type::hasArgument).findFirst().get().argument();
            }
            return (Type) get(3);
        } else {
            return this;
        }
    }

    public Type setArgument(Type argument) {
        if (hasArgument()) {
            if (argument().equals(argument)) {
                return this;
            } else {
                if (isMany()) {
                    return set(0, many().replaceAll(t -> t.hasArgument() ? t.setArgument(argument) : t));
                }
                Set<Type> supers = supers().replaceAll(s -> s.hasArgument() ? s.setArgument(argument) : s);
                return set(3, argument).set(1, supers);
            }
        } else {
            return argument;
        }
    }

    @Override
    public Type setBinding(Node declaration, Map<Variable, Object> vars, boolean setFunctorOrType) {
        Variable var = variable();
        if (var != null && vars.get(var) instanceof Type elt) {
            return elt;
        }
        return (Type) super.setBinding(declaration(), vars, setFunctorOrType);
    }

    public String group() {
        return (String) get(2);
    }

    public Type setGroup(String group) {
        return set(2, group);
    }

    @SuppressWarnings("unchecked")
    public Set<Type> many() {
        return (Set<Type>) get(0);
    }

    public boolean hasArgument() {
        if (isMany()) {
            return many().anyMatch(Type::hasArgument);
        }
        return length() == 4;
    }

    public boolean isMany() {
        return get(0) instanceof Set;
    }

    public Type toFunction() {
        if (isFunction()) {
            return this;
        } else if (function == null) {
            return function = equals(OBJECT) ? FUNCTION : new Type(this, FUNCTION);
        }
        return function;
    }

    public Type nonFunction() {
        if (isFunction()) {
            Optional<Type> first = supers().findFirst(s -> s != FUNCTION);
            if (first.isEmpty()) {
                throw new IllegalStateException("No non-function supertype for " + this);
            }
            return first.get();
        } else {
            return this;
        }
    }

    public boolean isFunction() {
        return FUNCTION.isAssignableFrom(this);
    }

    public Type nonLiteral() {
        if (isLiteral()) {
            Optional<Type> first = supers().findFirst(s -> s != LITERAL);
            if (first.isEmpty()) {
                throw new IllegalStateException("No non-literal supertype for " + this);
            }
            return first.get();
        } else {
            return this;
        }
    }

    public Type toLiteral() {
        if (isLiteral()) {
            return this;
        } else if (literal == null) {
            return literal = equals(OBJECT) ? LITERAL : new Type(this, LITERAL);
        }
        return literal;
    }

    public boolean isLiteral() {
        return LITERAL.isAssignableFrom(this);
    }

    public Type nonVariable() {
        if (isVariable()) {
            Optional<Type> first = supers().findFirst(s -> s != VARIABLE);
            if (first.isEmpty()) {
                throw new IllegalStateException("No non-variable supertype for " + this);
            }
            return first.get();
        } else {
            return this;
        }
    }

    public Type toVariable() {
        if (isVariable()) {
            return this;
        } else if (variable == null) {
            return variable = equals(OBJECT) ? VARIABLE : new Type(this, VARIABLE);
        }
        return variable;
    }

    public boolean isVariable() {
        return VARIABLE.isAssignableFrom(this);
    }

    public Type toList() {
        return toList(group());
    }

    public Type toSet() {
        return toSet(group());
    }

    public Type toList(String group) {
        if (list == null) {
            list = LIST.setArgument(this);
        }
        if (!group.equals(group())) {
            return list.setGroup(group);
        }
        return list;
    }

    public Type toSet(String group) {
        if (set == null) {
            set = SET.setArgument(this);
        }
        if (!group.equals(group())) {
            return set.setGroup(group);
        }
        return set;
    }

    public TokenType tokenType() {
        Object type = get(0);
        if (type instanceof Variable var) {
            type = var.type().tokenType();
        }
        return type instanceof TokenType ? ((TokenType) type) : null;
    }

    public String name() {
        String name = rawName();
        if (length() == 4) {
            return name + "<" + argument().name() + ">";
        }
        return name;
    }

    @SuppressWarnings("unchecked")
    public String rawName() {
        Object type = get(0);
        if (type instanceof Set<?> s) {
            return "(" + ((Set<Type>) s).map(Type::name).sorted().sequential().reduce("",
                    (a, b) -> a.isEmpty() ? b : a + "," + b) + ")";
        } else if (type instanceof TokenType tt) {
            return tt.name();
        } else if (type instanceof Variable var) {
            return var.name();
        } else if (type instanceof Class<?> cls) {
            return "$" + cls.getSimpleName();
        }
        return (String) type;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Node> clss() {
        Object type = get(0);
        return type instanceof Class<?> clss ? (Class<? extends Node>) clss : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Variable variable() {
        Object type = length() > 0 ? get(0) : null;
        if (type instanceof Set<?> s) {
            for (Type t : (Set<Type>) s) {
                Variable var = t.variable();
                if (var != null) {
                    return var;
                }
            }
        }
        return type instanceof Variable var ? var : null;
    }

    public Type rewrite(Type type) {
        if (isLiteral()) {
            return type.toLiteral();
        } else if (isFunction()) {
            return type.toFunction();
        } else if (isVariable()) {
            return type.toVariable();
        } else {
            return type;
        }
    }

    @Override
    public String toString(TokenType[] previous) {
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER) {
            previous[0] = TokenType.NAME;
            return " " + name();
        }
        previous[0] = TokenType.NAME;
        return name();
    }

    @SuppressWarnings("unchecked")
    public Set<Type> supers() {
        return (Set<Type>) get(1);
    }

    public List<Type> allSupers() {
        if (allSupers == null) {
            List<Type> pre = List.of(), post = List.of(this);
            do {
                int i = pre.size();
                pre = post;
                for (; i < pre.size(); i++) {
                    post = post.addAllUnique(pre.get(i).supers());
                }
            } while (post.size() > pre.size());
            allSupers = post;
        }
        return allSupers;
    }

    @Override
    protected Type set(NodeInfo nodeInfo, Object[] args) {
        return new Type(nodeInfo, args);
    }

    @Override
    public Type set(int i, Object... a) {
        return (Type) super.set(i, a);
    }

    public boolean isAssignableFrom(Type type) {
        if (isMany()) {
            return many().allMatch(s -> s.isAssignableFrom(type));
        }
        for (Type s : type.allSupers()) {
            if (equals(s)) {
                return true;
            } else if (hasArgument() && s.hasArgument() && get(0).equals(s.get(0))) {
                if (argument().isAssignableFrom(s.argument())) {
                    return true;
                } else if (argument().get(0) instanceof Variable || s.argument().get(0) instanceof Variable) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public boolean isAssignableFrom(Class<?> type) {
        Class<?> clss = clss();
        return clss != null && clss.isAssignableFrom(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing) {
            if (length() > 2 && get(2) instanceof String) {
                return this;
            }
            if (super.functorOrType() instanceof Functor functor
                    && functor.astElements().first() instanceof Type type) {
                Type result = type.setAstElements(astElements());
                if (result.hasArgument() && get(0) instanceof Type elem) {
                    result = result.setArgument(elem);
                }
                return result.setFunctorOrType(functor);
            }
            Set<Type> supers = Set.of();
            for (Type sup : (List<Type>) get(2)) {
                supers = supers.add(sup);
            }
            String group = (String) get(3);
            if (group == null) {
                group = DEFAULT_GROUP;
            }
            Type type;
            String name = (String) get(0);
            Type arg = (Type) get(1);
            if (arg != null) {
                Variable var = arg.variable();
                if (var == null || !Type.TYPE.equals(var.type())) {
                    knowledgeBase.addException(
                            new ParseException("Type argument " + arg + " must be a Variable of type <Type>", arg));
                }
                type = new Type(astElements(), name, supers, group, arg);
            } else {
                type = new Type(astElements(), name, supers, group);
            }
            return knowledgeBase.addType(type, ctx);
        }
        return this;
    }

    public Type common(Type other) {
        if (!hasArgument() && !other.hasArgument()) {
            if (isAssignableFrom(other)) {
                return this;
            } else if (other.isAssignableFrom(this)) {
                return other;
            }
        } else if (hasArgument() && other.hasArgument()) {
            Type element = argument().common(other.argument());
            if (element != null) {
                Type te = setArgument(element);
                Type oe = other.setArgument(element);
                if (te.isAssignableFrom(oe)) {
                    return te;
                } else if (oe.isAssignableFrom(te)) {
                    return oe;
                }
            }
        }
        return null;
    }

    @Override
    public Type resultType() {
        return this;
    }

    @Override
    public Type setTypeArgs(Map<Variable, Type> typeArgs) {
        return (Type) super.setTypeArgs(typeArgs);
    }

}
