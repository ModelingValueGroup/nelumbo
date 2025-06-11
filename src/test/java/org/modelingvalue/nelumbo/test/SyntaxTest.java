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

public class SyntaxTest {

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
        BinaryOperator.of(":=", 10, (t, l, r) -> new RuleImpl((RelationImpl) l, (PredicateImpl<?>) r));
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
            //  printTokens(tokens);
            assertEquals(16, tokens.size());
        } catch (ParseException e) {
            fail(e);
        }
    }

    @Test
    public void parser() {
        String example = """
                    a<=b := a<b | a=b
                    a>=b := a>b | a=b
                """;
        try {
            LinkedList<Token> tokens = new Tokenizer(example).tokenize();
            printTokens(tokens);
            Parser parser = new Parser(tokens);
            parser.register(TokenType.IDENTIFIER, VAR);
            var structures = parser.parseExpression();
            assertEquals("[rule(le(a,b),or(lt(a,b),eq(a,b))),rule(ge(a,b),or(gt(a,b),eq(a,b)))]", structures.toString().substring(4));
        } catch (ParseException e) {
            fail(e);
        }
    }

    private static void printTokens(LinkedList<Token> tokens) {
        for (Token token : tokens) {
            System.out.println("Token: " + token);
        }
    }

}
