//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

public final class Parser implements ParseExceptionHandler {

    public static List<Node> parse(String string) throws ParseException {
        Tokenizer tokenizer = new Tokenizer(string + "\n", string);
        return new Parser(tokenizer.tokenize()).parseEvaluate().roots();
    }

    public static List<Node> parse(Class<?> clss, String fileName) throws ParseException {
        try {
            InputStream stream = clss.getResourceAsStream(fileName);
            if (stream == null) {
                throw new ParseException("Nelumbo resource " + fileName + " does not exist", fileName);
            }
            InputStream buffer = new BufferedInputStream(stream);
            String base = new String(buffer.readAllBytes());
            return new Parser(new Tokenizer(base, fileName).tokenize()).parseEvaluate().roots();
        } catch (IOException e) {
            throw new ParseException(e, "IOException during parse", fileName);
        }
    }

    public static ParserResult parse(TokenizerResult tokenizerResult) {
        return parse(KnowledgeBase.BASE, tokenizerResult);
    }

    public static ParserResult parse(KnowledgeBase knowledgeBase, TokenizerResult tokenizerResult) {
        ParserResult[] pra = new ParserResult[1];
        knowledgeBase.run(() -> pra[0] = new Parser(tokenizerResult).parseNonThrowing());
        return pra[0];
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
            return parse(new ParserResult(tokenizerResult, false));
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    public ParserResult parseThrowing() throws ParseException {
        return parse(new ParserResult(tokenizerResult, true));
    }

    public ParserResult parseEvaluate() throws ParseException {
        ParserResult parserResult = parse(new ParserResult(tokenizerResult, true));
        parserResult.evaluate();
        return parserResult;
    }

    private ParserResult parse(ParserResult result) throws ParseException {
        this.result = result;
        knowledgeBase.setExceptionHandler(this);
        try {
            Token token = tokenizerResult.first();
            ParseContext ctx = ParseContext.of(Type.TOP_GROUP, Integer.MIN_VALUE, knowledgeBase.parseContext());
            Node node = parseNode(token, ctx, null);
            if (node != null) {
                result.setRoot(node);
                token = node.nextToken();
                if (token != null) {
                    addException(new ParseException("Unexpected token " + token + " after end of input", token));
                }
            } else if (exceptions().isEmpty()) {
                addException(new ParseException("No syntax pattern found for " + token, token));
            }
            result.checkAssertions();
            return result;
        } finally {
            knowledgeBase.endParsing();
            this.result = null;
            tokenizerResult.checkAssertions();
        }
    }

    protected Node parseNode(Token token, ParseContext inner, ParseContext outer) throws ParseException {
        PatternResult result = new PatternResult(this, inner);
        if (!preParse(token, null, result, outer)) {
            return null;
        }
        Node left = result.postParse();
        token = left != null && inner.precedence() < Integer.MAX_VALUE ? left.nextToken() : null;
        while (token != null && preParse(token, left, result, null)) {
            if (inner.precedence() >= result.leftPrecedence()) {
                return left;
            }
            left = result.postParse();
            token = left != null ? left.nextToken() : null;
        }
        return left;
    }

    private boolean preParse(Token token, Node left, PatternResult result, ParseContext outer) throws ParseException {
        String group = result.context().group();
        if (outer != null) {
            return outer.preParse(group, token, left, result);
        }
        for (ParseContext pc = result.context(); pc != null; pc = pc.outer()) {
            if (pc.preParse(group, token, left, result)) {
                return true;
            }
        }
        return false;
    }

    public KnowledgeBase knowledgeBase() {
        return knowledgeBase;
    }

    @Override
    public void addException(ParseException exception) throws ParseException {
        result.addException(exception);
    }

    @Override
    public List<ParseException> exceptions() {
        return result.exceptions();
    }

}
