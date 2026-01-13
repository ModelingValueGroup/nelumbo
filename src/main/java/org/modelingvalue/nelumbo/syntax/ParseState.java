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

package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Mergeable;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;

public class ParseState implements Mergeable<ParseState> {
    public static final ParseState EMPTY = new ParseState((Functor) null);

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
        this.transitions      = transitions;
        this.functor          = functor;
        this.leftPrecedence   = leftPrecedence;
        this.innerPrecedence  = innerPrecedence;
        this.group            = group;
        this.startRepetitions = startRepetitions;
        this.endRepetitions   = endRepetitions;
        this.branches         = branches;
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

    public boolean parse(Token token, PatternResult result, Map<RepetitionPattern, ParseState> outerRepetitions, boolean pre) throws ParseException {
        ParseContext ctx = result.context();
        if (ctx.state() == this && ctx.token() == token) {
            return false;
        }
        if (pre && !startRepetitions().isEmpty() && !result.isEmpty()) {
            result.endPreParse(this, token);
            return true;
        }
        Map<RepetitionPattern, ParseState> innerRepetitions = outerRepetitions;
        for (RepetitionPattern start : startRepetitions()) {
            innerRepetitions = innerRepetitions.put(start, this);
        }
        int nrOfExceptions;
        do {
            nrOfExceptions = result.exceptions().size();
            if (!token(token, result, ctx, innerRepetitions, pre)) {
                if (pre && group() != null) {
                    result.endPreParse(this, token);
                    return true;
                } else if (!pre && token != null && outerEnd(token, result, ctx, outerRepetitions)) {
                    result.endPostParse(functor(), token);
                } else if (!node(token, result, innerRepetitions, pre)) {
                    if (result.exceptions().size() > nrOfExceptions && token.type() != TokenType.ENDOFFILE && Pattern.isEndOfLine(token)) {
                        do {
                            token = token.next();
                        } while (!Pattern.isEndOfLine(token));
                        if (token.type() != TokenType.ENDOFFILE) {
                            continue;
                        }
                    }
                    break;
                }
            }
            if (startRepetitions().noneMatch(result.endRepetitions()::contains)) {
                result.countDepth();
                return true;
            }
            token = result.nextToken();
        } while (true);
        if (endRepetitions().anyMatch(outerRepetitions::containsKey)) {
            result.endRepetition(endRepetitions(), token, 1);
            return true;
        }
        if (result.functor() == null) {
            if (functor() == null || result.hasException()) {
                if ((!pre || !result.isEmpty()) && nrOfExceptions == result.exceptions().size()) {
                    result.addException(new ParseException("Unexpected token " + token + ", expected " + expectedTokens(ctx), token));
                }
                return false;
            }
            result.endPostParse(functor(), token);
        }
        return true;
    }

    private String expectedTokens(ParseContext ctx) {
        return outerStates(ctx) //
                                .add(this) //
                                .flatMap(s -> s.transitions().toKeys()) //
                                .filter(k -> k instanceof String || k instanceof TokenType) //
                                .map(o -> o instanceof String ? ("'" + o + "'") : o.toString()) //
                                .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
    }

    private Set<ParseState> outerStates(ParseContext ctx) {
        Set<ParseState> result = Set.of();
        if (functor() != null) {
            Type type = functor().resultType();
            for (ParseContext pc = ctx; pc != null && pc.state() != null; pc = pc.outer()) {
                for (Type sup : type.allSupers()) {
                    ParseState next = pc.state().transitions().get(sup);
                    if (next != null) {
                        result = result.add(next);
                    }
                }
            }
        }
        return result;
    }

    private boolean token(Token token, PatternResult result, ParseContext ctx, Map<RepetitionPattern, ParseState> repetitions, boolean pre) throws ParseException {
        if (transitions().isEmpty()) {
            return false;
        }
        AstElement element = null;
        TokenType  type    = token.type();
        String     text    = token.text();
        ParseState next    = transitions().get(text);
        if (next != null) {
            element = token;
            token.setKeyword();
        } else if (isNumeric(type) && token.text().startsWith("-") && transitions().get(type) == null) {
            String key = "-";
            next = transitions().get(key);
            if (next != null) {
                token   = result.addSplit(token, token.split(1));
                element = token;
                token.setKeyword();
            }
        } else if (type == TokenType.OPERATOR || type == TokenType.NAME) {
            for (int i = text.length() - 1; i > 0; i--) {
                String key = text.substring(0, i);
                next = transitions().get(key);
                if (next != null) {
                    token   = result.addSplit(token, token.split(i));
                    element = token;
                    token.setKeyword();
                    break;
                }
            }
        }
        if (next == null) {
            next = transitions().get(TokenType.NEWLINE);
            if (next != null) {
                if (Pattern.isEndOfLine(token)) {
                    for (Token prev = token.previousAll(); prev != token.previous(); prev = prev.previousAll()) {
                        if (prev.type() == TokenType.NEWLINE) {
                            element = prev;
                            break;
                        }
                    }
                    token = token.previous();
                } else {
                    next = null;
                }
            }
        }
        if (next == null && (type == TokenType.NAME || type == TokenType.TYPE)) {
            Variable var = result.parser().variable(token, ctx);
            if (var != null) {
                TokenType tt = var.type().tokenType();
                next = tt != null ? transitions().get(tt) : null;
                if (next == null && tt == TokenType.NAME) {
                    next = transitions().get(TokenType.TYPE);
                    if (next != null) {
                        var = var.rename("<" + var.name() + ">");
                    }
                }
                if (next != null) {
                    element = var;
                } else {
                    return false;
                }
            }
        }
        if (next == null) {
            next = transitions().get(type);
            if (next != null) {
                element = token;
            }
        }
        if (next != null) {
            if (repetitions == null) {
                return true;
            }
            if (element != null) {
                result.add(element);
                element.setBranches(next.branches);
            }
            return next.parse(token.next(), result, repetitions, pre);
        }
        return false;

    }

