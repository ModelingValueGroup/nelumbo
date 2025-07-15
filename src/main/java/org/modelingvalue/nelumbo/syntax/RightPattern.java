package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.nelumbo.Type;

public class RightPattern extends StructImpl {
    private static final long serialVersionUID = 8748858846804198533L;

    public RightPattern(Type right, TokenPattern seperator, TokenPattern end) {
        super(right, seperator, end);
    }

    public final Type right() {
        return (Type) get(0);
    }

    public final TokenPattern seperator() {
        return (TokenPattern) get(1);
    }

    public final TokenPattern end() {
        return (TokenPattern) get(2);
    }

}
