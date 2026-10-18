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

public class NSet extends NCollection {
    @Serial
    private static final long serialVersionUID = 840888260991475386L;

    @NelumboConstructor
    public NSet(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, initArgs(args));
    }

    private static Object[] initArgs(Object... args) {
        if (args.length == 2 && args[0] instanceof Set && args[1] instanceof Boolean) {
            return args;
        }
        int open = open(args);
        return new Object[] { Set.of(remove(args, open)), open < 0 };
    }

    public NSet(Type elementType, Set<?> set, boolean complete) {
        super(NodeInfo.of(elementType.toSet().toLiteral()), set, complete);
    }

    @Override
    public Object functorOrTypeForEquals() {
        return Type.SET;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<T> collection() {
        return (Set<T>) get(0);
    }

    @Override
    public List<Object> args() {
        List<Object> args = collection().asList();
        return complete() ? args : args.add("..");
    }

    @Override
    public String toString(RenderOptions options) {
        Set<Object> collection = collection();
        String string = collection.toString();
        return "{" + string.substring(4, string.length() - 1)
                + (complete() ? "" : (collection.isEmpty() ? "" : ",") + "..") + "}";
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        return this;
    }
}
