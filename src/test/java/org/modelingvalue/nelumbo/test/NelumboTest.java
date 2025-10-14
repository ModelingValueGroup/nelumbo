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

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Collectors;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.U;
import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public class NelumboTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "true");
        setProp("TRACE_NELUMBO", "true");
        setProp("VERBOSE_TESTS", "false");
    }

    @RepeatedTest(10)
    public void initTest() {
        run(() -> {
            String example = """
                    // Init only
                    """;
            try {
                new Parser(new Tokenizer(example, "NelumboTest.initTest").tokenize()).parseEvaluate();
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @RepeatedTest(10)
    public void logicTest() {
        run(() -> {
            try {
                U.printResults(Parser.parse(NelumboTest.class, "logicTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @RepeatedTest(10)
    public void familyTest() {
        run(() -> {
            try {
                U.printResults(Parser.parse(NelumboTest.class, "familyTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @RepeatedTest(10)
    public void integersTest() {
        run(() -> {
            try {
                Parser.parse(Integer.class);
                U.printResults(Parser.parse(NelumboTest.class, "integersTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @RepeatedTest(10)
    public void stringsTest() {
        run(() -> {
            try {
                Parser.parse(org.modelingvalue.nelumbo.integers.Integer.class); // ?
                Parser.parse(org.modelingvalue.nelumbo.strings.String.class);
                U.printResults(Parser.parse(NelumboTest.class, "stringsTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @RepeatedTest(10)
    public void fibonacciTest() {
        run(() -> {
            try {
                Parser.parse(Integer.class);
                U.printResults(Parser.parse(NelumboTest.class, "fibonacciTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @Test
    public void tokenSplitTest() {
        run(() -> {
            try {
                Parser.parse(org.modelingvalue.nelumbo.integers.Integer.class);
                String nl = "-4=-(2+2) ?";

                TokenizerResult tr = new Tokenizer(nl, "NelumboTest.tokenSplitTest").tokenize();
                //U.printTokens("before-parse", tokens);
                List<Token> all = tr.listAll();
                assertEquals(11, all.size(), "wrong number of tokens returned by tokenize()");
                assertEquals("-,4,=-,(,2,+,2,), ,?,", //
                        all.map(Token::text).collect(Collectors.joining(",")), //
                        "token texts before-parse not as expected");

                List<Node> result = new Parser(tr).parseEvaluate().roots();
                //U.printTokens("after-parse", tokens);
                all = tr.listAll();
                assertEquals(12, all.size(), "wrong number of tokens after parse()");
                assertEquals("-,4,=,-,(,2,+,2,), ,?,", //
                        all.map(Token::text).collect(Collectors.joining(",")), //
                        "token texts after-parse not as expected");
                assertEquals(1, result.size(), "wrong number of result nodes");

                assertEquals("-,4,=,-,(,2,+,2,),?,", tr.list().map(Token::text).collect(Collectors.joining(",")), //
                        "result tokens text not as expected");
                assertEquals("OPERATOR,NUMBER,OPERATOR,OPERATOR,LEFT,NUMBER,OPERATOR,NUMBER,RIGHT,OPERATOR", //
                        result.first().tokens().map(Token::type).map(Enum::toString).collect(Collectors.joining(",")), //
                        "result tokens type not as expected");

                U.printNode("all result nodes", result);
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

}
