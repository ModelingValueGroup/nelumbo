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

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;
import java.lang.reflect.Constructor;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseExceptionHandler;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.PatternMergeException;
import org.modelingvalue.nelumbo.syntax.ThrowingTriFunction;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class Functor extends Node {
    @Serial
    private static final long serialVersionUID = -1901047746034698364L;

    public static Functor of(List<AstElement> elements, Pattern pattern, Type result, boolean local, Constructor<?> constructor) {
        return new Functor(elements, pattern, result, local, constructor);
    }

    public static Functor of(List<AstElement> elements, Pattern pattern, Type result, boolean local, ThrowingTriFunction<List<AstElement>, Object[], Functor, ? extends Node> function) {
        return new Functor(elements, pattern, result, local, function);
    }

    public static Functor of(Pattern pattern, Type result, boolean local, ThrowingTriFunction<List<AstElement>, Object[], Functor, ? extends Node> function) {
        return new Functor(List.of(), pattern, result, local, function);
    }

    public static Functor of(Pattern pattern, Type result, boolean local) {
        return new Functor(List.of(), pattern, result, local, null);
    }

    private String     name;
    private List<Type> argTypes;
    private ParseState start;

    private Functor(List<AstElement> elements, Object... args) {
        super(Type.FUNCTOR, elements, args);
    }

    private Functor(Object[] array, Functor declaration) {
        super(array, declaration);
    }

    @Override
    protected Functor struct(Object[] array, Node declaration) {
        return new Functor(array, (Functor) declaration);
    }

    public Pattern pattern() {
        return (Pattern) get(0);
    }

    public Type resultType() {
        return (Type) get(1);
    }

    public boolean local() {
        return (Boolean) get(2);
    }

    @Override
    public Functor setBinding(Map<Variable, Object> vars) {
        return (Functor) super.setBinding(vars);
    }

    @Override
    public Functor resetDeclaration() {
        return (Functor) super.resetDeclaration();
    }

    @SuppressWarnings("unchecked")
    public Constructor<? extends Node> constructor() {
        Object val = get(3);
        return val instanceof Constructor ? (Constructor<? extends Node>) val : null;
    }

    @SuppressWarnings("unchecked")
    public ThrowingTriFunction<List<AstElement>, Object[], Functor, ? extends Node> function() {
        Object val = get(3);
        return val instanceof ThrowingTriFunction ? (ThrowingTriFunction<List<AstElement>, Object[], Functor, ? extends Node>) val : null;
    }

    public NodeTypePattern left() {
        Pattern pattern = pattern();
        if (pattern instanceof SequencePattern sp) {
            if (sp.elements().first() instanceof NodeTypePattern ntp) {
                return ntp;
            }
        }
        return null;
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

    @SuppressWarnings("unchecked")
    public List<Type> argTypes() {
        if (argTypes == null) {
            argTypes = pattern().argTypes(List.of());
        }
        return argTypes;
    }

    @Override
    public String toString(TokenType[] previous) {
        return resultType() + "::=" + pattern();
    }

    public Node construct(List<AstElement> elements, Object[] args, ParseExceptionHandler handler) throws ParseException {
        Constructor<? extends Node> constructor = constructor();
        if (constructor != null) {
            try {
                return constructor.newInstance(this, elements, args);
            } catch (Exception e) {
                handleException(elements, handler, e);
            }
        }
        ThrowingTriFunction<List<AstElement>, Object[], Functor, ? extends Node> function = function();
        if (function != null) {
            try {
                return function.apply(elements, args, this);
            } catch (Exception e) {
                handleException(elements, handler, e);
            }
        }
        return Type.BOOLEAN.isAssignableFrom(resultType()) ? new Predicate(this, elements, args) : new Node(this, elements, args);
    }

    private void handleException(List<AstElement> elements, ParseExceptionHandler handler, Exception e) throws ParseException {
        if (e instanceof ParseException pe) {
            handler.addException(pe);
        } else {
            handler.addException(new ParseException(e, "Exception during Node construction: " + e, elements));
        }
    }

    public ParseState start() {
        if (start == null) {
            start = pattern().state(new ParseState(this), left(), this, List.of());
        }
        return start;
    }

    @Override
    public Functor setFunctor(Functor functor) {
        return (Functor) super.setFunctor(functor);
    }

    @Override
    public Functor setAstElements(List<AstElement> elements) {
        return (Functor) super.setAstElements(elements);
    }

    public Object[] args(List<AstElement> elements) {
        Pattern pattern = pattern();
        List<Object> args = pattern.args(List.of(), new Pattern.ElementIterator(elements, start(), this), List.of(), false);
        return pattern instanceof SequencePattern && args.size() == 1 && args.get(0) instanceof List<?> seq ? seq.toArray() : args.toArray();
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
    public Functor init(KnowledgeBase knowledgeBase) throws ParseException {
        return knowledgeBase.register(this);
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
        throw new PatternMergeException("Non deterministic pattern merge " + this + " <> " + other);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected Functor setBinding(Node declaration, Map<Variable, Object> vars) {
        Functor functor = (Functor) super.setBinding(declaration, vars);
        List<AstElement> from = astElements();
        List to = (List) setBinding(from, from, vars, -1);
        to = to.replaceAll(e -> e instanceof String s ? Pattern.t(s) : e);
        return from.equals(to) ? functor : functor.setAstElements(to);
    }

}
