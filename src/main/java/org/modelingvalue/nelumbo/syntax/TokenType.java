package org.modelingvalue.nelumbo.syntax;

import java.util.regex.Pattern;

public enum TokenType {
    COMMA(",", true), //
    LPAREN("\\(", true), //
    RPAREN("\\)", false), //
    LBRACKET("\\[", true), //
    RBRACKET("\\]", false), //
    LBRACE("\\{", true), //
    RBRACE("\\}", false), //
    STRING("\"([^\"\\\\]|\\\\[\\s\\S])*\"", false), //
    NUMBER("[1-9][0-9]*", false), //
    DECIMAL("[1-9][0-9]*\\.[0-9]+", false), //
    QNAME("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+", false), //
    IDENTIFIER("[a-zA-Z_][a-zA-Z0-9_]*", false), //
    IDENTIFIERDCL("[a-zA-Z_][a-zA-Z0-9_]*+\\([1-9][0-9]*\\)", false), //
    TYPE("<[a-zA-Z_][a-zA-Z0-9_]*([\\*|\\+])?>", false), //
    OPERATOR("[:\\=\\-\\*\\+<>/!@#$%^&|~]+", true), //
    OPERATORDCL("[:\\=\\-\\*\\+<>/!@#$%^&|~]+\\([1-9][0-9]*\\)", true), //
    HSPACE("\\h+", false), //
    NEWLINE("((//[^\\v]*)?\\v)+", false);

    private final Pattern pattern;
    private final boolean more;

    private TokenType(String regexp, boolean more) {
        this.pattern = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        this.more = more;
    }

    public boolean more() {
        return more;
    }

    public Pattern pattern() {
        return pattern;
    }
}
