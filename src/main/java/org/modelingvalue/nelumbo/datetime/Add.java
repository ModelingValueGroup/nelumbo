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
import org.modelingvalue.nelumbo.NelumboMethod;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

import java.io.Serial;
import java.time.*;
import java.time.temporal.Temporal;

public final class Add extends Predicate {
    @Serial
    private static final long serialVersionUID = 3174905661027178736L;

    @NelumboConstructor
    public Add(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        Object a = getVal(0, 0);
        IsoDuration d = getVal(1, 0);
        Object b = getVal(2, 0);

        if (a != null && d != null) {
            Temporal sum = plus(a, d);
            if (sum == null) {
                return unresolvable();
            }
            if (b != null) {
                return sum.equals(b) ? factCC() : falsehoodCC();
            }
            return set(2, wrap(sum)).factCI();
        } else if (a != null) {
            IsoDuration between = between(a, b);
            return between == null ? unresolvable() : set(1, wrap(between)).factCI();
        } else if (b != null && d != null) {
            Temporal diff = minus(b, d);
            return diff == null ? unresolvable() : set(0, wrap(diff)).factCI();
        }

        return unknown();
    }

    @NelumboMethod
    public InferResult period_add(NPeriod a, NPeriod b, NPeriod c) {
        if (nrOfUnbound() > 1) {
            return unresolvable();
        }

        IsoDuration av = a == null ? null : a.value();
        IsoDuration bv = b == null ? null : b.value();
        IsoDuration cv = c == null ? null : c.value();
        if (av != null && bv != null) {
            IsoDuration sum = av.plus(bv);
            if (cv != null) {
                return sum.equals(cv) ? factCC() : falsehoodCC();
            }
            return set(2, wrap(sum)).factCI();
        } else if (av != null && cv != null) {
            return set(1, wrap(cv.minus(av))).factCI();
        } else if (bv != null && cv != null) {
            return set(0, wrap(cv.minus(bv))).factCI();
        }

        return unknown();
    }

    private static Temporal plus(Object instant, IsoDuration d) {
        if (instant instanceof LocalDate date) {
            return d.duration().isZero() ? date.plus(d.period()) : null;
        }
        if (instant instanceof LocalTime time) {
            return d.period().isZero() ? time.plus(d.duration()) : null;
        }
        if (instant instanceof LocalDateTime ldt) {
            return ldt.plus(d.period()).plus(d.duration());
        }

        return null;
    }

    private static Temporal minus(Object instant, IsoDuration d) {
        if (instant instanceof LocalDate date) {
            return d.duration().isZero() ? date.minus(d.period()) : null;
        }
        if (instant instanceof LocalTime time) {
            return d.period().isZero() ? time.minus(d.duration()) : null;
        }
        if (instant instanceof LocalDateTime ldt) {
            return ldt.minus(d.period()).minus(d.duration());
        }

        return null;
    }

    private static Node wrap(Object instant) {
        if (instant instanceof IsoDuration odt) {
            return NPeriod.of(odt);
        }
        if (instant instanceof LocalDate date) {
            return NDate.of(date);
        }
        if (instant instanceof LocalTime time) {
            return NTime.of(time);
        }
        if (instant instanceof LocalDateTime ldt) {
            return NDateTime.of(ldt);
        }

        return null;
    }

    private static IsoDuration between(Object a, Object b) {
        if (a instanceof LocalDate da && b instanceof LocalDate db) {
            return new IsoDuration(Period.between(da, db), Duration.ZERO);
        }
        if (a instanceof LocalTime ta && b instanceof LocalTime tb) {
            return new IsoDuration(Period.ZERO, Duration.between(ta, tb));
        }
        if (a instanceof LocalDateTime na && b instanceof LocalDateTime nb) {
            return new IsoDuration(Period.ZERO, Duration.between(na, nb));
        }

        return null;
    }

}
