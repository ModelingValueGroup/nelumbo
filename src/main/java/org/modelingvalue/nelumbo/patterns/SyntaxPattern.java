package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;

import org.modelingvalue.nelumbo.Functor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;

public class SyntaxPattern extends Node {
    @Serial
    private static final long         serialVersionUID = -1901047746034698364L;

    public static final SyntaxPattern PATTERN          = of(RepetitionPattern.of(SequencePattern.PATTERN), null, null /* ,Type.PATTERN */, Type.PATTERN);

    public static SyntaxPattern of(AbstractPattern pattern, Integer precedence, Type expected, Functor functor, Constructor<? extends Node> constructor) {
        return new SyntaxPattern(Type.PATTERN, Token.EMPTY, pattern, precedence, expected, functor, constructor);
    }

    public static SyntaxPattern of(AbstractPattern pattern, Integer precedence, Type expected, Type result, Constructor<? extends Node> constructor) {
        return new SyntaxPattern(Type.PATTERN, Token.EMPTY, pattern, precedence, expected, result, constructor);
    }

    public static SyntaxPattern of(AbstractPattern pattern, Integer precedence, Type expected, Functor functor, BiFunction<Token[], Object[], ? extends Node> function) {
        return new SyntaxPattern(Type.PATTERN, Token.EMPTY, pattern, precedence, expected, functor, function);
    }

    public static SyntaxPattern of(AbstractPattern pattern, Integer precedence, Type expected, Type result, BiFunction<Token[], Object[], ? extends Node> function) {
        return new SyntaxPattern(Type.PATTERN, Token.EMPTY, pattern, precedence, expected, result, function);
    }

    public static SyntaxPattern of(AbstractPattern pattern, Integer precedence, Type expected, Functor functor) {
        return new SyntaxPattern(Type.PATTERN, Token.EMPTY, pattern, precedence, expected, functor, null);
    }

    public static SyntaxPattern of(AbstractPattern pattern, Integer precedence, Type expected, Type result) {
        return new SyntaxPattern(Type.PATTERN, Token.EMPTY, pattern, precedence, expected, result, null);
    }

    public SyntaxPattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    private SyntaxPattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected SyntaxPattern struct(Object[] array, int start) {
        return new SyntaxPattern(array, start);
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

    public Functor resultFunctor() {
        Object val = get(3);
        return val instanceof Functor ? (Functor) val : null;
    }

    public Type resultType() {
        Object val = get(3);
        return val instanceof Type ? (Type) val : null;
    }

    @SuppressWarnings("unchecked")
    public Constructor<? extends Node> constructor() {
        Object val = get(4);
        return val instanceof Constructor ? (Constructor<? extends Node>) val : null;
    }

    @SuppressWarnings("unchecked")
    public BiFunction<Token[], Object[], ? extends Node> function() {
        Object val = get(4);
        return val instanceof BiFunction ? (BiFunction<Token[], Object[], ? extends Node>) val : null;
    }

    private Node construct(Token[] tokens, Object[] args) throws ParseException {
        Functor functor = resultFunctor();
        Type type = resultType();
        Constructor<? extends Node> constructor = constructor();
        if (constructor != null) {
            try {
                return constructor.newInstance(functor != null ? functor : type, tokens, args);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                throw new ParseException(e, "Exception during createNode()", tokens);
            }
        }
        BiFunction<Token[], Object[], ? extends Node> function = function();
        if (function != null) {
            return function.apply(tokens, args);
        }
        if (functor != null) {
            return functor.resultType() == Type.PREDICATE ? new Predicate(functor, tokens, args) : new Node(functor, tokens, args);
        } else {
            return type == Type.PREDICATE ? new Predicate(type, tokens, args) : new Node(type, tokens, args);
        }
    }

    public Patterns patterns() {
        Integer precedence = precedence();
        return pattern().patterns(Patterns.EMPTY.setPattern(this), precedence != null ? precedence : Integer.MIN_VALUE);
    }

    @SuppressWarnings("unchecked")
    public Node postParse(Type expected, Parser parser, ParseResult result) throws ParseException {
        Integer precedence = precedence();
        pattern().parse(expected, precedence != null ? precedence : Integer.MIN_VALUE, parser, null, result);
        return construct(result.tokens().toArray(i -> new Token[i]), result.args().toArray());
    }

}
