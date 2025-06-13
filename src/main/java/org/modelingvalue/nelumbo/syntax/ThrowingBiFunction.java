package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;

@FunctionalInterface
public interface ThrowingBiFunction<T, U, R> {
    R apply(T t, U u) throws ParseException;
}
