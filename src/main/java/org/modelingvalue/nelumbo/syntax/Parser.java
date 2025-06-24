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

import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public final class Parser {

    // Instance

    private final KnowledgeBase     knowledgeBase;
    private final LinkedList<Token> tokens;

    public Parser(LinkedList<Token> tokens) {
        this.knowledgeBase = KnowledgeBase.CURRENT.get();
        this.tokens = tokens;
    }

    public Node parseNode(int precedence, Type desired) throws ParseException {
        Token token = tokens.poll();
        Node left;
        PrefixParselet prefix = knowledgeBase.prefix(desired, token.type());
        if (prefix == null) {
            prefix = knowledgeBase.prefix(token.type(), tokens.peek().text());
        }
        if (prefix == null) {
            prefix = knowledgeBase.prefix(token.type());
        }
        if (prefix == null) {
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        left = prefix.parse(this, token);
        while (precedence < precedence(left)) {
            token = tokens.poll();
            InfixParselet infix = knowledgeBase.infix(token.type());
            left = infix.parse(this, left, token);
        }
        return left;
    }

    public KnowledgeBase knowledgeBase() {
        return knowledgeBase;
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
            InfixParselet infix = knowledgeBase.infix(token.type());
            if (infix != null) {
                return infix.precedence(this, left, token);
            }
        }
        return 0;
    }

}
