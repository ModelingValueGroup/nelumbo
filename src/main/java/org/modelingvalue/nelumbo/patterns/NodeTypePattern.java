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

public class NodeTypePattern extends AbstractPattern {
    @Serial
    private static final long serialVersionUID = 6828401544789430678L;

    public NodeTypePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected NodeTypePattern(Object[] args) {
        super(args);
    }

    @Override
    protected NodeTypePattern struct(Object[] array) {
        return new NodeTypePattern(array);
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
                throw new ParseException("Expected element of type " + type + " but found " + node + " of type " + node.type(), node);
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
