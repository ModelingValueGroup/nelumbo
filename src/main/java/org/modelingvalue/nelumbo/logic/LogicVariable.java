package org.modelingvalue.nelumbo.logic;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class LogicVariable extends Predicate {

    @Serial
    private static final long serialVersionUID = 5130317339420169185L;

    private InferResult       result;

    public LogicVariable(Variable var) {
        super(var.type(), List.of(var), var);
    }

    @Override
    public List<Object> args() {
        return List.of(variable());
    }

    private LogicVariable(Object[] args, LogicVariable declaration) {
        super(args, declaration);
    }

    @Override
    protected LogicVariable struct(Object[] array, Node declaration) {
        return new LogicVariable(array, (LogicVariable) declaration);
    }

    @Override
    public LogicVariable set(int i, Object... a) {
        return (LogicVariable) super.set(i, a);
    }

    @Override
    public LogicVariable setAstElements(List<AstElement> elements) {
        return (LogicVariable) super.setAstElements(elements);
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
            result = InferResult.of(this, Set.of(set(0, Boolean.TRUE)), true, //
                    Set.of(set(0, Boolean.FALSE)), true, Set.of());
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

    @Override
    public String toString(TokenType[] previous) {
        Node node = (Node) get(0);
        if (node instanceof Variable var) {
            String string = var.name();
            if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER || previous[0] == TokenType.DECIMAL) {
                previous[0] = TokenType.NAME;
                return " " + string;
            }
            previous[0] = TokenType.NAME;
            return string;
        } else {
            return node.toString(previous);
        }
    }

}
