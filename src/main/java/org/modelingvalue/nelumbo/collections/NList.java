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
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class NList extends NCollection {
    @Serial
    private static final long serialVersionUID = 2275866157289787141L;

    @NelumboConstructor
    public NList(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, initArgs(args));
    }

    private static Object[] initArgs(Object... args) {
        if (args.length == 2 && args[0] instanceof List && args[1] instanceof Boolean) {
            return args;
        }
        int open = open(args);
        return new Object[] { List.of(remove(args, open)), open < 0 };
    }

    public NList(Type elementType, List<?> list, boolean complete) {
        super(NodeInfo.of(elementType.toList().toLiteral()), list, complete);
    }

    public NList(List<AstElement> elements, Type elementType) {
        super(NodeInfo.of(elementType.toList().toLiteral(), elements), List.of(), true);
    }

    public NList(Type elementType, List<AstElement> elements, List<?> args) {
        super(NodeInfo.of(elementType.toList().toLiteral(), elements), args, true);
    }

    public NList(Functor functor, List<AstElement> elements, List<?> args) {
        super(NodeInfo.of(functor, elements), args, true);
    }

    public NList(List<AstElement> elements, NList list, Node last) {
        super(NodeInfo.of(list.type(), list.astElements().addAll(elements).add(last)), list.collection().add(last),
                true);
    }

    @Override
    protected NList set(NodeInfo nodeInfo, Object[] args) {
        return new NList(nodeInfo, args);
    }

    @Override
    public Object functorOrTypeForEquals() {
        return Type.LIST;
    }

    @Override
    public List<Object> args() {
        List<Object> args = collection();
        return complete() ? args : args.add("..");
    }

    @Override
    public NList setAstElements(List<AstElement> elements) {
        return (NList) super.setAstElements(elements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> collection() {
        return (List<T>) get(0);
    }

    public <T extends Node> List<T> elementsFlattened() {
        List<T> result = List.of();
        for (T e : this.<T>collection()) {
            if (e instanceof NList nl) {
                result = result.addAll(nl.<T>elementsFlattened());
            } else {
                result = result.add(e);
            }
        }
        return result;
    }

    @Override
    public String toString(RenderOptions options) {
        List<Object> collection = collection();
        String string = collection.toString();
        return "[" + string.substring(5, string.length() - 1)
                + (complete() ? "" : (collection.isEmpty() ? "" : ",") + "..") + "]";
    }

    @Override
    public Node add(Node added) {
        return new NList(List.of(), this, added);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        return this;
    }
}
