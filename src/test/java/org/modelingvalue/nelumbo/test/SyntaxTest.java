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

import java.text.ParseException;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.Rule;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class SyntaxTest extends NelumboTestBase {

    @Test
    public void tokenizer() {
        String example = """
                    -abb + bcc *
                       c - dee //*COMMEND*!@
                    e = 8.9 / 2
                """;
        try {
            LinkedList<Token> tokens = new Tokenizer(example).tokenize();
            // printTokens(tokens);
            assertEquals(17, tokens.size());
        } catch (ParseException e) {
            fail(e);
        }
    }

    @Test
    public void parser1() {
        run(() -> {
            String example = """
                        org.my.test :
                            nelumbo.logic,
                            nelumbo.integers

                        <Node>
                        <Lit>    : <Node>
                        <Pred>   : <Node>
                        <Rel>    : <Pred>
                        <Rule>   : <Node>
                        <Int>    : <Node>
                        <IntLit> : <Int>, <Lit>
                        <Str>    : <Node>
                        <StrLit> : <Str>, <Lit>

                        NUMBER : <IntLit>
                        STRING : <StrLit>

                        @org.modelingvalue.nelumbo.Rule
                        <Rel> <==(10) <Pred> : <Rule>

                        <Node> =(30) <Node>  : <Rel>

                        @org.modelingvalue.nelumbo.integers.GreaterThen
                        gt(<IntLit>,<IntLit>) : <Rel>

                        <Int> <(30)  <Int> : <Rel>
                        <Int> >(30)  <Int> : <Rel>
                        <Int> <=(30) <Int> : <Rel>
                        <Int> >=(30) <Int> : <Rel>

                        <Int> -(40) <Int> : <Int>
                        <Int> +(40) <Int> : <Int>

                        -(50) <Int> : <Int>

                        a : <Int>
                        b : <Int>

                        x : <IntLit>
                        y : <IntLit>

                        a>b  <== a=x & b=y & gt(x,y)

                        a<b  <== b>a
                        a<=b <== a<b | a=b
                        a>=b <== !(a<b)
                        -a=b <== 0-a=b

                    """;
            try {
                LinkedList<Token> tokens = new Tokenizer(example).tokenize();
                Parser parser = new Parser(tokens);
                var roots = parser.parseRoots();
                roots = roots.removeAll(s -> !(s instanceof Rule));
                assertEquals(5, roots.size());
                String expected = "[" + //
                        "rule(gt(a,b),and(and(eq(a,x),eq(b,y)),gt(x,y)))," + //
                        "rule(lt(a,b),gt(b,a))," + //
                        "rule(le(a,b),or(lt(a,b),eq(a,b)))," + //
                        "rule(ge(a,b),not(lt(a,b)))," + //
                        "rule(eq(min(a),b),eq(min(int(0),a),b))" + //
                        "]";
                assertEquals(expected, roots.toString().substring(4));
            } catch (ParseException e) {
                fail(e);
            }
        });
    }

    @Test
    public void parser2() {
        run(() -> {
            String example = """
                        org.mvg.fib :
                            nelumbo.logic,
                            nelumbo.integers

                        <Rel>    : <Pred>
                        <IntLit> : <Int>, <Lit>
                        <IntFun> : <Int>, <Fun>

                        fib(<IntConst>,<IntConst>) : <Rel>
                        fib(<Int>)                 : <IntFun>

                        // Literal Integer Variables
                        x : <IntLit>
                        y : <IntLit>

                        // Int Variables
                        a : <Int>
                        b : <Int>

                        // Function-like Syntaxtual Suggar
                        fib(a)=b <== a=x & b=y & fib(x,y)

                        // Facts
                        fib(0,0)
                        fib(1,1)

                        // Rule
                        fib(a,b) <== a>1 & b=fib(a-1)+fib(a-2)

                    """;
            try {
                LinkedList<Token> tokens = new Tokenizer(example).tokenize();
                Parser parser = new Parser(tokens);
                var roots = parser.parseRoots();
                roots = roots.removeAll(s -> !(s instanceof Rule));
                assertEquals(5, roots.size());
                String expected = "[" + //
                        "rule(gt(a,b),and(and(eq(a,x),eq(b,y)),gt(x,y)))," + //
                        "rule(lt(a,b),gt(b,a))," + //
                        "rule(le(a,b),or(lt(a,b),eq(a,b)))," + //
                        "rule(ge(a,b),not(lt(a,b)))," + //
                        "rule(eq(min(a),b),eq(min(int(0),a),b))" + //
                        "]";
                assertEquals(expected, roots.toString().substring(4));
            } catch (ParseException e) {
                fail(e);
            }
        });
    }

    @Test
    public void parser3() {
        run(() -> {
            String example = """
                        org.mvg.fib :
                            nelumbo.logic

                        <Person>    : <Node>
                        <PersonLit> : <Person>, <Literal>
                        <PersonFun> : <Person>, <Function>

                        pc(<PersonLit>,<PersonLit>) : <Rel>
                        p(<Person>)                 : <PersonFun>
                        c(<Person>)                 : <PersonFun>

                        ad(<PersonLit>,<PersonLit>) : <Rel>
                        a(<Person>)                 : <PersonFun>
                        d(<Person>)                 : <PersonFun>

                        x : <PersonLit>
                        y : <PersonLit>
                        z : <PersonLit>

                        a : <Person>
                        b : <Person>
                        c : <Person>

                        ad(x,z) <== pc(x,z) | (ad(x,y) & pc(y, z))

                        c(a)=b <== a=x & b=y & pc(x,y)
                        p(a)=b <== c(b)=a
                        d(a)=b <== a=x & b=y & ad(x,y)
                        a(a)=b <== d(b)=a
                    """;
            try {
                LinkedList<Token> tokens = new Tokenizer(example).tokenize();
                Parser parser = new Parser(tokens);
                var roots = parser.parseRoots();
                roots = roots.removeAll(s -> !(s instanceof Rule));
                assertEquals(5, roots.size());
                String expected = "[" + //
                        "rule(gt(a,b),and(and(eq(a,x),eq(b,y)),gt(x,y)))," + //
                        "rule(lt(a,b),gt(b,a))," + //
                        "rule(le(a,b),or(lt(a,b),eq(a,b)))," + //
                        "rule(ge(a,b),not(lt(a,b)))," + //
                        "rule(eq(min(a),b),eq(min(int(0),a),b))" + //
                        "]";
                assertEquals(expected, roots.toString().substring(4));
            } catch (ParseException e) {
                fail(e);
            }
        });
    }

    @SuppressWarnings("unused")
    private static void printTokens(LinkedList<Token> tokens) {
        for (Token token : tokens) {
            System.out.println("Token: " + token);
        }
    }

}
