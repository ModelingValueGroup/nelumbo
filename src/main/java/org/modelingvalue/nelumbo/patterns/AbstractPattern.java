package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;

public abstract class AbstractPattern extends Node {
    @Serial
    private static final long serialVersionUID = -1788203180486332564L;

    protected AbstractPattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected AbstractPattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected abstract AbstractPattern struct(Object[] array, int start);

    public abstract void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException;

    public boolean peekIs(Parser parser) {
        return false;
    }

    public Patterns patterns(Patterns patterns, int precedence) {
        return patterns;
    }

    public abstract boolean isFixed();

}
