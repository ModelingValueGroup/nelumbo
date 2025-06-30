package org.modelingvalue.nelumbo.integers;

import org.modelingvalue.nelumbo.Functor;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.Relation;

public class Multiply extends Relation {
    private static final long serialVersionUID = 2630128775301942610L;

    public Multiply(Functor fuctor, Object[] args) {
        super(fuctor, args[0], args[1], args[2]);
    }

    private Multiply(Object[] array, Multiply declaration) {
        super(array, declaration);
    }

    @Override
    protected Multiply struct(Object[] array, Predicate declaration) {
        return new Multiply(array, (Multiply) declaration);
    }

    @Override
    public Multiply set(int i, Object... a) {
        return (Multiply) super.set(i, a);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        return falsehoodCC();
    }

}
