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

import org.junit.jupiter.api.Test;
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

    @Test
    public void tokenizerTest() {
        String example = """
                // COMMEND
                    -abb + bcc *
                       c - dee // MORE COMMEND
                    e = 8.9 / 2
                """;
        try {
            LinkedList<Token> tokens = new Tokenizer(example, "tokenizerTest").tokenize();
            // printTokens(tokens);
            assertEquals(16, tokens.size());
        } catch (ParseException e) {
            fail(e);
        }
    }

    @Test
    public void initTest() {
        run(() -> {
            String example = """

                    // Init only

                    """;
            try {
                new Parser(new Tokenizer(example, "initTest").tokenize()).parse();
                // printKnowledgeBase();
            } catch (ParseException e) {
                fail(e);
            }
        });
    }

    @Test
    public void familyTest() {
        run(() -> {
            String example = """
                        // org.mvg.family ::
                        //     nelumbo.logic,
                        //     nelumbo.integers

                        <Person>    :: <Node>
                        <PersonLit> :: <Person>, <Literal>
                        <PersonFun> :: <Person>, <Function>

                        <Fact>      ::= pc(<PersonLit>,<PersonLit>),
                                        ad(<PersonLit>,<PersonLit>)

                        <PersonFun> ::= p(<Person>),
                                        c(<Person>),
                                        a(<Person>),
                                        d(<Person>)

                        <PersonLit> x, y, z
                        <Person>    a, b, c

                        ad(x,z) <==  pc(x,z),
                                     ad(x,y) & pc(y, z)

                        c(a)=b  <==  a=x & b=y & pc(x,y)
                        p(a)=b  <==  c(b)=a
                        d(a)=b  <==  a=x & b=y & ad(x,y)
                        a(a)=b  <==  d(b)=a

                        <PersonLit> ::= Piet, Jan, Hein

                        pc(Piet,Jan)
                        pc(Jan, Hein)

                        ? p(Piet)=Jan
                        ? p(Jan)=Hein

                        ? p(Hein)=Jan
                        ? p(Jan)=Piet

                        ? p(Piet)=x
                        ? p(Jan)=x
                        ? p(Hein)=x

                        ? c(Piet)=x
                        ? c(Jan)=x
                        ? c(Hein)=x

                        ? a(Piet)=x
                        ? a(Jan)=x
                        ? a(Hein)=x

                        ? d(Piet)=x
                        ? d(Jan)=x
                        ? d(Hein)=x

                    """;
            try {
                new Parser(new Tokenizer(example, "familyTest").tokenize()).parse();
                // printKnowledgeBase();
            } catch (ParseException e) {
                fail(e);
            }
        });
    }

    @Test
    public void integersTest() {
        run(() -> {
            String example = """

                        <IntegerLit>  a, b, c

                        ? -1=-1

                        ? 0>1
                        ? 0<1
                        ? 0>=1
                        ? 0<=1
                        ? 0!=1
                        ? 0=1

                        ? 10+11=22
                        ? 10+11=21
                        ? a+11=21
                        ? 10+a=21
                        ? 10+11=a

                        ? 10-11=1
                        ? 10-11=-1
                        ? a-11=-1
                        ? 10-a=-1
                        ? 10-11=a

                        ? 10*11=120
                        ? 10*11=110
                        ? a*11=110
                        ? 10*a=110
                        ? 10*11=a

                        ? abs(-10) = 10
                        ? abs(a) = 10
                        ? abs(10) = a
                        ? abs(10) = 10


                    """;
            try {
                Parser.parseLogic(Integer.class);
                new Parser(new Tokenizer(example, "integersTest").tokenize()).parse();
                // printKnowledgeBase();
            } catch (ParseException e) {
                fail(e);
            }
        });
    }

    public void fibTest() {
        run(() -> {
            String example = """

                        <Fact>        ::= fib(<IntegerLit>,<IntegerLit>)
                        <IntegerFun>  ::= fib(<Integer>)

                        // Literal Integer Variables
                        <IntegerLit> x, y

                        // Int Variables
                        <Integer>    a, b

                        // Function-like Syntaxtual Suggar
                        fib(a)=b  <==  a=x & b=y & fib(x,y)

                        // Facts
                        fib(0,0)
                        fib(1,1)

                        // Rule
                        fib(a,b)  <==  a>1 & b=fib(a-1)+fib(a-2)

                    """;
            try {
                Parser.parseLogic(Integer.class);
                new Parser(new Tokenizer(example, "fibTest").tokenize()).parse();
                // printKnowledgeBase();
            } catch (ParseException e) {
                fail(e);
            }
        });
    }
}
