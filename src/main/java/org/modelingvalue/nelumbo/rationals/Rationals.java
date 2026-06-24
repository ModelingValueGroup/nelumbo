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

package org.modelingvalue.nelumbo.rationals;

import java.io.Serial;
import java.math.BigInteger;

import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboMethod;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.integers.NInteger;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

public final class Rationals extends Predicate {
    @Serial
    private static final long serialVersionUID = 3839770269634935346L;

    @NelumboConstructor
    public Rationals(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @NelumboMethod
    protected InferResult add(Rational addend1, Rational addend2, Rational sum) {
        if (nrOfUnbound() > 1) {
            return unknown();
        }
        if (addend1 != null && addend2 != null) {
            BigInteger sn = addend1.numerator().multiply(addend2.denominator())
                    .add(addend2.numerator().multiply(addend1.denominator()));
            BigInteger sd = addend1.denominator().multiply(addend2.denominator());
            Rational s = Rational.of(sn, sd);
            if (sum != null) {
                return s.equals(sum) ? factCC() : falsehoodCC();
            }
            return set(2, s).factCI();
        } else if (addend1 != null && sum != null) {
            BigInteger an = sum.numerator().multiply(addend1.denominator())
                    .subtract(addend1.numerator().multiply(sum.denominator()));
            BigInteger ad = addend1.denominator().multiply(sum.denominator());
            return set(1, Rational.of(an, ad)).factCI();
        } else if (addend2 != null && sum != null) {
            BigInteger an = sum.numerator().multiply(addend2.denominator())
                    .subtract(addend2.numerator().multiply(sum.denominator()));
            BigInteger ad = addend2.denominator().multiply(sum.denominator());
            return set(0, Rational.of(an, ad)).factCI();
        }
        return unknown();
    }

    @NelumboMethod
    protected InferResult mult(Rational factor1, Rational factor2, Rational product) {
        if (nrOfUnbound() > 1) {
            return unknown();
        }
        if (factor1 != null && factor2 != null) {
            BigInteger pn = factor1.numerator().multiply(factor2.numerator());
            BigInteger pd = factor1.denominator().multiply(factor2.denominator());
            Rational p = Rational.of(pn, pd);
            if (product != null) {
                return p.equals(product) ? factCC() : falsehoodCC();
            }
            return set(2, p).factCI();
        } else if (factor1 != null && product != null) {
            BigInteger dn = product.numerator().multiply(factor1.denominator());
            BigInteger dd = product.denominator().multiply(factor1.numerator());
            return set(1, Rational.of(dn, dd)).factCI();
        } else if (factor2 != null && product != null) {
            BigInteger dn = product.numerator().multiply(factor2.denominator());
            BigInteger dd = product.denominator().multiply(factor2.numerator());
            return set(0, Rational.of(dn, dd)).factCI();
        }
        return unknown();
    }

    @NelumboMethod
    protected InferResult gt(Rational left, Rational right) {
        if (nrOfUnbound() > 1) {
            return unknown();
        }
        if (left == null) {
            return set(0, get(1)).falsehoodsII();
        }
        if (right == null) {
            return set(1, get(0)).falsehoodsII();
        }
        return left.numerator().multiply(right.denominator())
                .compareTo(left.denominator().multiply(right.numerator())) > 0 ? factCC() : falsehoodCC();
    }

    @NelumboMethod
    protected InferResult iir(NInteger numerator, NInteger denominator, Rational rational) {
        if (nrOfUnbound() > 2) {
            return unknown();
        }
        BigInteger in = numerator == null ? null : numerator.value();
        BigInteger id = denominator == null ? null : denominator.value();
        BigInteger rn = rational == null ? null : rational.numerator();
        BigInteger rd = rational == null ? null : rational.denominator();
        if (in != null && id != null) {
            Rational ir = Rational.of(in, id);
            if (rn != null && rd != null) {
                return ir.equals(rational) ? factCC() : falsehoodCC();
            }
            return set(2, ir).factCI();
        } else if (in == null && id == null) {
            return set(0, NInteger.of(rn), NInteger.of(rd)).factCI();
        } else if (in == null && rn != null && rd != null) {
            // numerator unknown, denominator and value known:
            // in*rd == rn*id => in = rn*id/rd
            BigInteger[] dr = rn.multiply(id).divideAndRemainder(rd);
            return dr[1].equals(BigInteger.ZERO) ? set(0, NInteger.of(dr[0])).factCI() : falsehoodCI();
        } else if (id == null && rn != null && rd != null) {
            // denominator unknown, numerator and value known:
            // in*rd == rn*id => id = in*rd/rn
            if (rn.equals(BigInteger.ZERO)) {
                return in.equals(BigInteger.ZERO) ? unknown() : falsehoodCI();
            }
            BigInteger[] dr = in.multiply(rd).divideAndRemainder(rn);
            return dr[1].equals(BigInteger.ZERO) ? set(1, NInteger.of(dr[0])).factCI() : falsehoodCI();
        }
        return unknown();
    }

}
