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

import java.util.Objects;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.util.Mergeable;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;

public class ParseState implements Mergeable<ParseState> {
    public static final ParseState           EMPTY = new ParseState(Map.of(), Map.of(), Map.of(), null, null, null, null, Set.of(), Set.of(), false);

    private final Map<String, ParseState>    tokenTexts;
    private final Map<TokenType, ParseState> tokenTypes;
    private final Map<Type, ParseState>      nodeTypes;
    private final Functor                    functor;
    private final Integer                    leftPrecedence;
    private final Integer                    innerPrecedence;
    private final String                     group;
    private final Set<RepetitionPattern>     startRepetitions;
    private final Set<RepetitionPattern>     endRepetitions;
    private final boolean                    isKeyword;

    public ParseState(Functor functor) {
        this(Map.of(), Map.of(), Map.of(), functor, null, null, null, Set.of(), Set.of(), false);
    }

    public ParseState(Set<RepetitionPattern> startRepetitions, Set<RepetitionPattern> endRepetitions) {
        this(Map.of(), Map.of(), Map.of(), null, null, null, null, startRepetitions, endRepetitions, false);
    }

    public ParseState(String text, boolean isKeyword, ParseState next) {
        this(Map.of(Entry.of(text, isKeyword ? next.setIsKeyword() : next)), Map.of(), Map.of(), null, null, null, null, Set.of(), Set.of(), false);
    }

    public ParseState(TokenType tokenType, ParseState next) {
        this(Map.of(), Map.of(Entry.of(tokenType, next)), Map.of(), null, null, null, null, Set.of(), Set.of(), false);
    }

    public ParseState(Type nodeType, ParseState next, Integer innerPrecedence) {
        this(Map.of(), Map.of(), Map.of(Entry.of(nodeType, next)), null, null, innerPrecedence, nodeType.group(), Set.of(), Set.of(), false);
    }

    private ParseState(Map<String, ParseState> tokenTexts, Map<TokenType, ParseState> tokenTypes, Map<Type, ParseState> nodeTypes, //
            Functor functor, Integer leftPrecedence, Integer innerPrecedence, String group, //
            Set<RepetitionPattern> startRepetitions, Set<RepetitionPattern> endRepetitions, boolean isKeyword) {
        this.tokenTexts = tokenTexts;
        this.tokenTypes = tokenTypes;
        this.nodeTypes = nodeTypes;
        this.functor = functor;
        this.leftPrecedence = leftPrecedence;
        this.innerPrecedence = innerPrecedence;
        this.group = group;
        this.startRepetitions = startRepetitions;
        this.endRepetitions = endRepetitions;
        this.isKeyword = isKeyword;
    }

    public Map<String, ParseState> tokenTexts() {
        return tokenTexts;
    }

    public Map<TokenType, ParseState> tokenTypes() {
        return tokenTypes;
    }

