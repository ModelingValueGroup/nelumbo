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

package org.modelingvalue.nelumbo.strings;

import java.io.Serial;
import java.math.BigInteger;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class Integer extends Predicate {
    @Serial
    private static final long serialVersionUID = -2874326869672600959L;

    public Integer(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1]);
    }

    private Integer(Object[] array, Integer declaration) {
        super(array, declaration);
    }

    @Override
    protected Integer struct(Object[] array, Node declaration) {
        return new Integer(array, (Integer) declaration);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        BigInteger integer = getVal(0, 0);
        java.lang.String string = getVal(1, 0);
        if (string != null) {
            try {
                BigInteger parsed = BigInteger.valueOf(java.lang.Integer.parseInt(string));
                if (integer != null) {
                    boolean eq = integer.equals(parsed);
                    return eq ? factCC() : falsehoodCC();
                } else {
                    return set(0, org.modelingvalue.nelumbo.integers.Integer.of(parsed)).factCI();
                }
            } catch (NumberFormatException e) {
                return integer != null ? falsehoodCC() : falsehoodCI();
            }
        } else if (integer != null) {
            java.lang.String s = integer.toString();
            return set(1, org.modelingvalue.nelumbo.strings.String.of(s)).factCI();
        }

        return unknown();
    }

}
