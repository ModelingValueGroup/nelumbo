package org.modelingvalue.nelumbo.integers;

import org.modelingvalue.nelumbo.Functor;
import org.modelingvalue.nelumbo.Terminal;

public class Integer extends Terminal {
    private static final long serialVersionUID = 2454372545442550574L;

    public Integer(Functor functor, Object... args) {
        super(functor, args);
    }

    protected Integer(Object[] array) {
        super(array);
    }

    @Override
    protected Integer struct(Object[] array) {
        return new Integer(array);
    }

    @Override
    public Integer set(int i, Object... a) {
        return (Integer) super.set(i, a);
    }

}
