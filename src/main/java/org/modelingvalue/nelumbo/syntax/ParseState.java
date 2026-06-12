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
import org.modelingvalue.collections.util.Mergeable;
import org.modelingvalue.collections.util.NotMergeableException;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.RepetitionPattern;

public class ParseState implements Mergeable<ParseState> {
    public static final ParseState EMPTY = new ParseState(Map.of(), Map.of(), Map.of(), null, null, null, null,
            Set.of(), Set.of(), false, Visibility.optional, false, Set.of());

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
    private final Set<Variable>              typeArgs;

    private List<String> connected = null;

    public ParseState(Functor functor) {
        this(Map.of(), Map.of(), Map.of(), functor, null, null, null, Set.of(), Set.of(), false, Visibility.optional,
                false, Set.of());
    }

    public ParseState(Set<RepetitionPattern> startRepetitions, Set<RepetitionPattern> endRepetitions) {
        this(Map.of(), Map.of(), Map.of(), null, null, null, null, startRepetitions, endRepetitions, false,
                Visibility.optional, false, Set.of());
    }

    public ParseState(String text, boolean isKeyword, ParseState next) {
        this(Map.of(Entry.of(text, isKeyword ? next.setIsKeyword() : next)), Map.of(), Map.of(), null, null, null, null,
                Set.of(), Set.of(), false, Visibility.optional, false, Set.of());
    }

    public ParseState(TokenType tokenType, ParseState next) {
        this(Map.of(), Map.of(Entry.of(tokenType, next)), Map.of(), null, null, null, null, Set.of(), Set.of(), false,
                Visibility.optional, false, Set.of());
    }

    public ParseState(Type nodeType, Integer innerPrecedence, ParseState next) {
        this(Map.of(), Map.of(), Map.of(Entry.of(nodeType, next)), null, null, innerPrecedence, nodeType.group(),
                Set.of(), Set.of(), false, Visibility.optional, false, Set.of());
    }