    public Map<Type, ParseState> nodeTypes() {
        return nodeTypes;
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

    public boolean isKeyword() {
        return isKeyword;
    }

    public ParseState pre() {
        if (tokenTexts.isEmpty() && tokenTypes.isEmpty()) {
            return null;
        }
        return new ParseState(tokenTexts, tokenTypes, Map.of(), functor, null, null, group, startRepetitions, endRepetitions, isKeyword);
    }

    public ParseState post() {
        if (nodeTypes.isEmpty()) {
            return null;
        }
        return new ParseState(Map.of(), Map.of(), nodeTypes, functor, innerPrecedence, null, group, startRepetitions, endRepetitions, isKeyword);
    }

    public ParseState setLeftPrecedence(Integer leftPrecedence) {
        Map<String, ParseState> a = tokenTexts.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setLeftPrecedence(leftPrecedence)));
        Map<TokenType, ParseState> b = tokenTypes.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setLeftPrecedence(leftPrecedence)));
        Map<Type, ParseState> c = nodeTypes.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setLeftPrecedence(leftPrecedence)));
        return new ParseState(a, b, c, functor, leftPrecedence, innerPrecedence, group, startRepetitions, endRepetitions, isKeyword);
    }

    private ParseState setIsKeyword() {
        return new ParseState(tokenTexts, tokenTypes, nodeTypes, functor, leftPrecedence, innerPrecedence, group, startRepetitions, endRepetitions, true);
    }

    public boolean parse(Token token, PatternResult result, Map<RepetitionPattern, ParseState> outerRepetitions, boolean pre) throws ParseException {
        ParseContext ctx = result.context();
        if (ctx.state() == this && ctx.token() == token) {
            return false;
        }
        Parser parser = result.parser();
        Map<RepetitionPattern, ParseState> innerRepetitions = outerRepetitions;
        for (RepetitionPattern start : startRepetitions()) {
            innerRepetitions = innerRepetitions.put(start, this);
        }
        do {
            if (pre && isPreComplete(result)) {
                result.endPreParse(this, token, leftPrecedence());
                return true;
            }
            int nrOfExceptions = result.nrOfExceptions();
            Direction direction = direction(token, parser, outerRepetitions, ctx);
            if (direction == Direction.outer && isPostComplete(result)) {
                result.endPostParse(functor(), token, leftPrecedence());
                return true;
            }
            if (direction == Direction.repeat) {
                result.endRepetition(endRepetitions(), token);
                return true;
            }
            TokenState next = null;
            if (direction == Direction.node) {
                next = nodeNext(token, result);
            }
            if (direction == Direction.token) {
                next = tokenNext(token, parser, ctx, result);
            }
            if (direction == null) {
                next = tokenNext(token, parser, ctx, result);
                if (next == null) {
                    next = nodeNext(token, result);
                }
                if (next == null && endRepetitions().anyMatch(outerRepetitions::containsKey)) {
                    result.endRepetition(endRepetitions(), token);
                    return true;
                }
            }
            if (next != null && next.state.parse(next.token, result, innerRepetitions, pre)) {
                if (result.endRepetitions().isEmpty()) {
                    break;
                } else if (startRepetitions().anyMatch(result.endRepetitions()::contains)) {
                    token = result.nextToken();
                    result.startRepetition();
                    continue;
                } else {
                    return true;
                }
            }
            if (result.nrOfExceptions() > nrOfExceptions) {
                if (!startRepetitions().isEmpty() && token.type() != TokenType.ENDOFFILE && Pattern.isEndOfLine(token)) {
                    do {
                        token = token.next();
                    } while (!Pattern.isEndOfLine(token));
                    if (token.type() != TokenType.ENDOFFILE) {
                        result.startRepetition();
                        continue;
                    }
                }
                return false;
            }
            break;
        } while (true);
        if (result.functor() == null && result.state() == null) {
            if (isPostComplete(result)) {
                result.endPostParse(functor(), token, leftPrecedence());
            } else {
                if (!pre) {
                    String expectedTokens = expectedTokens(parser, outerRepetitions, ctx);
                    result.addException(new ParseException("Unexpected token " + token + ", expected " + expectedTokens, token));
                }
                return false;
            }
        }
        return true;
    }

    private boolean isPreComplete(PatternResult result) {
        return !result.isEmpty() && (!result.hasLeft() || leftPrecedence() != null);
    }

    private boolean isPostComplete(PatternResult result) {
        return functor() != null && (!result.hasLeft() || leftPrecedence() != null);
    }

    private String expectedTokens(Parser parser, Map<RepetitionPattern, ParseState> outerRepetitions, ParseContext ctx) {
        return states(parser, outerRepetitions, ctx) //
                .flatMap(s -> Collection.concat(s.tokenTexts().toKeys(), s.tokenTypes().toKeys())) //
                .map(o -> o instanceof String ? ("'" + o + "'") : o.toString()) //
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
    }

    private Direction direction(Token token, Parser parser, Map<RepetitionPattern, ParseState> outerRepetitions, ParseContext ctx) throws ParseException {
        if (token == null) {
            return null;
        }
        Map<Direction, Set<TokenState>> dirStates = dirStates(token, parser, outerRepetitions, ctx);
        do {
            for (Entry<Direction, Set<TokenState>> e : dirStates) {
                Set<TokenState> nexts = Set.of();
                for (TokenState ts : e.getValue()) {
                    TokenState next = ts.state.tokenNext(ts.token, parser, ctx, null);
                    if (next != null) {
                        nexts = nexts.add(next);
                        Token t = next.token;
                        nexts = nexts.addAll(next.state().nodeStates(ctx).replaceAll(s -> new TokenState(t, s)));
                    }
                }
                dirStates = nexts.isEmpty() ? dirStates.removeKey(e.getKey()) : dirStates.put(e.getKey(), nexts);
            }
        } while (dirStates.size() > 1);
        return dirStates.size() == 1 ? dirStates.get(0).getKey() : null;
    }

    private Map<Direction, Set<TokenState>> dirStates(Token token, Parser parser, Map<RepetitionPattern, ParseState> outerRepetitions, ParseContext ctx) {
        Map<Direction, Set<TokenState>> dirStates = Map.of();
        Set<TokenState> states = Set.of(tokenState(token));
        dirStates = dirStates.put(Direction.token, states);
        states = nodeStates(ctx).replaceAll(s -> new TokenState(token, s));
        if (!states.isEmpty()) {
            dirStates = dirStates.put(Direction.node, states);
        }
        states = repetitionStates(outerRepetitions).replaceAll(s -> new TokenState(token, s));
        if (!states.isEmpty()) {
            dirStates = dirStates.put(Direction.repeat, states);
        }
        states = outerStates(ctx).replaceAll(s -> new TokenState(token, s));
        if (!states.isEmpty()) {
            dirStates = dirStates.put(Direction.outer, states);
        }
        return dirStates;
    }

    private Set<ParseState> states(Parser parser, Map<RepetitionPattern, ParseState> outerRepetitions, ParseContext ctx) {
        Set<ParseState> states = Set.of(this);
        states = states.addAll(nodeStates(ctx));
        states = states.addAll(repetitionStates(outerRepetitions));
        states = states.addAll(outerStates(ctx));
        return states;
    }

    private Set<ParseState> nodeStates(ParseContext ctx) {
        Set<ParseState> result = Set.of();
        if (!nodeTypes().isEmpty()) {
            for (ParseContext pc = ctx; pc != null; pc = pc.outer()) {
                ParseState state = pc.groupState(group);
                if (state != null) {
                    result = result.add(state);
                }
            }
        }
        return result;
    }

    private Set<ParseState> repetitionStates(Map<RepetitionPattern, ParseState> repetitions) {
        Set<ParseState> result = Set.of();
        if (!endRepetitions().isEmpty()) {
            for (Entry<RepetitionPattern, ParseState> r : repetitions) {
                if (endRepetitions().contains(r.getKey())) {
                    result = result.add(r.getValue());
                }
            }
        }
        return result;
    }

    private Set<ParseState> outerStates(ParseContext ctx) {
        Set<ParseState> result = Set.of();
        if (functor() != null) {
            Type type = functor().resultType();
            for (ParseContext pc = ctx; pc != null && pc.state() != null; pc = pc.outer()) {
                if (!pc.state().nodeTypes().isEmpty()) {
                    for (Type sup : type.allSupers()) {
                        ParseState next = pc.state().nodeTypes().get(sup);
                        if (next != null) {
                            result = result.add(next);
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private TokenState tokenNext(Token token, Parser parser, ParseContext ctx, PatternResult result) throws ParseException {
        TokenState next = tokenTextNext(token, result);
        if (next == null) {
            next = tokenTypeNext(token, parser, ctx, result);
        }
        return next;
    }

    private TokenState tokenTextNext(Token token, PatternResult result) {
        if (token == null || tokenTexts().isEmpty()) {
            return null;
        }
        TokenType type = token.type();
        String text = token.text();
        ParseState next = tokenTexts().get(text);
        if (next != null) {
            if (result != null) {
                result.add(token);
                token.setTextMatch(next.isKeyword());
                token.setState(next);
            }
            return next.tokenState(token.next());
        }
        if (isNumeric(type) && text.startsWith("-") && tokenTypes().get(type) == null) {
            String key = "-";
            next = tokenTexts().get(key);
            if (next != null) {
                Token min = token.split(1);
                if (result != null) {
                    result.addSplit(token, min);
                    result.add(min);
                    min.setTextMatch(next.isKeyword());
                    min.setState(next);
                }
                return next.tokenState(min.next());
            }
        }
        if (type == TokenType.OPERATOR) {
            for (int i = text.length() - 1; i > 0; i--) {
                String key = text.substring(0, i);
                next = tokenTexts().get(key);
                if (next != null) {
                    Token pre = token.split(1);
                    if (result != null) {
                        result.addSplit(token, pre);
                        result.add(pre);
                        pre.setTextMatch(next.isKeyword());
                        pre.setState(next);
                    }
                    return next.tokenState(pre.next());
                }
            }
        }
        return null;
    }

    private TokenState tokenTypeNext(Token token, Parser parser, ParseContext ctx, PatternResult result) throws ParseException {
        if (token == null || tokenTypes().isEmpty()) {
            return null;
        }
        TokenType type = token.type();
        ParseState next = tokenTypes().get(TokenType.NEWLINE);
        if (next != null && Pattern.isEndOfLine(token)) {
            if (result != null) {
                for (Token prev = token.previousAll(); prev != token.previous(); prev = prev.previousAll()) {
                    if (prev.type() == TokenType.NEWLINE) {
                        result.add(prev);
                        prev.setState(next);
                        break;
                    }
                }
            }
            return next.tokenState(token);
        }
        if (type == TokenType.NAME) {
            Variable var = null;
            for (ParseContext pc = ctx; pc != null && var == null; pc = pc.outer()) {
                var = ctx.variable(token, parser);
            }
            if (var != null) {
                TokenType tt = var.type().tokenType();
                next = tt != null ? tokenTypes().get(tt) : null;
                if (next != null) {
                    if (result != null) {
                        result.add(var);
                        token.setState(next);
                    }
                    return next.tokenState(token.next());
                }
            }
        }
        if (result != null || !type.isVariableContent()) {
            next = tokenTypes().get(type);
            if (next != null) {
                if (result != null) {
                    result.add(token);
                    token.setState(next);
                }
                return next.tokenState(token.next());
            }
        }
        return null;
    }

    private TokenState nodeNext(Token token, PatternResult result) throws ParseException {
        if (token == null || nodeTypes().isEmpty()) {
            return null;
        }
        Token nextToken = token.next();
        if (nextToken != null && token.text().equals("-") && isNumeric(nextToken.type()) && !nextToken.text().startsWith("-")) {
            token = result.addMerge(token, nextToken.prepend("-"));
        }
        ParseContext ctx = ParseContext.of(this, token, MutableMap.of(Map.of()), MutableMap.of(Map.of()), result.context());
        Node node = result.parser().parseNode(token, ctx);
        if (node != null) {
            Variable var = node.variable();
            if (var != null) {
                ParseState next = nodeTypes().get(Type.VARIABLE);
                if (next != null) {
                    result.add(var);
                    return next.tokenState(node.nextToken());
                }
            }
            result.add(node);
            Type type = node.type();
            for (Type sup : type.allSupers()) {
                ParseState next = nodeTypes().get(sup);
                if (next != null) {
                    return next.tokenState(node.nextToken());
                }
            }
            Entry<Type, ParseState> ts = nodeTypes().findAny(e -> e.getKey().variable() != null).orElse(null);
            if (ts != null) {
                var = ts.getKey().variable();
                Type sup = result.getTypeArg(var);
                if (sup != null) {
                    if (sup.isAssignableFrom(type)) {
                        return ts.getValue().tokenState(node.nextToken());
                    }
                    if (sup.isAssignableFrom(type)) {
                        result.putTypeArg(var, type);
                        return ts.getValue().tokenState(node.nextToken());
                    }
                } else {
                    result.putTypeArg(var, type);
                    return ts.getValue().tokenState(node.nextToken());
                }
            }
            result.removeLast();
            result.addException(new ParseException("Node " + node + " of unexpected type " + type + ", expected " + expectedTypes(), node));
        }
        return null;
    }

    private String expectedTypes() { //
        return nodeTypes() //
                .toKeys() //
                .map(Object::toString) //
                .reduce("", (a, b) -> a.isEmpty() ? b : a + " or " + b);
    }

    private static boolean isNumeric(TokenType type) {
        return type == TokenType.NUMBER || type == TokenType.DECIMAL;
    }

    public ParseState merge(ParseState state) {
        if (state == null) {
            return this;
        }
        Map<String, ParseState> tokenTexts = tokenTexts().addAll(state.tokenTexts(), ParseState::merge);
        Map<TokenType, ParseState> tokenTypes = tokenTypes().addAll(state.tokenTypes(), ParseState::merge);
        Map<Type, ParseState> nodeTypes = nodeTypes().addAll(state.nodeTypes(), ParseState::merge);
        for (Type subType : nodeTypes.toKeys()) {
            for (Type superType : subType.allSupers()) {
                if (!superType.equals(subType)) {
                    ParseState superState = nodeTypes.get(superType);
                    if (superState != null) {
                        ParseState subState = nodeTypes.get(subType);
                        ParseState mergedState = subState.merge(superState);
                        nodeTypes = nodeTypes.put(subType, mergedState);
                    }
                }
            }
        }
        return new ParseState(tokenTexts, tokenTypes, nodeTypes, //
                funtorMerge(state), //
                leftPrecedenceMerge(state), //
                elementMerge(innerPrecedence(), state.innerPrecedence()), //
                elementMerge(group(), state.group()), //
                startRepetitions().addAll(state.startRepetitions()), //
                endRepetitions().addAll(state.endRepetitions()), //
                elementMerge(isKeyword(), state.isKeyword()));
    }

    private Functor funtorMerge(ParseState state) {
        return functor() == null ? state.functor() : //
                state.functor() == null ? functor() : //
                        functor().equals(state.functor()) ? functor() : //
                                functor().mostSpecific(state.functor());
    }

    private Integer leftPrecedenceMerge(ParseState state) {
        return Objects.equals(leftPrecedence(), state.leftPrecedence()) ? leftPrecedence() : null;
    }

    private static <T> T elementMerge(T t1, T t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            throw new PatternMergeException("Non deterministic pattern merge " + t1 + " <> " + t2);
        }
        return t1 == null ? t2 : t1;
    }

    @Override
    public String toString() {
        return tokenTexts().toKeys().asSet().toString().substring(3) + //
                tokenTypes().toKeys().asSet().toString().substring(3) + //
                nodeTypes().toKeys().asSet().toString().substring(3);
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

    private static enum Direction {
        outer,
        repeat,
        node,
        token;
    }

    public TokenState tokenState(Token token) {
        return new TokenState(token, this);
    }

    public static record TokenState(Token token, ParseState state) {
    }

}
