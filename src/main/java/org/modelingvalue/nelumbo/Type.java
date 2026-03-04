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

import java.io.Serial;
import java.util.Optional;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class Type extends Node {
    @Serial
    private static final long   serialVersionUID = -4583279157841144493L;
    //
    public static final String  DEFAULT_GROUP    = "_";
    public static final String  TOP_GROUP        = "TOP";
    public static final String  PATTERN_GROUP    = "PATTERN";
    //
    private static final Object EQUALS_TYPE      = new Object() {
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
    public static final Type    $OBJECT          = new Type(Object.class);
    public static final Type    $STRING          = new Type(String.class, $OBJECT);
    //
    public static final Type    OBJECT           = new Type("Object", $OBJECT);
    public static final Type    TYPE             = new Type("Type", OBJECT);
    public static final Type    UNIVERSE         = new Type("Universe", OBJECT);
    public static final Type    NAMESPACE        = new Type("Namespace", OBJECT);
    public static final Type    FUNCTION         = new Type("Function", OBJECT);
    public static final Type    LITERAL          = new Type("Literal", OBJECT);
    public static final Type    ROOT             = new Type("Root", OBJECT);
    public static final Type    BOOLEAN          = new Type("Boolean", OBJECT);
    public static final Type    FACT_TYPE        = new Type("FactType", BOOLEAN);
    public static final Type    VARIABLE         = new Type("Variable", OBJECT);
    public static final Type    RULE             = new Type("Rule", ROOT);
    public static final Type    FUNCTOR          = new Type("Functor", ROOT);
    public static final Type    FACT             = new Type("Fact", ROOT);
    public static final Type    PATTERN          = new Type("Pattern", PATTERN_GROUP, Type.OBJECT);
    public static final Type    QUERY            = new Type("Query", Type.ROOT);
    public static final Type    TRANSFORM        = new Type("Transform", Type.ROOT, NAMESPACE);
    public static final Type    IMPORT           = new Type("Import", Type.ROOT);
    private static final Type   TYPE_ARG_VAR     = new Type(new Variable(List.of(), TYPE, "E", false));
    public static final Type    COLLECTION       = new Type("Collection", OBJECT, TYPE_ARG_VAR, DEFAULT_GROUP);
    public static final Type    SET              = new Type("Set", COLLECTION, TYPE_ARG_VAR, DEFAULT_GROUP);
    public static final Type    LIST             = new Type("List", COLLECTION, TYPE_ARG_VAR, DEFAULT_GROUP);
    public static final Type    ROOT_LIST        = new Type("RootList", ROOT.list(TOP_GROUP), NAMESPACE);
    public static final Type    ROOT_NAMESPACE   = new Type("RootNamespace", ROOT.list(), NAMESPACE, ROOT);

    public static List<Type> predefined() {
        return List.of(//
                OBJECT, //
                TYPE, //
                UNIVERSE, //
                FUNCTION, //
                NAMESPACE, //
                LITERAL, //
                ROOT, //
                BOOLEAN, //
                FACT_TYPE, //
                VARIABLE, //
                RULE, //
                FUNCTOR, //
                FACT, //
                PATTERN, //
                QUERY, //
                TRANSFORM, //
                IMPORT, //
                COLLECTION, //
                SET, //
                LIST, //
                ROOT_LIST);
    }

    private Type       list;
    private Type       set;
    private Type       literal;
    private Type       function;
    private List<Type> allSupers;

    @Override
    public Node typeOrFunctor() {
        return TYPE;
    }

    @Override
    protected Object typeForEquals() {
        return EQUALS_TYPE;
    }

    private Type(Object[] array, List<AstElement> elements, Type declaration) {
        super(array, elements, declaration);
    }

    public Type(Class<?> clss, Type... supers) {
        super(TYPE, List.of(), clss, supers.length == 0 ? Set.of() : Set.of(supers), group(supers));
    }

    public Type(String name, String group, Type... supers) {
        super(TYPE, List.of(), name, supers.length == 0 ? Set.of(OBJECT) : Set.of(supers), group);
    }

    public Type(String name, Type... supers) {
        super(TYPE, List.of(), name, supers.length == 0 ? Set.of(OBJECT) : Set.of(supers), group(supers));
    }

    public Type(TokenType type) {
        super(TYPE, List.of(), type, Set.of(), DEFAULT_GROUP);
    }

    public Type(Variable var) {
        this(List.of(var), var, DEFAULT_GROUP);
    }

    public Type(List<AstElement> elements, Variable var, String group) {
        super(TYPE, elements, var, Set.of(OBJECT), group);
        assert Type.TYPE.equals(var.type());
    }

    public Type(List<AstElement> elements, String name, Collection<Type> supers, String group) {
        super(TYPE, elements, name, supers.asSet(), group);
    }

    public Type(List<AstElement> elements, String name, Collection<Type> supers, String group, Type element) {
        super(TYPE, elements, name, supers.asSet(), group, element);
    }

    private Type(String name, Type sup, Type element, String group) {
        this(List.of(), name, Set.of(sup), group, element);
    }

    public Type(Type super1, Type super2) {
        super(TYPE, //
                List.of(), //
                Set.of(super1, super2), //
                Set.of(super1, super2) //
                        .addAll(super1.supers().remove(OBJECT).replaceAll(s1 -> new Type(s1, super2))) //
                        .addAll(super2.supers().remove(OBJECT).replaceAll(s2 -> new Type(super1, s2))) //
                , super1.group());
    }

    private static Object group(Type... supers) {
        return supers.length > 0 ? supers[0].group() : DEFAULT_GROUP;
    }

    @Override
    public Type setFunctor(Functor functor) {
        return (Type) super.setFunctor(functor);
    }

    @Override
    public Type setAstElements(List<AstElement> elements) {
        return (Type) super.setAstElements(elements);
    }

    public Type element() {
        if (isCollection()) {
            return (Type) get(3);
        } else {
            return this;
        }
    }

    @Override
    protected Type setBinding(Node declaration, Map<Variable, Object> vars) {
        if (isCollection()) {
            Variable var = element().variable();
            if (var != null && vars.get(var) instanceof Type elt) {
                return setElement(elt).setBinding(declaration, vars);
            }
        }
        return (Type) super.setBinding(declaration, vars);
    }

    public Type setElement(Type element) {
        Set<Type> supers = supers().replaceAll(s -> s.isCollection() ? s.setElement(element) : s);
        for (Type s : element.supers()) {
            supers = supers.add(setElement(s));
        }
        return set(3, element).set(1, supers);
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

    public boolean isCollection() {
        return length() == 4;
    }

    public boolean isMany() {
        return get(0) instanceof Set;
    }

    public Type function() {
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

    @SuppressWarnings("unused")
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

    public Type literal() {
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

    public Type list() {
        return list(group());
    }

    public Type set() {
        return set(group());
    }

    public Type list(String group) {
        if (list == null) {
            list = LIST.setElement(this);
        }
        if (!group.equals(group())) {
            return list.setGroup(group);
        }
        return list;
    }

    public Type set(String group) {
        if (set == null) {
            set = SET.setElement(this);
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
        if (isCollection()) {
            return name + "<" + element().name() + ">";
        }
        return name;
    }

    @SuppressWarnings("unchecked")
    public String rawName() {
        Object type = get(0);
        if (type instanceof Set<?> s) {
            return "(" + ((Set<Type>) s).map(Type::name).sorted().sequential().reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) + ")";
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
        Object type = get(0);
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
            return type.literal();
        } else if (isFunction()) {
            return type.function();
        } else {
            return type;
        }
    }

    @Override
    public String toString(TokenType[] previous) {
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER || previous[0] == TokenType.DECIMAL) {
            previous[0] = TokenType.NAME;
            return " " + name();
        }
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
    protected Type struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new Type(array, elements, (Type) declaration);
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
            }
        }
        return false;
    }

    public boolean isAssignableFrom(Class<?> type) {
        Class<?> clss = clss();
        return clss != null && clss.isAssignableFrom(type);
    }

}
