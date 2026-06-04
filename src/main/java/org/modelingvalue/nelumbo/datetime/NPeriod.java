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

import org.modelingvalue.nelumbo.*;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

import java.io.Serial;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

// Period 	::= P <(> <(> <NUMBER> <(> Y <|> M <|> W <|> D <)> <)+> <(> T <(> <NUMBER> <(> H <|> M <|> S <)> <)+> <)?> <|> T <(> <NUMBER> <(> H <|> M <|> S <)> <)+> <)>
public final class NPeriod extends Node {
    @Serial
    private static final long serialVersionUID = 8217354196602437781L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    @NelumboConstructor
    public NPeriod(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public static NPeriod of(IsoDuration val) {
        return new NPeriod(NodeInfo.of(FUNCTOR), val);
    }

    @Override
    public String toString(TokenType[] previous) {
        Object value = getVal(0);
        return String.valueOf(value);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing && !(get(0) instanceof IsoDuration)) {
            try {
                return setArgs(new Object[] { build() });
            } catch (DateTimeException | NumberFormatException e) {
                knowledgeBase.addException(new ParseException("Invalid ISO 8601 period: " + e.getMessage(), this));
            }
        }
        return this;
    }

    // The grammar yields two shapes:
    //   left branch  (P date+ (T time+)?):  get(0) = list of [num,unit] date pairs,
    //                                        get(1) = Optional.empty or a list of [num,unit] time pairs
    //   right branch (P T time+):           each top-level arg is a [num,unit] time pair
    // 'M' is months in the date section, minutes in the time section.
    private static final String DATE_ORDER = "YMWD";
    private static final String TIME_ORDER = "HMS";

    private Object build() {
        Period   period   = Period.ZERO;
        Duration duration = Duration.ZERO;
        if (isDateSection(get(0))) {
            period = foldDate((Iterable<?>) get(0));
            if (length() > 1 && get(1) instanceof Iterable<?> timePairs) {
                duration = foldTime(timePairs);
            }
        } else {
            List<Object> timePairs = new ArrayList<>();
            for (int i = 0; i < length(); i++) {
                timePairs.add(get(i));
            }
            duration = foldTime(timePairs);
        }
        return new IsoDuration(period, duration);
    }

    // Left (date) branch: get(0)'s elements are themselves [num,unit] pairs. Right (time-only) branch:
    // get(0) IS a [num,unit] pair, so its first element is the number String.
    private static boolean isDateSection(Object arg) {
        if (arg instanceof Iterable<?> elements) {
            for (Object first : elements) {
                return first instanceof Iterable<?>;
            }
        }
        return false;
    }

    private static Period foldDate(Iterable<?> pairNodes) {
        Period period = Period.ZERO;
        int    last   = -1;
        for (Object pairNode : pairNodes) {
            List<String> pair   = pairOf(pairNode);
            int          amount = Integer.parseInt(pair.get(0));
            String       unit   = pair.get(1);
            last = checkOrder(unit, DATE_ORDER.indexOf(unit), last);
            period = switch (unit) {
                case "Y" -> period.plusYears(amount);
                case "M" -> period.plusMonths(amount);
                case "W" -> period.plusDays(7L * amount);
                case "D" -> period.plusDays(amount);
                default -> throw new DateTimeException("unknown date unit " + unit);
            };
        }
        return period;
    }

    private static Duration foldTime(Iterable<?> pairNodes) {
        Duration duration = Duration.ZERO;
        int      last     = -1;
        for (Object pairNode : pairNodes) {
            List<String> pair   = pairOf(pairNode);
            int          amount = Integer.parseInt(pair.get(0));
            String       unit   = pair.get(1);
            last = checkOrder(unit, TIME_ORDER.indexOf(unit), last);
            duration = switch (unit) {
                case "H" -> duration.plusHours(amount);
                case "M" -> duration.plusMinutes(amount);
                case "S" -> duration.plusSeconds(amount);
                default -> throw new DateTimeException("unknown time unit " + unit);
            };
        }
        return duration;
    }

    // Each unit must advance strictly along its canonical order, so repeats (== last) and out-of-order
    // units (< last) are both rejected.
    private static int checkOrder(String unit, int position, int last) {
        if (position == last) {
            throw new DateTimeException("duplicate unit " + unit);
        }
        if (position < last) {
            throw new DateTimeException("unit " + unit + " out of order");
        }
        return position;
    }

    // A [num,unit] pair arrives as a 2-element iterable of Strings.
    private static List<String> pairOf(Object pairNode) {
        List<String> pair = new ArrayList<>();
        if (pairNode instanceof Iterable<?> elements) {
            for (Object element : elements) {
                pair.add((String) element);
            }
        }
        return pair;
    }

}
