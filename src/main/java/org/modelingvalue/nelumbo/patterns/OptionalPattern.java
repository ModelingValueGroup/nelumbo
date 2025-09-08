package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;

public class OptionalPattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = 3011113311569598643L;

    public static OptionalPattern of(AbstractPattern optional) {
        return new OptionalPattern(Type.PATTERN, Token.EMPTY, optional);
    }

    public OptionalPattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected OptionalPattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected OptionalPattern struct(Object[] array, int start) {
        return new OptionalPattern(array, start);
    }

    public AbstractPattern optional() {
        return (AbstractPattern) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        AbstractPattern optional = optional();
        if (optional.peekIs(parser) || (next != null && !next.peekIs(parser))) {
            optional.parse(expected, precedence, parser, next, result);
        }
    }

    @Override
    public boolean isFixed() {
        return false;
    }

}
