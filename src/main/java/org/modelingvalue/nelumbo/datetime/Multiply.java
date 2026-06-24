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

package org.modelingvalue.nelumbo.datetime;

import java.io.Serial;
import java.math.BigInteger;

import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboMethod;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.integers.NInteger;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

public final class Multiply extends Predicate {
    @Serial
    private static final long serialVersionUID = 1284905661027178738L;

    @NelumboConstructor
    public Multiply(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @NelumboMethod
    public InferResult period_multiply(NPeriod d, NInteger n, NPeriod e) {
        if (nrOfUnbound() > 1) {
            return unknown();
        }

        IsoDuration dv = d == null ? null : d.value();
        BigInteger nv = n == null ? null : n.value();
        IsoDuration ev = e == null ? null : e.value();

        if (dv != null && nv != null) {
            IsoDuration scaled = dv.multipliedBy(nv.intValueExact());
            if (ev != null) {
                return scaled.equals(ev) ? factCC() : falsehoodCC();
            }
            return set(2, NPeriod.of(scaled)).factCI();
        }

        return unknown();
    }

}
