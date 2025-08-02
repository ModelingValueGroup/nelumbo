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

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.ListNode;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;

public final class Parser {
    public static List<Node> parse(String string) throws ParseException {
        Tokenizer         tokenizer = new Tokenizer(string + "\n", string);
        LinkedList<Token> tokens    = tokenizer.tokenize();
        return new Parser(tokens).parse();
    }

    public static List<Node> parse(Class<?> clss) throws ParseException {
        String packageName = clss.getPackageName();
        String name        = packageName.substring(packageName.lastIndexOf('.') + 1) + ".nl";
        return parse(clss, name);
    }

    public static List<Node> parse(Class<?> clss, String fileName) throws ParseException {
        try {
            InputStream stream = clss.getResourceAsStream(fileName);
            if (stream == null) {
                throw new ParseException("Nelumbo resource " + fileName + " does not exist", fileName);
            }
            InputStream       buffer = new BufferedInputStream(stream);
            String            base   = new String(buffer.readAllBytes());
            LinkedList<Token> tokens = new Tokenizer(base, fileName).tokenize();
            return new Parser(tokens).parse();
        } catch (IOException e) {
            throw new ParseException(e.getClass().getSimpleName() + ": " + e.getMessage(), fileName);
        }
    }

    // Instance

    private final KnowledgeBase     knowledgeBase;
    private final LinkedList<Token> tokens;

    public Parser(LinkedList<Token> tokens) {
        this.knowledgeBase = KnowledgeBase.CURRENT.get();
        this.tokens        = tokens;
    }

    public Node parseNode(int precedence, Type expected) throws ParseException {
        Node left;
        if (expected.isList()) {
            Type  elemType = expected.element();
            Token start    = peek();
            left = new ListNode(Token.EMPTY, elemType);
            if (!start.type().end()) {
                do
                {
                    Node node = parseNode(precedence, elemType);
                    if (!elemType.isAssignableFrom(node.type())) {
                        throw new ParseException("Expected element of type " + elemType + " but found " + node + " of type " + node.type(), node.tokens());
                    }
                    left = new ListNode(Token.EMPTY, (ListNode) left, node);
                } while (match(TokenType.COMMA));
            }
        } else {
            Token token1 = poll();
            Token token2 = peek();
            if (token1 == null) {
                throw new ParseException("Expected something but found nothing");
            }
            if (token2 == null) {
                throw new ParseException("Expected something after " + token1.text() + " but found nothing", token1);
            }
            AtomicParselet prefix = prefix(expected, token1, token2);
            left = prefix.parse(expected, this, token1);
        }
        Token           token1  = poll();
        Token           token2  = peek();
        PostfixParselet postfix = postfix(expected, left.type(), token1, token2, precedence);
        while (postfix != null) {
            left    = postfix.parse(expected, this, left, token1);
            token1  = poll();
            token2  = peek();
            postfix = postfix(expected, left.type(), token1, token2, precedence);
        }
        tokens.addFirst(token1);
        return left;
    }

    public Token peek() {
        Token t = tokens.peek();
        while (t != null && t.isCommentOrHspace()) {
            tokens.poll();
            t = tokens.peek();
        }
        return t;
    }

    private Token poll() {
        Token t = tokens.poll();
        while (t != null && t.isCommentOrHspace()) {
            t = tokens.poll();
        }
        return t;
    }

    private AtomicParselet prefix(Type expected, Token t1, Token t2) throws ParseException {
        AtomicParselet prefix = doPrefix(expected, t1, t2);
        if (prefix == null && t1.type() == TokenType.OPERATOR) {

            // no prefix found, but we might chop the operator so we have one from the knowledgeBase:
            String text = t1.text();
            int    line = t1.line();
            int    pos  = t1.position();
            int    ind  = t1.index();
            String file = t1.fileName();

            for (int len = text.length() - 1; 0 < len && !knowledgeBase.isOperator(text); len--) {
                String beg = text.substring(0, len);
                String end = text.substring(len);
                t1     = new Token(TokenType.OPERATOR, beg, line, pos, ind, file);
                t2     = new Token(TokenType.OPERATOR, end, line, pos + len, ind + len, file);
                prefix = doPrefix(expected, t1, t2);
                if (prefix != null) {
                    tokens.addFirst(t2);
                    return prefix;
                } else if (len == 1) {
                    t1 = new Token(TokenType.OPERATOR, t1.text() + t2.text(), line, pos, ind, file);
                    throw new ParseException("Operator " + t1.text() + " not defined", t1);
                }
            }
        }
        if (prefix == null) {
            throw new ParseException("Prefix " + t1.text() + " not defined", t1);
        }
        return prefix;
    }

    private AtomicParselet doPrefix(Type expected, Token token1, Token token2) {
        return knowledgeBase.prefix(expected, token1, token2);
    }

    private PostfixParselet postfix(Type expected, Type left, Token token1, Token token2, int precedence) throws ParseException {
        PostfixParselet postfix = doPostfix(expected, left, token1, token2);
        if (postfix == null && token1.type() == TokenType.OPERATOR) {
            String text = token1.text();
            int    len  = text.length();
            while (len-- > 1 && !knowledgeBase.isOperator(text)) {
                token1  = new Token(token1.type(), text.substring(0, len), token1.line(), //
                                    token1.position(), token1.index(), token1.fileName());
                token2  = new Token(token1.type(), text.substring(len), token1.line(), //
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
            pre  = post;
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
            //noinspection StatementWithEmptyBody
            while (match(TokenType.NEWLINE)) {
            }
            if (!tokens.isEmpty()) {
                Node node = parseNode(0, Type.ROOT);
                if (node.type().equals(Type.RELATION)) {
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
        return roots;
    }

    private void checkRoot(Node node) throws ParseException {
        Type type = node instanceof Variable ? Type.VARIABLE : node.type();
        if (!Type.ROOT.isAssignableFrom(type)) {
            throw new ParseException("Expected type, functor, variable, rule, fact or query. Found " + node + " of type " + type, node.tokens());
        }
    }

    public boolean next(TokenType expected) {
        Token token = peek();
        return token != null && token.type() == expected;
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
