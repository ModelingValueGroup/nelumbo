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
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;

public class ParseState {
    @Serial
    private static final long                 serialVersionUID = 7933114430825879121L;

    public static final ParseState            EMPTY            = new ParseState((Functor) null);

    private final Map<Object, ParseState>     transitions;
    private final Functor                     functor;
    private final Integer                     leftPrecedence;
    private final Integer                     innerPrecedence;
    private final String                      group;
    private final Set<RepetitionPattern>      startRepetitions;
    private final Set<RepetitionPattern>      endRepetitions;
    private final Map<Functor, List<Integer>> branches;

    public ParseState(Functor functor) {
        this(Map.of(), functor, null, null, null, Set.of(), Set.of(), Map.of());
    }

    public ParseState(Functor functor, List<Integer> branche) {
        this(Map.of(), null, null, null, null, Set.of(), Set.of(), Map.of(Entry.of(functor, branche)));
    }

    public ParseState(RepetitionPattern endRepetition) {
        this(Map.of(), null, null, null, null, Set.of(), Set.of(endRepetition), Map.of());
    }

    public ParseState(RepetitionPattern startRepetition, Integer leftPrecedence) {
        this(Map.of(), null, leftPrecedence, null, null, Set.of(startRepetition), Set.of(), Map.of());
    }

    public ParseState(String text, ParseState next) {
        this(Map.of(Entry.of(text, next)), null, null, null, null, Set.of(), Set.of(), Map.of());
    }

    public ParseState(TokenType tokenType, ParseState next) {
        this(Map.of(Entry.of(tokenType, next)), null, null, null, null, Set.of(), Set.of(), Map.of());
    }

    public ParseState(Type nodeType, ParseState next, Integer leftPrecedence, Integer innerPrecedence) {
        this(Map.of(Entry.of(nodeType, next)), null, leftPrecedence, innerPrecedence, nodeType.group(), Set.of(), Set.of(), Map.of());
    }

    private ParseState(Map<Object, ParseState> transitions, Functor functor, Integer leftPrecedence, Integer innerPrecedence, String group, //
            Set<RepetitionPattern> startRepetitions, Set<RepetitionPattern> endRepetitions, Map<Functor, List<Integer>> branches) {
        this.transitions = transitions;
        this.functor = functor;
        this.leftPrecedence = leftPrecedence;
        this.innerPrecedence = innerPrecedence;
        this.group = group;
        this.startRepetitions = startRepetitions;
        this.endRepetitions = endRepetitions;
        this.branches = branches;
    }

