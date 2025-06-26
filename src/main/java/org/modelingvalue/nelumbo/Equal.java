//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo;

import java.util.Objects;

import org.modelingvalue.collections.List;

public class Equal extends Relation {
    private static final long   serialVersionUID = -5516286818572134367L;
    public static final Functor FUNCTOR          = new Functor(Relation.TYPE, "eq", List.of(Node.TYPE, Node.TYPE));

    public Equal(Node left, Node right) {
        super(FUNCTOR, left, right);
    }

    private Equal(Object[] array, Equal declaration) {
        super(array, declaration);
    }

    public Node left() {
        return (Node) get(1);
    }

    public Node right() {
        return (Node) get(2);
    }

    @Override
    protected Equal struct(Object[] array, Predicate declaration) {
        return new Equal(array, (Equal) declaration);
    }

    @Override
    public Equal set(int i, Object... a) {
        return (Equal) super.set(i, a);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        Node eq = eq(left(), right());
        return eq == null ? falsehoodCC() : set(1, eq).set(2, eq).factCC();
    }

    private static Node eq(Node left, Node right) {
        if (left.equals(right)) {
            return left;
        } else if (!(left instanceof Type) && right instanceof Type) {
            return ((Type) right).isAssignableFrom(((Node) left).type()) ? left : null;
        } else if (left instanceof Type && !(right instanceof Type)) {
            return ((Type) left).isAssignableFrom(((Node) right).type()) ? right : null;
        } else if (left instanceof Type && right instanceof Type) {
            return Objects.equals(left, right) ? left : null;
        } else if (left.length() != right.length()) {
            return null;
        }
        Object[] array = null;
        for (int i = 0; i < left.length(); i++) {
            Object leftVal = left.get(i);
            Object eq = eq(leftVal, right.get(i));
            if (eq == null) {
                return null;
            } else if (!Objects.equals(eq, leftVal)) {
                if (array == null) {
                    array = left.toArray();
                }
                array[i] = eq;
            }
        }
        return array != null ? left.struct(array) : left;
    }

    private static Object eq(Object left, Object right) {
        if (left != right) {
            if (left instanceof Node && right instanceof Node) {
                return eq((Node) left, (Node) right);
            } else if (right instanceof Type) {
                return ((Type) right).isAssignableFrom(left.getClass()) ? left : null;
            } else if (left instanceof Type) {
                return ((Type) left).isAssignableFrom(right.getClass()) ? right : null;
            } else if (!Objects.equals(left, right)) {
                return null;
            }
        }
        return left;
    }

}
