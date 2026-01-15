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

package org.modelingvalue.nelumbo.collections;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class NList extends Node {
    @Serial
    private static final long serialVersionUID = 2275866157289787141L;

    public NList(List<AstElement> elements, Type elementType) {
        super(elementType.list(), elements, List.of());
    }

    public NList(Type elementType, List<AstElement> elements, List<Node> args) {
        super(elementType.list(), elements, args);
    }

    @SuppressWarnings("unused")
    public NList(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args);
    }

    public NList(List<AstElement> elements, NList list, Node last) {
        super(list.type(), list.astElements().addAll(elements).add(last), list.elements().add(last));
    }

    private NList(Object[] array, NList declaration) {
        super(array, declaration);
    }

    @Override
    public NList setAstElements(List<AstElement> elements) {
        return (NList) super.setAstElements(elements);
    }

    @SuppressWarnings("unused")
    public Type elementType() {
        return type().element();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> elements() {
        return (List<T>) get(0);
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> List<T> elementsFlattened() {
        List<T> result = List.of();
        for (T e : this.<T> elements()) {
            if (e instanceof NList nl) {
                result = result.addAll(nl.<T> elementsFlattened());
            } else {
                result = result.add(e);
            }
        }
        return result;
    }

    @Override
    protected NList struct(Object[] array, Node declaration) {
        return new NList(array, (NList) declaration);
    }

    @Override
    public NList set(int i, Object... a) {
        return (NList) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        return elements().toString().substring(4);
    }
}
