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
import org.modelingvalue.nelumbo.integers.NInteger;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

public final class IntegersRational extends Predicate {
    @Serial
    private static final long serialVersionUID = -7882486910269514611L;

    @NelumboConstructor
    public IntegersRational(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 2) {
            return unresolvable();
        }

        BigInteger in = getVal(0, 0);
        BigInteger id = getVal(1, 0);
        BigInteger rn = getVal(2, 0);
        BigInteger rd = getVal(2, 1);

        if (in != null && id != null) {
            Rational ir = Rational.of(in, id);
            if (rn != null && rd != null) {
                boolean eq = ir.equals(get(2));
                return eq ? factCC() : falsehoodCC();
            } else {
                return set(2, Rational.of(in, id)).factCI();
            }
        } else if (in == null && id == null) {
            set(0, NInteger.of(rn), NInteger.of(rd)).factCI();
        } else {
            return unresolvable();
        }

        return unknown();
    }

}
