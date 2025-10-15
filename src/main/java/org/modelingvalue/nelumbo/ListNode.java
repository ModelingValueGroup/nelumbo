//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

import java.io.Serial;

import org.modelingvalue.collections.List;

public class ListNode extends Node {
    @Serial
    private static final long serialVersionUID = 2275866157289787141L;

    public ListNode(List<AstElement> elements, Type elementType) {
        super(elementType.list(), elements, List.of());
    }

    public ListNode(List<AstElement> elements, ListNode list, Node last) {
        super(list.type(), list.astElements().addAll(elements).add(last), list.elements().add(last));
    }

    public ListNode(List<AstElement> elements, Type elementType, Node... nodes) {
        super(elementType.list(), elements.addAll(List.of(nodes)), List.of(nodes));
    }

    private ListNode(Object[] array) {
        super(array);
    }

    @Override
    public ListNode setAstElements(List<AstElement> elements) {
        return (ListNode) super.setAstElements(elements);
    }

    @SuppressWarnings("unused")
    public Type elementType() {
        return type().element();
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> List<T> elements() {
        return (List<T>) get(0);
    }

    @Override
    protected ListNode struct(Object[] array) {
        return new ListNode(array);
    }

    @Override
    public ListNode set(int i, Object... a) {
        return (ListNode) super.set(i, a);
    }

    @Override
    public String toString() {
        return elements().toString().substring(4);
    }
}
