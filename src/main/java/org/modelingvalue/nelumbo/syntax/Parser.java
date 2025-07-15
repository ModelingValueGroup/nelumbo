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
    private Token                                 last = null;

    public Parser(LinkedList<Token> tokens) {
        this.knowledgeBase = KnowledgeBase.CURRENT.get();
        this.tokens = tokens;
        this.nodePosition = new HashMap<>();
    }

    public Node parseNode(int precedence, Type expected) throws ParseException {
        LanguagePattern pattern = first(expected);
        List<Token> tokens = poll(pattern.tokens().size());
        Node left = pattern.parse(this, expected, tokens);
        nodePosition.put(left, Pair.of(tokens.get(0), last));
        pattern = next(expected, left.type());
        while (pattern != null && precedence < pattern.precedence()) {
            tokens = poll(pattern.tokens().size());
            Pair<Token, Token> pos = nodePosition.get(left);
            left = pattern.parse(this, expected, left, tokens);
            nodePosition.put(left, Pair.of(pos.a(), last));
            pattern = next(expected, left.type());
        }
        return left;
    }

    private List<Token> poll(int len) {
        List<Token> list = List.of();
        for (int i = 0; i < len; i++) {
            list = list.add(poll());
        }
        return list;
    }

    public Token peek() {
        return tokens.peek();
    }

    public Token last() {
        return last;
    }

    private Token poll() {
        last = tokens.poll();
        return last;
    }

    private LanguagePattern first(Type expected) throws ParseException {
        LanguagePattern prefix = knowledgeBase.first(expected, tokens);
        if (prefix == null) {
            Token token = tokens.peek();
            throw new ParseException("Prefix " + token.text() + " not defined", token);
        }
        return prefix;
    }

    private LanguagePattern next(Type expected, Type left) {
        Set<Type> pre, post = Set.of(left);
        while (!post.isEmpty()) {
            pre = post;
            post = Set.of();
            for (Type type : pre) {
                LanguagePattern postfix = knowledgeBase.next(expected, type, tokens);
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
            while (match(TokenType.NEWLINE) != null) {
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

    public String toString(Token[] tokens) {
        String result = "";
        for (int i = 0; i < tokens.length; i++) {
            result += tokens[i].text();
        }
        return result;
    }

    public List<Token> match(List<Object> expected) {
        int len = expected.size();
        for (int i = 0; i < len; i++) {
            Object e = expected.get(i);
            Token token = tokens.get(i);
            if (!e.equals(token.text()) && !e.equals(token.type())) {
                return null;
            }
        }
        return poll(len);
    }

    public Token match(Object expected) {
        Token token = peek();
        if (expected.equals(token.text()) || expected.equals(token.type())) {
            return poll();
        } else {
            return null;
        }
    }

    public List<Token> consume(List<Object> expected) throws ParseException {
        int len = expected.size();
        List<Token> list = List.of();
        for (int i = 0; i < len; i++) {
            list = list.add(consume(expected.get(i)));
        }
        return list;
    }

    public Token consume(Object expected) throws ParseException {
        Token token = poll();
        if (!expected.equals(token.text()) && !expected.equals(token.type())) {
            throw new ParseException("Expected token " + expected + " and found " + token.text() + " of type " + token.type(), token);
        }
        return token;
    }

}
