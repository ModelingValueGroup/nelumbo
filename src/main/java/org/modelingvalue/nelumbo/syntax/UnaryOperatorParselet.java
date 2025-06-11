package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.modelingvalue.nelumbo.impl.StructureImpl;

public final class UnaryOperatorParselet extends PrefixParselet {

    public final static UnaryOperatorParselet INSTANCE       = new UnaryOperatorParselet();

    private final Map<String, UnaryOperator>  unaryOperators = new HashMap<>();

    private UnaryOperatorParselet() {
    }

    @Override
    public StructureImpl<?> parse(Parser parser, Token token) throws ParseException {
        UnaryOperator unaryOperator = unaryOperators.get(token.text());
        StructureImpl<?> right = parser.parseExpression(unaryOperator.precedence());
        return unaryOperator.construct(token, right);
    }

    public static void register(UnaryOperator operator) {
        INSTANCE.unaryOperators.put(operator.text(), operator);
    }

}
