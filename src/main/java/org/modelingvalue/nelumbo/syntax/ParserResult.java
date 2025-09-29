package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.ListNode;
import org.modelingvalue.nelumbo.Node;

public class ParserResult {

    private final boolean                     throwing;
    private final MutableList<ParseException> exceptions;

    private Node                              root;

    public ParserResult(boolean throwing) {
        this.throwing = throwing;
        this.exceptions = MutableList.of(List.of());
    }

    public List<Node> roots() {
        return root instanceof ListNode ? ((ListNode) root).elements() : root == null ? List.of() : List.of(root);
    }

    public Node root() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    public void addException(ParseException exception) throws ParseException {
        if (throwing) {
            throw exception;
        }
        this.exceptions.add(exception);
    }

    public List<ParseException> exceptions() {
        return exceptions.toImmutable();
    }

    public void throwException() throws ParseException {
        if (!exceptions.isEmpty()) {
            throw exceptions.getFirst();
        }
    }

    public void evaluate() throws ParseException {
        throwException();
        KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
        for (Node root : roots()) {
            if (root instanceof Evaluatable eval) {
                eval.evaluate(knowledgeBase);
            }
        }
    }

}
