package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Typed;

public abstract class TypedImpl<S extends Typed<T>, T extends Typed<T>> extends StructureImpl<S> {
    private static final long serialVersionUID = 7991111235846850518L;

    public TypedImpl(Functor<S> functor, Object... args) {
        super(functor, args);
    }

    protected TypedImpl(Object[] array) {
        super(array);
    }

    @Override
    protected abstract TypedImpl<S, T> struct(Object[] array);

    protected TypedImpl<S, T> signature(int depth) {
        Object[] array = signatureArray(depth);
        return array != null ? struct(array) : this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public TypedImpl<S, T> set(int i, Object... a) {
        return (TypedImpl<S, T>) super.set(i, a);
    }

    protected abstract boolean atomic();
}
