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

package org.modelingvalue.nelumbo.syntax;

import java.io.Serial;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.util.Quadruple;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.Functor;

public class Patterns extends Quadruple<Map<Object, Patterns>, Functor, Integer, Type> {
    @Serial
    private static final long    serialVersionUID = 7933114430825879121L;

    public static final Patterns EMPTY            = new Patterns(Map.of(), null, null, null);

    private Patterns(Map<Object, Patterns> map, Functor functor, Integer precedence, Type nodeType) {
        super(map, functor, precedence, nodeType);
    }

    public Patterns setFunctor(Functor functor) {
        if (b() != null) {
            throw new IllegalArgumentException();
        }
        return new Patterns(a(), functor, c(), d());
    }

    public Patterns put(Object key, Patterns patterns) {
        return new Patterns(a().put(key, patterns), b(), c(), d());
    }

    public Patterns setPrecedence(Integer precedence) {
        return new Patterns(a(), b(), precedence, d());
    }

    public Patterns setExpected(Type expected) {
        return new Patterns(a(), b(), c(), expected);
    }

    public Patterns get(Object key) {
        return a().get(key);
    }

    public Map<Object, Patterns> map() {
        return a();
    }

    public Functor functor() {
        return b();
    }

    public Integer precedence() {
        return c();
    }

    public Type nodeType() {
        return d();
    }

    public ParseResult preParse(Token token, ParseResult result, Parser parser) throws ParseException {
        if (token(token, result, parser) != null) {
            return result;
        }
        Type nodeType = nodeType();
        if (nodeType != null) {
            return node(token, result, parser, nodeType);
        }
        Functor functor = functor();
        if (functor == null) {
            return null;
        }
        result.endPreParse(functor(), token);
        return result;
    }

    private ParseResult token(Token token, ParseResult result, Parser parser) throws ParseException {
        Map<Object, Patterns> map = a();
        String text = token.text();
        Patterns patterns = map.get(text);
        if (patterns == null) {
            if (token.type() == TokenType.OPERATOR) {
                for (int i = text.length() - 1; i > 0; i--) {
                    String key = text.substring(0, i);
                    patterns = map.get(key);
                    if (patterns != null) {
                        token = token.split(i);
                        break;
                    }
                }
            }
            if (patterns == null) {
                patterns = map.get(token.type());
                if (patterns != null) {
                    result.add(text);
                }
            }
        }
        if (patterns != null) {
            result.add(token);
            if (patterns.preParse(token.next(), result, parser) != null) {
                return result;
            }
        }
        return null;
    }

    private ParseResult node(Token token, ParseResult result, Parser parser, Type type) throws ParseException {
        Node node = parser.parseNode(token, precedence(), type.group());
        if (!type.isAssignableFrom(node.type())) {
            return null;
        }
        result.add(node);
        token = node.nextToken();
        Map<Object, Patterns> map = a();
        for (Type sup : node.type().allsupers()) {
            Patterns patterns = map.get(sup);
            if (patterns != null) {
                if (patterns.preParse(token, result, parser) != null) {
                    return result;
                }
            }
        }
        throw new ParseException("No functor found for type " + node.type(), node);
    }

    public Patterns merge(Patterns patterns) {
        if (patterns == null) {
            return this;
        }
        Functor s = merge(functor(), patterns.functor());
        Integer p = merge(precedence(), patterns.precedence());
        Type e = merge(nodeType(), patterns.nodeType());
        Map<Object, Patterns> m = map().addAll(patterns.map(), (a, b) -> a.merge(b));
        return new Patterns(m, s, p, e);
    }

    private static <T> T merge(T t1, T t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            throw new IllegalArgumentException("Non deterministic pattern merge " + t1 + " <>  " + t2);
        }
        return t1 == null ? t2 : t1;
    }

    @Override
    public String toString() {
        Map<Object, Patterns> map = map();
        return map.isEmpty() ? functor().toString() : map.toString().substring(3);
    }
}