    public Map<Object, ParseState> transitions() {
        return transitions;
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

    public Set<RepetitionPattern> endRepetitions() {
        return endRepetitions;
    }

    public Map<Functor, List<Integer>> branches() {
        return branches;
    }

    public PatternResult parse(Token token, PatternResult result, Map<RepetitionPattern, ParseState> outerRepetitions, boolean pre) throws ParseException {
        if (pre && !startRepetitions().isEmpty()) {
            result.endPreParse(this, token);
            return result;
        }
        Map<RepetitionPattern, ParseState> innerRepetitions = outerRepetitions;
        for (RepetitionPattern start : startRepetitions()) {
            innerRepetitions = innerRepetitions.put(start, this);
        }
        do {
            if (token(token, result, innerRepetitions, pre) == null) {
                if (pre && group() != null) {
                    result.endPreParse(this, token);
                    return result;
                } else if (group() != null && matchOuter(token, result, outerRepetitions, pre)) {
                    result.endRepetition(endRepetitions(), token, 1);
                    return result;
                } else if (node(token, result, innerRepetitions, pre) == null) {
                    break;
                }
            }
            if (!startRepetitions().anyMatch(result.endRepetitions()::contains)) {
                result.countDepth();
                return result;
            }
            token = result.nextToken();
        } while (true);
        if (endRepetitions().anyMatch(outerRepetitions::containsKey)) {
            result.endRepetition(endRepetitions(), token, 1);
            return result;
        }
        if (functor() == null) {
            if (pre) {
                return null;
            } else {
                result.addException(new ParseException("Unexpected token " + token + ", expected " + expectedTokens(), token));
            }
        }
        result.endPostParse(functor(), token);
        return result;
    }

    private boolean matchOuter(Token token, PatternResult result, Map<RepetitionPattern, ParseState> outerRepetitions, boolean pre) throws ParseException {
        for (RepetitionPattern end : endRepetitions()) {
            ParseState state = outerRepetitions.get(end);
            if (state != null && state.token(token, result, null, pre) != null) {
                return true;
            }
        }
        return false;
    }

    private String expectedTokens() {
        return transitions().toKeys().filter(k -> k instanceof String || k instanceof TokenType).//
                map(o -> o instanceof String ? ("\"" + o + "\"") : o.toString()).//
                reduce("", (a, b) -> a.isEmpty() ? b : a + " or " + b);
    }

    private PatternResult token(Token token, PatternResult result, Map<RepetitionPattern, ParseState> repetitions, boolean pre) throws ParseException {
        if (transitions().isEmpty()) {
            return null;
        }
        Object input = null;
        String text = token.text();
        ParseState next = transitions().get(text);
        if (next != null) {
            input = text;
        } else {
            TokenType type = token.type();
            if (type == TokenType.OPERATOR) {
                for (int i = text.length() - 1; i > 0; i--) {
                    String key = text.substring(0, i);
                    next = transitions().get(key);
                    if (next != null) {
                        input = key;
                        token = result.addSplit(token, token.split(i));
                        break;
                    }
                }
            }
            if (next == null) {
                next = transitions().get(type);
                if (next != null) {
                    input = type;
                } else {
                    next = transitions().get(TokenType.NEWLINE);
                    if (next != null && !Pattern.isEndOfLine(token)) {
                        next = null;
                    }
                }
            }
        }
        if (next != null) {
            if (repetitions == null) {
                return result;
            }
            if (input != null) {
                result.add(token);
                token.setBranches(next.branches);
            }
            return next.parse(token.next(), result, repetitions, pre);
        }
        return null;

    }

    private PatternResult node(Token token, PatternResult result, Map<RepetitionPattern, ParseState> repetitions, boolean pre) throws ParseException {
        if (group() == null) {
            return null;
        }
        Node node = result.parser().parseNode(token, innerPrecedence(), group());
        if (node != null) {
            result.add(node);
            for (Type sup : node.type().allSupers()) {
                ParseState next = transitions().get(sup);
                if (next != null) {
                    if (next.parse(node.nextToken(), result, repetitions, pre) != null) {
                        node.setBranches(next.branches);
                        return result;
                    } else {
                        break;
                    }
                }
            }
            result.addException(new ParseException("Node " + node + " of unexpected type " + node.type() + ", expected " + expectedTypes(), node));
        }
        return result;
    }

    private String expectedTypes() {
        return transitions().toKeys().filter(Type.class).map(Object::toString).//
                reduce("", (a, b) -> a.isEmpty() ? b : a + " or " + b);
    }

    public ParseState merge(ParseState state) {
        return merge(state, false);
    }

    private ParseState merge(ParseState state, boolean override) {
        if (state == null) {
            return this;
        }
        Map<Object, ParseState> pre = transitions().addAll(state.transitions(), (a, b) -> a.merge(b, override));
        Map<Object, ParseState> post = pre;
        for (Entry<Object, ParseState> e : pre) {
            if (e.getKey() instanceof Type sub) {
                for (Type sup : sub.allSupers()) {
                    if (!sup.equals(sub)) {
                        ParseState supState = pre.get(sup);
                        if (supState != null) {
                            post = post.put(sub, e.getValue().merge(supState, true));
                        }
                    }
                }
            }
        }
        return new ParseState(post, //
                merge(functor(), state.functor(), override), //
                merge(leftPrecedence(), state.leftPrecedence(), false), //
                merge(innerPrecedence(), state.innerPrecedence(), false), //
                merge(group(), state.group(), false), //
                startRepetitions().addAll(state.startRepetitions()), //
                endRepetitions().addAll(state.endRepetitions()), //
                branches().addAll(state.branches(), (a, b) -> merge(a, b, false)));
    }

    private static <T> T merge(T t1, T t2, boolean override) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            if (override) {
                return t1;
            }
            throw new PatternMergeException("Non deterministic pattern merge " + t1 + " <> " + t2);
        }
        return t1 == null ? t2 : t1;
    }

    @Override
    public String toString() {
        return transitions().toKeys().asSet().toString().substring(3);
    }

}
