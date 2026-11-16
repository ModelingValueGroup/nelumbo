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

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.U;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public class TokenizerTest extends NelumboTestBase {
    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @Override
    protected void settings(KnowledgeBase kb) {
        kb.setReverseOrder(env("REVERSE_NELUMBO", false));
        kb.setRandomOrder(env("RANDOM_NELUMBO", true));
        kb.setTrace(env("TRACE_NELUMBO", false));
    }

    @Test
    public void tokenizerTest() {
        String example = """
                // COMMENT
                    -abb + bcc *
                       c - dee // ANOTHER COMMENT
                    e = 8.9 / 2
                """;

        TokenizerResult result = new Tokenizer(example, "tokenizerTest").tokenize();
        List<Token> tokens = result.list();
        List<Token> all = result.listAll();

        String reassembled = all.map(Token::text).collect(Collectors.joining());
        String types = all.map(t -> t.type().name()).collect(Collectors.joining(" "));
        String expectedTypes = "BEGINOFFILE END_LINE_COMMENT NEWLINE HSPACE OPERATOR NAME HSPACE OPERATOR HSPACE NAME HSPACE OPERATOR NEWLINE HSPACE NAME HSPACE OPERATOR HSPACE NAME HSPACE END_LINE_COMMENT NEWLINE HSPACE NAME HSPACE OPERATOR HSPACE NUMBER OPERATOR NUMBER HSPACE OPERATOR HSPACE NUMBER NEWLINE ENDOFFILE";

        U.printTokens("tokens", tokens);
        U.printTokens("all", all);

        assertEquals(19, tokens.size(), "wrong number of tokens returned by tokenize()");
        assertEquals(36, all.size(), "wrong number of tokens returned by tokenize(all)");
        assertEquals(example, reassembled, "could not reassemble tokens");
        assertEquals(expectedTypes, types, "unexpected token types in token list");

        // ==========================================================================================
        assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
        assertEqualsToken(0, 10, all, 2, TokenType.NEWLINE);
        assertEqualsToken(1, 0, all, 3, TokenType.HSPACE);
        assertEqualsToken(1, 4, all, 4, TokenType.OPERATOR);
        assertEqualsToken(1, 5, all, 5, TokenType.NAME);

        // ==========================================================================================
        assertEqualsToken(1, 4, tokens, 1, TokenType.OPERATOR);
        assertEqualsToken(1, 5, tokens, 2, TokenType.NAME);
        assertEqualsToken(1, 9, tokens, 3, TokenType.OPERATOR);
        assertEqualsToken(1, 11, tokens, 4, TokenType.NAME);
        assertEqualsToken(1, 15, tokens, 5, TokenType.OPERATOR);
    }

    @Test
    public void tokenizerComment1Test() {
        String example = "/* unterminated comment";

        TokenizerResult result = new Tokenizer(example, "tokenizerCommentTest").tokenize();
        List<Token> tokens = result.list();
        List<Token> all = result.listAll();

        U.printTokens("tokens", tokens);
        U.printTokens("all", all);

        assertEquals(2, tokens.size(), "wrong number of tokens returned by tokenize()");
        assertEquals(3, all.size(), "wrong number of tokens returned by tokenize(all)");

        assertEqualsToken(0, 0, all, 1, TokenType.IN_LINE_COMMENT);
    }

    @Test
    public void tokenizerComment2Test() {
        String example = "<a/*a*/>`a";

        TokenizerResult result = new Tokenizer(example, "tokenizerCommentTest").tokenize();
        List<Token> tokens = result.list();
        List<Token> all = result.listAll();
        String reassembled = all.map(Token::text).collect(Collectors.joining());
        String types = all.map(t -> t.type().name()).collect(Collectors.joining(" "));
        String expectedTypes = "BEGINOFFILE OPERATOR NAME IN_LINE_COMMENT OPERATOR ERROR NAME ENDOFFILE";

        U.printTokens("tokens", tokens);
        U.printTokens("all", all);

        assertEquals(7, tokens.size(), "wrong number of tokens returned by tokenize()");
        assertEquals(8, all.size(), "wrong number of tokens returned by tokenize(all)");
        assertEquals(example, reassembled, "could not reassemble tokens");
        assertEquals(expectedTypes, types, "unexpected token types in token list");

        // ==========================================================================================
        assertEqualsToken(0, 0, all, 1, TokenType.OPERATOR);
        assertEqualsToken(0, 1, all, 2, TokenType.NAME);
        assertEqualsToken(0, 2, all, 3, TokenType.IN_LINE_COMMENT);
        assertEqualsToken(0, 7, all, 4, TokenType.OPERATOR);
        assertEqualsToken(0, 8, all, 5, TokenType.ERROR);

        // ==========================================================================================
        assertEqualsToken(0, 0, tokens, 1, TokenType.OPERATOR);
        assertEqualsToken(0, 1, tokens, 2, TokenType.NAME);
        assertEqualsToken(0, 7, tokens, 3, TokenType.OPERATOR);
        assertEqualsToken(0, 8, tokens, 4, TokenType.ERROR);
    }

    @Test
    public void tokenizerEmptyCommentTest() {
        List<Token> all = tokenizeAll("//");
        assertEquals(3, all.size(), "wrong number of tokens for '//'");
        assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
        assertEquals("//", all.get(1).text(), "empty comment should have text '//'");
    }

    @Test
    public void tokenizerCommentWithSpaceTest() {
        List<Token> all = tokenizeAll("// ");
        assertEquals(3, all.size(), "wrong number of tokens for '// '");
        assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
        assertEquals("// ", all.get(1).text(), "comment with space should have text '// '");
    }

    @Test
    public void tokenizerCommentWithTextTest() {
        List<Token> all = tokenizeAll("// text");
        assertEquals(3, all.size(), "wrong number of tokens for '// text'");
        assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
        assertEquals("// text", all.get(1).text(), "comment should include full text");
    }

    @Test
    public void tokenizerTripleSlashCommentTest() {
        List<Token> all = tokenizeAll("///");
        assertEquals(3, all.size(), "wrong number of tokens for '///'");
        assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
        assertEquals("///", all.get(1).text(), "/// should be a comment");
    }

    @Test
    public void tokenizerCommentWithSpecialCharsTest() {
        List<Token> all = tokenizeAll("//===");
        assertEquals(3, all.size(), "wrong number of tokens for '//==='");
        assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
        assertEquals("//===", all.get(1).text(), "//=== should be a comment");
    }

    @Test
    public void tokenizerSingleSlashOperatorTest() {
        List<Token> all = tokenizeAll("/");
        assertEquals(3, all.size(), "wrong number of tokens for '/'");
        assertEqualsToken(0, 0, all, 1, TokenType.OPERATOR);
        assertEquals("/", all.get(1).text(), "single slash should be operator");
    }

    @Test
    public void tokenizerSlashEqualsOperatorTest() {
        List<Token> all = tokenizeAll("/=");
        assertEquals(3, all.size(), "wrong number of tokens for '/='");
        assertEqualsToken(0, 0, all, 1, TokenType.OPERATOR);
        assertEquals("/=", all.get(1).text(), "/= should be operator");
    }

    @Test
    public void tokenizerCodeFollowedByEmptyCommentTest() {
        List<Token> all = tokenizeAll("a//");
        assertEquals(4, all.size(), "wrong number of tokens for 'a//'");
        assertEqualsToken(0, 0, all, 1, TokenType.NAME);
        assertEqualsToken(0, 1, all, 2, TokenType.END_LINE_COMMENT);
        assertEquals("//", all.get(2).text(), "trailing // should be comment");
    }

    @Test
    public void tokenizerCodeWithSpaceAndEmptyCommentTest() {
        List<Token> all = tokenizeAll("a //");
        assertEquals(5, all.size(), "wrong number of tokens for 'a //'");
        assertEqualsToken(0, 0, all, 1, TokenType.NAME);
        assertEqualsToken(0, 1, all, 2, TokenType.HSPACE);
        assertEqualsToken(0, 2, all, 3, TokenType.END_LINE_COMMENT);
        assertEquals("//", all.get(3).text(), "trailing // should be comment");
    }

    @Test
    public void tokenizerDivisionExpressionTest() {
        List<Token> all = tokenizeAll("a/b");
        assertEquals(5, all.size(), "wrong number of tokens for 'a/b'");
        assertEqualsToken(0, 0, all, 1, TokenType.NAME);
        assertEqualsToken(0, 1, all, 2, TokenType.OPERATOR);
        assertEqualsToken(0, 2, all, 3, TokenType.NAME);
        assertEquals("/", all.get(2).text(), "/ in division should be operator");
    }

    @Test
    public void tokenizerEmptyCommentWithNewlineTest() {
        List<Token> all = tokenizeAll("//\n");
        assertEquals(4, all.size(), "wrong number of tokens for '//\\n'");
        assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
        assertEqualsToken(0, 2, all, 2, TokenType.NEWLINE);
        assertEquals("//", all.get(1).text(), "// before newline should be comment");
    }

    @Test
    public void tokenizerMultipleEmptyCommentsTest() {
        List<Token> all = tokenizeAll("//\n//\n//");
        assertEquals(7, all.size(), "wrong number of tokens for multiple //");
        assertEqualsToken(0, 0, all, 1, TokenType.END_LINE_COMMENT);
        assertEqualsToken(0, 2, all, 2, TokenType.NEWLINE);
        assertEqualsToken(1, 0, all, 3, TokenType.END_LINE_COMMENT);
        assertEqualsToken(1, 2, all, 4, TokenType.NEWLINE);
        assertEqualsToken(2, 0, all, 5, TokenType.END_LINE_COMMENT);
    }

    @Test
    public void tokenizerMixedOperatorsTest() {
        List<Token> all = tokenizeAll("+-*/");
        assertEquals(3, all.size(), "wrong number of tokens for '+-*/'");
        assertEqualsToken(0, 0, all, 1, TokenType.OPERATOR);
        assertEquals("+-*/", all.get(1).text(), "mixed operators should be single token");
    }

    @Test
    public void unicodeLettersMakeNames() {
        // Zoe and Mueller with their accents, a Japanese and a Greek name, Zoe with a combining diaeresis, x with an
        // Arabic-Indic digit
        String input = "Zo\u00eb M\u00fcller \u6771\u4eac \u0395\u03bb\u03bb\u03ac\u03b4\u03b1 Zoe\u0308 x\u0663 _a1";
        assertEquals("BEGINOFFILE NAME HSPACE NAME HSPACE NAME HSPACE NAME HSPACE NAME HSPACE NAME HSPACE NAME ENDOFFILE", types(input));
        List<Token> all = tokenizeAll(input);
        assertEquals("Zoe\u0308", all.get(9).text());
        assertEqualsToken(0, 21, all, 9, TokenType.NAME);
    }

    @Test
    public void unicodeSymbolsMakeOperators() {
        // a heart, less-or-equal, an emoji outside the BMP, a red heart with its variation selector, a superscript two,
        // a family emoji joined by zero-width joiners, a flag of two regional indicators
        String input = "a\u2665b \u2264 c \ud83d\ude00 d \u2764\ufe0f e\u00b2 \ud83d\udc68\u200d\ud83d\udc69\u200d\ud83d\udc67 \ud83c\uddf3\ud83c\uddf1";
        assertEquals("BEGINOFFILE NAME OPERATOR NAME HSPACE OPERATOR HSPACE NAME HSPACE OPERATOR HSPACE NAME HSPACE OPERATOR HSPACE NAME OPERATOR"
                + " HSPACE OPERATOR HSPACE OPERATOR ENDOFFILE", types(input));
        List<Token> all = tokenizeAll(input);
        assertEquals("\u2764\ufe0f", all.get(13).text());
        assertEquals("\ud83d\udc68\u200d\ud83d\udc69\u200d\ud83d\udc67", all.get(18).text());
    }

    @Test
    public void invisibleFormatCharactersAreSpace() {
        // a byte order mark, a zero-width space, a soft hyphen and a left-to-right mark
        assertEquals("BEGINOFFILE HSPACE NAME HSPACE NAME HSPACE NAME HSPACE NAME ENDOFFILE", types("\ufeffa\u200bb\u00adc\u200ed"));
    }

    @Test
    public void onlyTheLineBreaksOfTheEditorsAreNewlines() {
        // line separator, paragraph separator, next line, vertical tab and form feed: editors and LSP show them on one line
        List<Token> all = tokenizeAll("a\u2028b\u2029c\u0085d\u000Be\ff\r\ng\rh\ni");
        assertEquals("BEGINOFFILE NAME HSPACE NAME HSPACE NAME HSPACE NAME HSPACE NAME HSPACE NAME NEWLINE NAME NEWLINE NAME NEWLINE NAME ENDOFFILE",
                all.map(t -> t.type().name()).collect(Collectors.joining(" ")));
        assertEqualsToken(0, 10, all, 11, TokenType.NAME);
        assertEqualsToken(1, 0, all, 13, TokenType.NAME);
        assertEquals("BEGINOFFILE END_LINE_COMMENT ENDOFFILE", types("// a\u2028b"), "a comment ends only at a line break");
    }

    @Test
    public void positionsCountUtf16UnitsAsLsp() {
        List<Token> all = tokenizeAll("\"\ud83d\ude00\" x");
        assertEqualsToken(0, 5, all, 3, TokenType.NAME);
    }

    @Test
    public void unicodeNamesAndOperatorsParseAndEvaluate() {
        testString("""
                import nelumbo.logic
                import nelumbo.strings

                Person  :: Object
                Person  ::= Zo\u00eb, M\u00fcller, \u6771\u4eac
                Boolean ::= <Person> \u2665 <Person> #30,
                            <Person> \ud83d\ude00 <Person> #30
                Person  p

                fact Zo\u00eb \u2665 M\u00fcller, \u6771\u4eac \ud83d\ude00 Zo\u00eb

                Zo\u00eb \u2665 p ? [(p=M\u00fcller),..][..]
                \u6771\u4eac \ud83d\ude00 Zo\u00eb ? [()][]
                len("Zo\u00eb \ud83d\ude00") ? [(5)][..]
                """, "unicodeNamesAndOperators");
    }

    private static String types(String input) {
        return tokenizeAll(input).map(t -> t.type().name()).collect(Collectors.joining(" "));
    }

    private static List<Token> tokenizeAll(String input) {
        return new Tokenizer(input, "test").tokenize().listAll();
    }

    private static void assertEqualsToken(int expectedLine, int expectedPosition, List<Token> tokens, int index,
            TokenType type) {
        assertTrue(index < tokens.size(), "token index out of bounds");
        Token token = tokens.get(index);
        assertEquals(type, token.type());
        assertEquals(expectedLine, token.line());
        assertEquals(expectedPosition, token.position());
    }
}
