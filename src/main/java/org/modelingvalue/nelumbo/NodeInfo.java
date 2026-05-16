package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.lang.FunctorOrType;

public interface NodeInfo {

    FunctorOrType functorOrType();

    List<AstElement> elements();

    Node declaration();

    public static abstract class AbstractNodeInfo implements NodeInfo {
        @Override
        public String toString() {
            return functorOrType().toString();
        }
    }

    static NodeInfo of(FunctorOrType functorOrType, List<AstElement> elements, Node declaration) {
        if (elements.isEmpty()) {
            return of(functorOrType, declaration);
        }
        return new AbstractNodeInfo() {
            @Override
            public FunctorOrType functorOrType() {
                return functorOrType;
            }

            @Override
            public List<AstElement> elements() {
                return elements;
            }

            @Override
            public Node declaration() {
                return declaration;
            }
        };
    }

    static NodeInfo of(FunctorOrType functorOrType, List<AstElement> elements) {
        if (elements.isEmpty()) {
            return of(functorOrType);
        }
        return new AbstractNodeInfo() {
            @Override
            public FunctorOrType functorOrType() {
                return functorOrType;
            }

            @Override
            public List<AstElement> elements() {
                return elements;
            }

            @Override
            public Node declaration() {
                return null;
            }
        };
    }

    static NodeInfo of(FunctorOrType functorOrType, Node declaration) {
        return new AbstractNodeInfo() {
            @Override
            public FunctorOrType functorOrType() {
                return functorOrType;
            }

            @Override
            public List<AstElement> elements() {
                return List.of();
            }

            @Override
            public Node declaration() {
                return declaration;
            }
        };
    }

    static NodeInfo of(FunctorOrType functorOrType) {
        return new AbstractNodeInfo() {
            @Override
            public FunctorOrType functorOrType() {
                return functorOrType;
            }

            @Override
            public List<AstElement> elements() {
                return List.of();
            }

            @Override
            public Node declaration() {
                return null;
            }
        };
    }

    default NodeInfo setFunctorOrType(FunctorOrType functorOrType) {
        return of(functorOrType, elements(), declaration());
    }

    default NodeInfo setElements(List<AstElement> elements) {
        return of(functorOrType(), elements, declaration());
    }

    default NodeInfo setDeclaration(Node declaration) {
        return of(functorOrType(), elements(), declaration);
    }

    default NodeInfo resetDeclaration() {
        return of(functorOrType(), elements());
    }

}
