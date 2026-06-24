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
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class NSet extends NCollection {
    @Serial
    private static final long serialVersionUID = 840888260991475386L;

    @NelumboConstructor
    public NSet(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public NSet(Type elementType, Set<?> set) {
        super(NodeInfo.of(elementType.toSet().toLiteral()), set);
    }

    @Override
    protected Object typeForEquals() {
        return Type.SET;
    }

    public Type elementType() {
        return type().argument();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> collection() {
        return (Set<T>) get(0);
    }

    @Override
    public List<Object> args() {
        return collection().asList();
    }

    @Override
    public String toString(TokenType[] previous) {
        String string = collection().toString();
        return "{" + string.substring(4, string.length() - 1) + "}";
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason != ConstructionReason.parsing || (length() > 0 && get(0) instanceof Set)) {
            return this;
        }
        return set(nodeInfo().resetDeclaration(), new Object[] { super.args().asSet() });
    }
}
