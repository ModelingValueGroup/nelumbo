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

package org.modelingvalue.nelumbo.integers;

import java.math.BigInteger;

import org.modelingvalue.nelumbo.Functor;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.syntax.Token;

public final class Multiply extends Predicate {
    private static final long serialVersionUID = 2630128775301942610L;

    public Multiply(Functor fuctor, Token[] tokens, Object[] args) {
        super(fuctor, tokens, args[0], args[1], args[2]);
    }

    private Multiply(Object[] array, int start, Multiply declaration) {
        super(array, start, declaration);
    }

    @Override
    protected Multiply struct(Object[] array, int start, Predicate declaration) {
        return new Multiply(array, start, (Multiply) declaration);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unknown();
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
                return set(2, Integer.of(p)).factCI();
            }
        } else if (factor1 != null && product != null) {
            BigInteger[] dr = product.divideAndRemainder(factor1);
            return dr[1].equals(BigInteger.ZERO) ? set(1, Integer.of(dr[0])).factCI() : unknown();
        } else if (factor2 != null && product != null) {
            BigInteger[] dr = product.divideAndRemainder(factor2);
            return dr[1].equals(BigInteger.ZERO) ? set(0, Integer.of(dr[0])).factCI() : unknown();
        } else {
            return unknown();
        }
    }

}
