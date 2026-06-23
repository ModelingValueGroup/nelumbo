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

import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboMethod;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

public final class Integers extends Predicate {
    @Serial
    private static final long serialVersionUID = 2384355866476367685L;

    @NelumboConstructor
    public Integers(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @NelumboMethod
    protected InferResult add(NInteger addend1, NInteger addend2, NInteger sum) {
        if (nrOfUnbound() > 1) {
            return unresolvable();
        }
        BigInteger a1 = addend1 == null ? null : addend1.value();
        BigInteger a2 = addend2 == null ? null : addend2.value();
        BigInteger s = sum == null ? null : sum.value();
        if (a1 != null && a2 != null) {
            BigInteger r = a1.add(a2);
            if (s != null) {
                return r.equals(s) ? factCC() : falsehoodCC();
            }
            return set(2, NInteger.of(r)).factCI();
        } else if (a1 != null && s != null) {
            return set(1, NInteger.of(s.subtract(a1))).factCI();
        } else if (a2 != null && s != null) {
            return set(0, NInteger.of(s.subtract(a2))).factCI();
        }
        return unknown();
    }

    @NelumboMethod
    protected InferResult mult(NInteger factor1, NInteger factor2, NInteger product) {
        if (nrOfUnbound() > 1) {
            return unresolvable();
        }
        BigInteger f1 = factor1 == null ? null : factor1.value();
        BigInteger f2 = factor2 == null ? null : factor2.value();
        BigInteger p = product == null ? null : product.value();
        if (f1 != null && f2 != null) {
            BigInteger r = f1.multiply(f2);
            if (p != null) {
                return r.equals(p) ? factCC() : falsehoodCC();
            }
            return set(2, NInteger.of(r)).factCI();
        } else if (f1 != null && p != null) {
            BigInteger[] dr = p.divideAndRemainder(f1);
            return dr[1].equals(BigInteger.ZERO) ? set(1, NInteger.of(dr[0])).factCI() : falsehoodCI();
        } else if (f2 != null && p != null) {
            BigInteger[] dr = p.divideAndRemainder(f2);
            return dr[1].equals(BigInteger.ZERO) ? set(0, NInteger.of(dr[0])).factCI() : falsehoodCI();
        }
        return unknown();
    }

    @NelumboMethod
    protected InferResult gt(NInteger left, NInteger right) {
        if (nrOfUnbound() > 1) {
            return unresolvable();
        }
        if (left == null) {
            return set(0, get(1)).falsehoodsII();
        }
        if (right == null) {
            return set(1, get(0)).falsehoodsII();
        }
        return left.value().compareTo(right.value()) > 0 ? factCC() : falsehoodCC();
    }

}
