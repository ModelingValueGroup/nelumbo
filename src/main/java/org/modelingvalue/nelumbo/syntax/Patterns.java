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

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;

public class Patterns {
    @Serial
    private static final long            serialVersionUID = 7933114430825879121L;

    public static final Patterns         EMPTY            = new Patterns((Functor) null);

    private final Map<Object, Patterns>  map;
    private final Functor                functor;
    private final Integer                leftPrecedence;
    private final Integer                innerPrecedence;
    private final String                 group;
    private final Set<RepetitionPattern> startRepetitions;
    private final RepetitionPattern      endRepetition;

    public Patterns(Functor functor) {
        this(Map.of(), functor, null, null, null, Set.of(), null);
    }

    public Patterns(RepetitionPattern repetition) {
        this(Map.of(), null, null, null, null, Set.of(), repetition);
    }

    public Patterns(RepetitionPattern repetition, Integer leftPrecedence) {
        this(Map.of(), null, leftPrecedence, null, null, Set.of(repetition), null);
    }

    public Patterns(Object key, Patterns value) {
        this(Map.of(Entry.of(key, value)), null, null, null, null, Set.of(), null);
    }

    public Patterns(Type key, Patterns value, Integer leftPrecedence, Integer innerPrecedence) {
        this(Map.of(Entry.of(key, value)), null, leftPrecedence, innerPrecedence, key.group(), Set.of(), null);
    }

    private Patterns(Map<Object, Patterns> map, Functor functor, Integer leftPrecedence, Integer innerPrecedence, String group, Set<RepetitionPattern> startRepetitions, RepetitionPattern endRepetition) {
        this.map = map;
        this.functor = functor;
        this.leftPrecedence = leftPrecedence;
        this.innerPrecedence = innerPrecedence;
        this.group = group;
        this.startRepetitions = startRepetitions;
        this.endRepetition = endRepetition;
    }

    public Map<Object, Patterns> map() {
        return map;
    }

    public Functor functor() {
        return functor;
    }

    public Integer leftPrecedence() {
        return leftPrecedence;
    }

    public Integer innerPrecedence() {
        return innerPrecedence;
    }

    public String group() {
        return group;
    }

    public Set<RepetitionPattern> startRepetitions() {
        return startRepetitions;
    }

    public RepetitionPattern endRepetition() {
        return endRepetition;
    }

    public ParseResult parse(Token token, ParseResult result, Map<RepetitionPattern, Patterns> outer, boolean pre) throws ParseException {
        if (pre && !startRepetitions().isEmpty()) {
            result.endPreParse(this, token);
            return result;
        }
        Map<RepetitionPattern, Patterns> inner = outer;
        for (RepetitionPattern repetition : startRepetitions()) {
            inner = inner.put(repetition, this);
        }
        do {
            if (endRepetition() != null && (map().isEmpty() || outer.get(endRepetition()).token(token, result, null, pre) != null)) {
                result.endRepetition(endRepetition(), token);
                return result;
            }
            if (token(token, result, inner, pre) == null) {
                if (pre && group() != null) {
                    result.endPreParse(this, token);
                    return result;
                } else if (node(token, result, inner, pre) == null) {
                    break;
                }
            }
            if (result.endRepetition() == null || !startRepetitions().contains(result.endRepetition())) {
                return result;
            }
            token = result.nextToken();
            result.endRepetition(null, token);
        } while (true);
        if (functor() == null) {
            if (pre) {
                return null;
            } else {
                throw new ParseException("Unexpected token " + token + ", expected " + expectedTokens(), token);
            }
        }
        result.endPostParse(functor(), token);
        return result;
    }

    private String expectedTokens() {
        return map().toKeys().filter(k -> k instanceof String || k instanceof TokenType).//
                map(o -> o instanceof String ? ("\"" + o + "\"") : o.toString()).//
                reduce("", (a, b) -> a.isEmpty() ? b : a + " or " + b);
    }

    private ParseResult token(Token token, ParseResult result, Map<RepetitionPattern, Patterns> repetitions, boolean pre) throws ParseException {
        if (map().isEmpty()) {
            return null;
        }
        String text = token.text();
        Patterns patterns = map().get(text);
        if (patterns == null) {
            if (token.type() == TokenType.OPERATOR) {
                for (int i = text.length() - 1; i > 0; i--) {
                    String key = text.substring(0, i);
                    patterns = map().get(key);
                    if (patterns != null) {
                        token = result.addSplit(token.previous(), token.split(i));
                        break;
                    }
                }
            }
            if (patterns == null) {
                patterns = map().get(token.type());
                if (repetitions != null && patterns != null && token.type().variable()) {
                    result.add(text);
                }
            }
        }
        if (patterns != null) {
            if (repetitions == null) {
                return result;
            }
            result.add(token);
            if (patterns.parse(token.next(), result, repetitions, pre) != null) {
                return result;
            }
        }
        return null;
    }

    private ParseResult node(Token token, ParseResult result, Map<RepetitionPattern, Patterns> repetitions, boolean pre) throws ParseException {
        if (group() == null) {
            return null;
        }
        Node node = result.parser().parseNode(token, innerPrecedence(), group());
        result.add(node);
        for (Type sup : node.type().allsupers()) {
            Patterns patterns = map().get(sup);
            if (patterns != null) {
                if (patterns.parse(node.nextToken(), result, repetitions, pre) != null) {
                    return result;
                }
            }
        }
        throw new ParseException("Node " + node + " of unexpected type " + node.type() + ", expected " + expectedTypes(), node);
    }

    private String expectedTypes() {
        return map().toKeys().filter(Type.class).map(Object::toString).//
                reduce("", (a, b) -> a.isEmpty() ? b : a + " or " + b);
    }

    public Patterns merge(Patterns patterns) {
        if (patterns == null) {
            return this;
        }
        return new Patterns(map().addAll(patterns.map(), (a, b) -> a.merge(b)), //
                merge(functor(), patterns.functor()), //
                merge(leftPrecedence(), patterns.leftPrecedence()), //
                merge(innerPrecedence(), patterns.innerPrecedence()), //
                merge(group(), patterns.group()), //
                startRepetitions().addAll(patterns.startRepetitions()), //
                merge(endRepetition(), patterns.endRepetition()));
    }

    private static <T> T merge(T t1, T t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            throw new PatternMergeException("Non deterministic pattern merge " + t1 + " <> " + t2);
        }
        return t1 == null ? t2 : t1;
    }

    @Override
    public String toString() {
        return map().toKeys().asSet().toString().substring(3);
    }
}
