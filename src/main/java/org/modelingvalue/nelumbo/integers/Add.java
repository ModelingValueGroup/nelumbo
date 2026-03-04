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

import java.io.Serial;
import java.math.BigInteger;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class Add extends Predicate {
    @Serial
    private static final long serialVersionUID = 2384355866476367685L;

    @NelumboConstructor
    public Add(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1], args[2]);
    }

    private Add(Object[] array, List<AstElement> elements, Add declaration) {
        super(array, elements, declaration);
    }

    @Override
    protected Add struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new Add(array, elements, (Add) declaration);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        BigInteger addend1 = getVal(0, 0);
        BigInteger addend2 = getVal(1, 0);
        BigInteger sum = getVal(2, 0);
        if (addend1 != null && addend2 != null) {
            BigInteger s = addend1.add(addend2);
            if (sum != null) {
                boolean eq = s.equals(sum);
                return eq ? factCC() : falsehoodCC();
            } else {
                return set(2, NInteger.of(s)).factCI();
            }
        } else if (addend1 != null && sum != null) {
            return set(1, NInteger.of(sum.subtract(addend1))).factCI();
        } else if (addend2 != null && sum != null) {
            return set(0, NInteger.of(sum.subtract(addend2))).factCI();
        }

        return unknown();
    }

}
