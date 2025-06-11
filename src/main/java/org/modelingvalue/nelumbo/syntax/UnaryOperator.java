package org.modelingvalue.nelumbo.syntax;

import java.util.function.BiFunction;

import org.modelingvalue.nelumbo.impl.StructureImpl;

public abstract class UnaryOperator {

    private final String text;
    private final int    precedence;

    public UnaryOperator(String text, int precedence) {
        this.text = text;
        this.precedence = precedence;
        UnaryOperatorParselet.register(this);
    }

    public String text() {
        return text;
    }

    public int precedence() {
        return precedence;
    }

    public abstract StructureImpl<?> construct(Token token, StructureImpl<?> right);

    public static UnaryOperator of(String text, int precedence, BiFunction<Token, StructureImpl<?>, StructureImpl<?>> constructor) {
        return new UnaryOperator(text, precedence) {
            @Override
            public StructureImpl<?> construct(Token token, StructureImpl<?> right) {
                return constructor.apply(token, right);
            }
        };
    }

}
