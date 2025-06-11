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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.impl.StructureImpl;
import org.modelingvalue.nelumbo.syntax.Token.TokenType;

public class Parser {

    private final LinkedList<Token>              tokens;
    private final Map<TokenType, PrefixParselet> prefixParselets;
    private final Map<TokenType, InfixParselet>  infixParselets;

    public Parser(LinkedList<Token> tokens) {
        this.tokens = tokens;
        this.prefixParselets = new HashMap<>();
        this.infixParselets = new HashMap<>();
        register(TokenType.OPERATOR, UnaryOperatorParselet.INSTANCE);
        register(TokenType.OPERATOR, BinaryOperatorParselet.INSTANCE);
    }

    public void register(TokenType token, PrefixParselet parselet) {
        prefixParselets.put(token, parselet);
    }

    public void register(TokenType token, InfixParselet parselet) {
        infixParselets.put(token, parselet);
    }

    public StructureImpl<?> parseExpression(int precedence) throws ParseException {
        Token token = tokens.poll();
        PrefixParselet prefix = prefixParselets.get(token.type());
        if (prefix == null) {
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        StructureImpl<?> left = prefix.parse(this, token);
        while (precedence < precedence()) {
            token = tokens.poll();
            InfixParselet infix = infixParselets.get(token.type());
            left = infix.parse(this, left, token);
        }
        return left;
    }

    public List<StructureImpl<?>> parseExpression() throws ParseException {
        List<StructureImpl<?>> result = List.of();
        while (!tokens.isEmpty()) {
            while (tokens.peek() != null && tokens.peek().type() == TokenType.V) {
                tokens.poll();
            }
            if (!tokens.isEmpty()) {
                result = result.add(parseExpression(0));
                if (tokens.peek() == null && tokens.peek().type() != TokenType.V) {
                    break;
                }
            }
        }
        if (!tokens.isEmpty()) {
            Token token = tokens.peek();
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        return result;
    }

    private int precedence() throws ParseException {
        Token token = tokens.peek();
        if (token != null) {
            InfixParselet parser = infixParselets.get(token.type());
            if (parser != null) {
                return parser.precedence(token);
            }
        }
        return 0;
    }

}
