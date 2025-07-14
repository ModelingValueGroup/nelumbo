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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.ListNode;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;

public final class Parser {

    public static List<Node> parse(String string) throws ParseException {
        return new Parser(new Tokenizer(string + "\n", string).tokenize()).parse();
    }

    public static List<Node> parse(Class<?> clss) throws ParseException {
        String packageName = clss.getPackageName();
        String name = packageName.substring(packageName.lastIndexOf('.') + 1) + ".nl";
        return parse(clss, name);
    }

    public static List<Node> parse(Class<?> clss, String name) throws ParseException {
        try {
            InputStream stream = clss.getResourceAsStream(name);
            if (stream == null) {
                throw new ParseException("Nelumbo resource " + name + " does not exist", 0, 0, 0, 0, name);
            }
            InputStream buffer = new BufferedInputStream(stream);
            String base = new String(buffer.readAllBytes());
            return new Parser(new Tokenizer(base, name).tokenize()).parse();
        } catch (IOException e) {
            throw new ParseException(e.getClass().getSimpleName() + ": " + e.getMessage(), 0, 0, 0, 0, name);
        }
    }

    // Instance

    private final KnowledgeBase                   knowledgeBase;
    private final LinkedList<Token>               tokens;
    protected final Map<Node, Pair<Token, Token>> nodePosition;
    private Token                                 lastLast = null;
    private Token                                 last     = null;

    public Parser(LinkedList<Token> tokens) {
        this.knowledgeBase = KnowledgeBase.CURRENT.get();
        this.tokens = tokens;
        this.nodePosition = new HashMap<>();
    }

    public Node parseNode(int precedence, Type expected) throws ParseException {
        Node left;
        if (expected.isList()) {
            Type elemType = expected.element();
            left = new ListNode(elemType);
            Token start = peek();
            if (!start.type().end()) {
                do {
                    Node node = parseNode(precedence, elemType);
                    if (!elemType.isAssignableFrom(node.type())) {
                        Pair<Token, Token> pos = nodePosition.get(node);
                        throw new ParseException("Expected element of type " + elemType + " and found " + node + " of type " + node.type(), pos.a(), pos.b());
                    }
                    left = new ListNode((ListNode) left, node);
                } while (match(TokenType.COMMA));
            }
            nodePosition.put(left, Pair.of(start, last));
        } else {
            Token token1 = poll();
            Token token2 = peek();
            AtomicParselet prefix = prefix(expected, token1, token2);
            left = prefix.parse(expected, this, token1);
            nodePosition.put(left, Pair.of(token1, last));
        }
        Token token1 = poll();
        Token token2 = peek();
        PostfixParselet postfix = postfix(expected, left.type(), token1, token2, precedence);
        while (postfix != null) {
            Pair<Token, Token> pos = nodePosition.get(left);
            left = postfix.parse(expected, this, left, token1);
            nodePosition.put(left, Pair.of(pos.a(), last));
            token1 = poll();
            token2 = peek();
            postfix = postfix(expected, left.type(), token1, token2, precedence);
        }
        last = lastLast;
        tokens.addFirst(token1);
        return left;
    }

    public Token peek() {
        return tokens.peek();
    }

    public Token last() {
        return last;
    }

    private Token poll() {
        lastLast = last;
        last = tokens.poll();
        return last;
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
                }
                if (expected == Type.PREDICATE && !type.isLiteral()) {
                    postfix = knowledgeBase.postfix(expected, type.literal(), token1, token2);
                }
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

    public List<Node> parse() throws ParseException {
        List<Node> roots = List.of();
        while (!tokens.isEmpty()) {
            while (match(TokenType.NEWLINE)) {
            }
            if (!tokens.isEmpty()) {
                Node node = parseNode(0, Type.ROOT);
                if (node.type() == Type.RELATION) {
                    knowledgeBase.addFact((Predicate) node);
                }
                if (node instanceof ListNode) {
                    for (Node e : ((ListNode) node).elements()) {
                        checkRoot(e);
                        roots = roots.add(e);
                    }
                } else {
                    checkRoot(node);
                    roots = roots.add(node);
                }
                consume(TokenType.NEWLINE);
            }
        }
        if (!tokens.isEmpty()) {
            Token token = peek();
            throw new ParseException("Could not parse '" + token.text() + "'", token);
        }
        return roots;
    }

    private void checkRoot(Node node) throws ParseException {
        Type type = node instanceof Variable ? Type.VARIABLE : node.type();
        if (!Type.ROOT.isAssignableFrom(type)) {
            Pair<Token, Token> pos = nodePosition.get(node);
            throw new ParseException("Expected type, functor, variable, rule, fact or query. Found " + node + " of type " + type, pos.a(), pos.b());
        }
    }

    public boolean next(TokenType expected) {
        Token token = peek();
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
            poll();
            return true;
        } else {
            return false;
        }
    }

    public Token consume(TokenType expected) throws ParseException {
        Token token = poll();
        if (token.type() != expected) {
            throw new ParseException("Expected token " + expected + " and found " + token.text() + " of type " + token.type(), token);
        }
        return token;
    }

}
