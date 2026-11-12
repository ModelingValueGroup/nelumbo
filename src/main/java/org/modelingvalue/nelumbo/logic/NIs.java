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
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.logic;

import static org.modelingvalue.nelumbo.patterns.Pattern.n;
import static org.modelingvalue.nelumbo.patterns.Pattern.s;
import static org.modelingvalue.nelumbo.patterns.Pattern.t;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;

public class NIs extends Predicate {
    @Serial
    private static final long serialVersionUID = -7316551393714994267L;

    private static Functor FUNCTOR;

    static {
        try {
            FUNCTOR = Functor.of(s(n(Type.OBJECT, true), t("="), n(Type.OBJECT, true)), Type.BOOLEAN, null, NIs.class,
                    30);
        } catch (ParseException e) {
            throw new IllegalStateException("Cannot create functor for " + NIs.class, e);
        }
    }

    public NIs(List<AstElement> elements, Node left, Node right) {
        super(NodeInfo.of(FUNCTOR, elements), left, right);
    }

    @NelumboConstructor
    public NIs(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public Node left() {
        return (Node) get(0);
    }

    public Node right() {
        return (Node) get(1);
    }

    @Override
    public NIs set(int i, Object... a) {
        return (NIs) super.set(i, a);
    }

    @Override
    public NIs setBinding(Node declaration, Map<Variable, Object> vars) {
        return (NIs) super.setBinding(declaration, vars);
    }

    @Override
    protected boolean isShallow(int nrOfUnbound, Functor functor) {
        Node a = getVal(0);
        Node b = getVal(1);
        return (b == null && a != null && Type.LITERAL.isAssignableFrom(a.type())) || //
                (a == null && b != null && Type.LITERAL.isAssignableFrom(b.type()));
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing) {
            Node a = (Node) get(0);
            Node b = (Node) get(1);
            if (!(a instanceof Variable) && Type.LITERAL.isAssignableFrom(a.type())) {
                Node x = a.coerceTo(b.type().nonFunction().nonVariable());
                if (x != null && !x.equals(a)) {
                    return set(0, x);
                }
            }
            if (!(b instanceof Variable) && Type.LITERAL.isAssignableFrom(b.type())) {
                Node y = b.coerceTo(a.type().nonFunction().nonVariable());
                if (y != null && !y.equals(b)) {
                    return set(1, y);
                }
            }
        }
        return this;
    }

}
