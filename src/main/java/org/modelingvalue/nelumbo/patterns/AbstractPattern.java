package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.TokenType;

public abstract class AbstractPattern extends Node {
    @Serial
    private static final long serialVersionUID = -1788203180486332564L;

    public static AlternationPattern a(AbstractPattern... options) {
        return new AlternationPattern(Type.PATTERN, List.of(), List.of(options));
    }

    public static NodeTypePattern n(Type nodeType) {
        return new NodeTypePattern(Type.PATTERN, List.of(), nodeType);
    }

    public static OptionalPattern o(AbstractPattern optional) {
        return new OptionalPattern(Type.PATTERN, List.of(), optional);
    }

    public static RepetitionPattern r(AbstractPattern repeated) {
        return new RepetitionPattern(Type.PATTERN, List.of(), repeated);
    }

    public static SequencePattern s(AbstractPattern... elements) {
        return new SequencePattern(Type.PATTERN, List.of(), List.of(elements));
    }

    public static TokenTextPattern t(String tokenText) {
        return new TokenTextPattern(Type.PATTERN, List.of(), tokenText);
    }

    public static TokenTypePattern t(TokenType tokenType) {
        return new TokenTypePattern(Type.PATTERN, List.of(), tokenType);
    }

    protected AbstractPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected AbstractPattern(Object[] args) {
        super(args);
    }

    @Override
    protected abstract AbstractPattern struct(Object[] array);

    public abstract void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException;

    public boolean peekIs(Parser parser) {
        return false;
    }

    public Patterns patterns(Patterns patterns, int precedence) {
        return patterns;
    }

    public abstract boolean isFixed();

}
