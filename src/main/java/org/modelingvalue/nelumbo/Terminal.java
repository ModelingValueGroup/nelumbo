package org.modelingvalue.nelumbo;

public class Terminal extends Structure {
    private static final long serialVersionUID = 7548506547559092927L;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Terminal(Functor functor, Object... args) {
        super(functor, args);
    }

    private Terminal(Object[] array) {
        super(array);
    }

    @Override
    protected Terminal struct(Object[] array) {
        return new Terminal(array);
    }

    @Override
    public Terminal set(int i, Object... a) {
        return (Terminal) super.set(i, a);
    }

}
