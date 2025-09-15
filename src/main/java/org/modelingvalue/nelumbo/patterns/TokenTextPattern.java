package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;

public class TokenTextPattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = -7116490422223451839L;

    public TokenTextPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected TokenTextPattern(Object[] args) {
        super(args);
    }

    @Override
    protected TokenTextPattern struct(Object[] array) {
        return new TokenTextPattern(array);
    }

    public String tokenText() {
        return (String) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        if (!result.isDone()) {
            Token token = parser.consume(tokenText());
            result.add(token);
        }
    }

    @Override
    public boolean peekIs(Parser parser) {
        return parser.peekIs(tokenText());
    }

    @Override
    public Patterns patterns(Patterns patterns, int precedence) {
        return Patterns.EMPTY.put(tokenText(), patterns);
    }

    @Override
    public boolean isFixed() {
        return true;
    }

}
