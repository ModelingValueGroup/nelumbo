package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.List;

public class ListNode extends Node {
    private static final long serialVersionUID = 2275866157289787141L;

    public ListNode(Type elementType) {
        super(elementType.list(), List.of());
    }

    public ListNode(ListNode list, Node last) {
        super(list.type(), list.elements().add(last));
    }

    public Type elementType() {
        return type().element();
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> List<T> elements() {
        return (List<T>) get(1);
    }

    private ListNode(Object[] array) {
        super(array);
    }

    @Override
    protected ListNode struct(Object[] array) {
        return new ListNode(array);
    }

    @Override
    public ListNode set(int i, Object... a) {
        return (ListNode) super.set(i, a);
    }
}
