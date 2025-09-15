package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;

public class RepetitionPattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = 7257418785045060245L;

    public RepetitionPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected RepetitionPattern(Object[] args) {
        super(args);
    }

    @Override
    protected RepetitionPattern struct(Object[] array) {
        return new RepetitionPattern(array);
    }

    public AbstractPattern repeated() {
        return (AbstractPattern) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        AbstractPattern repeated = repeated();
        while (repeated.peekIs(parser) || (next != null && !next.peekIs(parser))) {
            repeated.parse(expected, precedence, parser, next, result);
        }
    }

    @Override
    public boolean isFixed() {
        return false;
    }

}
