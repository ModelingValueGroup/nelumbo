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
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Terminal;
import org.modelingvalue.nelumbo.Type;

public final class Parser {

    private static final java.util.Map<Pair<TokenType, TokenType>, Prefix2Parselet> prefix2Parselets =                      //
            Map.<Pair<TokenType, TokenType>, Prefix2Parselet> of().toMutable();
    private static final java.util.Map<TokenType, Prefix1Parselet>                  prefix1Parselets =                      //
            Map.<TokenType, Prefix1Parselet> of().toMutable();
    private static final java.util.Map<TokenType, InfixParselet>                    infixParselets   =                      //
            Map.<TokenType, InfixParselet> of().toMutable();

    private static final Type                                                       TYPE_DECL        = new Type("TypeDecl");

    static {
        register(TokenType.OPERATOR, UnaryOperatorParselet.INSTANCE);
        register(TokenType.OPERATOR, BinaryOperatorParselet.INSTANCE);
        register(TokenType.IDENTIFIER, UnaryOperatorParselet.INSTANCE);
        register(TokenType.IDENTIFIER, BinaryOperatorParselet.INSTANCE);
        register(TokenType.IDENTIFIER, TokenType.LPAREN, CallWithArgsParselet.INSTANCE);
        register(TokenType.LPAREN, ParenParselet.INSTANCE);
        register(TokenType.TYPE, AtomicParselet.of(t -> {
            String name = t.text();
            name = name.substring(1, name.length() - 1);
            Node node = KnowledgeBase.CURRENT.get().getype(name);
            if (node == null) {
                node = new Terminal(TYPE_DECL, name);
            }
            return node;
        }));
        register(BinaryOperator.of(TYPE_DECL, ":", Type.TYPE(), 10, (t, l, r) -> {
            return new Type((String) l.get(1), (Type) r);
        }));
    }

    public static void register(TokenType token, Prefix1Parselet parselet) {
        if (prefix1Parselets.containsKey(token)) {
            throw new IllegalArgumentException();
        }
        prefix1Parselets.put(token, parselet);
    }

    public static void register(TokenType token1, TokenType token2, Prefix2Parselet parselet) {
        Pair<TokenType, TokenType> pair = Pair.of(token1, token2);
        if (prefix2Parselets.containsKey(pair)) {
            throw new IllegalArgumentException();
        }
        prefix2Parselets.put(pair, parselet);
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

    public Node parseNode(int precedence) throws ParseException {
        Token token = tokens.poll();
        Node left;
        Prefix2Parselet prefix2 = prefix2Parselets.get(Pair.of(token.type(), tokens.peek().type()));
        if (prefix2 != null) {
            left = prefix2.parse(this, token, tokens.poll());
        } else {
            Prefix1Parselet prefix1 = prefix1Parselets.get(token.type());
            if (prefix1 == null) {
                throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
            }
            left = prefix1.parse(this, token);
        }
        while (precedence < precedence(left)) {
            token = tokens.poll();
            InfixParselet infix = infixParselets.get(token.type());
            left = infix.parse(this, left, token);
        }
        return left;
    }

    public List<Node> parseRoots() throws ParseException {
        List<Node> result = List.of();
        while (!tokens.isEmpty()) {
            while (match(TokenType.NEWLINE)) {
            }
            if (!tokens.isEmpty()) {
                result = result.add(parseNode(0));
                consume(TokenType.NEWLINE);
            }
        }
        if (!tokens.isEmpty()) {
            Token token = tokens.peek();
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        return result;
    }

    public int position() {
        return tokens.peek().position();
    }

    public boolean match(TokenType expected) {
        Token token = tokens.peek();
        if (token.type() != expected) {
            return false;
        }
        tokens.poll();
        return true;
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
