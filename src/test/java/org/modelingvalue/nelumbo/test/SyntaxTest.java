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
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.impl.FunctorImpl;
import org.modelingvalue.nelumbo.impl.FunctorImpl.FunctorImpl2;
import org.modelingvalue.nelumbo.impl.KnowledgeBaseImpl;
import org.modelingvalue.nelumbo.impl.OrImpl;
import org.modelingvalue.nelumbo.impl.PredicateImpl;
import org.modelingvalue.nelumbo.impl.RelationImpl;
import org.modelingvalue.nelumbo.impl.RuleImpl;
import org.modelingvalue.nelumbo.impl.VariableImpl;
import org.modelingvalue.nelumbo.syntax.BinaryOperator;
import org.modelingvalue.nelumbo.syntax.IdentifierParselet;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Token.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

public class SyntaxTest extends NelumboTestBase {

    static final FunctorImpl2<Relation, Integer, Integer> LE  = FunctorImpl.of2(Relation.class, "le", Integer.class, Integer.class);
    static final FunctorImpl2<Relation, Integer, Integer> GE  = FunctorImpl.of2(Relation.class, "ge", Integer.class, Integer.class);
    static final FunctorImpl2<Relation, Integer, Integer> LT  = FunctorImpl.of2(Relation.class, "lt", Integer.class, Integer.class);
    static final FunctorImpl2<Relation, Integer, Integer> GT  = FunctorImpl.of2(Relation.class, "gt", Integer.class, Integer.class);
    static final FunctorImpl2<Relation, Integer, Integer> EQ  = FunctorImpl.of2(Relation.class, "eq", Integer.class, Integer.class);

    static final IdentifierParselet                       VAR = IdentifierParselet.of(t -> new VariableImpl<>(Integer.class, t.text()));

    static {
        BinaryOperator.of("=", 30, (t, l, r) -> new RelationImpl(EQ, l, r));
        BinaryOperator.of("<", 30, (t, l, r) -> new RelationImpl(LT, l, r));
        BinaryOperator.of(">", 30, (t, l, r) -> new RelationImpl(GT, l, r));
        BinaryOperator.of("<=", 30, (t, l, r) -> new RelationImpl(LE, l, r));
        BinaryOperator.of(">=", 30, (t, l, r) -> new RelationImpl(GE, l, r));
        BinaryOperator.of("|", 20, (t, l, r) -> new OrImpl((PredicateImpl<?>) l, (PredicateImpl<?>) r));
        BinaryOperator.of(":=", 10, (t, l, r) -> {
            RuleImpl ruleImpl = new RuleImpl((RelationImpl) l, (PredicateImpl<?>) r);
            KnowledgeBaseImpl.CURRENT.get().addRule(ruleImpl);
            return ruleImpl;
        });
    }

    @Test
    public void tokenizer() {
        String example = """
                    abb + bcc *
                       c - dee //*COMMEND*!@
                    e = 8.9 / 2
                """;
        try {
            LinkedList<Token> tokens = new Tokenizer(example).tokenize();
            assertEquals(16, tokens.size());
        } catch (ParseException e) {
            fail(e);
        }
    }

    @Test
    public void parser() {
        run(() -> {
            setPrettyPrinting(false);
            String example = """
                        a<=b := a<b | a=b
                        a>=b := a>b | a=b
                    """;
            try {
                LinkedList<Token> tokens = new Tokenizer(example).tokenize();
                Parser parser = new Parser(tokens);
                parser.register(TokenType.IDENTIFIER, VAR);
                var structures = parser.parseExpression();
                assertEquals("[rule(le(a,b),or(lt(a,b),eq(a,b))),rule(ge(a,b),or(gt(a,b),eq(a,b)))]", structures.toString().substring(4));
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
