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

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public class SyntaxTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "true");
        setProp("TRACE_NELUMBO", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @Test()
    public void test1() {
        run(() -> {
            String example = """

                    <Set>      ::  <Node>
                    <Int>      ::  <Node>

                    <Int>      ::= <Set>.size,
                                   <NUMBER>
                    <Set>      ::= <Set> + <Node>                           #40,
                                   { <[> <Node> <{> , <Node> <}> <]> }      @org.modelingvalue.nelumbo.Node,
                                   <Set> - <Node>                           #40

                    <Set>  s, t, u
                    <Int>  i, j, k
                    <Node> n

                    s=t+n ?
                    s=t+n ? [..][..]

                    s.size=i   <==> i=10

                    """;
            try {
                TokenizerResult tr = new Tokenizer(example, "SyntaxTest.test1").tokenize();
                ParserResult pr = new Parser(tr).parseThrowing();
                pr.print();
                pr.evaluate();
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    @Test()
    public void test2() {
        run(() -> {
            try {
                Parser.parse(Integer.class);
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

}
