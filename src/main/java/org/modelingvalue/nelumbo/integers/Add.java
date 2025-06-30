package org.modelingvalue.nelumbo.integers;

import org.modelingvalue.nelumbo.Functor;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.Relation;

public class Add extends Relation {
    private static final long serialVersionUID = 2384355866476367685L;

    public Add(Functor fuctor, Object[] args) {
        super(fuctor, args[0], args[1], args[2]);
    }

    private Add(Object[] array, Add declaration) {
        super(array, declaration);
    }

    @Override
    protected Add struct(Object[] array, Predicate declaration) {
        return new Add(array, (Add) declaration);
    }

    @Override
    public Add set(int i, Object... a) {
        return (Add) super.set(i, a);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        return falsehoodCC();
    }

}
