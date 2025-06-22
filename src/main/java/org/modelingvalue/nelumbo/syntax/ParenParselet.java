package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;

import org.modelingvalue.nelumbo.Structure;
import org.modelingvalue.nelumbo.syntax.Token.TokenType;

public class ParenParselet extends Prefix1Parselet {

    public final static ParenParselet INSTANCE = new ParenParselet();

    private ParenParselet() {
    }

    @Override
    public Structure parse(Parser parser, Token token) throws ParseException {
        Structure expression = parser.parseExpression(0);
        parser.consume(TokenType.RPAREN);
        return expression;
    }

}
