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
