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

import java.io.Serializable;
import java.time.Duration;
import java.time.Period;

// An ISO 8601 duration as the composite java.time models it: a calendar Period (Y/M/W/D) plus an exact
// time Duration (H/M/S). Either part may be zero. Value equality is field-based (P1M != P30D), which is
// the correct ISO 8601 semantics. Immutable and Serializable so it can live in a Node value slot.
public record IsoDuration(Period period, Duration duration) implements Serializable {

    public static final IsoDuration ZERO = new IsoDuration(Period.ZERO, Duration.ZERO);

    // Date units (before any 'T') become the Period; time units (after 'T') become the Duration.
    public static IsoDuration parse(String text) {
        int t = text.indexOf('T');
        String datePart = (t < 0 ? text : text.substring(0, t)).substring(1); // strip leading 'P'
        String timePart = t < 0 ? "" : text.substring(t);                     // keeps the leading 'T'
        Period   period   = datePart.isEmpty() ? Period.ZERO : Period.parse("P" + datePart);
        Duration duration = timePart.isEmpty() ? Duration.ZERO : Duration.parse("P" + timePart.replace(',', '.'));
        return new IsoDuration(period, duration);
    }

    public IsoDuration plus(IsoDuration o) {
        return new IsoDuration(period.plus(o.period), duration.plus(o.duration));
    }

    public IsoDuration minus(IsoDuration o) {
        return new IsoDuration(period.minus(o.period), duration.minus(o.duration));
    }

    public IsoDuration multipliedBy(int scalar) {
        return new IsoDuration(period.multipliedBy(scalar), duration.multipliedBy(scalar));
    }

    public boolean isZero() {
        return period.isZero() && duration.isZero();
    }

    @Override
    public String toString() {
        if (isZero()) {
            return "PT0S";
        }
        String date = period.isZero() ? "" : period.toString().substring(1);     // "P1Y2M3D" -> "1Y2M3D"
        String time = duration.isZero() ? "" : duration.toString().substring(1); // "PT1H30M" -> "T1H30M"
        return "P" + date + time;
    }
}
