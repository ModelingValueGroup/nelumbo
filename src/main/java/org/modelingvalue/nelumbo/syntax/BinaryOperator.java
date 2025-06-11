package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.util.TriFunction;
import org.modelingvalue.nelumbo.impl.StructureImpl;

public abstract class BinaryOperator {

    private final String text;
    private final int    precedence;

    public BinaryOperator(String text, int precedence) {
        this.text = text;
        this.precedence = precedence;
        BinaryOperatorParselet.register(this);
    }

    public String text() {
        return text;
    }

    public int precedence() {
        return precedence;
    }

    public abstract StructureImpl<?> construct(Token token, StructureImpl<?> left, StructureImpl<?> right);

    public static BinaryOperator of(String text, int precedence, TriFunction<Token, StructureImpl<?>, StructureImpl<?>, StructureImpl<?>> constructor) {
        return new BinaryOperator(text, precedence) {
            @Override
            public StructureImpl<?> construct(Token token, StructureImpl<?> left, StructureImpl<?> right) {
                return constructor.apply(token, left, right);
            }
        };
    }

}
