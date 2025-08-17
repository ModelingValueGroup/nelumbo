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
import java.util.ListIterator;

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
            throw new ParseException(e, "IOException during parse", fileName);
        }
    }

    // Instance

    private final KnowledgeBase       knowledgeBase;
    private final ListIterator<Token> iterator;

    public Parser(LinkedList<Token> tokens) {
        this(tokens, false);
    }

    public Parser(LinkedList<Token> tokens, boolean noInfer) {
        this.knowledgeBase = KnowledgeBase.CURRENT.get();
        this.iterator      = tokens.listIterator();
        knowledgeBase.noInfer(noInfer);
    }

    public List<Node> parse() throws ParseException {
        List<Node> roots = List.of();
        while (true) {
            //noinspection StatementWithEmptyBody
            while (match(TokenType.NEWLINE)) {
            }
            if (noMoreTokens()) {
                return roots;
            }
            Node node = parseNode(0, Type.ROOT);
            if (node.type().equals(Type.RELATION)) {
                knowledgeBase.addFact((Predicate) node);
            }
            if (node instanceof ListNode list) {
                for (Node e : list.elements()) {
                    mustBeRoot(e);
                    roots = roots.add(e);
                }
            } else {
                mustBeRoot(node);
                roots = roots.add(node);
            }
            if (noMoreTokens()) {
                return roots;
            }
            consume(TokenType.NEWLINE);
        }
    }

    public Node parseNode(int precedence, Type expected) throws ParseException {
        if (noMoreTokens()) {
            throw new ParseException("premature end of input while expecting a " + expected.name());
        }
        // made this an array so that prefix() and postfix() can return new values in case a token was split
        // otherwise the unsplit Token is put in the Node
        Token[] t12 = new Token[2];
        Node    left;
        if (expected.isList()) {
            Type  elemType = expected.element();
            left = new ListNode(Token.EMPTY, elemType);
            if (!peek().type().end()) {
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
            t12[0] = consume();
            t12[1] = peek();
            assert t12[0] != null;
            AtomicParselet prefix = prefix(expected, t12);
            left = prefix.parse(expected, this, t12[0]);
        }
        if (moreTokens()) {
            t12[0] = consume();
            t12[1] = peek();
            assert t12[0] != null;
            PostfixParselet postfix = postfix(expected, left.type(), t12, precedence);
            while (postfix != null) {
                left = postfix.parse(expected, this, left, t12[0]);
                if (noMoreTokens()) {
                    return left;
                }
                t12[0]  = consume();
                t12[1]  = peek();
                postfix = postfix(expected, left.type(), t12, precedence);
            }
            // unread the token that ended the postfix chain
            unconsume(t12[0]);
        }
        return left;
    }

    private AtomicParselet prefix(Type expected, Token[] t12) throws ParseException {
        Token          t1     = t12[0];
        Token          t2     = t12[1];
        AtomicParselet prefix = knowledgeBase.prefix(expected, t1, t2);
        if (prefix != null) {
            return prefix;
        }
        if (t1.type() == TokenType.OPERATOR) {
            // no prefix found, so we try chop some chars from the operator and look for that part in the knowledgeBase:
            Token t1Init = t1;
            for (int len = t1Init.text().length() - 1; 0 < len && !knowledgeBase.isOperator(t1.text()); len--) {
                t1     = t1Init.splitGet1(len);
                t2     = t1Init.splitGet2(len);
                prefix = knowledgeBase.prefix(expected, t1, t2);
                if (prefix != null) {
                    splitCurrentToken(t1, t2);
                    t12[0] = t1;
                    t12[1] = t2;
                    return prefix;
                }
                if (len == 1) {
                    throw new ParseException("Operator " + t1Init.text() + " not defined", t1Init);
                }
            }
        }
        throw new ParseException("Prefix " + t1.text() + " not defined", t1);
    }

    private PostfixParselet postfix(Type expected, Type left, Token[] t12, int precedence) throws ParseException {
        Token           t1      = t12[0];
        Token           t2      = t12[1];
        PostfixParselet postfix = doPostfix(expected, left, t1, t2);
        if (postfix != null) {
            if (precedence < postfix.precedence()) {
                return postfix;
            } else {
                return null;
            }
        }
        if (t1.type() == TokenType.OPERATOR) {
            // no postfix found, so we try chop some chars from the operator and look for that part in the knowledgeBase:
            Token t1Init = t1;
            for (int len = t1Init.text().length() - 1; 0 < len && !knowledgeBase.isOperator(t1.text()); len--) {
                t1      = t1Init.splitGet1(len);
                t2      = t1Init.splitGet2(len);
                postfix = doPostfix(expected, left, t1, t2);
                if (postfix != null) {
                    if (precedence < postfix.precedence()) {
                        splitCurrentToken(t1, t2);
                        t12[0] = t1;
                        t12[1] = t2;
                        return postfix;
                    } else {
                        return null;
                    }
                } else if (len == 1) {
                    throw new ParseException("Operator " + t1Init.text() + " not defined", t1Init);
                }
            }
        }
        return null;
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

    public Token peek() {
        Token t = consume();
        unconsume(t);
        return t;
    }

    public boolean peekTypeIs(TokenType expected) {
        Token token = peek();
        return token != null && token.type() == expected;
    }

    private Token consume() {
        while (true) {
            if (noMoreTokens()) {
                return null;
            }
            Token t = iterator.next();
            if (!t.isCommentOrHspace()) {
                return t;
            }
        }
    }

    private boolean moreTokens() {
        return iterator.hasNext();
    }

    private boolean noMoreTokens() {
        return !iterator.hasNext();
    }

    private void unconsume(Token t) {
        if (t != null) {
            Token un = iterator.previous();
            while (un != null && un.isCommentOrHspace()) {
                un = iterator.previous();
            }
            if (un != t) {
                System.err.println("WARNING: unconsume did not find the right token: found " + un + " instead of " + t);
            }
        }
    }

    public boolean match(TokenType expected) {
        boolean isType = peekTypeIs(expected);
        if (isType) {
            consume();
        }
        return isType;
    }

    public Token consume(TokenType expected) throws ParseException {
        Token token = consume();
        if (token == null) {
            throw new ParseException("Expected token " + expected + " but found end of input");
        }
        if (token.type() != expected) {
            throw new ParseException("Expected token " + expected + " but found " + token.text() + " of type " + token.type(), token);
        }
        return token;
    }

    private void splitCurrentToken(Token t1, Token t2) {
        // iterator is positioned just after current (it was consumed).
        // Move back to the position of current, replace with t2, insert t1 before it,
        if (iterator.hasPrevious()) {
            iterator.previous();
            iterator.set(t2);
            iterator.add(t1);
        }
    }

    private void mustBeRoot(Node node) throws ParseException {
        Type type = node instanceof Variable ? Type.VARIABLE : node.type();
        if (!Type.ROOT.isAssignableFrom(type)) {
            throw new ParseException("Expected type, functor, variable, rule, fact or query. Found " + node + " of type " + type, node.tokens());
        }
    }

}
