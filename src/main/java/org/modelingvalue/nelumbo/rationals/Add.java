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
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

public final class Add extends Predicate {
    @Serial
    private static final long serialVersionUID = 3839770269634935346L;

    @NelumboConstructor
    public Add(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected Add set(NodeInfo nodeInfo, Object[] args) {
        return new Add(nodeInfo, args);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        BigInteger a1n = getVal(0, 0);
        BigInteger a1d = getVal(0, 1);
        BigInteger a2n = getVal(1, 0);
        BigInteger a2d = getVal(1, 1);
        BigInteger smn = getVal(2, 0);
        BigInteger smd = getVal(2, 1);
        if (a1n != null && a2n != null) {
            BigInteger sn = a1n.multiply(a2d).add(a2n.multiply(a1d));
            BigInteger sd = a1d.multiply(a2d);
            Rational s = Rational.of(sn, sd);
            if (smn != null) {
                boolean eq = s.equals(get(2));
                return eq ? factCC() : falsehoodCC();
            } else {
                return set(2, s).factCI();
            }
        } else if (a1n != null) {
            BigInteger an = smn.multiply(a1d).subtract(a1n.multiply(smd));
            BigInteger ad = a1d.multiply(smd);
            return set(1, Rational.of(an, ad)).factCI();
        } else if (a2n != null) {
            BigInteger an = smn.multiply(a2d).subtract(a2n.multiply(smd));
            BigInteger ad = a2d.multiply(smd);
            return set(0, Rational.of(an, ad)).factCI();
        }

        return unknown();
    }

}
