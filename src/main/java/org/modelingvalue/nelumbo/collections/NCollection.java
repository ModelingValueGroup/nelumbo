package org.modelingvalue.nelumbo.collections;

import java.io.Serial;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;

public abstract class NCollection extends Node {
    @Serial
    private static final long serialVersionUID = -4533295959981910935L;

    protected NCollection(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> collection() {
        return (Collection<T>) get(0);
    }

    public int size() {
        return collection().size();
    }

}
