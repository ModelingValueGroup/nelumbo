package org.modelingvalue.nelumbo.syntax;

public abstract class Parselet {

    private final TokenType type1;
    private final String    oper1;
    private final TokenType type2;
    private final String    oper2;

    protected Parselet(TokenType type1, String oper1, TokenType type2, String oper2) {
        this.type1 = type1;
        this.oper1 = oper1;
        this.type2 = type2;
        this.oper2 = oper2;
    }

    public TokenType type1() {
        return type1;
    }

    public String oper1() {
        return oper1;
    }

    public TokenType type2() {
        return type2;
    }

    public String oper2() {
        return oper2;
    }

    public Object key1() {
        return oper1 != null ? oper1 : type1;
    }

    public Object key2() {
        return oper2 != null ? oper2 : type2;
    }

}
