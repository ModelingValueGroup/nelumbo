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
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
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

                    <Set>      ::= { <[> <Node> <{> , <Node> <}> <]> } #10 #5 @org.modelingvalue.nelumbo.Node
                    <Set>      ::= <Set> + <Node> #40
                    <Int>      ::= <Set>.size
                    <Int>      ::= <NUMBER>
                    <Relation> ::= <Int> = <Int>

                    <Set>  s, t
                    <Int>  i, j

                    10=10

                    s.size=i <==> i=10

                    """;
            try {
                TokenizerResult result = new Tokenizer(example, "SyntaxTest.test1").tokenize();
                for (Node root : new Parser(result).parse()) {
                    System.out.println(root);
                }
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

}
