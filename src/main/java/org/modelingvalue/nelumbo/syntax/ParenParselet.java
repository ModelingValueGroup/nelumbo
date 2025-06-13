package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;

import org.modelingvalue.nelumbo.impl.StructureImpl;
import org.modelingvalue.nelumbo.syntax.Token.TokenType;

public class ParenParselet extends Prefix1Parselet {

    public final static ParenParselet INSTANCE = new ParenParselet();

    private ParenParselet() {
    }

    @Override
    public StructureImpl<?> parse(Parser parser, Token token) throws ParseException {
        StructureImpl<?> expression = parser.parseExpression(0);
        parser.consume(TokenType.RPAREN);
        return expression;
    }

}
