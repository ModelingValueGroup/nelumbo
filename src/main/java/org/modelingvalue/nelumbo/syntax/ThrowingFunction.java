package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;

@FunctionalInterface
public interface ThrowingFunction<T, R> {
    R apply(T t) throws ParseException;
}
