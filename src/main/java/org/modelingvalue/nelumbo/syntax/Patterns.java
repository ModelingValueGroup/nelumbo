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
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.patterns.Functor;

public class Patterns extends Pair<Map<Object, Patterns>, Functor> {
    @Serial
    private static final long    serialVersionUID = 7933114430825879121L;

    public static final Patterns EMPTY            = new Patterns(Map.of(), null);

    private Patterns(Map<Object, Patterns> map, Functor functor) {
        super(map, functor);
    }

    public Patterns setFunctor(Functor functor) {
        if (b() != null) {
            throw new IllegalArgumentException();
        }
        return new Patterns(a(), functor);
    }

    public Patterns put(Object key, Patterns patterns) {
        return new Patterns(a().put(key, patterns), b());
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

    public ParseResult preParse(Token token, ParseResult result, Parser parser) throws ParseException {
        if (token(token, result, parser) != null) {
            return result;
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

    public Patterns merge(Patterns patterns) {
        if (patterns == null) {
            return this;
        }
        Functor f = merge(functor(), patterns.functor());
        Map<Object, Patterns> m = map().addAll(patterns.map(), (a, b) -> a.merge(b));
        return new Patterns(m, f);
    }

    private static Functor merge(Functor t1, Functor t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            throw new PatternMergeException("Non deterministic pattern merge " + t1 + " <>  " + t2);
        }
        return t1 == null ? t2 : t1;
    }

    @Override
    public String toString() {
        Map<Object, Patterns> map = map();
        return map.isEmpty() ? functor().toString() : map.toString().substring(3);
    }
}
