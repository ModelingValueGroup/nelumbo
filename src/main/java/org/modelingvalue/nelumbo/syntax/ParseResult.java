package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class ParseResult {

    private final MutableList<AstElement> elements;
    private final MutableList<Object>  args;

    private Functor                    pattern;
    private int                        pre  = 0;
    private int                        post = 0;

    public ParseResult() {
        elements = MutableList.of(List.of());
        args = MutableList.of(List.of());
    }

    public Functor pattern() {
        return pattern;
    }

    public int precedence() {
        Integer precedence = pattern.precedence();
        return precedence != null ? precedence : Integer.MAX_VALUE;
    }

    public void setPattern(Functor pattern) {
        this.pattern = pattern;
    }

    public List<AstElement> elements() {
        return elements.toImmutable();
    }

    public void add(Node node) {
        elements.add(node);
        args.add(node);
        if (pattern == null) {
            pre++;
        }
    }

    public void add(Token token) {
        elements.add(token);
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
