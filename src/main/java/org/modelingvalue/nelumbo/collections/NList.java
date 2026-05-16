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
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class NList extends Node {
    @Serial
    private static final long serialVersionUID = 2275866157289787141L;

    public NList(List<AstElement> elements, Type elementType) {
        super(NodeInfo.of(elementType.list(), elements), List.of());
    }

    public NList(Type elementType, List<AstElement> elements, List<Node> args) {
        super(NodeInfo.of(elementType.list(), elements), args);
    }

    public NList(Functor functor, List<AstElement> elements, List<Node> args) {
        super(NodeInfo.of(functor, elements), args);
    }

    @NelumboConstructor
    public NList(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public NList(List<AstElement> elements, NList list, Node last) {
        super(NodeInfo.of(list.type(), list.astElements().addAll(elements).add(last)), list.elements().add(last));
    }

    @Override
    protected NList set(NodeInfo nodeInfo, Object[] args) {
        return new NList(nodeInfo, args);
    }

    @Override
    public List<Object> args() {
        return elements();
    }

    @Override
    public NList setAstElements(List<AstElement> elements) {
        return (NList) super.setAstElements(elements);
    }

    public Type elementType() {
        return type().element();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> elements() {
        return (List<T>) get(0);
    }

    public <T extends Node> List<T> elementsFlattened() {
        List<T> result = List.of();
        for (T e : this.<T>elements()) {
            if (e instanceof NList nl) {
                result = result.addAll(nl.<T>elementsFlattened());
            } else {
                result = result.add(e);
            }
        }
        return result;
    }

    @Override
    public NList set(int i, Object... a) {
        return (NList) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        return elements().toString().substring(4);
    }

    @Override
    public Node add(Node added) {
        return new NList(List.of(), this, added);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason != ConstructionReason.parsing || (length() > 0 && get(0) instanceof List)) {
            return this;
        }
        return setArgs(new Object[] { Set.of(super.args()) });
    }
}
