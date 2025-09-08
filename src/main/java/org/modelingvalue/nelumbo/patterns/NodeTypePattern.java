package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;

public class NodeTypePattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = 6828401544789430678L;

    public static NodeTypePattern of(Type nodeType) {
        return new NodeTypePattern(Type.PATTERN, Token.EMPTY, nodeType);
    }

    public NodeTypePattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected NodeTypePattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected NodeTypePattern struct(Object[] array, int start) {
        return new NodeTypePattern(array, start);
    }

    public Type nodeType() {
        return (Type) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        if (!result.isDone()) {
            Type type = nodeType();
            Node node = parser.parseNode(precedence, type);
            if (!type.isAssignableFrom(node.type())) {
                throw new ParseException("Expected element of type " + type + " but found " + node + " of type " + node.type(), node.tokens());
            }
            result.add(node);
        }
    }

    @Override
    public Patterns patterns(Patterns patterns, int precedence) {
        return Patterns.EMPTY.put(nodeType(), patterns).setPrecedence(precedence).setExpected(nodeType());
    }

    @Override
    public boolean isFixed() {
        return true;
    }

}
