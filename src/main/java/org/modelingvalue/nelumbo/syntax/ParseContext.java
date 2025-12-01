package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.Set;

public interface ParseContext {

    ParseState state();

    int precedence();

    String group();

    ParseContext outer();

    static ParseContext of(ParseState state, String group, int precedence, ParseContext outer) {
        return new ParseContext() {

            @Override
            public ParseState state() {
                return state;
            }

            @Override
            public int precedence() {
                return precedence;
            }

            @Override
            public String group() {
                return group;
            }

            @Override
            public ParseContext outer() {
                return outer;
            }

            @Override
            public String toString() {
                return (state == null ? "" : state + " ") + precedence + " " + group + (outer == null ? "" : " " + outer);
            }

        };
    }

    default Set<ParseState> outerStates() {
        Set result = Set.of();

        return result;
    }

}
