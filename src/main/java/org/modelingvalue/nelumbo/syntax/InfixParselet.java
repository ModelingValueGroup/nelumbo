package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;

import org.modelingvalue.nelumbo.impl.StructureImpl;

public abstract class InfixParselet {

    public abstract int precedence(Token token) throws ParseException;

    public abstract StructureImpl<?> parse(Parser parser, StructureImpl<?> left, Token token) throws ParseException;

}
