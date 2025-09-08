package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

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

    public static TokenTypePattern of(TokenType tokenType) {
        return new TokenTypePattern(Type.PATTERN, Token.EMPTY, tokenType);
    }

    public TokenTypePattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected TokenTypePattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected TokenTypePattern struct(Object[] array, int start) {
        return new TokenTypePattern(array, start);
    }

    public TokenType tokenType() {
        return (TokenType) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        if (!result.isDone()) {
            Token token = parser.consume(tokenType());
            result.add(token);
            result.add(token.text());
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
