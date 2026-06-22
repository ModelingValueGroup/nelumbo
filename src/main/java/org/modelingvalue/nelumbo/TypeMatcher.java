package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
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

    public TypeMatcher match(Type type, MutableMap<Variable, Type> typeArgs) {
        for (Type sup : type.allSupersList()) {
            TypeMatcher state = transitions.get(sup);
            if (state != null) {
                return sup.hasArgument() ? state.match(type.argument(), typeArgs) : state;
            }
        }
        return generics(type, typeArgs);
    }

    private TypeMatcher generics(Type type, MutableMap<Variable, Type> typeArgs) {
        for (Entry<Variable, TypeMatcher> e : typeArgs()) {
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

}
