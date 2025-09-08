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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public final class Parser {

    public static List<Node> parse(String string) throws ParseException {
        Tokenizer tokenizer = new Tokenizer(string + "\n", string);
        Token[] tokens = tokenizer.tokenize();
        return new Parser(tokens[Tokenizer.FIRST]).parse();
    }

    public static List<Node> parse(Class<?> clss) throws ParseException {
        String packageName = clss.getPackageName();
        String name = packageName.substring(packageName.lastIndexOf('.') + 1) + ".nl";
        return parse(clss, name);
    }

    public static List<Node> parse(Class<?> clss, String fileName) throws ParseException {
        try {
            InputStream stream = clss.getResourceAsStream(fileName);
            if (stream == null) {
                throw new ParseException("Nelumbo resource " + fileName + " does not exist", fileName);
            }
            InputStream buffer = new BufferedInputStream(stream);
            String base = new String(buffer.readAllBytes());
            return new Parser(new Tokenizer(base, fileName).first()).parse();
        } catch (IOException e) {
            throw new ParseException(e, "IOException during parse", fileName);
        }
    }

    // Instance

    private final KnowledgeBase knowledgeBase;
    private Token               token;

    public Parser(Token token) {
        this(token, false);
    }

    public Parser(Token token, boolean noInfer) {
        this.knowledgeBase = KnowledgeBase.CURRENT.get();
        this.token = token;
        knowledgeBase.noInfer(noInfer);
    }

    public List<Node> parse() throws ParseException {
        Node node = parseNode(Integer.MIN_VALUE, Type.ROOT);
        return node != null ? List.of(node) : List.of();
    }

    public Node parseNode(int precedence, Type expected) throws ParseException {
        if (noMoreTokens()) {
            throw new ParseException("premature end of input while expecting a " + expected.name(), token);
        }
        ParseResult result = preParse(expected, null);
        if (result == null) {
            throw new ParseException("No syntaxt pattern found", token);
        }
        Node left = result.postParse(expected, this);
        if (moreTokens()) {
            result = preParse(expected, left);
            while (result != null) {
                if (precedence >= result.precedence()) {
                    return left;
                }
                left = result.postParse(expected, this);
                if (noMoreTokens()) {
                    return left;
                }
                result = preParse(expected, left);
            }
        }
        return left;
    }

    private ParseResult preParse(Type expected, Node left) throws ParseException {
        return knowledgeBase.preParse(expected, left, this);
    }

    public KnowledgeBase knowledgeBase() {
        return knowledgeBase;
    }

    public Token peek() {
        return token;
    }

    public boolean peekIs(TokenType expected) {
        Token token = peek();
        return token != null && token.type() == expected;
    }

    public boolean peekIs(String expected) {
        Token token = peek();
        return token != null && token.text().equals(expected);
    }

    public Token consume() {
        Token t = token;
        if (t != null) {
            token = t.next();
        }
        return t;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    private boolean moreTokens() {
        return peek() != null;
    }

    private boolean noMoreTokens() {
        return peek() == null;
    }

    public boolean match(TokenType expected) {
        boolean isType = peekIs(expected);
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

    public Token consume(String expected) throws ParseException {
        Token token = consume();
        if (token == null) {
            throw new ParseException("Expected token " + expected + " but found end of input");
        }
        if (!token.text().equals(expected)) {
            throw new ParseException("Expected token " + expected + " but found " + token.text(), token);
        }
        return token;
    }

}
