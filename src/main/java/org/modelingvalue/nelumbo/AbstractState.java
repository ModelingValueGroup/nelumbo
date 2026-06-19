package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;

@SuppressWarnings("rawtypes")
public abstract class AbstractState<S extends AbstractState> {

    private Map<Variable, S> typeArgs = null;

    public Map<Variable, S> typeArgs() {
        if (typeArgs == null) {
            Map<Variable, S> map = Map.of();
            for (Entry<Object, S> e : typeTransitions()) {
                if (e.getKey() instanceof Type t) {
                    Variable arg = t.variable();
                    if (arg != null) {
                        map = map.put(arg, e.getValue());
                    }
                }
            }
            typeArgs = map;
        }
        return typeArgs;
    }

    protected abstract Map<Object, S> typeTransitions();

    @SuppressWarnings("unchecked")
    public S matchType(Type type, MutableMap<Variable, Type> typeArgs) {
        for (Type sup : type.allSupersList()) {
            S state = typeTransitions().get(sup);
            if (state != null) {
                return sup.hasArgument() ? (S) state.matchType(type.argument(), typeArgs) : state;
            }
        }
        return generics(type, typeArgs);
    }

    private S generics(Type type, MutableMap<Variable, Type> typeArgs) {
        for (Entry<Variable, S> e : typeArgs()) {
            Type found = typeArgs.get(e.getKey());
            if (found == null) {
                typeArgs.put(e.getKey(), type);
                return e.getValue();
            }
            found = type.common(found);
            if (found != null) {
                typeArgs.put(e.getKey(), found);
                return e.getValue();
            }
        }
        return null;
    }

}
