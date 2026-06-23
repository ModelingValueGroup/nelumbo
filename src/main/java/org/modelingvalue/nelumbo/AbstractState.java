package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.util.Mergeable;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;

@SuppressWarnings("rawtypes")
public abstract class AbstractState<S extends AbstractState> implements Mergeable<S> {

    private final TypeMatcher typeMatcher;

    protected AbstractState(TypeMatcher typeMatcher) {
        this.typeMatcher = typeMatcher;
    }

    protected final TypeMatcher typeMatcher() {
        return typeMatcher;
    }

    @SuppressWarnings("unchecked")
    public S matchType(Type type, MutableMap<Variable, Type> typeArgs) {
        S s = null;
        for (Type m : typeMatcher().match(type, typeArgs)) {
            if (s == null) {
                s = typeTransitions().get(m);
            } else {
                s = (S) s.merge(typeTransitions().get(m));
            }
        }
        return s;
    }

    protected abstract Map<Object, S> typeTransitions();

    @SuppressWarnings("unchecked")
    protected <K> Map<K, S> inherit(Map<K, S> transitions) {
        for (Object key : transitions.toKeys()) {
            if (key instanceof Type subType) {
                for (Type superType : subType.allSupersList()) {
                    if (!superType.equals(subType)) {
                        S superState = transitions.get((K) superType);
                        if (superState != null) {
                            S subState = transitions.get((K) subType);
                            S mergedState = (S) subState.merge(superState);
                            transitions = transitions.put((K) subType, mergedState);
                        }
                    }
                }
            }
        }
        return transitions;
    }

    public abstract S merge(S merged);

}
