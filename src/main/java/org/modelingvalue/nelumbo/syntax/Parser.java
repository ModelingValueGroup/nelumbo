//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.syntax;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public final class Parser {

    public static List<Node> parse(String string) throws ParseException {
        Tokenizer tokenizer = new Tokenizer(string + "\n", string);
        return new Parser(tokenizer.tokenize()).parseThrowing().roots();
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
            return new Parser(new Tokenizer(base, fileName).tokenize()).parseThrowing().roots();
        } catch (IOException e) {
            throw new ParseException(e, "IOException during parse", fileName);
        }
    }

    // Instance

    private final KnowledgeBase   knowledgeBase;
    private final TokenizerResult tokenizerResult;

    private ParserResult          result;

    public Parser(TokenizerResult tokenizerResult) {
        this.knowledgeBase = KnowledgeBase.CURRENT.get();
        this.tokenizerResult = tokenizerResult;
    }

    public ParserResult parseNonThrowing() {
        try {
            return parse(new ParserResult(false));
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    public ParserResult parseThrowing() throws ParseException {
        return parse(new ParserResult(true));
    }

    private ParserResult parse(ParserResult result) throws ParseException {
        this.result = result;
        knowledgeBase.startParsing(this);
        try {
            Token token = tokenizerResult.first();
            Node node = parseNode(token, Integer.MIN_VALUE, Type.TOP_GROUP);
            if (node != null) {
                result.setRoot(node);
                token = node.nextToken();
                if (token != null) {
                    addException(new ParseException("Unexpected token " + token.text() + " after end of input", token));
                }
            }
            return result;
        } finally {
            knowledgeBase.endParsing();
            this.result = null;
        }
    }

    public Node parseNode(Token token, int precedence, String group) throws ParseException {
        PatternResult result = preParse(token, group, null);
        if (result == null) {
            addException(new ParseException("No syntax pattern found for " + token.text(), token));
            return null;
        }
        Node left = result.postParse(this);
        if (left != null && precedence < Integer.MAX_VALUE) {
            token = left.nextToken();
            if (token != null) {
                result = preParse(token, group, left);
                while (result != null) {
                    if (precedence >= result.leftPrecedence()) {
                        return left;
                    }
                    left = result.postParse(this);
                    if (left == null) {
                        break;
                    }
                    token = left.nextToken();
                    if (token == null) {
                        return left;
                    }
                    result = preParse(token, group, left);
                }
            }
        }
        return left;
    }

    public PatternResult preParse(Token token, String group, Node left) throws ParseException {
        return knowledgeBase.preParse(token, group, left, this);
    }

    public KnowledgeBase knowledgeBase() {
        return knowledgeBase;
    }

    public void addException(ParseException exception) throws ParseException {
        result.addException(exception);
    }

}
