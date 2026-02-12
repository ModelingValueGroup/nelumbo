package org.modelingvalue.nelumbo.logic;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;

public class BooleanVariable extends Predicate {

    @Serial
    private static final long serialVersionUID = 5130317339420169185L;

    private InferResult       result;

    public BooleanVariable(Functor functor, List<AstElement> elements, Variable var) {
        super(functor, elements, var);
    }

    @Override
    public List<Object> args() {
        return List.of(variable());
    }

    private BooleanVariable(Object[] args, BooleanVariable declaration) {
        super(args, declaration);
    }

    @Override
    protected BooleanVariable struct(Object[] array, Node declaration) {
        return new BooleanVariable(array, (BooleanVariable) declaration);
    }

    @Override
    public BooleanVariable set(int i, Object... a) {
        return (BooleanVariable) super.set(i, a);
    }

    @Override
    public BooleanVariable setAstElements(List<AstElement> elements) {
        return (BooleanVariable) super.setAstElements(elements);
    }

    @Override
    protected BooleanVariable setBinding(Node declaration, Map<Variable, Object> vars) {
        Variable var = variable();
        if (var != null && vars.get(var) instanceof Type t && !t.equals(functor().resultType())) {
            return (BooleanVariable) super.setBinding(declaration, vars).setFunctor(functor().setResultType(t));
        }
        if (var != null && vars.get(var) instanceof Variable v && !v.type().equals(functor().resultType())) {
            return (BooleanVariable) super.setBinding(declaration, vars).setFunctor(functor().setResultType(v.type()));
        }
        return (BooleanVariable) super.setBinding(declaration, vars);
    }

    @Override
    public InferResult resolve(InferContext context) {
        if (get(0) instanceof Predicate pred) {
            return pred.resolve(context);
        }
        return infer(context);
    }

    @Override
    protected InferResult infer(InferContext context) {
        if (get(0) instanceof Predicate pred) {
            return pred.infer(context);
        }
        if (context != null && context.shallow()) {
            return unresolvable();
        }
        if (result == null) {
            result = InferResult.of(declaration(), Set.of(set(0, NBoolean.TRUE)), true, //
                    Set.of(set(0, NBoolean.FALSE)), true, Set.of());
        }
        return result;
    }

    @Override
    public Variable variable() {
        if (get(0) instanceof Variable var) {
            return var;
        }
        return null;
    }

}
