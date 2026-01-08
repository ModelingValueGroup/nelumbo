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

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.RepeatedTest;
import org.modelingvalue.nelumbo.U;
import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class NelumboTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "true");
        setProp("TRACE_NELUMBO", "false");
        setProp("TRACE_SYNTATIC", "false");
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
    public void friendsTest() {
        run(() -> {
            try {
                U.printResults(Parser.parse(NelumboTest.class, "friendsTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @RepeatedTest(10)
    public void whoIsTest() {
        run(() -> {
            try {
                U.printResults(Parser.parse(NelumboTest.class, "friendsTest.nl"));
                U.printResults(Parser.parse(NelumboTest.class, "whoIsTest.nl"));
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
                Parser.parse(Integer.class, "integers.nl");
                U.printResults(Parser.parse(NelumboTest.class, "integersTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @RepeatedTest(10)
    public void queryOnlyTest() {
        run(() -> {
            try {
                Parser.parse(Integer.class, "integers.nl");
                U.printResults(Parser.parse(NelumboTest.class, "queryOnly.nl"));
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
                Parser.parse(Integer.class, "integers.nl"); // ?
                Parser.parse(org.modelingvalue.nelumbo.strings.String.class, "strings.nl");
                U.printResults(Parser.parse(NelumboTest.class, "stringsTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @RepeatedTest(10)
    public void belastingTest() {
        run(() -> {
            try {
                Parser.parse(Integer.class, "integers.nl"); // ?
                U.printResults(Parser.parse(NelumboTest.class, "belastingTest.nl"));
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
                Parser.parse(Integer.class, "integers.nl");
                U.printResults(Parser.parse(NelumboTest.class, "fibonacciTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @RepeatedTest(10)
    public void transformationTest() {
        run(() -> {
            try {
                Parser.parse(Integer.class, "integers.nl");
                Parser.parse(org.modelingvalue.nelumbo.strings.String.class, "strings.nl");
                U.printResults(Parser.parse(NelumboTest.class, "transformationTest.nl"));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

}
