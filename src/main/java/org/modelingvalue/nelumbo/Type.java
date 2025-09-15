//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
    private static final long serialVersionUID = -4583279157841144493L;
    //
    public static final Type  NODE             = new Type(Node.class);
    public static final Type  FUNCTION         = new Type("Function", NODE);
    public static final Type  TERMINAL         = new Type(Terminal.class, NODE);
    public static final Type  LITERAL          = new Type("Literal", TERMINAL);
    public static final Type  ROOT             = new Type("Root", NODE);
    public static final Type  PREDICATE        = new Type(Predicate.class, NODE);
    public static final Type  RELATION         = new Type("Relation", PREDICATE, ROOT);
    public static final Type  RESULT           = new Type("Result", ROOT);
    public static final Type  VARIABLE         = new Type(Variable.class, ROOT);
    public static final Type  RULE             = new Type(Rule.class, ROOT);
    public static final Type  FUNCTOR          = new Type(Functor.class, ROOT);
    public static final Type  STRING           = new Type(String.class);
    public static final Type  PATTERN          = new Type("Pattern", Type.ROOT);

    public static final Type  TYPE_NAME        = new Type("TypeName", Type.NODE);
    public static final Type  VAR_NAME         = new Type("VarName", Type.NODE);
    public static final Type  NATIVE           = new Type("Native", Type.NODE);
    public static final Type  PRECEDENCE       = new Type("Precedence", Type.NODE);
    public static final Type  FACTS            = new Type("Facts", Type.NODE);
    public static final Type  FALSEHOODS       = new Type("Falsehoods", Type.NODE);

    public static List<Type> predefined() {
        return List.of(TYPE(), //
                NODE, //
                FUNCTION, //
                TERMINAL, //
                LITERAL, //
                ROOT, //
                PREDICATE, //
                RELATION, //
                RESULT, //
                VARIABLE, //
                RULE, //
                FUNCTOR, //
                STRING //
        );
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
                    return Set.of(ROOT);
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
        super((Type) null, List.of(), Type.class, null);
    }

    private Type(Object[] array) {
        super(array);
    }

    private Type(Class<?> clss, Type... supers) {
        super(TYPE(), List.of(), clss, Set.of(supers));
    }

    protected Type(TokenType type) {
        super(TYPE(), List.of(), type, Set.of());
    }

    public Type(List<AstElement> elements, String name, Collection<Type> supers) {
        super(TYPE(), elements, name, supers.asSet());
    }

    public Type(String name, Type... supers) {
        super(TYPE(), List.of(), name, supers.length == 0 ? Set.of(NODE) : Set.of(supers));
    }

    public Type(Type super1, Type super2) {
        super(TYPE(), //
                List.of(), //
                Set.of(super1, super2), //
                Set.of(super1, super2) //
                        .addAll(super1.supers().remove(NODE).replaceAll(s1 -> new Type(s1, super2))) //
                        .addAll(super2.supers().remove(NODE).replaceAll(s2 -> new Type(super1, s2))) //
        );
    }

    private Type(Type element) {
        super(TYPE(), List.of(element), "List", Set.of(NODE), element);
    }

    public Type element() {
        if (isList()) {
            return (Type) get(2);
        } else {
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<Type> many() {
        return (Set<Type>) get(0);
    }

    public boolean isList() {
        return length() == 3;
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
        if (list == null) {
            list = new Type(this);
        }
        return list;
    }

    public TokenType tokenType() {
        Object type = get(0);
        return type instanceof TokenType ? ((TokenType) type) : null;
    }

    public String name() {
        Object type = get(0);
        if (type instanceof Set) {
            String many = many().toString();
            return many.substring(5, many.length() - 2).replace(">,<", "");
        }
        if (type instanceof TokenType) {
            return ((TokenType) type).name();
        }
        if (type instanceof Class) {
            return ((Class<?>) type).getSimpleName();
        }
        return (String) type;
    }

    @Override
    public String toString() {
        return "<" + name() + ">";
    }

    @SuppressWarnings("unchecked")
    public Set<Type> supers() {
        return (Set<Type>) get(1);
    }

    public List<Type> allsupers() {
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

}
