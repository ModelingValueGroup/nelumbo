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

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.U;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public class TokenizerTest extends NelumboTestBase {
    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "true");
        setProp("TRACE_NELUMBO", "false");
        setProp("VERBOSE_TESTS", "false");
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
        String expectedTypes = "BEGINOFFILE END_LINE_COMMENT NEWLINE HSPACE OPERATOR NAME HSPACE OPERATOR HSPACE NAME HSPACE OPERATOR NEWLINE HSPACE NAME HSPACE OPERATOR HSPACE NAME HSPACE END_LINE_COMMENT NEWLINE HSPACE NAME HSPACE OPERATOR HSPACE DECIMAL HSPACE OPERATOR HSPACE NUMBER NEWLINE ENDOFFILE";

        U.printTokens("tokens", tokens);
        U.printTokens("all", all);

        assertEquals(17, tokens.size(), "wrong number of tokens returned by tokenize()");
        assertEquals(34, all.size(), "wrong number of tokens returned by tokenize(all)");
        assertEquals(example, reassembled, "could not reassemble tokens");
        assertEquals(expectedTypes, types, "unexpected token types in token list");

        //==========================================================================================
        assertEquals(TokenType.END_LINE_COMMENT, all.get(1).type());
        assertEquals(0, all.get(1).line());
        assertEquals(0, all.get(1).position());

        assertEquals(TokenType.NEWLINE, all.get(2).type());
        assertEquals(0, all.get(2).line());
        assertEquals(10, all.get(2).position());

        assertEquals(TokenType.HSPACE, all.get(3).type());
        assertEquals(1, all.get(3).line());
        assertEquals(0, all.get(3).position());

        assertEquals(TokenType.OPERATOR, all.get(4).type());
        assertEquals(1, all.get(4).line());
        assertEquals(4, all.get(4).position());

        assertEquals(TokenType.NAME, all.get(5).type());
        assertEquals(1, all.get(5).line());
        assertEquals(5, all.get(5).position());

        //==========================================================================================
        assertEquals(TokenType.OPERATOR, tokens.get(1).type());
        assertEquals(1, tokens.get(1).line());
        assertEquals(4, tokens.get(1).position());

        assertEquals(TokenType.NAME, tokens.get(2).type());
        assertEquals(1, tokens.get(2).line());
        assertEquals(5, tokens.get(2).position());

        assertEquals(TokenType.OPERATOR, tokens.get(3).type());
        assertEquals(1, tokens.get(3).line());
        assertEquals(9, tokens.get(3).position());

        assertEquals(TokenType.NAME, tokens.get(4).type());
        assertEquals(1, tokens.get(4).line());
        assertEquals(11, tokens.get(4).position());

        assertEquals(TokenType.OPERATOR, tokens.get(5).type());
        assertEquals(1, tokens.get(5).line());
        assertEquals(15, tokens.get(5).position());
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

        assertEquals(TokenType.IN_LINE_COMMENT, all.get(1).type());
        assertEquals(0, all.get(1).line());
        assertEquals(0, all.get(1).position());
    }

    @Test
    public void tokenizerComment2Test() {
        String example = "<a/*a*/>•a";

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

        //==========================================================================================
        assertEquals(TokenType.OPERATOR, all.get(1).type());
        assertEquals(0, all.get(1).line());
        assertEquals(0, all.get(1).position());

        assertEquals(TokenType.NAME, all.get(2).type());
        assertEquals(0, all.get(2).line());
        assertEquals(1, all.get(2).position());

        assertEquals(TokenType.IN_LINE_COMMENT, all.get(3).type());
        assertEquals(0, all.get(3).line());
        assertEquals(2, all.get(3).position());

        assertEquals(TokenType.OPERATOR, all.get(4).type());
        assertEquals(0, all.get(4).line());
        assertEquals(7, all.get(4).position());

        assertEquals(TokenType.ERROR, all.get(5).type());
        assertEquals(0, all.get(5).line());
        assertEquals(8, all.get(5).position());

        //==========================================================================================
        assertEquals(TokenType.OPERATOR, tokens.get(1).type());
        assertEquals(0, tokens.get(1).line());
        assertEquals(0, tokens.get(1).position());

        assertEquals(TokenType.NAME, tokens.get(2).type());
        assertEquals(0, tokens.get(2).line());
        assertEquals(1, tokens.get(2).position());

        assertEquals(TokenType.OPERATOR, tokens.get(3).type());
        assertEquals(0, tokens.get(3).line());
        assertEquals(7, tokens.get(3).position());

        assertEquals(TokenType.ERROR, tokens.get(4).type());
        assertEquals(0, tokens.get(4).line());
        assertEquals(8, tokens.get(4).position());
    }

}
