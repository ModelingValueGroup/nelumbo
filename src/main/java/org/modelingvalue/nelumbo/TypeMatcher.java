package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.util.NotMergeableException;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;

public class TypeMatcher {

    public static final TypeMatcher EMPTY = new TypeMatcher(Map.of(), null);

    private final Map<Type, TypeMatcher> transitions;
    private final Type                   type;

    private Map<Variable, TypeMatcher> typeArgs = null;

    public TypeMatcher(Map<Type, TypeMatcher> transitions, Type type) {
        this.transitions = transitions;
        this.type = type;
    }

    public Type type() {
        return type;
    }

    private Map<Variable, TypeMatcher> typeArgs() {
        if (typeArgs == null) {
            Map<Variable, TypeMatcher> map = Map.of();
            for (Entry<Type, TypeMatcher> e : transitions) {
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

    public Set<Type> match(Type type, MutableMap<Variable, Type> typeArgs) {
        Set<Type> result = Set.of();
        for (Type sup : type.allSupersList()) {
            TypeMatcher state = transitions.get(sup);
            if (state != null) {
                result = sup.hasArgument() ? result.addAll(state.match(type.argument(), typeArgs))
                        : result.add(state.type);
                break;
            }
        }
        if (!type.hasArgument() && (result.isEmpty() || type.variable() == null)) {
            for (Entry<Variable, TypeMatcher> e : typeArgs()) {
                Type found = typeArgs.get(e.getKey());
                if (found == null) {
                    typeArgs.put(e.getKey(), type);
                    result = result.add(e.getValue().type);
                } else {
                    found = type.common(found);
                    if (found != null) {
                        typeArgs.put(e.getKey(), found);
                        result = result.add(e.getValue().type);
                    } else {
                        typeArgs.put(e.getKey(), Type.$NONE);
                    }
                }
            }
        }
        return result;
    }

    public TypeMatcher merge(TypeMatcher merged) {
        return new TypeMatcher(transitions.addAll(merged.transitions, TypeMatcher::merge),
                elementMerge(type, merged.type));
    }

    private static <T> T elementMerge(T t1, T t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            throw new NotMergeableException("Non deterministic pattern merge " + t1 + " <> " + t2);
        }
        return t1 == null ? t2 : t1;
    }

    @Override
    public String toString() {
        return transitions.toKeys().asSet().toString().substring(3);
    }

}
