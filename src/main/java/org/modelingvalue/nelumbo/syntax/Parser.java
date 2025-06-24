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

import java.text.ParseException;
import java.util.LinkedList;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.*;

public final class Parser {

    private static final java.util.Map<Pair<Type, TokenType>, PrefixParselet>   prefix3Parselets =                        //
            Map.<Pair<Type, TokenType>, PrefixParselet> of().toMutable();
    private static final java.util.Map<Pair<TokenType, String>, PrefixParselet> prefix2Parselets =                        //
            Map.<Pair<TokenType, String>, PrefixParselet> of().toMutable();
    private static final java.util.Map<TokenType, PrefixParselet>               prefix1Parselets =                        //
            Map.<TokenType, PrefixParselet> of().toMutable();
    private static final java.util.Map<TokenType, InfixParselet>                infixParselets   =                        //
            Map.<TokenType, InfixParselet> of().toMutable();

    private static final Type                                                   TYPE_NAME        = new Type("TypeName");
    private static final Type                                                   VAR_NAME         = new Type("VarName");
    private static final Type                                                   SIGNATURE        = new Type("Signature");
    private static final Type                                                   FUNCTOR_SET      = new Type("FunctorSet");
    private static final Type                                                   VAR_SET          = new Type("VarSet");

    static {
        init();
    }

    @SuppressWarnings("unchecked")
    private static void init() {
        new Type(Relation.class);
        register(TokenType.OPERATOR, UnaryOperatorParselet.INSTANCE);
        register(TokenType.OPERATOR, BinaryOperatorParselet.INSTANCE);
        // register(TokenType.NAME, UnaryOperatorParselet.INSTANCE);
        register(TokenType.NAME, BinaryOperatorParselet.INSTANCE);
        register(TokenType.NAME, "(", CallWithArgsParselet.INSTANCE);
        register(TokenType.LPAREN, ParenParselet.INSTANCE);
        register(TokenType.TYPE, "::", AtomicParselet.of(t -> {
            String name = t.text();
            name = name.substring(1, name.length() - 1);
            return new Terminal(TYPE_NAME, name);
        }));
        register(TokenType.TYPE, AtomicParselet.of(t -> {
            String name = t.text();
            name = name.substring(1, name.length() - 1);
            Type type = KnowledgeBase.CURRENT.get().getype(name);
            if (type != null) {
                return type;
            }
            throw new ParseException("Could not find type " + t.text() + " at position " + t.position() + ".", t.position());
        }));
        register(TokenType.NAME, AtomicParselet.of(t -> {
            String name = t.text();
            Variable var = KnowledgeBase.CURRENT.get().getVar(name);
            if (var != null) {
                return var;
            }
            throw new ParseException("Could not find variable " + t.text() + " at position " + t.position() + ".", t.position());
        }));
        register(BinaryOperator.of(TYPE_NAME, "::", Type.TYPE().list(), 10, (t, l, r) -> {
            return new Type((String) l.get(1), ((ListNode) r).elements());
        }));
        register(CallWithArgs.of((t, l) -> {
            return new Node(SIGNATURE, t.text(), l);
        }, Type.TYPE().list()));
        register(BinaryOperator.of(Type.TYPE(), "::=", SIGNATURE.list(), 10, (t, l, r) -> {
            Set<Functor> set = Set.of();
            for (Node s : ((ListNode) r).elements()) {
                Functor functor = new Functor((Type) l, (String) s.get(1), (List<Type>) s.get(2));
                register(CallWithArgs.of(functor.name(), (tt, ll) -> createNode(functor, ll.toArray()), //
                        functor.args().toArray(i -> new Type[i])));
                set = set.add(functor);
            }
            return new Node(FUNCTOR_SET, set);
        }));
        register(VAR_NAME, TokenType.NAME, AtomicParselet.of(t -> {
            String name = t.text();
            return new Terminal(VAR_NAME, name);
        }));
        register(BinaryOperator.of(Type.TYPE(), ":", VAR_NAME.list(), 10, (t, l, r) -> {
            Set<Variable> set = Set.of();
            for (Node v : ((ListNode) r).elements()) {
                set = set.add(new Variable((Type) l, (String) v.get(1)));
            }
            return new Node(VAR_SET, set);
        }));
        register(BinaryOperator.of(Relation.TYPE, "<==", Predicate.TYPE, 10, (t, l, r) -> {
            return new Rule((Relation) l, (Predicate) r);
        }));
        register(UnaryOperator.of("!", Predicate.TYPE, 10, (t, r) -> {
            return new Not((Predicate) r);
        }));
        register(BinaryOperator.of(Predicate.TYPE, "&", Predicate.TYPE, 20, (t, l, r) -> {
            return new And((Predicate) l, (Predicate) r);
        }));
        register(BinaryOperator.of(Predicate.TYPE, "|", Predicate.TYPE, 20, (t, l, r) -> {
            return new Or((Predicate) l, (Predicate) r);
        }));
        register(BinaryOperator.of(Node.TYPE, "=", Node.TYPE, 30, (t, l, r) -> {
            return new Equal((Node) l, (Node) r);
        }));
    }

