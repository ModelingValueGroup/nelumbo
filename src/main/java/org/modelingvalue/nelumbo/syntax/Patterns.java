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

package org.modelingvalue.nelumbo.syntax;

import java.io.Serial;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.util.Quadruple;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.SyntaxPattern;

public class Patterns extends Quadruple<Map<Object, Patterns>, SyntaxPattern, Integer, Type> {
    @Serial
    private static final long    serialVersionUID = 7933114430825879121L;

    public static final Patterns EMPTY            = new Patterns(Map.of(), null, -1, null);

    private Patterns(Map<Object, Patterns> map, SyntaxPattern pattern, Integer precedence, Type expected) {
        super(map, pattern, precedence, expected);
    }

    public Patterns setPattern(SyntaxPattern pattern) {
        if (b() != null) {
            throw new IllegalArgumentException();
        }
        return new Patterns(a(), pattern, c(), d());
    }

    public Patterns put(Object key, Patterns patterns) {
        return new Patterns(a().put(key, patterns), b(), c(), d());
    }

    public Patterns setPrecedence(int precedence) {
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

    public SyntaxPattern pattern() {
        return b();
    }

    public Integer precedence() {
        return c();
    }

    public Type expected() {
        return d();
    }

    public ParseResult preParse(ParseResult result, Parser parser) throws ParseException {
        if (a().isEmpty()) {
            result.setPattern(pattern());
            return result;
        } else {
            if (token(result, parser) != null) {
                return result;
            }
            Type expected = expected();
            if (expected != null) {
                return node(result, parser, expected);
            }
        }
        return null;
    }

    private ParseResult token(ParseResult result, Parser parser) throws ParseException {
        Token token = parser.peek();
        String text = token.text();
        Map<Object, Patterns> map = a();
        Patterns patterns = map.get(text);
        if (patterns == null) {
            if (token.type() == TokenType.OPERATOR) {
                for (int i = text.length() - 1; i > 0; i--) {
                    String key = text.substring(0, i);
                    patterns = map.get(key);
                    if (patterns != null) {
                        parser.setToken(token.split(i));
                        break;
                    }
                }
            }
            if (patterns == null) {
                patterns = a().get(token.type());
                if (patterns != null) {
                    result.add(text);
                }
            }
        }
        if (patterns != null) {
            result.add(parser.consume());
            return patterns.preParse(result, parser);
        }
        return null;
    }

    private ParseResult node(ParseResult result, Parser parser, Type expected) throws ParseException {
        Node node = parser.parseNode(precedence(), expected);
        result.add(node);
        Map<Object, Patterns> map = a();
        for (Type type : node.type().allsupers()) {
            Patterns patterns = map.get(type);
            if (patterns != null) {
                return patterns.preParse(result, parser);
            }
        }
        throw new ParseException("no functor found for type " + node.type(), node.tokens());
    }

    public Patterns merge(Patterns patterns) {
        if (patterns == null) {
            return this;
        }
        SyntaxPattern s = merge(pattern(), patterns.pattern());
        Integer p = merge(precedence(), patterns.precedence());
        Type e = merge(expected(), patterns.expected());
        Map<Object, Patterns> m = map().addAll(patterns.map(), (a, b) -> a.merge(b));
        return new Patterns(m, s, p, e);
    }

    private static <T> T merge(T t1, T t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            throw new IllegalArgumentException();
        }
        return t1 == null ? t2 : t1;
    }
}
