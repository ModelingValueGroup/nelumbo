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

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class Type extends Node {
    private static final long serialVersionUID = -4583279157841144493L;

    public static final Type  FUNCTION         = new Type("Function", Node.TYPE);
    public static final Type  LITERAL          = new Type("Literal", Terminal.TYPE);
    public static final Type  ROOT             = new Type("Root", Node.TYPE);
    public static final Type  FACT             = new Type("Fact", Relation.TYPE, ROOT);

    private static Type       TYPE             = null;

    public static final Type TYPE() {
        if (TYPE == null) {
            TYPE = new Type() {
                private static final long serialVersionUID = -2303866849518548877L;

                @Override
                public Object get(int i) {
                    return i == 0 ? this : i == 2 ? Set.of(Node.TYPE) : super.get(i);
                }

                @Override
                public int hashCode() {
                    return 0;
                }

                @Override
                public Set<Type> supers() {
                    return Set.of(Type.ROOT);
                }
            };
        }
        return TYPE;
    }

    private Type list;
    private Type literal;
    private Type function;

    private Type() {
        super((Type) null, Type.class, null);
        KnowledgeBase.CURRENT.get().addType(this);
    }

    public Type(Class<?> clss, Type... supers) {
        super(TYPE(), clss, Set.of(supers));
        KnowledgeBase.CURRENT.get().addType(this);
    }

    public Type(TokenType type) {
        super(TYPE(), type, Set.of());
        KnowledgeBase.CURRENT.get().addType(this);
    }

    public Type(String name, Collection<Type> supers) {
        super(TYPE(), name, supers.asSet());
        KnowledgeBase.CURRENT.get().addType(this);
    }

    public Type(String name, Type... supers) {
        super(TYPE(), name, supers.length == 0 ? Set.of(Node.TYPE) : Set.of(supers));
        KnowledgeBase.CURRENT.get().addType(this);
    }

    private Type(Type element) {
        super(TYPE(), "List", element.supers(), element);
    }

    private Type(Object[] array) {
        super(array);
    }

    public Type element() {
        if (isList()) {
            return (Type) get(3);
        } else {
            return this;
        }
    }

    public boolean isList() {
        return length() == 4;
    }

    public Type function() {
        if (isFunction()) {
            return this;
        } else if (function == null) {
            return function = new Type(name() + "Fun", this, Type.FUNCTION);
        }
        return function;
    }

    public boolean isFunction() {
        return Type.FUNCTION.isAssignableFrom(this);
    }

    public Type literal() {
        if (isLiteral()) {
            return this;
        } else if (literal == null) {
            return literal = new Type(name() + "Lit", this, Type.LITERAL);
        }
        return literal;
    }

    public boolean isLiteral() {
        return Type.LITERAL.isAssignableFrom(this);
    }

    public Type list() {
        if (list == null) {
            list = new Type(this);
        }
        return list;
    }

    public TokenType tokenType() {
        Object type = get(1);
        return type instanceof TokenType ? ((TokenType) type) : null;
    }

    public String name() {
        Object type = get(1);
        return type instanceof TokenType ? ((TokenType) type).name() : type instanceof Class ? ((Class<?>) type).getSimpleName() : (String) type;
    }

    @Override
    public String toString() {
        if (isList()) {
            return "<" + element().name() + "*>";
        }
        return "<" + name() + ">";
    }

    @SuppressWarnings("unchecked")
    public Set<Type> supers() {
        return (Set<Type>) get(2);
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
        return equals(type) || type.supers().anyMatch(this::isAssignableFrom);
    }

    public boolean isAssignableFrom(Class<?> type) {
        Object clss = get(1);
        return clss instanceof Class && ((Class<?>) clss).isAssignableFrom(type);
    }

}
