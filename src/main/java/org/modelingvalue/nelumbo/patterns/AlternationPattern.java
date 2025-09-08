package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;

public class AlternationPattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = -2652813935675033086L;

    public static AlternationPattern of(AbstractPattern... options) {
        return new AlternationPattern(Type.PATTERN, Token.EMPTY, List.of(options));
    }

    public AlternationPattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected AlternationPattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected AlternationPattern struct(Object[] array, int start) {
        return new AlternationPattern(array, start);
    }

    @SuppressWarnings("unchecked")
    public List<AbstractPattern> options() {
        return (List<AbstractPattern>) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        for (AbstractPattern option : options()) {
            if (option.peekIs(parser)) {
                option.parse(expected, precedence, parser, next, result);
                return;
            }
        }
        Token token = parser.peek();
        throw new ParseException("Expected " + this + " but found " + token.text() + " of type " + token.type(), token);
    }

    @Override
    public boolean peekIs(Parser parser) {
        List<AbstractPattern> options = options();
        return options.anyMatch(o -> o.peekIs(parser));
    }

    @Override
    public boolean isFixed() {
        return false;
    }

}
