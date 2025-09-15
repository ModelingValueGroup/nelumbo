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
import org.modelingvalue.nelumbo.syntax.TokenType;

public class TokenTypePattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = 2405616043878166113L;

    public TokenTypePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected TokenTypePattern(Object[] args) {
        super(args);
    }

    @Override
    protected TokenTypePattern struct(Object[] array) {
        return new TokenTypePattern(array);
    }

    public TokenType tokenType() {
        return (TokenType) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        if (!result.isDone()) {
            TokenType type = tokenType();
            Token token = parser.consume(type);
            result.add(token);
            if (type.variable()) {
                result.add(token.text());
            }
        }
    }

    @Override
    public boolean peekIs(Parser parser) {
        return parser.peekIs(tokenType());
    }

    @Override
    public Patterns patterns(Patterns patterns, int precedence) {
        return Patterns.EMPTY.put(tokenType(), patterns);
    }

    @Override
    public boolean isFixed() {
        return true;
    }

}
