package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;

public class SequencePattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = 1477171023667359130L;

    public SequencePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected SequencePattern(Object[] args) {
        super(args);
    }

    @Override
    protected SequencePattern struct(Object[] array) {
        return new SequencePattern(array);
    }

    @SuppressWarnings("unchecked")
    public List<AbstractPattern> elements() {
        return (List<AbstractPattern>) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        List<AbstractPattern> elements = elements();
        for (int i = 0; i < elements.size(); i++) {
            AbstractPattern pattern = elements.get(i);
            pattern.parse(expected, precedence, parser, i + 1 < elements.size() ? elements.get(i + 1) : next, result);
        }
    }

    @Override
    public boolean peekIs(Parser parser) {
        List<AbstractPattern> elements = elements();
        return !elements.isEmpty() && elements.first().peekIs(parser);
    }

    @Override
    public Patterns patterns(Patterns patterns, int precedence) {
        List<AbstractPattern> elements = elements();
        int max = elements.size() - 1;
        for (int i = 0; i <= max; i++) {
            if (!elements.get(i).isFixed()) {
                max = i;
                break;
            }
        }
        for (int i = max; i >= 0; i--) {
            patterns = elements.get(i).patterns(patterns, precedence);
        }
        return patterns;
    }

    @Override
    public boolean isFixed() {
        return true;
    }

}
