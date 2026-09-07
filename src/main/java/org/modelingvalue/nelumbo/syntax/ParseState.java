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
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.util.NotMergeableException;
import org.modelingvalue.nelumbo.AbstractState;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.TypeMatcherState;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;
import org.modelingvalue.nelumbo.syntax.Token.Completion;

public class ParseState extends AbstractState<ParseState> {
    public static final ParseState EMPTY = new ParseState(TypeMatcherState.EMPTY, Map.of(), Map.of(), Map.of(), null,
            null, null, null, Set.of(), Set.of(), false, Visibility.optional, false);

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
    private final Visibility                 visibility;
    private final boolean                    isConnected;

    private List<String> connected = null;

    public ParseState(Functor functor) {
        this(TypeMatcherState.EMPTY, Map.of(), Map.of(), Map.of(), functor, null, null, null, Set.of(), Set.of(), false,
                Visibility.optional, false);
    }

    public ParseState(Set<RepetitionPattern> startRepetitions, Set<RepetitionPattern> endRepetitions) {
        this(TypeMatcherState.EMPTY, Map.of(), Map.of(), Map.of(), null, null, null, null, startRepetitions,
                endRepetitions, false, Visibility.optional, false);
    }

    public ParseState(String text, boolean isKeyword, ParseState next) {
        this(TypeMatcherState.EMPTY, Map.of(Entry.of(text, isKeyword ? next.setIsKeyword() : next)), Map.of(), Map.of(),
                null, null, null, null, Set.of(), Set.of(), false, Visibility.optional, false);
    }

    public ParseState(TokenType tokenType, ParseState next) {
        this(TypeMatcherState.EMPTY, Map.of(), Map.of(Entry.of(tokenType, next)), Map.of(), null, null, null, null,
                Set.of(), Set.of(), false, Visibility.optional, false);
    }

    public ParseState(Type nodeType, Integer innerPrecedence, ParseState next) {
        this(nodeType.typeMatcher(), Map.of(), Map.of(), Map.of(Entry.of(nodeType, next)), null, null, innerPrecedence,
                nodeType.group(), Set.of(), Set.of(), false, Visibility.optional, false);
    }

