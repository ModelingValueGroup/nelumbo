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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Functor;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.ListNode;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Relation;
import org.modelingvalue.nelumbo.Type;

public final class Parser {

    public static void parseLogic(Class<?> clss) throws ParseException {
        String packageName = clss.getPackageName();
        String name = packageName.substring(packageName.lastIndexOf('.') + 1) + ".nl";
        try {
            InputStream stream = clss.getResourceAsStream(name);
            if (stream == null) {
                throw new ParseException("Nelumbo resource " + name + " does not exist", 0, 0, 0, "", name);
            }
            InputStream buffer = new BufferedInputStream(stream);
            String base = new String(buffer.readAllBytes());
            new Parser(new Tokenizer(base, name).tokenize()).parse();
        } catch (IOException e) {
            throw new ParseException(e.getClass().getSimpleName() + ": " + e.getMessage(), 0, 0, 0, "", name);
        }
    }

    // Instance

    private final KnowledgeBase     knowledgeBase;
    private final LinkedList<Token> tokens;

    private Functor                 eqFunctor;

    public Parser(LinkedList<Token> tokens) {
        this.knowledgeBase = KnowledgeBase.CURRENT.get();
        this.tokens = tokens;
    }

    public Node parseNode(int precedence, Type expected) throws ParseException {
        Node left;
        Token position = tokens.peek();
        if (expected.isList()) {
            Type elemType = expected.element();
            left = new ListNode(elemType);
            do {
                Node node = parseNode(precedence, elemType);
                left = new ListNode((ListNode) left, node);
            } while (match(TokenType.COMMA));
        } else {
            Token token1 = tokens.poll();
            Token token2 = tokens.peek();
            AtomicParselet prefix = prefix(expected, token1, token2);
            left = prefix.parse(expected, this, token1);
        }
        Token token1 = tokens.poll();
        Token token2 = tokens.peek();
        PostfixParselet postfix = postfix(expected, left.type(), token1, token2, precedence);
        while (postfix != null) {
            left = postfix.parse(this, left, token1);
            token1 = tokens.poll();
            token2 = tokens.peek();
            postfix = postfix(expected, left.type(), token1, token2, precedence);
        }
        tokens.addFirst(token1);
        if (!expected.isAssignableFrom(left.type())) {
            throw new ParseException("Expected type " + expected + " and found " + left + " of type " + left.type(), position);
        }
        return left;
    }

    private AtomicParselet prefix(Type expected, Token token1, Token token2) throws ParseException {
        AtomicParselet prefix = doPrefix(expected, token1, token2);
        if (prefix == null && token1.type() == TokenType.OPERATOR) {
            String text = token1.text();
            int len = text.length();
            while (len-- > 1 && !knowledgeBase.isOperator(text)) {
                token1 = new Token(token1.type(), text.substring(0, len), token1.line(), //
                        token1.position(), token1.index(), token1.fileName());
                token2 = new Token(token1.type(), text.substring(len), token1.line(), //
                        token1.position() + len, token1.index() + len, token1.fileName());
                prefix = doPrefix(expected, token1, token2);
                if (prefix != null) {
                    tokens.addFirst(token2);
                    return prefix;
                } else if (len == 1) {
                    token1 = new Token(token1.type(), token1.text() + token2.text(), token1.line(), //
                            token1.position(), token1.index(), token1.fileName());
                    throw new ParseException("Operator " + token1.text() + " not defined", token1);
                }
            }
        }
        return prefix;
    }

    private AtomicParselet doPrefix(Type expected, Token token1, Token token2) throws ParseException {
        AtomicParselet prefix = knowledgeBase.prefix(expected, token1, token2);
        if (prefix == null) {
            throw new ParseException("Prefix " + token1.text() + " not defined", token1);
        }
        return prefix;
    }

    private PostfixParselet postfix(Type expected, Type left, Token token1, Token token2, int precedence) throws ParseException {
        PostfixParselet postfix = doPostfix(expected, left, token1, token2);
        if (postfix == null && token1.type() == TokenType.OPERATOR) {
            String text = token1.text();
            int len = text.length();
            while (len-- > 1 && !knowledgeBase.isOperator(text)) {
                token1 = new Token(token1.type(), text.substring(0, len), token1.line(), //
                        token1.position(), token1.index(), token1.fileName());
                token2 = new Token(token1.type(), text.substring(len), token1.line(), //
                        token1.position() + len, token1.index() + len, token1.fileName());
                postfix = doPostfix(expected, left, token1, token2);
                if (postfix != null) {
                    if (precedence < postfix.precedence()) {
                        tokens.addFirst(token2);
                        return postfix;
                    } else {
                        return null;
                    }
                } else if (len == 1) {
                    token1 = new Token(token1.type(), token1.text() + token2.text(), token1.line(), //
                            token1.position(), token1.index(), token1.fileName());
                    throw new ParseException("Operator " + token1.text() + " not defined", token1);
                }
            }
        }
        if (postfix != null && precedence < postfix.precedence()) {
            return postfix;
        } else {
            return null;
        }
    }

    private PostfixParselet doPostfix(Type expected, Type left, Token token1, Token token2) {
        Set<Type> pre, post = Set.of(left);
        while (!post.isEmpty()) {
            pre = post;
            post = Set.of();
            for (Type type : pre) {
                PostfixParselet postfix = knowledgeBase.postfix(expected, type, token1, token2);
                if (postfix != null) {
                    return postfix;
                } else {
                    post = post.addAll(type.supers());
                }
            }
        }
        return null;
    }

    public KnowledgeBase knowledgeBase() {
        return knowledgeBase;
    }

    public void parse() throws ParseException {
        while (!tokens.isEmpty()) {
            while (match(TokenType.NEWLINE)) {
            }
            if (!tokens.isEmpty()) {
                Node node = parseNode(0, Type.ROOT);
                if (node instanceof Relation) {
                    knowledgeBase.addFact((Relation) node);
                }
                consume(TokenType.NEWLINE);
            }
        }
        if (!tokens.isEmpty()) {
            Token token = tokens.peek();
            throw new ParseException("Could not parse '" + token.text() + "'", token);
        }
    }

    public boolean next(TokenType expected) {
        Token token = tokens.peek();
        return token != null && token.type() == expected;
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
            throw new ParseException("Expected token " + expected + " and found " + token.text() + " of type " + token.type(), token);
        }
        return token;
    }

    public Functor eqFunctor() {
        if (eqFunctor == null) {
            eqFunctor = knowledgeBase.functors().get(new Functor(Relation.TYPE, "=", null, 30, Node.TYPE, Node.TYPE));
        }
        return eqFunctor;
    }

}
