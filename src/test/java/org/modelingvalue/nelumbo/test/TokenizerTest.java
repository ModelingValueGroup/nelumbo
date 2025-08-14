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

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class TokenizerTest extends NelumboTestBase {
    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "true");
        setProp("TRACE_NELUMBO", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @Test
    public void tokenizerTest() throws ParseException {
        String example = """
                         // COMMENT
                             -abb + bcc *
                                c - dee // ANOTHER COMMENT
                             e = 8.9 / 2
                         """;

        LinkedList<Token> tokens        = new Tokenizer(example, "tokenizerTest").tokenize();
        LinkedList<Token> all           = new Tokenizer(example, "tokenizerTest.all", true).tokenize();
        String            reassembled   = all.stream().map(Token::text).collect(Collectors.joining());
        String            types         = all.stream().map(t -> t.type().name()).collect(Collectors.joining(" "));
        String            expectedTypes = "END_LINE_COMMENT NEWLINE HSPACE OPERATOR NAME HSPACE OPERATOR HSPACE NAME HSPACE OPERATOR NEWLINE HSPACE NAME HSPACE OPERATOR HSPACE NAME HSPACE END_LINE_COMMENT NEWLINE HSPACE NAME HSPACE OPERATOR HSPACE DECIMAL HSPACE OPERATOR HSPACE NUMBER NEWLINE";

        printTokens("tokens", tokens);
        printTokens("all", all);

        assertEquals(15, tokens.size(), "wrong number of tokens returned by tokenize()");
        assertEquals(32, all.size(), "wrong number of tokens returned by tokenize(all)");
        assertEquals(example, reassembled, "could not reassemble tokens");
        assertEquals(expectedTypes, types, "unexpected token types in token list");

        //==========================================================================================
        assertEquals(TokenType.END_LINE_COMMENT, all.get(0).type());
        assertEquals(0, all.get(0).line());
        assertEquals(0, all.get(0).position());

        assertEquals(TokenType.NEWLINE, all.get(1).type());
        assertEquals(0, all.get(1).line());
        assertEquals(10, all.get(1).position());

        assertEquals(TokenType.HSPACE, all.get(2).type());
        assertEquals(1, all.get(2).line());
        assertEquals(0, all.get(2).position());

        assertEquals(TokenType.OPERATOR, all.get(3).type());
        assertEquals(1, all.get(3).line());
        assertEquals(4, all.get(3).position());

        assertEquals(TokenType.NAME, all.get(4).type());
        assertEquals(1, all.get(4).line());
        assertEquals(5, all.get(4).position());

        //==========================================================================================
        assertEquals(TokenType.OPERATOR, tokens.get(0).type());
        assertEquals(1, tokens.get(0).line());
        assertEquals(4, tokens.get(0).position());

        assertEquals(TokenType.NAME, tokens.get(1).type());
        assertEquals(1, tokens.get(1).line());
        assertEquals(5, tokens.get(1).position());

        assertEquals(TokenType.OPERATOR, tokens.get(2).type());
        assertEquals(1, tokens.get(2).line());
        assertEquals(9, tokens.get(2).position());

        assertEquals(TokenType.NAME, tokens.get(3).type());
        assertEquals(1, tokens.get(3).line());
        assertEquals(11, tokens.get(3).position());

        assertEquals(TokenType.OPERATOR, tokens.get(4).type());
        assertEquals(1, tokens.get(4).line());
        assertEquals(15, tokens.get(4).position());
    }

    @Test
    public void tokenizerCommentTest() throws ParseException {
        String example = "<a/*a*/>•a";

        LinkedList<Token> tokens        = new Tokenizer(example, "tokenizerCommentTest").tokenize();
        LinkedList<Token> all           = new Tokenizer(example, "tokenizerCommentTest.all", true).tokenize();
        String            reassembled   = all.stream().map(Token::text).collect(Collectors.joining());
        String            types         = all.stream().map(t -> t.type().name()).collect(Collectors.joining(" "));
        String            expectedTypes = "OPERATOR NAME IN_LINE_COMMENT OPERATOR ERROR NAME";

        printTokens("tokens", tokens);
        printTokens("all", all);

        assertEquals(5, tokens.size(), "wrong number of tokens returned by tokenize()");
        assertEquals(6, all.size(), "wrong number of tokens returned by tokenize(all)");
        assertEquals(example, reassembled, "could not reassemble tokens");
        assertEquals(expectedTypes, types, "unexpected token types in token list");

        //==========================================================================================
        assertEquals(TokenType.OPERATOR, all.get(0).type());
        assertEquals(0, all.get(0).line());
        assertEquals(0, all.get(0).position());

        assertEquals(TokenType.NAME, all.get(1).type());
        assertEquals(0, all.get(1).line());
        assertEquals(1, all.get(1).position());

        assertEquals(TokenType.IN_LINE_COMMENT, all.get(2).type());
        assertEquals(0, all.get(2).line());
        assertEquals(2, all.get(2).position());

        assertEquals(TokenType.OPERATOR, all.get(3).type());
        assertEquals(0, all.get(3).line());
        assertEquals(7, all.get(3).position());

        assertEquals(TokenType.ERROR, all.get(4).type());
        assertEquals(0, all.get(4).line());
        assertEquals(8, all.get(4).position());

        //==========================================================================================
        assertEquals(TokenType.OPERATOR, tokens.get(0).type());
        assertEquals(0, tokens.get(0).line());
        assertEquals(0, tokens.get(0).position());

        assertEquals(TokenType.NAME, tokens.get(1).type());
        assertEquals(0, tokens.get(1).line());
        assertEquals(1, tokens.get(1).position());

        assertEquals(TokenType.OPERATOR, tokens.get(2).type());
        assertEquals(0, tokens.get(2).line());
        assertEquals(7, tokens.get(2).position());

        assertEquals(TokenType.ERROR, tokens.get(3).type());
        assertEquals(0, tokens.get(3).line());
        assertEquals(8, tokens.get(3).position());
    }

}