    private boolean outerEnd(Token token, PatternResult result, ParseContext ctx, Map<RepetitionPattern, ParseState> repetitions) throws ParseException {
        if (functor() != null) {
            for (Entry<RepetitionPattern, ParseState> r : repetitions) {
                if (endRepetitions().contains(r.getKey()) && r.getValue().token(token, result, ctx, null, true)) {
                    return false;
                }
            }
            Type type = functor().resultType();
            for (ParseContext pc = ctx; pc != null && pc.state() != null; pc = pc.outer()) {
                for (Type sup : type.allSupers()) {
                    ParseState next = pc.state().transitions().get(sup);
                    if (next != null && next.token(token, result, ctx.outer(), null, true)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isNumeric(TokenType type) {
        return type == TokenType.NUMBER || type == TokenType.DECIMAL;
    }

    private boolean node(Token token, PatternResult result, Map<RepetitionPattern, ParseState> repetitions, boolean pre) throws ParseException {
        if (group() == null) {
            return false;
        }
        Token nextToken = token.next();
        if (nextToken != null && token.text().equals("-") && isNumeric(nextToken.type()) && !nextToken.text().startsWith("-")) {
            token = result.addMerge(token, nextToken.prepend("-"));
        }
        Integer inner = innerPrecedence();
        if (transitions().get(Type.TYPE()) != null || transitions().get(Type.VARIABLE) != null) {
            inner = Integer.MAX_VALUE;
        }
        Node node = result.parser().parseNode(token, ParseContext.of(this, token, group(), inner, result.context()));
        if (node != null) {
            result.add(node);
            if (node instanceof Variable) {
                ParseState next = transitions().get(Type.VARIABLE);
                if (next != null) {
                    if (next.parse(node.nextToken(), result, repetitions, pre)) {
                        node.setBranches(next.branches);
                        return true;
                    }
                }
            }
            for (Type sup : node.type().allSupers()) {
                ParseState next = transitions().get(sup);
                if (next != null) {
                    if (next.parse(node.nextToken(), result, repetitions, pre)) {
                        node.setBranches(next.branches);
                        return true;
                    } else {
                        break;
                    }
                }
            }
            result.addException(new ParseException("Node " + node + " of unexpected type " + node.type() + ", expected " + expectedTypes(), node));
            return true;
        }
        return false;
    }

    private String expectedTypes() { //
        return transitions() //
                             .toKeys() //
                             .filter(Type.class) //
                             .map(Object::toString) //
                             .reduce("", (a, b) -> a.isEmpty() ? b : a + " or " + b);
    }

    public ParseState merge(ParseState state) {
        if (state == null) {
            return this;
        }
        Map<Object, ParseState> transitions = transitions().addAll(state.transitions(), ParseState::merge);
        for (Object key : transitions.toKeys()) {
            if (key instanceof Type subType) {
                for (Type superType : subType.allSupers()) {
                    if (!superType.equals(subType)) {
                        ParseState superState = transitions.get(superType);
                        if (superState != null) {
                            ParseState subState    = transitions.get(subType);
                            ParseState mergedState = subState.merge(superState);
                            transitions = transitions.put(subType, mergedState);
                        }
                    }
                }
            }
        }
        return new ParseState(transitions, //
                              functor() == null //
                              ? state.functor() //
                              : state.functor() == null //
                                ? functor() //
                                : functor().equals(state.functor()) //
                                  ? functor() //
                                  : functor().mostSpecific(state.functor()), //
                              elementMerge(leftPrecedence(), state.leftPrecedence()), //
                              elementMerge(innerPrecedence(), state.innerPrecedence()), //
                              elementMerge(group(), state.group()), //
                              startRepetitions().addAll(state.startRepetitions()), //
                              endRepetitions().addAll(state.endRepetitions()), //
                              branches().addAll(state.branches(), ParseState::elementMerge));
    }

    private static <T> T elementMerge(T t1, T t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            throw new PatternMergeException("Non deterministic pattern merge " + t1 + " <> " + t2);
        }
        return t1 == null ? t2 : t1;
    }

    @Override
    public String toString() {
        return transitions().toKeys().asSet().toString().substring(3);
    }

    @Override
    public ParseState merge(ParseState[] branches, int length) {
        ParseState state = this;
        for (int i = 0; i < length; i++) {
            state = branches[i].merge(state);
        }
        return state;
    }

    @Override
    public ParseState getMerger() {
        return EMPTY;
    }

    @Override
    public Class<?> getMeetClass() {
        return ParseState.class;
    }

}
