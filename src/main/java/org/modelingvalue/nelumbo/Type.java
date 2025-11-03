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

import java.io.Serial;
import java.util.Optional;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class Type extends Node {
    @Serial
    private static final long  serialVersionUID = -4583279157841144493L;
    //
    public static final String DEFAULT_GROUP    = "_";
    public static final String TOP_GROUP        = "TOP";
    public static final String PATTERN_GROUP    = "PATTERN";
    //
    public static final Type   OBJECT           = new Type(Object.class);
    public static final Type   STRING           = new Type(String.class, OBJECT);
    //
    public static final Type   NODE             = new Type(Node.class, OBJECT);
    public static final Type   FUNCTION         = new Type("Function", NODE);
    public static final Type   TERMINAL         = new Type(Terminal.class, NODE);
    public static final Type   LITERAL          = new Type("Literal", TERMINAL);
    public static final Type   ROOT             = new Type("Root", NODE);
    public static final Type   PREDICATE        = new Type(Predicate.class, NODE);
    public static final Type   RELATION         = new Type("Relation", PREDICATE);
    public static final Type   VARIABLE         = new Type(Variable.class, NODE);
    public static final Type   RULE             = new Type(Rule.class, ROOT);
    public static final Type   FUNCTOR          = new Type(Functor.class, ROOT);
    public static final Type   FACT             = new Type(Fact.class, ROOT);
    public static final Type   PATTERN          = new Type("Pattern", PATTERN_GROUP, Type.NODE);
    public static final Type   QUERY            = new Type(Query.class, Type.ROOT);

    public static List<Type> predefined() {
        return List.of(TYPE(), //
                NODE, //
                FUNCTION, //
                TERMINAL, //
                LITERAL, //
                ROOT, //
                PREDICATE, //
                RELATION, //
                VARIABLE, //
                RULE, //
                FUNCTOR, //
                FACT, //
                PATTERN, //
                QUERY);
    }

    private static Type TYPE = null;

    public static Type TYPE() {
        if (TYPE == null) {
            TYPE = new Type() {
                @Serial
                private static final long serialVersionUID = -2303866849518548877L;

                @Override
                public Node typeOrFunctor() {
                    return this;
                }

                @Override
                public int hashCode() {
                    return 0;
                }

                @Override
                public Set<Type> supers() {
                    return Set.of();
                }

                @Override
                public String group() {
                    return DEFAULT_GROUP;
                }
            };
        }
        return TYPE;
    }

    private Type       list;
    private Type       literal;
    private Type       function;
    private List<Type> allSupers;

    private Type() {
        super((Type) null, List.of(), Type.class, null, DEFAULT_GROUP);
    }

    private Type(Object[] array) {
        super(array);
    }

    private Type(Class<?> clss, String group, Type... supers) {
        super(TYPE(), List.of(), clss, Set.of(supers), group);
    }

    private Type(Class<?> clss, Type... supers) {
        super(TYPE(), List.of(), clss, Set.of(supers), group(supers));
    }

    public Type(String name, String group, Type... supers) {
        super(TYPE(), List.of(), name, supers.length == 0 ? Set.of(NODE) : Set.of(supers), group);
    }

    public Type(String name, Type... supers) {
        super(TYPE(), List.of(), name, supers.length == 0 ? Set.of(NODE) : Set.of(supers), group(supers));
    }

    protected Type(TokenType type) {
        super(TYPE(), List.of(), type, Set.of(), DEFAULT_GROUP);
    }

    public Type(List<AstElement> elements, String name, Collection<Type> supers, String group) {
        super(TYPE(), elements, name, supers.asSet(), group);
    }

    public Type(Type super1, Type super2) {
        super(TYPE(), //
                List.of(), //
                Set.of(super1, super2), //
                Set.of(super1, super2) //
                        .addAll(super1.supers().remove(NODE).replaceAll(s1 -> new Type(s1, super2))) //
                        .addAll(super2.supers().remove(NODE).replaceAll(s2 -> new Type(super1, s2))) //
                , super1.group());
    }

    private Type(Type element, String group) {
        super(TYPE(), List.of(element), "List" + element, Set.of(NODE), group, element);
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
        if (isList()) {
            return (Type) get(3);
        } else {
            return this;
        }
    }

    public String group() {
        return (String) get(2);
    }

    @SuppressWarnings("unchecked")
    public Set<Type> many() {
        return (Set<Type>) get(0);
    }

    public boolean isList() {
        return length() == 4;
    }

    public boolean isMany() {
        return get(0) instanceof Set;
    }

    public Type function() {
        if (isFunction()) {
            return this;
        } else if (function == null) {
            return function = equals(NODE) ? FUNCTION : new Type(this, FUNCTION);
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
            return literal = equals(NODE) ? LITERAL : new Type(this, LITERAL);
        }
        return literal;
    }

    public boolean isLiteral() {
        return LITERAL.isAssignableFrom(this);
    }

    public Type list() {
        return list(group());
    }

    public Type list(String group) {
        if (!group.equals(group())) {
            return new Type(this, group);
        }
        if (list == null) {
            list = new Type(this, group);
        }
        return list;
    }

    public TokenType tokenType() {
        Object type = get(0);
        return type instanceof TokenType ? ((TokenType) type) : null;
    }

    @SuppressWarnings("unchecked")
    public String name() {
        Object type = get(0);
        if (type instanceof Set) {
            return ((Set<Type>) type).map(t -> t.name()).sorted().sequential().reduce("", (a, b) -> a + b);
        }
        if (type instanceof TokenType) {
            return ((TokenType) type).name();
        }
        if (type instanceof Class) {
            return ((Class<?>) type).getSimpleName();
        }
        return (String) type;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Node> clss() {
        Object type = get(0);
        return type instanceof Class clss ? clss : null;
    }

    @Override
    public String toString(TokenType[] previous) {
        return "<" + name() + ">";
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
    protected Type struct(Object[] array) {
        return new Type(array);
    }

    @Override
    public Type set(int i, Object... a) {
        return (Type) super.set(i, a);
    }

    public boolean isAssignableFrom(Type type) {
        if (isMany()) {
            return many().allMatch(s -> s.isAssignableFrom(type));
        }
        return equals(type) || type.supers().anyMatch(this::isAssignableFrom);
    }

    public boolean isAssignableFrom(Class<?> type) {
        Object clss = get(0);
        return clss instanceof Class && ((Class<?>) clss).isAssignableFrom(type);
    }

    @Override
    protected Node typeForEquals() {
        return TYPE();
    }

}
