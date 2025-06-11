package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.regex.Matcher;

import org.modelingvalue.nelumbo.syntax.Token.TokenType;

public class Tokenizer {

    private final String input;

    public Tokenizer(String input) {
        this.input = input;
    }

    public LinkedList<Token> tokenize() throws ParseException {
        LinkedList<Token> tokens = new LinkedList<>();
        TokenType[] tokenTypes = TokenType.values();
        Matcher[] matchers = new Matcher[tokenTypes.length];
        for (int i = 0; i < tokenTypes.length; i++) {
            matchers[i] = tokenTypes[i].pattern().matcher(input);
            if (!matchers[i].find()) {
                matchers[i] = null;
            }
        }
        int pos = 0;
        while (pos < input.length()) {
            String text = null;
            TokenType type = null;
            for (int i = 0; i < tokenTypes.length; i++) {
                while (matchers[i] != null && matchers[i].start() < pos) {
                    if (!matchers[i].find()) {
                        matchers[i] = null;
                    }
                }
                if (matchers[i] != null && matchers[i].start() == pos) {
                    String group = matchers[i].group();
                    if (text == null || text.length() < group.length()) {
                        text = group;
                        type = tokenTypes[i];
                    }
                }
            }
            if (text == null) {
                throw new ParseException("Unexpected character '" + input.charAt(pos) + "' at position " + pos + ".", pos);
            } else {
                if (type != TokenType.H && (type != TokenType.V || tokens.isEmpty() || !tokens.getLast().type().more())) {
                    tokens.add(new Token(type, text, pos));
                }
                pos += text.length();
            }
        }
        return tokens;
    }
}
