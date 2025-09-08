package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.SyntaxPattern;

public final class ParseResult {

    private final MutableList<Token>  tokens;
    private final MutableList<Object> args;

    private SyntaxPattern             pattern;
    private int                       pre  = 0;
    private int                       post = 0;

    public ParseResult() {
        tokens = MutableList.of(List.of());
        args = MutableList.of(List.of());
    }

    public SyntaxPattern pattern() {
        return pattern;
    }

    public int precedence() {
        Integer precedence = pattern.precedence();
        return precedence != null ? precedence : Integer.MAX_VALUE;
    }

    public void setPattern(SyntaxPattern pattern) {
        this.pattern = pattern;
    }

    public List<Token> tokens() {
        return tokens.toImmutable();
    }

    public void add(Node node) {
        args.add(node);
        if (pattern == null) {
            pre++;
        }
    }

    public void add(Token token) {
        tokens.add(token);
        if (pattern == null) {
            pre++;
        }
    }

    public void add(String val) {
        args.add(val);
    }

    public List<Object> args() {
        return args.toImmutable();
    }

    public Node postParse(Type expected, Parser parser) throws ParseException {
        return pattern.postParse(expected, parser, this);
    }

    public boolean isDone() {
        return post++ < pre;
    }

}
