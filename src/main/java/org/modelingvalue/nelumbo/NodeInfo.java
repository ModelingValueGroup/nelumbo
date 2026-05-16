//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
