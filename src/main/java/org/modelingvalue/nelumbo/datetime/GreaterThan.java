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

import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

import java.io.Serial;
import java.time.OffsetDateTime;

public final class GreaterThan extends Predicate {
    @Serial
    private static final long serialVersionUID = 5338681256251602012L;

    @NelumboConstructor
    public GreaterThan(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }
        Object left = getVal(0, 0);
        Object right = getVal(1, 0);
        if (left == null) {
            return set(0, get(1)).falsehoodsII();
        }
        if (right == null) {
            return set(1, get(0)).falsehoodsII();
        }
        Integer comparison = compare(left, right);
        if (comparison == null) {
            return unresolvable(); // not comparable (e.g. mixing offset and offset-less datetimes)
        }
        return comparison > 0 ? factCC() : falsehoodCC();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Integer compare(Object left, Object right) {
        // Periods compare by a nominal magnitude: months are 30 days, years 365 (P1M vs P30D has no
        // exact answer, so this is an explicit convention, not an exact ordering).
        if (left instanceof IsoDuration a && right instanceof IsoDuration b) {
            return Long.compare(nominalSeconds(a), nominalSeconds(b));
        }
        // OffsetDateTime compares by instant: the same moment in different offsets is equal,
        // unlike OffsetDateTime.compareTo which adds local date-time as a secondary key.
        if (left instanceof OffsetDateTime a && right instanceof OffsetDateTime b) {
            return a.toInstant().compareTo(b.toInstant());
        }
        // Other Comparable java.time values must share an exact runtime type.
        if (left instanceof Comparable && left.getClass() == right.getClass()) {
            return ((Comparable) left).compareTo(right);
        }
        return null;
    }

    private static long nominalSeconds(IsoDuration value) {
        long days = value.period().toTotalMonths() * 30L + value.period().getDays();
        return days * 86_400L + value.duration().getSeconds();
    }

}
