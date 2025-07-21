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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.LinkedList;

import org.junit.jupiter.api.RepeatedTest;
import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class NelumboTest extends NelumboTestBase {

    static {
        System.setProperty("PARALLEL_COLLECTIONS", "false");
        System.setProperty("REVERSE_NELUMBO", "false");
        System.setProperty("RANDOM_NELUMBO", "true");
        System.setProperty("TRACE_NELUMBO", "false");
    }

    @RepeatedTest(100)
    public void tokenizerTest() {
        String example = """
                // COMMEND
                    -abb + bcc *
                       c - dee // MORE COMMEND
                    e = 8.9 / 2
                """;
        try {
            LinkedList<Token> tokens = new Tokenizer(example, "tokenizerTest").tokenize();
            assertEquals(16, tokens.size());
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            fail(e);
        }
    }

    @RepeatedTest(100)
    public void initTest() {
        run(() -> {
            String example = """
                    // Init only
                    """;
            try {
                new Parser(new Tokenizer(example, "initTest").tokenize()).parse();
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
                printResults(Parser.parse(NelumboTest.class, "familyTest.nl"));
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
                printResults(Parser.parse(NelumboTest.class, "integersTest.nl"));
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
                Parser.parse(org.modelingvalue.nelumbo.integers.Integer.class);  // ?
                Parser.parse(org.modelingvalue.nelumbo.strings.String.class);
                printResults(Parser.parse(NelumboTest.class, "stringsTest.nl"));
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
                printResults(Parser.parse(NelumboTest.class, "fibonacciTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }
}
