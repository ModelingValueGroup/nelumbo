package org.modelingvalue.nelumbo.syntax;

import java.util.regex.Pattern;

public class Token {

    public enum TokenType {
        COLON(":", true), //
        COMMA(",", true), //
        DOT("\\.", true), //
        LPAREN("\\(", true), //
        RPAREN("\\)", false), //
        STRING("\"[^\"]*\"", false), //
        NUMBER("[0-9]+", false), //
        IDENTIFIER("[a-zA-Z_][a-zA-Z0-9_]*", false), //
        OPERATOR("[:\\=\\-\\*\\+<>/!@#$%^&|]+", true), //
        H("\\h+", false), //
        V("((//[^\\v]*)?\\v)+", false);

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

    private final TokenType type;
    private final String    text;
    private final int       position;

    public Token(TokenType type, String text, int position) {
        this.type = type;
        this.text = text;
        this.position = position;
    }

    @Override
    public String toString() {
        return "'" + text + "' " + type + " @" + position;
    }

    public String text() {
        return text;
    }

    public TokenType type() {
        return type;
    }

    public int position() {
        return position;
    }
}
