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

    private final TypeInfo typeInfo;

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
        if (args.length > 1 && get(1) instanceof Set) {
            KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
            Type canonical = knowledgeBase.getType(this);
            if (canonical == null) {
                this.typeInfo = initTypeInfo();
                knowledgeBase.register(this);
            } else {
                this.typeInfo = canonical.typeInfo;
            }
        } else {
            this.typeInfo = null;
        }
    }

    private Type(NodeInfo nodeInfo, TypeInfo typeInfo, Object... args) {
        super(nodeInfo, args);
        this.typeInfo = typeInfo;
    }

    public Type(Class<?> clss, Type... supers) {
        this(NodeInfo.of(TYPE), clss, Set.of(supers), group(supers));
    }

    public Type(String name, String group, Type... supers) {
        this(NodeInfo.of(TYPE), name, Set.of(supers), group);
    }

    public Type(String name, Type... supers) {
        this(NodeInfo.of(TYPE), name, Set.of(supers), group(supers));
    }

    public Type(TokenType type) {
        this(NodeInfo.of(TYPE), type, Set.of(), DEFAULT_GROUP);
    }

    public Type(Variable var) {
        this(NodeInfo.of(TYPE, List.of(var)), var, Set.of(OBJECT), DEFAULT_GROUP);
        assert Type.TYPE.equals(var.type());
    }

    public Type(List<AstElement> elements, String name, Collection<Type> supers, String group) {
        this(NodeInfo.of(TYPE, elements), name, supers.asSet(), group);
    }

    public Type(List<AstElement> elements, String name, Collection<Type> supers, String group, Type arg) {
        this(NodeInfo.of(TYPE, elements), name, supers.asSet(), group, arg);
    }

    private Type(String name, Type sup, Type arg, String group) {
        this(List.of(), name, Set.of(sup), group, arg);
    }

    private Type(Set<Type> all, String group) {
        this(NodeInfo.of(TYPE), all, Set.of(), group);
    }

    private static Object group(Type... supers) {
        return supers.length > 0 ? supers[0].group() : DEFAULT_GROUP;
    }

    @Override
    protected Type set(NodeInfo nodeInfo, Object[] args) {
        return new Type(nodeInfo, args);
    }

    @Override
    public Type setAstElements(List<AstElement> elements) {
        return (Type) super.setAstElements(elements);
    }

    public boolean isMany() {
        return get(0) instanceof Set;
    }

    @SuppressWarnings("unchecked")
    public Set<Type> many() {
        return (Set<Type>) get(0);
    }

    public boolean hasArgument() {
        if (isMany()) {
            for (Type m : many()) {
                if (m.hasArgument()) {
                    return true;
                }
            }
            return false;
        }
        return length() == 4;
    }

    public Type argument() {
        if (hasArgument()) {
            if (isMany()) {
                for (Type m : many()) {
                    if (m.hasArgument()) {
                        return m.argument();
                    }
                }
                return this;
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
                Set<Type> supers = supersDeclaration().replaceAll(s -> s.hasArgument() ? s.setArgument(argument) : s);
                return set(3, argument).set(1, supers);
            }
        } else {
            return argument;
        }
    }

    @Override
    public Variable variable() {
        if (isMany()) {
            for (Type m : many()) {
                Variable var = m.variable();
                if (var != null) {
                    return var;
                }
            }
            return null;
        }
        Object type = get(0);
        return type instanceof Variable var ? var : null;
    }

    public String group() {
        return (String) get(2);
    }

    public Type setGroup(String group) {
        return group.equals(group()) ? this : set(2, group);
    }

    @Override
    public Type setBinding(Node declaration, Map<Variable, Object> vars, boolean setFunctorOrType) {
        Variable var = variable();
        if (var != null && vars.get(var) instanceof Type elt) {
            return elt;
        }
        return (Type) super.setBinding(declaration(), vars, setFunctorOrType);
    }

    private static record TypeInfo(Set<Type> supers, List<Type> allSupersList, Set<Type> allSupersSet) {
    }

    private TypeInfo initTypeInfo() {
        Set<Type> supersSet = initSupersSet();
        List<Type> allSupersList = initSupersList(supersSet);
        return new TypeInfo(supersSet, allSupersList, allSupersList.asSet());
    }

    private Set<Type> initSupersSet() {
        if (isMany()) {
            Set<Type> result = Set.of();
            Set<Type> many = many();
            for (Type sub : many) {
                for (Type sup : sub.supers()) {
                    if (many.remove(sub).anyMatch(m -> sup.isAssignableFrom(m))) {
                        Set<Type> set = many.remove(sub);
                        result = result.add(set.size() == 1 ? set.get(0) : new Type(set, group()));
                    } else {
                        result = result.add(new Type(many.replace(sub, sup), group()));
                    }
                }
            }
            return result;
        }
        if (hasArgument()) {
            Set<Type> result = supersDeclaration();
            Type arg = argument();
            for (Type sup : arg.supers()) {
                result = result.add(setArgument(sup));
            }
            return result;
        }
        Set<Type> supers = supersDeclaration();
        return supers.size() > 1 ? Set.of(new Type(supers, group())) : supers;
    }

    private List<Type> initSupersList(Set<Type> supersSet) {
        List<Type> pre = List.of(), post = List.of(this);
        do {
            int i = pre.size();
            pre = post;
            for (; i < pre.size(); i++) {
                Type type = pre.get(i);
                Set<Type> superSuperSet = type == this ? supersSet : type.supers();
                post = post.removeAll(superSuperSet).addAll(superSuperSet);
            }
        } while (post.size() > pre.size());
        return post;
    }

    public Set<Type> supers() {
        return typeInfo.supers;
    }

    public List<Type> allSupersList() {
        return typeInfo.allSupersList;
    }

    public Set<Type> allSupersSet() {
        return typeInfo.allSupersSet;
    }

    public Type toFunction() {
        return equals(OBJECT) ? FUNCTION : new Type(Set.of(nonLiteral(), FUNCTION), group());
    }

    public Type nonFunction() {
        if (equals(FUNCTION)) {
            return OBJECT;
        } else if (isFunction()) {
            Set<Type> set = many().remove(FUNCTION);
            return set.size() == 1 ? set.get(0) : new Type(set, group());
        } else {
            return this;
        }
    }

    public boolean isFunction() {
        return FUNCTION.isAssignableFrom(this);
    }

    public Type nonLiteral() {
        if (equals(LITERAL)) {
            return OBJECT;
        } else if (isLiteral()) {
            Set<Type> set = many().remove(LITERAL);
            return set.size() == 1 ? set.get(0) : new Type(set, group());
        } else {
            return this;
        }
    }

    public Type toLiteral() {
        return equals(OBJECT) ? LITERAL : new Type(Set.of(nonFunction(), LITERAL), group());
    }

    public boolean isLiteral() {
        return LITERAL.isAssignableFrom(this);
    }

    public Type nonVariable() {
        if (equals(VARIABLE)) {
            return OBJECT;
        } else if (isVariable()) {
            Set<Type> set = many().remove(VARIABLE);
            return set.size() == 1 ? set.get(0) : new Type(set, group());
        } else {
            return this;
        }
    }

    public Type toVariable() {
        return equals(OBJECT) ? VARIABLE : new Type(Set.of(this, VARIABLE), group());
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
        return LIST.setArgument(this).setGroup(group());

    }

    public Type toSet(String group) {
        return SET.setArgument(this).setGroup(group());
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
    public Set<Type> supersDeclaration() {
        return (Set<Type>) get(1);
    }

    @Override
    public Type set(int i, Object... a) {
        return (Type) super.set(i, a);
    }

    public boolean isAssignableFrom(Type type) {
        return type.allSupersSet().contains(this);
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
            List<Type> supers = (List<Type>) get(2);
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

    @Override
    public Type resultType() {
        return this;
    }

    @Override
    public Type setTypeArgs(Map<Variable, Type> typeArgs) {
        return (Type) super.setTypeArgs(typeArgs);
    }

}
