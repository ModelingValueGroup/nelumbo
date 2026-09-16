package org.modelingvalue.nelumbo.lang;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.NodeInfo;

public interface FunctorInfo extends NodeInfo {

    Functor original();

    public static abstract class AbstractFunctorInfo extends AbstractNodeInfo implements FunctorInfo {
    }

    static FunctorInfo of(FunctorOrType functorOrType, List<AstElement> elements, Functor original) {
        if (elements.isEmpty()) {
            return of(functorOrType, original);
        }
        return new AbstractFunctorInfo() {
            @Override
            public FunctorOrType functorOrType() {
                return functorOrType;
            }

            @Override
            public List<AstElement> elements() {
                return elements;
            }

            @Override
            public Functor original() {
                return original;
            }

        };
    }

    static FunctorInfo of(FunctorOrType functorOrType, Functor original) {
        return new AbstractFunctorInfo() {
            @Override
            public FunctorOrType functorOrType() {
                return functorOrType;
            }

            @Override
            public List<AstElement> elements() {
                return List.of();
            }

            @Override
            public Functor original() {
                return original;
            }
        };
    }

    @Override
    default FunctorInfo setFunctorOrType(FunctorOrType functorOrType) {
        return functorOrType.equals(functorOrType()) ? this : of(functorOrType, elements(), original());
    }

    @Override
    default FunctorInfo setElements(List<AstElement> elements) {
        return elements.equals(elements()) ? this : of(functorOrType(), elements, original());
    }

}
