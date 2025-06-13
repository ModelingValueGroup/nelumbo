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
import org.modelingvalue.nelumbo.Integers.Integer;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.Structure;
import org.modelingvalue.nelumbo.impl.*;
import org.modelingvalue.nelumbo.impl.FunctorImpl.FunctorImpl1;
import org.modelingvalue.nelumbo.impl.FunctorImpl.FunctorImpl2;
import org.modelingvalue.nelumbo.syntax.AtomicParselet;
import org.modelingvalue.nelumbo.syntax.BinaryOperator;
import org.modelingvalue.nelumbo.syntax.FunctionWithArgs;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Token.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.UnaryOperator;

public class SyntaxTest extends NelumboTestBase {

    static final FunctorImpl1<Predicate, Predicate>            NOT  = FunctorImpl.of1(Predicate.class, "not", Predicate.class);
    static final FunctorImpl1<Integer, String>                 INT1 = FunctorImpl.of1(Integer.class, "int", String.class);

    static final FunctorImpl1<Relation, Integer>               MIN1 = FunctorImpl.of1(Relation.class, "min", Integer.class);

    static final FunctorImpl2<Integer, Integer, Integer>       MIN2 = FunctorImpl.of2(Integer.class, "min", Integer.class, Integer.class);

    static final FunctorImpl2<Relation, Integer, Integer>      LE   = FunctorImpl.of2(Relation.class, "le", Integer.class, Integer.class);
    static final FunctorImpl2<Relation, Integer, Integer>      GE   = FunctorImpl.of2(Relation.class, "ge", Integer.class, Integer.class);
    static final FunctorImpl2<Relation, Integer, Integer>      LT   = FunctorImpl.of2(Relation.class, "lt", Integer.class, Integer.class);
    static final FunctorImpl2<Relation, Integer, Integer>      GT   = FunctorImpl.of2(Relation.class, "gt", Integer.class, Integer.class);
    static final FunctorImpl2<Relation, Integer, Integer>      EQ   = FunctorImpl.of2(Relation.class, "eq", Integer.class, Integer.class);

    static final AtomicParselet                                VAR  = AtomicParselet.of(t -> new VariableImpl<>(Structure.class, t.text()));
    static final AtomicParselet                                INT  = AtomicParselet.of(t -> new StructureImpl<>(INT1, t.text()));

    static final FunctorImpl2<Structure, Structure, Structure> DECL = FunctorImpl.of2(Structure.class, "decl", Structure.class, Structure.class);

    static final FunctorImpl2<Structure, Structure, Structure> PREC = FunctorImpl.of2(Structure.class, "prec", Structure.class, Structure.class);

    static {
        FunctionWithArgs.of("gt", 2, (t, a) -> new RelationImpl(GT, a.get(0), a.get(1)));

        UnaryOperator.of("-", (t, r) -> new StructureImpl<>(MIN1, r));
        UnaryOperator.of("!", (t, r) -> new NotImpl(r));

        BinaryOperator.of("-", 40, (t, l, r) -> new StructureImpl<>(MIN2, l, r));
        BinaryOperator.of("=", 30, (t, l, r) -> new RelationImpl(EQ, l, r));
        BinaryOperator.of("<", 30, (t, l, r) -> new RelationImpl(LT, l, r));
        BinaryOperator.of(">", 30, (t, l, r) -> new RelationImpl(GT, l, r));
        BinaryOperator.of("<=", 30, (t, l, r) -> new RelationImpl(LE, l, r));
        BinaryOperator.of(">=", 30, (t, l, r) -> new RelationImpl(GE, l, r));
        BinaryOperator.of("|", 20, (t, l, r) -> new OrImpl(l, r));
        BinaryOperator.of("&", 20, (t, l, r) -> new AndImpl(l, r));
        BinaryOperator.of(":=", 10, (t, l, r) -> {
            RuleImpl ruleImpl = new RuleImpl(l, r);
            if (l instanceof RelationImpl && r instanceof PredicateImpl) {
                KnowledgeBaseImpl.CURRENT.get().addRule(ruleImpl);
            }
            return ruleImpl;
        });
        BinaryOperator.of("->", 5, (t, l, r) -> {
            StructureImpl<?> declaration = new StructureImpl<>(DECL, l, r);
            return declaration;
        });
        BinaryOperator.of("#", 3, (t, l, r) -> {
            StructureImpl<?> precedence = new StructureImpl<>(PREC, l, r);
            return precedence;
        });
    }

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
    public void parser() {
        run(() -> {
            setPrettyPrinting(false);
            String example = """
                        // org.my.test :
                        //     nelumbo.logic,
                        //     nelumbo.integers

                        // Struct
                        // Const    : Struct
                        // Pred     : Struct
                        // Rel      : Pred
                        // Rule     : Struct
                        // Int      : Struct
                        // IntConst : Int, Const

                        Rel    := Pred   -> Rule #10
                        Struct =  Struct -> Rel  #30

                        gt(IntConst,IntConst) -> Rel // Native

                        Int <  Int -> Rel #30
                        Int >  Int -> Rel #30
                        Int <= Int -> Rel #30
                        Int >= Int -> Rel #30

                        Int -  Int -> Int #40

                        - Int -> Int

                        a -> Int
                        b -> Int
                        c -> Int

                        x -> IntConst
                        y -> IntConst
                        z -> IntConst

                        a>b := a=x & b=y & gt(x,y)

                        a<b  := b>a
                        a<=b := a<b | a=b
                        a>=b := !(a<b)
                        -a=b := 0-a=b

                    """;
            try {
                LinkedList<Token> tokens = new Tokenizer(example).tokenize();
                Parser parser = new Parser(tokens);
                parser.register(TokenType.IDENTIFIER, VAR);
                parser.register(TokenType.NUMBER, INT);
                var roots = parser.parseRoots();
                roots = roots.removeAll(s -> !(s instanceof RuleImpl));
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
