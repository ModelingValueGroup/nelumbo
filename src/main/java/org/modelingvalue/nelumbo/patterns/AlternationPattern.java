package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;

public class AlternationPattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = -2652813935675033086L;

    public AlternationPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected AlternationPattern(Object[] args) {
        super(args);
    }

    @Override
    protected AlternationPattern struct(Object[] array) {
        return new AlternationPattern(array);
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
