package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;

import org.modelingvalue.nelumbo.impl.StructureImpl;

public abstract class PrefixParselet {

    public abstract StructureImpl<?> parse(Parser parser, Token token) throws ParseException;

}
