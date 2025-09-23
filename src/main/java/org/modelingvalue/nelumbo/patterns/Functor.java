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
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.ThrowingBiFunction;

public class Functor extends Node {
    @Serial
    private static final long serialVersionUID = -1901047746034698364L;

    public static Functor of(Pattern pattern, Type result, Constructor<? extends Node> constructor) {
        return new Functor(List.of(), pattern, result, constructor);
    }

    public static Functor of(Pattern pattern, Type result, ThrowingBiFunction<List<AstElement>, Object[], ? extends Node> function) {
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
    public ThrowingBiFunction<List<AstElement>, Object[], ? extends Node> function() {
        Object val = get(2);
        return val instanceof ThrowingBiFunction ? (ThrowingBiFunction<List<AstElement>, Object[], ? extends Node>) val : null;
    }

    public Type leftType() {
        Pattern pattern = pattern();
        if (pattern instanceof SequencePattern sp) {
            if (sp.elements().first() instanceof NodeTypePattern ntp) {
                return ntp.nodeType();
            }
        }
        return null;
    }

    public int leftPrecedence() {
        Pattern pattern = pattern();
        if (pattern instanceof SequencePattern sp) {
            if (sp.elements().first() instanceof NodeTypePattern ntp) {
                return ntp.leftPrecedence();
            }
        }
        throw new UnsupportedOperationException();
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

    private Node construct(List<AstElement> elements, Object[] args) throws ParseException {
        Constructor<? extends Node> constructor = constructor();
        if (constructor != null) {
            try {
                return constructor.newInstance(this, elements, args);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                throw new ParseException(e, "Exception during Node construction", elements.toArray(i -> new AstElement[i]));
            }
        }
        ThrowingBiFunction<List<AstElement>, Object[], ? extends Node> function = function();
        if (function != null) {
            return function.apply(elements, args);
        }
        return resultType() == Type.PREDICATE ? new Predicate(this, elements, args) : new Node(this, elements, args);
    }

    public Patterns patterns() {
        List<Pattern> fixed = pattern().fixed(List.of(), new boolean[]{false});
        Patterns patterns = Patterns.EMPTY.setFunctor(this);
        for (Pattern pattern : fixed.reverse()) {
            patterns = pattern.patterns(patterns);
        }
        return patterns.setPrecedence(null).setExpected(null);
    }

    @SuppressWarnings("unchecked")
    public Node postParse(String group, Parser parser, ParseResult result) throws ParseException {
        pattern().parse(result.nextToken(), group, parser, null, result);
        return construct(result.elements(), result.args().toArray());
    }

}
