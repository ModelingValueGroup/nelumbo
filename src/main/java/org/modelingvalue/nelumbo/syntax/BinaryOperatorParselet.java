package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.modelingvalue.nelumbo.impl.StructureImpl;

public final class BinaryOperatorParselet extends InfixParselet {

    public final static BinaryOperatorParselet INSTANCE        = new BinaryOperatorParselet();

    private final Map<String, BinaryOperator>  binaryOperators = new HashMap<>();

    private BinaryOperatorParselet() {
    }

    @Override
    public StructureImpl<?> parse(Parser parser, StructureImpl<?> left, Token token) throws ParseException {
        BinaryOperator binaryOperator = binaryOperators.get(token.text());
        StructureImpl<?> right = parser.parseExpression(binaryOperator.precedence());
        return binaryOperator.construct(token, left, right);
    }

    @Override
    public int precedence(Token token) throws ParseException {
        BinaryOperator binaryOperator = binaryOperators.get(token.text());
        if (binaryOperator == null) {
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        return binaryOperator.precedence();
    }

    public static void register(BinaryOperator operator) {
        INSTANCE.binaryOperators.put(operator.text(), operator);
    }

}
