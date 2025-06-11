package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;
import java.util.function.Function;

import org.modelingvalue.nelumbo.impl.StructureImpl;

public abstract class IdentifierParselet extends PrefixParselet {

    private IdentifierParselet() {
    }

    @Override
    public StructureImpl<?> parse(Parser parser, Token token) throws ParseException {
        return construct(token);
    }

    public abstract StructureImpl<?> construct(Token token);

    public static IdentifierParselet of(Function<Token, StructureImpl<?>> constructor) {
        return new IdentifierParselet() {
            @Override
            public StructureImpl<?> construct(Token token) {
                return constructor.apply(token);
            }
        };
    }

}
