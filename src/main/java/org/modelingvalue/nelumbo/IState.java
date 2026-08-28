package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.util.Mergeable;
import org.modelingvalue.nelumbo.lang.Type;

@SuppressWarnings("rawtypes")
public interface IState<S extends IState> extends Mergeable<S> {

    @SuppressWarnings("unchecked")
    default <K> Map<K, S> inherit(Map<K, S> transitions) {
        for (Object key : transitions.toKeys()) {
            if (key instanceof Type subType) {
                for (Entry<Type, Type> entry : subType.allSupersList()) {
                    Type superType = entry.getKey();
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
