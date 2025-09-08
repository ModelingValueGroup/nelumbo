package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;

public class RepetitionPattern extends AbstractPattern {
    @Serial
    private static final long           serialVersionUID = 7257418785045060245L;

    public static final AbstractPattern PATTERN          = SequencePattern.of(TokenTextPattern.of("`{`"), SequencePattern.PATTERN, TokenTextPattern.of("`}`"));

    public static RepetitionPattern of(AbstractPattern repeated) {
        return new RepetitionPattern(Type.PATTERN, Token.EMPTY, repeated);
    }

    public RepetitionPattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected RepetitionPattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected RepetitionPattern struct(Object[] array, int start) {
        return new RepetitionPattern(array, start);
    }

    public AbstractPattern repeated() {
        return (AbstractPattern) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        AbstractPattern repeated = repeated();
        while (repeated.peekIs(parser)) {
            repeated.parse(expected, precedence, parser, next, result);
        }
    }

    @Override
    public boolean isFixed() {
        return false;
    }

}
