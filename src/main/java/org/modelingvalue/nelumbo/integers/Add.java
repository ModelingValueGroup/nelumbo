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
import org.modelingvalue.nelumbo.Relation;

public final class Add extends Relation {
    private static final long serialVersionUID = 2384355866476367685L;

    public Add(Functor fuctor, Object[] args) {
        super(fuctor, args[0], args[1], args[2]);
    }

    private Add(Object[] array, Add declaration) {
        super(array, declaration);
    }

    @Override
    protected Add struct(Object[] array, Predicate declaration) {
        return new Add(array, (Add) declaration);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unknown();
        }
        BigInteger addend1 = getVal(1, 1);
        BigInteger addend2 = getVal(2, 1);
        BigInteger sum = getVal(3, 1);
        if (addend1 != null && addend2 != null) {
            BigInteger s = addend1.add(addend2);
            if (sum != null) {
                boolean eq = s.equals(sum);
                return eq ? factCC() : falsehoodCC();
            } else {
                return set(3, Integer.of(s)).factCI();
            }
        } else if (addend1 != null && sum != null) {
            return set(2, Integer.of(sum.subtract(addend1))).factCI();
        } else if (addend2 != null && sum != null) {
            return set(1, Integer.of(sum.subtract(addend2))).factCI();
        } else {
            return unknown();
        }
    }

}
