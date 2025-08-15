package org.modelingvalue.nelumbo;

public class U {
    public static String traceable(String s) {
        return s//
                .replaceAll(" ", "\\\\.")//
                .replaceAll("\n", "\\\\n")//
                .replaceAll("\r", "\\\\r");

    }
}