    private static Node createNode(Functor functor, Object[] values) {
        return Relation.TYPE.isAssignableFrom(functor.resultType()) ? new Relation(functor, values) : new Node(functor, values);
    }

    public static void register(TokenType token, PrefixParselet parselet) {
        if (prefix1Parselets.containsKey(token)) {
            throw new IllegalArgumentException();
        }
        prefix1Parselets.put(token, parselet);
    }

    public static void register(TokenType token, String next, PrefixParselet parselet) {
        Pair<TokenType, String> pair = Pair.of(token, next);
        if (prefix2Parselets.containsKey(pair)) {
            throw new IllegalArgumentException();
        }
        prefix2Parselets.put(pair, parselet);
    }

    public static void register(Type desired, TokenType token, PrefixParselet parselet) {
        Pair<Type, TokenType> pair = Pair.of(desired, token);
        if (prefix3Parselets.containsKey(pair)) {
            throw new IllegalArgumentException();
        }
        prefix3Parselets.put(pair, parselet);
    }

    public static void register(TokenType token, InfixParselet parselet) {
        if (infixParselets.containsKey(token)) {
            throw new IllegalArgumentException();
        }
        infixParselets.put(token, parselet);
    }

    public static void register(UnaryOperator operator) {
        UnaryOperatorParselet.INSTANCE.register(operator);
    }

    public static void register(BinaryOperator operator) {
        BinaryOperatorParselet.INSTANCE.register(operator);
    }

    public static void register(CallWithArgs call) {
        CallWithArgsParselet.INSTANCE.register(call);
    }

    // Instance

    private final LinkedList<Token> tokens;

    public Parser(LinkedList<Token> tokens) {
        this.tokens = tokens;
    }

    public Node parseNode(int precedence, Type desired) throws ParseException {
        Token token = tokens.poll();
        Node left;
        PrefixParselet prefix = prefix3Parselets.get(Pair.of(desired, token.type()));
        if (prefix == null) {
            prefix = prefix2Parselets.get(Pair.of(token.type(), tokens.peek().text()));
        }
        if (prefix == null) {
            prefix = prefix1Parselets.get(token.type());
        }
        if (prefix == null) {
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        left = prefix.parse(this, token);
        while (precedence < precedence(left)) {
            token = tokens.poll();
            InfixParselet infix = infixParselets.get(token.type());
            left = infix.parse(this, left, token);
        }
        return left;
    }

    public void parse() throws ParseException {
        while (!tokens.isEmpty()) {
            while (match(TokenType.NEWLINE)) {
            }
            if (!tokens.isEmpty()) {
                parseNode(0, Node.TYPE);
                consume(TokenType.NEWLINE);
            }
        }
        if (!tokens.isEmpty()) {
            Token token = tokens.peek();
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
    }

    public int position() {
        return tokens.peek().position();
    }

    public boolean next(TokenType expected) {
        Token token = tokens.peek();
        return token.type() == expected;
    }

    public boolean findInLine(TokenType expected) {
        for (Token token : tokens) {
            if (token.type() == expected) {
                return true;
            } else if (token.type() == TokenType.NEWLINE) {
                return false;
            }
        }
        return false;
    }

    public boolean match(TokenType expected) {
        if (next(expected)) {
            tokens.poll();
            return true;
        } else {
            return false;
        }
    }

    public Token consume(TokenType expected) throws ParseException {
        Token token = tokens.poll();
        if (token.type() != expected) {
            throw new ParseException("Expected token " + expected + " and found " + token.type(), token.position());
        }
        return token;
    }

    private int precedence(Node left) throws ParseException {
        Token token = tokens.peek();
        if (token != null) {
            InfixParselet parser = infixParselets.get(token.type());
            if (parser != null) {
                return parser.precedence(left, token);
            }
        }
        return 0;
    }

}
