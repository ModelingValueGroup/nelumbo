package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.struct.impl.StructImpl;

public final class TokenPattern extends StructImpl {
    private static final long         serialVersionUID = 5767830087901549875L;

    private static final TokenPattern EMPTY            = new TokenPattern();

    public static TokenPattern of() {
        return EMPTY;
    }

    public static TokenPattern of(String text1) {
        return new TokenPattern(text1);
    }

    public static TokenPattern of(TokenType type1) {
        return new TokenPattern(type1);
    }

    public static TokenPattern of(String text1, TokenType type2) {
        return new TokenPattern(text1, type2);
    }

    public static TokenPattern of(TokenType type1, String text2) {
        return new TokenPattern(type1, text2);
    }

    public static TokenPattern of(String text1, String text2) {
        return new TokenPattern(text1, text2);
    }

    public static TokenPattern of(TokenType type1, TokenType type2) {
        return new TokenPattern(type1, type2);
    }

    public static TokenPattern of(String text1, TokenType type2, String text3) {
        return new TokenPattern(text1, type2, text3);
    }

    public static TokenPattern of(TokenType type1, String text2, String text3) {
        return new TokenPattern(type1, text2, text3);
    }

    public static TokenPattern of(String text1, String text2, String text3) {
        return new TokenPattern(text1, text2, text3);
    }

    public static TokenPattern of(TokenType type1, TokenType type2, String text3) {
        return new TokenPattern(type1, type2, text3);
    }

    public static TokenPattern of(String text1, TokenType type2, TokenType type3) {
        return new TokenPattern(text1, type2, type3);
    }

    public static TokenPattern of(TokenType type1, String text2, TokenType type3) {
        return new TokenPattern(type1, text2, type3);
    }

    public static TokenPattern of(String text1, String text2, TokenType type3) {
        return new TokenPattern(text1, text2, type3);
    }

    public static TokenPattern of(TokenType type1, TokenType type2, TokenType type3) {
        return new TokenPattern(type1, type2, type3);
    }

    public static TokenPattern of(String text1, TokenType type2, String text3, String text4) {
        return new TokenPattern(text1, type2, text3, text4);
    }

    public static TokenPattern of(TokenType type1, String text2, String text3, String text4) {
        return new TokenPattern(type1, text2, text3, text4);
    }

    public static TokenPattern of(String text1, String text2, String text3, String text4) {
        return new TokenPattern(text1, text2, text3, text4);
    }

    public static TokenPattern of(TokenType type1, TokenType type2, String text3, String text4) {
        return new TokenPattern(type1, type2, text3, text4);
    }

    public static TokenPattern of(String text1, TokenType type2, TokenType type3, String text4) {
        return new TokenPattern(text1, type2, type3, text4);
    }

    public static TokenPattern of(TokenType type1, String text2, TokenType type3, String text4) {
        return new TokenPattern(type1, text2, type3, text4);
    }

    public static TokenPattern of(String text1, String text2, TokenType type3, String text4) {
        return new TokenPattern(text1, text2, type3, text4);
    }

    public static TokenPattern of(TokenType type1, TokenType type2, TokenType type3, String text4) {
        return new TokenPattern(type1, type2, type3, text4);
    }

    public static TokenPattern of(String text1, TokenType type2, String text3, TokenType type4) {
        return new TokenPattern(text1, type2, text3, type4);
    }

    public static TokenPattern of(TokenType type1, String text2, String text3, TokenType type4) {
        return new TokenPattern(type1, text2, text3, type4);
    }

    public static TokenPattern of(String text1, String text2, String text3, TokenType type4) {
        return new TokenPattern(text1, text2, text3, type4);
    }

    public static TokenPattern of(TokenType type1, TokenType type2, String text3, TokenType type4) {
        return new TokenPattern(type1, type2, text3, type4);
    }

    public static TokenPattern of(String text1, TokenType type2, TokenType type3, TokenType type4) {
        return new TokenPattern(text1, type2, type3, type4);
    }

    public static TokenPattern of(TokenType type1, String text2, TokenType type3, TokenType type4) {
        return new TokenPattern(type1, text2, type3, type4);
    }

    public static TokenPattern of(String text1, String text2, TokenType type3, TokenType type4) {
        return new TokenPattern(text1, text2, type3, type4);
    }

    public static TokenPattern of(TokenType type1, TokenType type2, TokenType type3, TokenType type4) {
        return new TokenPattern(type1, type2, type3, type4);
    }

    private TokenPattern(Object... tokens) {
        super(tokens);
    }
}