    private ParseState(Map<String, ParseState> tokenTexts, Map<TokenType, ParseState> tokenTypes,
            Map<Type, ParseState> nodeTypes, //
            Functor functor, Integer leftPrecedence, Integer innerPrecedence, String group, //
            Set<RepetitionPattern> startRepetitions, Set<RepetitionPattern> endRepetitions, boolean isKeyword,
            Visibility visibility, boolean isConnected, Set<Variable> typeArgs) {
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
        this.typeArgs = typeArgs;
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

    public boolean isConnected() {
        return isConnected;
    }

    public Visibility visibility() {
        return visibility;
    }

    public Set<Variable> typeArgs() {
        return typeArgs;
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
        return new ParseState(tokenTexts, tokenTypes, Map.of(), functor, null, null, group, startRepetitions,
                endRepetitions, isKeyword, visibility, isConnected, typeArgs);
    }

    public ParseState post() {
        if (isNodesEmpty()) {
            return null;
        }
        return new ParseState(Map.of(), Map.of(), nodeTypes, functor, innerPrecedence, null, group, startRepetitions,
                endRepetitions, isKeyword, visibility, isConnected, typeArgs);
    }

    public ParseState setLeftPrecedence(Integer leftPrecedence) {
        Map<String, ParseState> a = tokenTexts
                .replaceAll(e -> Entry.of(e.getKey(), e.getValue().setLeftPrecedence(leftPrecedence)));
        Map<TokenType, ParseState> b = tokenTypes
                .replaceAll(e -> Entry.of(e.getKey(), e.getValue().setLeftPrecedence(leftPrecedence)));
        Map<Type, ParseState> c = nodeTypes
                .replaceAll(e -> Entry.of(e.getKey(), e.getValue().setLeftPrecedence(leftPrecedence)));
        return new ParseState(a, b, c, functor, leftPrecedence, innerPrecedence, group, startRepetitions,
                endRepetitions, isKeyword, visibility, isConnected, typeArgs);
    }

    public ParseState setVisibility(boolean visible) {
        Visibility v = visible ? Visibility.visible : Visibility.hidden;
        Map<String, ParseState> a = tokenTexts.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setVisibility(v)));
        Map<TokenType, ParseState> b = tokenTypes.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setVisibility(v)));
        Map<Type, ParseState> c = nodeTypes.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setVisibility(v)));
        return new ParseState(a, b, c, functor, leftPrecedence, innerPrecedence, group, startRepetitions,
                endRepetitions, isKeyword, visibility, isConnected, typeArgs);
    }

    private ParseState setVisibility(Visibility visibility) {
        return new ParseState(tokenTexts, tokenTypes, nodeTypes, functor, leftPrecedence, innerPrecedence, group,
                startRepetitions, endRepetitions, isKeyword, visibility, isConnected, typeArgs);
    }

    public ParseState setIsKeyword() {
        return new ParseState(tokenTexts, tokenTypes, nodeTypes, functor, leftPrecedence, innerPrecedence, group,
                startRepetitions, endRepetitions, true, visibility, isConnected, typeArgs);
    }

    public ParseState setIsConnected() {
        return new ParseState(tokenTexts, tokenTypes, nodeTypes, functor, leftPrecedence, innerPrecedence, group,
                startRepetitions, endRepetitions, isKeyword, visibility, true, typeArgs);
    }

    public ParseState addTypeArg(Variable arg) {
        return new ParseState(tokenTexts, tokenTypes, nodeTypes, functor, leftPrecedence, innerPrecedence, group,
                startRepetitions, endRepetitions, isKeyword, visibility, isConnected, typeArgs.add(arg));
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
            DirectionContext dirState = directionContext(token, outerRepetitions, ctx);
            TokenState next = null;
            if (dirState != null) {
                if (dirState.direction == Direction.outer) {
                    if (isPostComplete(result)) {
                        result.endPostParse(functor(), token, leftPrecedence());
                    }
                    return true;
                }
                if (dirState.direction == Direction.repeat) {
                    result.endRepetition(endRepetitions(), token);
                    return true;
                }
                if (dirState.direction == Direction.node) {
                    next = nodeNext(token, result, dirState.ctx);
                }
                if (dirState.direction == Direction.tokenType) {
                    next = tokenTypeNext(token, ctx, result);
                }
                if (dirState.direction == Direction.tokenText) {
                    next = tokenTextNext(token, ctx, result);
                }
            } else {
                next = tokenTextNext(token, ctx, result);
                if (next == null) {
                    next = tokenTypeNext(token, ctx, result);
                }
                if (next == null) {
                    next = nodeNext(token, result, null);
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
                    String expectedTokens = expectedTokens(token, outerRepetitions, ctx);
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

    private String expectedTokens(Token token, Map<RepetitionPattern, ParseState> outerRepetitions, ParseContext ctx)
            throws ParseException {
        return dirStates(token, outerRepetitions, ctx, group).flatMap(Entry::getValue) //
                .flatMap(s -> Collection.concat(s.state.tokenTexts().toKeys(), s.state.tokenTypes().toKeys())) //
                .map(o -> o instanceof String ? ("'" + o + "'") : o.toString()) //
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
    }

    private DirectionContext directionContext(Token token, Map<RepetitionPattern, ParseState> outerRepetitions,
            ParseContext ctx) throws ParseException {
        if (token == null) {
            return null;
        }
        Map<DirectionContext, Set<TokenState>> dirStates = dirStates(token, outerRepetitions, ctx, group);
        while (dirStates.size() > 1) {
            int max = max(dirStates);
            for (Entry<DirectionContext, Set<TokenState>> e : dirStates) {
                Set<TokenState> prev, next = e.getValue();
                do {
                    prev = next;
                    next = Set.of();
                    for (TokenState ts : prev) {
                        Map<DirectionContext, Set<TokenState>> nextStates = ts.state.dirStates(ts.token,
                                outerRepetitions, ctx, group);
                        next = next.addAll(nextStates.flatMap(Entry::getValue));
                    }
                } while (!next.isEmpty() && max(next) <= max);
                dirStates = next.isEmpty() ? dirStates.removeKey(e.getKey()) : dirStates.put(e.getKey(), next);
            }
        }
        return dirStates.isEmpty() ? null : dirStates.get(0).getKey();
    }

    private static int max(Map<DirectionContext, Set<TokenState>> dirStates) {
        int max = -1;
        for (Entry<DirectionContext, Set<TokenState>> e : dirStates) {
            max = Math.max(max, max(e.getValue()));
        }
        return max;
    }

    private static int max(Set<TokenState> states) {
        int max = -1;
        for (TokenState ts : states) {
            max = Math.max(max, ts.token.index());
        }
        return max;
    }

    private Map<DirectionContext, Set<TokenState>> dirStates(Token token,
            Map<RepetitionPattern, ParseState> outerRepetitions, ParseContext ctx, String group) throws ParseException {
        MutableMap<DirectionContext, Set<TokenState>> dirStates = MutableMap.of(Map.of());
        tokenTextStates(token, ctx, dirStates);
        tokenTypeStates(token, ctx, dirStates);
        nodeStates(token, ctx, dirStates);
        repetitionStates(token, ctx, outerRepetitions, dirStates);
        outerStates(token, ctx, dirStates, group);
        return dirStates.get();
    }

    private void tokenTextStates(Token token, ParseContext ctx, MutableMap<DirectionContext, Set<TokenState>> dirStates)
            throws ParseException {
        TokenState next = tokenTextNext(token, ctx, null);
        if (next != null) {
            DirectionContext key = new DirectionContext(Direction.tokenText, ctx);
            dirStates.put(key, Set.of(next));
        }
    }

    private void tokenTypeStates(Token token, ParseContext ctx, MutableMap<DirectionContext, Set<TokenState>> dirStates)
            throws ParseException {
        TokenState next = tokenTypeNext(token, ctx, null);
        if (next != null) {
            DirectionContext key = new DirectionContext(Direction.tokenType, ctx);
            dirStates.put(key, Set.of(next));
        }
    }

    private void nodeStates(Token token, ParseContext ctx, MutableMap<DirectionContext, Set<TokenState>> dirStates)
            throws ParseException {
        if (!isNodesEmpty()) {
            for (ParseContext pc = ctx; pc != null; pc = pc.outer()) {
                Set<TokenState> states = Set.of();
                Map<Type, ParseState> pres = pc.preStates(group);
                if (pres != null) {
                    for (ParseState pre : pres.toValues()) {
                        for (TokenState next1 : pre.tokenNext(token, ctx)) {
                            states = states.add(next1);
                            if (next1.state.functor() != null) {
                                TokenState next2 = null;
                                Type type = next1.state.functor().resultType();
                                for (Type sup : type.allSupers().add(Type.DUMMY)) {
                                    ParseState state = nodeTypes().get(sup);
                                    if (state != null) {
                                        next2 = new TokenState(next1.token, state);
                                        break;
                                    }
                                }
                                if (next2 != null) {
                                    states = states.add(next2);
                                }
                            }
                        }
                    }
                    if (!states.isEmpty()) {
                        DirectionContext key = new DirectionContext(Direction.node, pc);
                        dirStates.put(key, states);
                    }
                }
            }
            if (dirStates.isEmpty()) {
                for (ParseContext pc = ctx; pc != null; pc = pc.outer()) {
                    Map<Type, Variable> hidden = pc.hiddenVariables(group);
                    if (hidden != null) {
                        Set<TokenState> states = Set.of();
                        for (Entry<Type, Variable> var : hidden) {
                            states = postStates(token, pc, var.getValue().type(), states, group);
                        }
                        if (!states.isEmpty()) {
                            DirectionContext key = new DirectionContext(Direction.node, pc);
                            dirStates.put(key, states);
                        }
                    }
                }
            }
        }
    }

    private void repetitionStates(Token token, ParseContext ctx, Map<RepetitionPattern, ParseState> repetitions, //
            MutableMap<DirectionContext, Set<TokenState>> dirStates) throws ParseException {
        if (!endRepetitions().isEmpty()) {
            Set<TokenState> states = Set.of();
            for (Entry<RepetitionPattern, ParseState> r : repetitions) {
                if (endRepetitions().contains(r.getKey())) {
                    for (TokenState next : r.getValue().tokenNext(token, ctx)) {
                        states = states.add(next);
                    }
                }
            }
            if (!states.isEmpty()) {
                DirectionContext key = new DirectionContext(Direction.repeat, ctx);
                dirStates.put(key, states);
            }
        }
    }

    private void outerStates(Token token, ParseContext ctx, MutableMap<DirectionContext, Set<TokenState>> dirStates,
            String group) throws ParseException {
        if (functor() != null) {
            Type type = functor().resultType();
            Set<TokenState> states = Set.of();
            for (ParseContext pc = ctx; pc != null; pc = pc.outer()) {
                if (pc.outer() != null && pc.state() != null && !pc.state().isNodesEmpty()) {
                    for (Type sup : type.allSupers().add(Type.DUMMY)) {
                        ParseState state = pc.state().nodeTypes().get(sup);
                        if (state != null) {
                            for (TokenState next : state.tokenNext(token, ctx)) {
                                states = states.add(next);
                            }
                            if (state.functor() != null) {
                                type = state.functor().resultType();
                            }
                            break;
                        }
                    }
                }
                if (group != null) {
                    states = postStates(token, pc, type, states, group);
                }
            }
            if (!states.isEmpty()) {
                DirectionContext key = new DirectionContext(Direction.outer, ctx);
                dirStates.put(key, states);
            }
        }
    }

    private static Set<TokenState> postStates(Token token, ParseContext ctx, Type type, Set<TokenState> states,
            String group) throws ParseException {
        Map<Type, ParseState> posts = ctx.postStates(group);
        if (posts != null) {
            for (ParseState post : posts.toValues()) {
                for (Type sup : type.allSupers().add(Type.DUMMY)) {
                    ParseState found = post.nodeTypes().get(sup);
                    if (found != null) {
                        for (TokenState next : found.tokenNext(token, ctx)) {
                            states = states.add(next);
                        }
                        break;
                    }
                }
            }
        }
        return states;
    }

    private TokenState[] tokenNext(Token token, ParseContext ctx) throws ParseException {
        TokenState next1 = tokenTextNext(token, ctx, null);
        TokenState next2 = tokenTypeNext(token, ctx, null);
        return next1 != null && next2 != null ? new TokenState[] { next1, next2 }
                : next1 != null ? new TokenState[] { next1 }
                        : next2 != null ? new TokenState[] { next2 } : new TokenState[0];
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

    private TokenState nodeNext(Token token, PatternResult result, ParseContext outer) throws ParseException {
        if (token == null || nodeTypes().isEmpty()) {
            return null;
        }
        ParseContext inner = ParseContext.of(this, token, result.context());
        Node node = result.parser().parseNode(token, inner, outer);
        if (node != null) {
            try {
                result.context().merge(inner);
            } catch (NotMergeableException exc) {
                result.addException(new ParseException(exc.getMessage(), node));
            }
            Type type = node.type();
            List<Type> supers = type.allSupers().add(Type.DUMMY.setGroup(group));
            if (type.hasArgument()) {
                supers = supers.addAll(type.setArgument(Type.DUMMY.setGroup(group)).allSupers());
            }
            for (Type sup : supers) {
                ParseState next = nodeTypes().get(sup);
                if (next != null) {
                    result.add(node);
                    for (Variable arg : next.typeArgs()) {
                        Type found = result.getTypeArg(arg);
                        if (found != null) {
                            found = sup.setArgument(found);
                            found = type.common(found);
                            if (found != null) {
                                result.putTypeArg(arg, found.argument());
                            } else {
                                next = null;
                                break;
                            }
                        } else {
                            result.putTypeArg(arg, type.argument());
                        }
                    }
                    if (next != null) {
                        return new TokenState(node.nextToken(), next);
                    }
                }
            }
            Variable var = node.variable();
            if (var != null) {
                ParseState next = nodeTypes().get(Type.VARIABLE);
                if (next != null) {
                    result.add(var);
                    return new TokenState(node.nextToken(), next);
                }
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
                functorMerge(state), //
                leftPrecedenceMerge(state), //
                elementMerge(innerPrecedence(), state.innerPrecedence()), //
                elementMerge(group(), state.group()), //
                startRepetitions().addAll(state.startRepetitions()), //
                endRepetitions().addAll(state.endRepetitions()), //
                isKeyword() || state.isKeyword(), //
                elementMerge(visibility(), state.visibility()), //
                isConnected() || state.isConnected(), //
                typeArgs().addAll(state.typeArgs()));
    }

    private Functor functorMerge(ParseState state) {
        return functor() == null ? state.functor() : //
                state.functor() == null ? functor() : //
                        functor().equals(state.functor()) ? functor().nonBootstrap(state.functor()) : //
                                functor().mostSpecific(state.functor());
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

    public static record DirectionContext(Direction direction, ParseContext ctx) {
    }

    public static enum Visibility {
        visible, hidden, optional;
    }

}
