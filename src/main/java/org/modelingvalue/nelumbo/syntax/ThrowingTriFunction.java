package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;

@FunctionalInterface
public interface ThrowingTriFunction<T, U, V, R> {
    R apply(T t, U u, V v) throws ParseException;
}
