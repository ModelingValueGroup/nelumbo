package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;

public class TokenTextPattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = -7116490422223451839L;

    public static TokenTextPattern of(String tokenText) {
        return new TokenTextPattern(Type.PATTERN, Token.EMPTY, tokenText);
    }

    public TokenTextPattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected TokenTextPattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected TokenTextPattern struct(Object[] array, int start) {
        return new TokenTextPattern(array, start);
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
