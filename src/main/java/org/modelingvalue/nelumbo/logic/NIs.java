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

package org.modelingvalue.nelumbo.logic;

import static org.modelingvalue.nelumbo.patterns.Pattern.n;
import static org.modelingvalue.nelumbo.patterns.Pattern.s;
import static org.modelingvalue.nelumbo.patterns.Pattern.t;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.syntax.ParseException;

public class NIs extends Predicate {
    @Serial
    private static final long serialVersionUID = -7316551393714994267L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    static {
        try {
            FUNCTOR = Functor.of(s(n(Type.OBJECT), t("="), n(Type.OBJECT)), Type.BOOLEAN, null, NIs.class, 30);
        } catch (ParseException e) {
            throw new IllegalStateException("Cannot create functor for NIs", e);
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
    public NIs setBinding(Map<Variable, Object> vars) {
        return (NIs) super.setBinding(vars);
    }

    @Override
    protected boolean isShallow(int nrOfUnbound, Functor functor) {
        Node a = getVal(0);
        Node b = getVal(1);
        return (b == null && a != null && Type.LITERAL.isAssignableFrom(a.type())) || //
                (a == null && b != null && Type.LITERAL.isAssignableFrom(b.type()));
    }

}
