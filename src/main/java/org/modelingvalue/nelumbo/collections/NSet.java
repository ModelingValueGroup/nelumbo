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
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class NSet extends Node {
    @Serial
    private static final long serialVersionUID = 840888260991475386L;

    public NSet(List<AstElement> elements, Type elementType, Object[] args) {
        super(elementType.set(), elements, Set.of(args));
    }

    private NSet(Object[] array, NSet declaration) {
        super(array, declaration);
    }

    @Override
    public NSet setAstElements(List<AstElement> elements) {
        return (NSet) super.setAstElements(elements);
    }

    @SuppressWarnings("unused")
    public Type elementType() {
        return type().element();
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> elements() {
        return (Set<T>) get(0);
    }

    @Override
    protected NSet struct(Object[] array, Node declaration) {
        return new NSet(array, (NSet) declaration);
    }

    @Override
    public NSet set(int i, Object... a) {
        return (NSet) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        String string = elements().toString();
        return "{" + string.substring(4, string.length() - 1) + "}";
    }
}
