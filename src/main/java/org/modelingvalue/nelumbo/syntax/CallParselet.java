package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.impl.StructureImpl;
import org.modelingvalue.nelumbo.syntax.Token.TokenType;

public class CallParselet extends Prefix2Parselet {

    public final static CallParselet            INSTANCE  = new CallParselet();

    private final Map<String, FunctionWithArgs> functions = new HashMap<>();

    private CallParselet() {
    }

    @Override
    public StructureImpl<?> parse(Parser parser, Token token1, Token token2) throws ParseException {
        FunctionWithArgs function = getFunction(token1);
        List<StructureImpl<?>> args = List.of();
        for (int i = 0; i < function.nrOfArgs(); i++) {
            args = args.add(parser.parseExpression(0));
            if (i < function.nrOfArgs() - 1) {
                parser.consume(TokenType.COMMA);
            }
        }
        parser.consume(TokenType.RPAREN);
        return function.construct(token1, args);
    }

    private FunctionWithArgs getFunction(Token token) throws ParseException {
        FunctionWithArgs function = functions.get(token.text());
        if (function == null) {
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        return function;
    }

    public static void register(FunctionWithArgs function) {
        INSTANCE.functions.put(function.text(), function);
    }

}
