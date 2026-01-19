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

import java.io.Serial;
import java.util.Objects;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.patterns.Functor;

public class Equal extends Predicate {
    @Serial
    private static final long serialVersionUID = -5516286818572134367L;

    @NelumboConstructor
    public Equal(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1]);
    }

    private Equal(Object[] array, Equal declaration) {
        super(array, declaration);
    }

    public Node left() {
        return (Node) get(0);
    }

    public Node right() {
        return (Node) get(1);
    }

    @Override
    protected Equal struct(Object[] array, Node declaration) {
        return new Equal(array, (Equal) declaration);
    }

    @Override
    public Equal set(int i, Object... a) {
        return (Equal) super.set(i, a);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        boolean[] complete = new boolean[]{true};
        Node eq = eq(left(), right(), complete);
        if (eq == null) {
            return complete[0] ? falsehoodCC() : falsehoodCI();
        } else {
            Equal r = set(0, eq).set(1, eq);
            return complete[0] ? r.factCC() : r.factCI();
        }
    }

    private static Node eq(Node left, Node right, boolean[] complete) {
        if (left.equals(right)) {
            return left;
        } else if (!(left instanceof Type) && right instanceof Type) {
            complete[0] = false;
            return ((Type) right).isAssignableFrom((left).type()) ? left : null;
        } else if (left instanceof Type && !(right instanceof Type)) {
            complete[0] = false;
            return ((Type) left).isAssignableFrom((right).type()) ? right : null;
        } else if (left instanceof Type && right instanceof Type) { // right always a Type here!
            complete[0] = false;
            return Objects.equals(left, right) ? left : null;
        } else if (!left.typeOrFunctor().equals(right.typeOrFunctor())) {
            return null;
        } else if (left.length() != right.length()) {
            return null;
        }
        Object[] array = null;
        for (int i = 0; i < left.length(); i++) {
            Object leftVal = left.get(i);
            Object eq = eq(leftVal, right.get(i), complete);
            if (eq == null) {
                return null;
            } else if (!Objects.equals(eq, leftVal)) {
                if (array == null) {
                    array = left.toArray();
                }
                array[i + START] = eq;
            }
        }
        return array != null ? left.struct(array) : left;
    }

    private static Object eq(Object left, Object right, boolean[] complete) {
        if (left != right) {
            if (left instanceof Node && right instanceof Node) {
                return eq((Node) left, (Node) right, complete);
            } else if (right instanceof Type) {
                complete[0] = false;
                return ((Type) right).isAssignableFrom(left.getClass()) ? left : null;
            } else if (left instanceof Type) {
                complete[0] = false;
                return ((Type) left).isAssignableFrom(right.getClass()) ? right : null;
            } else if (!Objects.equals(left, right)) {
                return null;
            }
        }
        return left;
    }

}
