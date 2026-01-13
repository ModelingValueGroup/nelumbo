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

package org.modelingvalue.nelumbo.integers;

import java.math.BigInteger;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class Multiply extends Predicate {
    private static final long serialVersionUID = 2630128775301942610L;

    public Multiply(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1], args[2]);
    }

    private Multiply(Object[] array, Multiply declaration) {
        super(array, declaration);
    }

    @Override
    protected Multiply struct(Object[] array, Node declaration) {
        return new Multiply(array, (Multiply) declaration);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        BigInteger factor1 = getVal(0, 0);
        BigInteger factor2 = getVal(1, 0);
        BigInteger product = getVal(2, 0);
        if (factor1 != null && factor2 != null) {
            BigInteger p = factor1.multiply(factor2);
            if (product != null) {
                boolean eq = p.equals(product);
                return eq ? factCC() : falsehoodCC();
            } else {
                return set(2, NInteger.of(p)).factCI();
            }
        } else if (factor1 != null && product != null) {
            BigInteger[] dr = product.divideAndRemainder(factor1);
            return dr[1].equals(BigInteger.ZERO) ? set(1, NInteger.of(dr[0])).factCI() : falsehoodCI();
        } else if (factor2 != null && product != null) {
            BigInteger[] dr = product.divideAndRemainder(factor2);
            return dr[1].equals(BigInteger.ZERO) ? set(0, NInteger.of(dr[0])).factCI() : falsehoodCI();
        }

        return unknown();
    }

}
