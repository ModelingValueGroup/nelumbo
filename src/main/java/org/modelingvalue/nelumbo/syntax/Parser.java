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
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.syntax;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.NotMergeableException;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
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
            String base = new String(buffer.readAllBytes(), StandardCharsets.UTF_8);
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
        knowledgeBase.invoke(() -> pra[0] = new Parser(tokenizerResult).parseNonThrowing());
        return pra[0];
    }

    // Instance

    private final KnowledgeBase   knowledgeBase;
    private final TokenizerResult tokenizerResult;

    private ParserResult result;
    private Token        crashToken;

    public Parser(TokenizerResult tokenizerResult) {
        this.knowledgeBase = KnowledgeBase.current();
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
            ParseContext ctx = ParseContext.of(Type.DEFAULT_GROUP, Integer.MIN_VALUE, knowledgeBase.parseContext());
            Node node = parseNode(token, ctx);
            if (node != null) {
                try {
                    knowledgeBase.parseContext().put(ctx);
                } catch (NotMergeableException exc) {
                    result.addException(new ParseException(exc.getMessage(), node));
                }
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

    protected Node parseNode(Token token, ParseContext inner) throws ParseException {
        try {
            crashToken = null;
            return parseNodeAt(token, inner);
        } catch (RuntimeException | StackOverflowError e) {
            if (crashToken == null) {
                crashToken = token; // set by the innermost node only
            }
            throw e;
        }
    }

    /**
     * The first token of the innermost node that was being parsed when an
     * unexpected exception escaped the parser, else null.
     */
    public Token crashToken() {
        return crashToken;
    }

    private Node parseNodeAt(Token token, ParseContext ctx) throws ParseException {
        String group = ctx.group();
        PatternResult result = new PatternResult(this, ctx);
        if (!preParse(group, token, null, result, ctx)) {
            return null;
        }
        Node left = result.postParse();
        token = left != null && ctx.precedence() < Integer.MAX_VALUE ? left.nextToken() : null;
        while (token != null && preParse(group, token, left, result, ctx)) {
            left = result.postParse();
            token = left != null ? left.nextToken() : null;
        }
        return left;
    }

    private boolean preParse(String group, Token token, Node left, PatternResult result, ParseContext ctx)
            throws ParseException {
        // The hidden variables first: the context holds the patterns of all scopes in
        // one state, so a pattern of an outer scope that starts with this token (a
        // type declaration for a name, a variable) would win from a hidden variable.
        if (left == null) {
            Set<Variable> vars = ctx.hiddenVariables(group);
            if (vars != null) {
                for (Variable var : vars) {
                    if (preParse(group, token, var, result, ctx) && ctx.precedence() < result.leftPrecedence()) {
                        return true;
                    }
                }
            }
        }
        if (ctx.preParse(group, token, left, result)) {
            if (left != null && ctx.precedence() < result.leftPrecedence()) {
                return true;
            }
            if (left == null && ctx.precedence() <= result.leftPrecedence()) {
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
