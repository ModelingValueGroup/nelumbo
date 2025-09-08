package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class SequencePattern extends AbstractPattern {
    @Serial
    private static final long                   serialVersionUID    = 1477171023667359130L;

    private static final List<TokenTypePattern> TOKEN_TYPE_PATTERNS = List.of(TokenType.values()).exclude(TokenType::skip).map(TokenTypePattern::of).asList();

    private static final List<AbstractPattern>  ALL_PATTERNS        = List.<AbstractPattern> of(RepetitionPattern.PATTERN).addAll(TOKEN_TYPE_PATTERNS);

    public static final AbstractPattern         PATTERN             = RepetitionPattern.of(AlternationPattern.of(ALL_PATTERNS.toArray(i -> new AbstractPattern[i])));

    public static SequencePattern of(AbstractPattern... elements) {
        return new SequencePattern(Type.PATTERN, Token.EMPTY, List.of(elements));
    }

    public SequencePattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected SequencePattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected SequencePattern struct(Object[] array, int start) {
        return new SequencePattern(array, start);
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
        int max = elements.size();
        for (int i = 0; i < max; i++) {
            if (!elements.get(i).isFixed()) {
                max = i;
                break;
            }
        }
        for (int i = max; i > 0; i--) {
            patterns = elements.get(i).patterns(patterns, precedence);
        }
        return patterns;
    }

    @Override
    public boolean isFixed() {
        return true;
    }

}