    private ParseState(TypeMatcherState typeMatcher, Map<String, ParseState> tokenTexts,
            Map<TokenType, ParseState> tokenTypes, Map<Type, ParseState> nodeTypes, //
            Functor functor, Integer leftPrecedence, Integer innerPrecedence, String group, //
            Set<RepetitionPattern> startRepetitions, Set<RepetitionPattern> endRepetitions, boolean isKeyword,
            Visibility visibility, boolean isConnected) {
        super(typeMatcher);
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
        this.visibility = visibility;
        this.isConnected = isConnected;
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Map<Object, ParseState> typeTransitions() {
        return (Map) nodeTypes;
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

    public boolean isConnected() {
        return isConnected;
    }

    public Visibility visibility() {
        return visibility;
    }

    public List<String> connected() {
        if (connected == null) {
            connected = tokenTexts()
                    .filter(e -> e.getValue().isConnected() && TokenType.of(e.getKey()) == TokenType.NAME)
                    .map(Entry::getKey).sortedByDesc(String::length).asList();
        }
        return connected;
    }

    public boolean isTokensEmpty() {
        return tokenTexts.isEmpty() && tokenTypes.isEmpty();
    }

    public boolean isNodesEmpty() {
        return nodeTypes.isEmpty();
    }

    public ParseState pre() {
        if (isTokensEmpty()) {
            return null;
        }
        return new ParseState(typeMatcher(), tokenTexts, tokenTypes, Map.of(), functor, null, null, group,
                startRepetitions, endRepetitions, isKeyword, visibility, isConnected);
    }

    public ParseState post() {
        if (isNodesEmpty()) {
            return null;
        }
        return new ParseState(typeMatcher(), Map.of(), Map.of(), nodeTypes, functor, innerPrecedence, null, group,
                startRepetitions, endRepetitions, isKeyword, visibility, isConnected);
    }

    public ParseState setLeftPrecedence(Integer leftPrecedence) {
        Map<String, ParseState> a = tokenTexts
                .replaceAll(e -> Entry.of(e.getKey(), e.getValue().setLeftPrecedence(leftPrecedence)));
        Map<TokenType, ParseState> b = tokenTypes
                .replaceAll(e -> Entry.of(e.getKey(), e.getValue().setLeftPrecedence(leftPrecedence)));
        Map<Type, ParseState> c = nodeTypes
                .replaceAll(e -> Entry.of(e.getKey(), e.getValue().setLeftPrecedence(leftPrecedence)));
        return new ParseState(typeMatcher(), a, b, c, functor, leftPrecedence, innerPrecedence, group, startRepetitions,
                endRepetitions, isKeyword, visibility, isConnected);
    }

    public ParseState setVisibility(boolean visible) {
        Visibility v = visible ? Visibility.visible : Visibility.hidden;
        Map<String, ParseState> a = tokenTexts.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setVisibility(v)));
        Map<TokenType, ParseState> b = tokenTypes.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setVisibility(v)));
        Map<Type, ParseState> c = nodeTypes.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setVisibility(v)));
        return new ParseState(typeMatcher(), a, b, c, functor, leftPrecedence, innerPrecedence, group, startRepetitions,
                endRepetitions, isKeyword, visibility, isConnected);
    }

    private ParseState setVisibility(Visibility visibility) {
        return new ParseState(typeMatcher(), tokenTexts, tokenTypes, nodeTypes, functor, leftPrecedence,
                innerPrecedence, group, startRepetitions, endRepetitions, isKeyword, visibility, isConnected);
    }

    public ParseState setIsKeyword() {
        return new ParseState(typeMatcher(), tokenTexts, tokenTypes, nodeTypes, functor, leftPrecedence,
                innerPrecedence, group, startRepetitions, endRepetitions, true, visibility, isConnected);
    }

    public ParseState setIsConnected() {
        return new ParseState(typeMatcher(), tokenTexts, tokenTypes, nodeTypes, functor, leftPrecedence,
                innerPrecedence, group, startRepetitions, endRepetitions, isKeyword, visibility, true);
    }

    public boolean parse(Token token, PatternResult result, Map<RepetitionPattern, ParseState> outerRepetitions,
            boolean pre) throws ParseException {
        ParseContext ctx = result.context();
        if (ctx.state() == this && ctx.token() == token) {
            return false;
        }
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
            Direction direction = direction(token, outerRepetitions, ctx, result.typeArgs().get());
            TokenState next = null;
            if (direction != null) {
                if (direction == Direction.outer) {
                    if (isPostComplete(result)) {
                        result.endPostParse(functor(), token, leftPrecedence());
                    }
                    return true;
                }
                if (direction == Direction.repeat) {
                    result.endRepetition(endRepetitions(), token);
                    return true;
                }
                if (direction == Direction.node) {
                    next = nodeNext(token, result);
                }
                if (direction == Direction.tokenType) {
                    next = tokenTypeNext(token, ctx, result);
                }
                if (direction == Direction.tokenText) {
                    next = tokenTextNext(token, ctx, result);
                }
            } else {
                next = tokenTextNext(token, ctx, result);
                if (next == null) {
                    next = tokenTypeNext(token, ctx, result);
                }
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
                if (!startRepetitions().isEmpty() && token.type() != TokenType.ENDOFFILE
                        && Pattern.isEndOfLine(token)) {
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
                    String expectedTokens = expectedTokens(outerRepetitions, ctx, result.typeArgs().get());
                    result.addException(
                            new ParseException("Unexpected token " + token + ", expected " + expectedTokens, token));
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

    private String expectedTokens(Map<RepetitionPattern, ParseState> outerRepetitions, ParseContext ctx,
            Map<Variable, Type> typeArgs) throws ParseException {
        return dirStates(outerRepetitions, ctx, typeArgs).flatMap(Entry::getValue) //
                .flatMap(sc -> Collection.concat(sc.state.tokenTexts().toKeys(), sc.state.tokenTypes().toKeys())) //
                .map(o -> o instanceof String ? ("'" + o + "'") : o.toString()) //
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
    }

    public List<Completion> completions(Token token, int cursor) {
        System.err.println("completions: " + tokenTexts().size());
        // stub: replaces the whole token, cursor ignored, kind/documentation not yet
        // determined
        return tokenTexts().toKeys().sorted().map(s -> new Completion(0, token.numChars(), s, TokenType.KEYWORD, null))
                .asList();
    }

    private Direction direction(Token token, Map<RepetitionPattern, ParseState> outerRepetitions, ParseContext ctx,
            Map<Variable, Type> typeArgs) throws ParseException {
        if (token == null) {
            return null;
        }
        Map<Direction, Set<TokenStateContext>> dirTokenStates = dirTokenStates(token, outerRepetitions, ctx, typeArgs);
        while (dirTokenStates.size() > 1) {
            int max = max(dirTokenStates);
            for (Entry<Direction, Set<TokenStateContext>> e : dirTokenStates) {
                Set<TokenStateContext> prev, next = e.getValue();
                do {
                    prev = next;
                    next = Set.of();
                    for (TokenStateContext tsc : prev) {
                        Map<Direction, Set<TokenStateContext>> nextStates = tsc.state.dirTokenStates(tsc.token,
                                outerRepetitions, tsc.ctx, typeArgs);
                        next = next.addAll(nextStates.flatMap(Entry::getValue));
                    }
                } while (!next.isEmpty() && max(next) <= max);
                dirTokenStates = next.isEmpty() ? dirTokenStates.removeKey(e.getKey())
                        : dirTokenStates.put(e.getKey(), next);
            }
        }
        return dirTokenStates.isEmpty() ? null : dirTokenStates.get(0).getKey();
    }

    private static int max(Map<Direction, Set<TokenStateContext>> dirStates) {
        int max = -1;
        for (Entry<Direction, Set<TokenStateContext>> e : dirStates) {
            max = Math.max(max, max(e.getValue()));
        }
        return max;
    }

    private static int max(Set<TokenStateContext> states) {
        int max = -1;
        for (TokenStateContext tsc : states) {
            max = Math.max(max, tsc.token.index());
        }
        return max;
    }

    private Map<Direction, Set<TokenStateContext>> dirTokenStates(Token token,
            Map<RepetitionPattern, ParseState> outerRepetitions, ParseContext ctx, Map<Variable, Type> typeArgs)
            throws ParseException {
        Map<Direction, Set<TokenStateContext>> dirTokenStates = Map.of();
        Map<Direction, Set<StateContext>> dirStates = dirStates(outerRepetitions, ctx, typeArgs);
        for (Entry<Direction, Set<StateContext>> e : dirStates) {
            Set<TokenStateContext> states = Set.of();
            Direction direction = e.getKey();
            for (StateContext sc : e.getValue()) {
                if (direction != Direction.tokenType) {
                    TokenState next = sc.state.tokenTextNext(token, sc.ctx, null);
                    if (next != null) {
                        states = states.add(new TokenStateContext(next.token, next.state, sc.ctx));
                    }
                }
                if (direction != Direction.tokenText) {
                    TokenState next = sc.state.tokenTypeNext(token, sc.ctx, null);
                    if (next != null) {
                        states = states.add(new TokenStateContext(next.token, next.state, sc.ctx));
                    }
                }
            }
            if (!states.isEmpty()) {
                dirTokenStates = dirTokenStates.put(direction, states);
            }
        }
        return dirTokenStates;
    }

    private Map<Direction, Set<StateContext>> dirStates(Map<RepetitionPattern, ParseState> outerRepetitions,
            ParseContext ctx, Map<Variable, Type> typeArgs) throws ParseException {
        MutableMap<Direction, Set<StateContext>> dirStates = MutableMap.of(Map.of());
        tokenTextStates(ctx, dirStates);
        tokenTypeStates(ctx, dirStates);
        nodeStates(ctx, dirStates, typeArgs);
        repetitionStates(ctx, outerRepetitions, dirStates, typeArgs);
        outerStates(ctx, dirStates, typeArgs);
        return dirStates.get();
    }

    private void tokenTextStates(ParseContext ctx, MutableMap<Direction, Set<StateContext>> dirStates) {
        if (!tokenTexts().isEmpty()) {
            dirStates.put(Direction.tokenText, Set.of(new StateContext(this, ctx)));
        }
    }

    private void tokenTypeStates(ParseContext ctx, MutableMap<Direction, Set<StateContext>> dirStates) {
        if (!tokenTypes().isEmpty()) {
            dirStates.put(Direction.tokenType, Set.of(new StateContext(this, ctx)));
        }
    }

    private void nodeStates(ParseContext ctx, MutableMap<Direction, Set<StateContext>> dirStates,
            Map<Variable, Type> typeArgs) {
        if (!isNodesEmpty()) {
            Set<StateContext> states = Set.of();
            ParseContext inner = ParseContext.of(this, null, ctx);
            for (ParseContext pc = ctx; pc != null; pc = pc.outer()) {
                Map<Type, ParseState> pres = pc.preStates(group);
                if (pres != null) {
                    for (Entry<Type, ParseState> entry : pres) {
                        states = states.addAll(entry.getValue().tokenStates(inner, typeArgs));
                    }
                }
                Map<Type, Variable> hidden = pc.hiddenVariables(group);
                if (hidden != null) {
                    for (Entry<Type, Variable> var : hidden) {
                        states = postStates(inner, var.getValue().type(), states, group, typeArgs);
                    }
                }
            }
            if (!states.isEmpty()) {
                dirStates.put(Direction.node, states);
            }
        }
    }

    private void repetitionStates(ParseContext ctx, Map<RepetitionPattern, ParseState> repetitions,
            MutableMap<Direction, Set<StateContext>> dirStates, Map<Variable, Type> typeArgs) {
        if (!endRepetitions().isEmpty()) {
            Set<StateContext> states = Set.of();
            for (Entry<RepetitionPattern, ParseState> r : repetitions) {
                if (endRepetitions().contains(r.getKey())) {
                    states = states.addAll(r.getValue().tokenStates(ctx, typeArgs));
                }
            }
            if (!states.isEmpty()) {
                dirStates.put(Direction.repeat, states);
            }
        }
    }

    private void outerStates(ParseContext ctx, MutableMap<Direction, Set<StateContext>> dirStates,
            Map<Variable, Type> typeArgs) {
        if (functor() != null) {
            Type type = functor().resultType();
            Set<StateContext> states = Set.of();
            for (ParseContext pc = ctx; pc != null; pc = pc.outer()) {
                if (pc.outer() != null && pc.state() != null && !pc.state().isNodesEmpty()) {
                    ParseState state = pc.state().matchType(type, MutableMap.of(typeArgs));
                    if (state != null) {
                        states = states.addAll(state.tokenStates(pc, typeArgs));
                        if (state.functor() != null) {
                            type = state.functor().resultType();
                        }
                    }
                }
                if (pc.group() != null) {
                    states = postStates(pc, type, states, pc.group(), typeArgs);
                }
            }
            if (!states.isEmpty()) {
                dirStates.put(Direction.outer, states);
            }
        }

    }

    private Set<StateContext> tokenStates(ParseContext ctx, Map<Variable, Type> typeArgs) {
        MutableMap<Direction, Set<StateContext>> dirStates = MutableMap.of(Map.of());
        tokenTextStates(ctx, dirStates);
        tokenTypeStates(ctx, dirStates);
        nodeStates(ctx, dirStates, typeArgs);
        return dirStates.get().flatMap(Entry::getValue).asSet();
    }

    private static Set<StateContext> postStates(ParseContext ctx, Type type, Set<StateContext> states, String group,
            Map<Variable, Type> typeArgs) {
        Map<Type, ParseState> posts = ctx.postStates(group);
        if (posts != null) {
            for (ParseState post : posts.toValues()) {
                ParseState state = post.matchType(type, MutableMap.of(typeArgs));
                if (state != null) {
                    states = states.addAll(state.tokenStates(ctx, typeArgs));
                }
            }
        }
        return states;
    }

    private TokenState tokenTextNext(Token token, ParseContext ctx, PatternResult result) {
        if (token == null || tokenTexts().isEmpty()) {
            return null;
        }
        Visibility notVisibility = notVisibility(token, result);
        TokenType type = token.type();
        String text = token.text();
        ParseState next = tokenTexts().get(text);
        if (next != null && next.visibility() != notVisibility && isConnectedOk(token, next, ctx)) {
            if (result != null) {
                result.add(token);
                token.setTextMatch(next.isKeyword(), next.isConnected());
                token.setState(next);
            }
            return new TokenState(token.next(), next);
        }
        if (type == TokenType.OPERATOR) {
            for (int i = text.length() - 1; i > 0; i--) {
                String key = text.substring(0, i);
                next = tokenTexts().get(key);
                if (next != null && next.visibility() != notVisibility && isConnectedOk(token, next, ctx)) {
                    Token pre = token.split(i);
                    if (result != null) {
                        result.addSplit(token, pre);
                        result.add(pre);
                        pre.setTextMatch(next.isKeyword(), next.isConnected());
                        pre.setState(next);
                    }
                    return new TokenState(pre.next(), next);
                }
            }
        }
        if (type == TokenType.NAME && text.length() > 1) {
            for (String conn : connected()) {
                if (conn.length() < text.length() && text.startsWith(conn)) {
                    next = tokenTexts().get(conn);
                    if (next.visibility() != notVisibility && isConnectedOk(token, next, ctx)) {
                        String sub = text.substring(conn.length());
                        if (TokenType.of(sub) == null) {
                            for (int end = 1;; end++) {
                                if (TokenType.of(sub.substring(end)) != null) {
                                    Token pre = token.split(conn.length() + end);
                                    if (result != null) {
                                        result.addSplit(token, pre);
                                    }
                                    token = pre;
                                    break;
                                }
                            }
                        }
                        Token pre = token.split(conn.length());
                        if (result != null) {
                            result.addSplit(token, pre);
                            result.add(pre);
                            pre.setTextMatch(next.isKeyword(), next.isConnected());
                            pre.setState(next);
                        }
                        return new TokenState(pre.next(), next);
                    }
                }
            }
        }
        return null;
    }

    private TokenState tokenTypeNext(Token token, ParseContext ctx, PatternResult result) throws ParseException {
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
            return new TokenState(token, next);
        }
        if (type == TokenType.NAME) {
            Variable var = null;
            for (ParseContext pc = ctx; pc != null && var == null; pc = pc.outer()) {
                var = pc.variable(token.text());
            }
            if (var != null) {
                TokenType tt = var.type().tokenType();
                next = tt != null ? tokenTypes().get(tt) : null;
                if (next != null && next.visibility() != notVisibility(token, result)
                        && isConnectedOk(token, next, ctx)) {
                    if (result != null) {
                        result.add(var.setAstElements(List.of(token)));
                        token.setState(next);
                    }
                    return new TokenState(token.next(), next);
                }
            }
        }
        next = tokenTypes().get(type);
        if (next != null && isConnectedOk(token, next, ctx)) {
            if (result != null) {
                result.add(token);
                token.setState(next);
            }
            return new TokenState(token.next(), next);
        }
        return null;
    }

    private TokenState nodeNext(Token token, PatternResult result) throws ParseException {
        if (token == null || nodeTypes().isEmpty()) {
            return null;
        }
        ParseContext inner = ParseContext.of(this, token, result.context());
        Node node = result.parser().parseNode(token, inner);
        if (node != null) {
            try {
                result.context().merge(inner);
            } catch (NotMergeableException exc) {
                result.addException(new ParseException(exc.getMessage(), node));
            }
            Type type = node instanceof Variable ? node.type().toVariable() : node.type();
            ParseState next = matchType(type, result.typeArgs());
            if (next != null) {
                result.add(node);
                return new TokenState(node.nextToken(), next);
            }
            result.addException(new ParseException(
                    "Node " + node + " of unexpected type " + type + ", expected " + expectedTypes(), node));
        }
        return null;

    }

    private String expectedTypes() { //
        return nodeTypes() //
                .toKeys() //
                .map(Object::toString) //
                .reduce("", (a, b) -> a.isEmpty() ? b : a + " or " + b);
    }

    private Visibility notVisibility(Token token, PatternResult result) {
        return isLeftHidden(token, result) ? Visibility.visible : Visibility.hidden;
    }

    private boolean isLeftHidden(Token token, PatternResult result) {
        Node left = result != null ? result.left() : null;
        return left instanceof Variable var ? var.hidden() && var.lastToken() != token.previous() : false;
    }

    private boolean isConnectedOk(Token token, ParseState next, ParseContext ctx) {
        return !isConnected || !next.isConnected || token.previous() == token.previousAll();
    }

    @Override
    public ParseState merge(ParseState merged) {
        if (merged == null) {
            return this;
        }
        TypeMatcherState typeMatcher = typeMatcher().merge(merged.typeMatcher());
        Map<String, ParseState> tokenTexts = tokenTexts().addAll(merged.tokenTexts(), ParseState::merge);
        Map<TokenType, ParseState> tokenTypes = tokenTypes().addAll(merged.tokenTypes(), ParseState::merge);
        Map<Type, ParseState> nodeTypes = nodeTypes().addAll(merged.nodeTypes(), ParseState::merge);
        return new ParseState(typeMatcher, tokenTexts, tokenTypes, inherit(nodeTypes), //
                functorMerge(merged), //
                leftPrecedenceMerge(merged), //
                elementMerge(innerPrecedence(), merged.innerPrecedence()), //
                elementMerge(group(), merged.group()), //
                startRepetitions().addAll(merged.startRepetitions()), //
                endRepetitions().addAll(merged.endRepetitions()), //
                isKeyword() || merged.isKeyword(), //
                elementMerge(visibility(), merged.visibility()), //
                isConnected() || merged.isConnected());
    }

    private Functor functorMerge(ParseState state) {
        return functor() == null ? state.functor() : //
                state.functor() == null ? functor() : //
                        functor().equals(state.functor()) ? functor().nonBootstrap(state.functor()) : //
                                functor().mostSpecific(state.functor(), TYPE_ARGS.get());
    }

    private Integer leftPrecedenceMerge(ParseState state) {
        return Objects.equals(leftPrecedence(), state.leftPrecedence()) ? leftPrecedence() : null;
    }

    private static <T> T elementMerge(T t1, T t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            throw new NotMergeableException("Non deterministic pattern merge " + t1 + " <> " + t2);
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
        outer, repeat, node, tokenText, tokenType;
    }

    public static record TokenState(Token token, ParseState state) {
    }

    public static record TokenStateContext(Token token, ParseState state, ParseContext ctx) {
    }

    public static record StateContext(ParseState state, ParseContext ctx) {
    }

    public static enum Visibility {
        visible, hidden, optional;
    }

}
