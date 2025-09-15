package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.ThrowingBiFunction;

public class Functor extends Node {
    @Serial
    private static final long serialVersionUID = -1901047746034698364L;

    public static Functor of(AbstractPattern pattern, Integer precedence, Type expected, Type result, Constructor<? extends Node> constructor) {
        return new Functor(Type.PATTERN, List.of(), pattern, precedence, expected, result, constructor);
    }

    public static Functor of(AbstractPattern pattern, Integer precedence, Type expected, Type result, ThrowingBiFunction<List<AstElement>, Object[], ? extends Node> function) {
        return new Functor(Type.PATTERN, List.of(), pattern, precedence, expected, result, function);
    }

    public static Functor of(AbstractPattern pattern, Integer precedence, Type expected, Type result) {
        return new Functor(Type.PATTERN, List.of(), pattern, precedence, expected, result, null);
    }

    public Functor(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    private Functor(Object[] args) {
        super(args);
    }

    @Override
    protected Functor struct(Object[] array) {
        return new Functor(array);
    }

    public AbstractPattern pattern() {
        return (AbstractPattern) get(0);
    }

    public Integer precedence() {
        return (Integer) get(1);
    }

    public Type expected() {
        return (Type) get(2);
    }

    public Type resultType() {
        return (Type) get(3);
    }

    @SuppressWarnings("unchecked")
    public Constructor<? extends Node> constructor() {
        Object val = get(4);
        return val instanceof Constructor ? (Constructor<? extends Node>) val : null;
    }

    @SuppressWarnings("unchecked")
    public ThrowingBiFunction<List<AstElement>, Object[], ? extends Node> function() {
        Object val = get(4);
        return val instanceof ThrowingBiFunction ? (ThrowingBiFunction<List<AstElement>, Object[], ? extends Node>) val : null;
    }

    public String name() {
        return (String) get(1);
    }

    @SuppressWarnings("unchecked")
    public List<Type> args() {
        return (List<Type>) get(2);
    }

    @Override
    public String toString() {
        String types = args().map(Type::toString).collect(Collectors.joining(", "));
        return name() + "(" + types + ")";
    }

    private Node construct(List<AstElement> elements, Object[] args) throws ParseException {
        Constructor<? extends Node> constructor = constructor();
        if (constructor != null) {
            try {
                return constructor.newInstance(this, elements, args);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                throw new ParseException(e, "Exception during createNode()", AstElement.firstToken(elements));
            }
        }
        ThrowingBiFunction<List<AstElement>, Object[], ? extends Node> function = function();
        if (function != null) {
            return function.apply(elements, args);
        }
        return resultType() == Type.PREDICATE ? new Predicate(this, elements, args) : new Node(this, elements, args);
    }

    public Patterns patterns() {
        Integer precedence = precedence();
        if (precedence == null) {
            precedence = Integer.MIN_VALUE;
        }
        return pattern().patterns(Patterns.EMPTY.setPattern(this).setPrecedence(precedence), precedence);
    }

    @SuppressWarnings("unchecked")
    public Node postParse(Type expected, Parser parser, ParseResult result) throws ParseException {
        Integer precedence = precedence();
        pattern().parse(expected, precedence != null ? precedence : Integer.MIN_VALUE, parser, null, result);
        return construct(result.elements(), result.args().toArray());
    }

}
