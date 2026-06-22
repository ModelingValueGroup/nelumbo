package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;

@SuppressWarnings("rawtypes")
public abstract class AbstractState<S extends AbstractState> {

    private final TypeMatcher typeMatcher;

    protected AbstractState(TypeMatcher typeMatcher) {
        this.typeMatcher = typeMatcher;
    }

    protected final TypeMatcher typeMatcher() {
        return typeMatcher;
    }

    public S matchType(Type type, MutableMap<Variable, Type> typeArgs) {
        TypeMatcher match = typeMatcher().match(type, typeArgs);
        return match != null ? typeTransitions().get(match.type()) : null;
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
