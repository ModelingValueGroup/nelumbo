package org.modelingvalue.nelumbo.integers;

import org.modelingvalue.nelumbo.Functor;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.Relation;

public class GreaterThan extends Relation {
    private static final long serialVersionUID = 5338681256251602011L;

    public GreaterThan(Functor fuctor, Object[] args) {
        super(fuctor, args[0], args[1], args[2]);
    }

    private GreaterThan(Object[] array, GreaterThan declaration) {
        super(array, declaration);
    }

    @Override
    protected GreaterThan struct(Object[] array, Predicate declaration) {
        return new GreaterThan(array, (GreaterThan) declaration);
    }

    @Override
    public GreaterThan set(int i, Object... a) {
        return (GreaterThan) super.set(i, a);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        return falsehoodCC();
    }

}
