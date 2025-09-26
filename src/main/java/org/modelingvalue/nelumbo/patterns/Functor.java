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

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.ThrowingTriFunction;

public class Functor extends Node {
    @Serial
    private static final long serialVersionUID = -1901047746034698364L;

    public static Functor of(Pattern pattern, Type result, Constructor<? extends Node> constructor) {
        return new Functor(List.of(), pattern, result, constructor);
    }

    public static Functor of(Pattern pattern, Type result, ThrowingTriFunction<List<AstElement>, Object[], Functor, ? extends Node> function) {
        return new Functor(List.of(), pattern, result, function);
    }

    public static Functor of(Pattern pattern, Type result) {
        return new Functor(List.of(), pattern, result, null);
    }

    private String     name;
    private List<Type> args;

    public Functor(List<AstElement> elements, Object... args) {
        super(Type.FUNCTOR, elements, args);
    }

    private Functor(Object[] array) {
        super(array);
    }

    @Override
    protected Functor struct(Object[] array) {
        return new Functor(array);
    }

    public Pattern pattern() {
        return (Pattern) get(0);
    }

    public Type resultType() {
        return (Type) get(1);
    }

    @SuppressWarnings("unchecked")
    public Constructor<? extends Node> constructor() {
        Object val = get(2);
        return val instanceof Constructor ? (Constructor<? extends Node>) val : null;
    }

    @SuppressWarnings("unchecked")
    public ThrowingTriFunction<List<AstElement>, Object[], Functor, ? extends Node> function() {
        Object val = get(2);
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

    public ParseException error() {
        return length() > 3 ? (ParseException) get(3) : null;
    }

    public Functor setError(ParseException exception) {
        return set(3, exception);
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
    public List<Type> args() {
        if (args == null) {
            args = pattern().args();
        }
        return args;
    }

    @Override
    public String toString() {
        String types = args().map(Type::toString).collect(Collectors.joining(", "));
        return name() + "(" + types + ")";
    }

    public Node construct(List<AstElement> elements, Object[] args) throws ParseException {
        Constructor<? extends Node> constructor = constructor();
        if (constructor != null) {
            try {
                return constructor.newInstance(this, elements, args);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                throw new ParseException(e, "Exception during Node construction", elements.toArray(i -> new AstElement[i]));
            }
        }
        ThrowingTriFunction<List<AstElement>, Object[], Functor, ? extends Node> function = function();
        if (function != null) {
            return function.apply(elements, args, this);
        }
        return Type.PREDICATE.isAssignableFrom(resultType()) ? new Predicate(this, elements, args) : new Node(this, elements, args);
    }

    public Patterns patterns() {
        return pattern().patterns(new Patterns(this), left());
    }

    @Override
    public Functor setAstElements(List<AstElement> elements) {
        return (Functor) super.setAstElements(elements);
    }

}
