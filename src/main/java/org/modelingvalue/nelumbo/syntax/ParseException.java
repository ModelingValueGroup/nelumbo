package org.modelingvalue.nelumbo.syntax;

public class ParseException extends Exception {
    private static final long serialVersionUID = -8359192414582977261L;

    private final int         line;
    private final int         position;
    private final int         index;
    private final String      text;

    public ParseException(String s, Token token) {
        this(s, token.line(), token.position(), token.index(), token.text());
    }

    public ParseException(String s, int line, int position, int index, String text) {
        super(s + ", line=" + line + ", position=" + position);
        this.line = line;
        this.position = position;
        this.index = index;
        this.text = text;
    }

    public int line() {
        return line;
    }

    public int position() {
        return position;
    }

    public int index() {
        return index;
    }

    public String text() {
        return text;
    }

}
