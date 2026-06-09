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
import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

// Time	::= <NUMBER> : <NUMBER> <(> : <NUMBER> <)?>
public final class NTime extends Node {
    @Serial
    private static final long serialVersionUID = 9221816666027178738L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    @NelumboConstructor
    public NTime(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public static NTime of(Object value) {
        return new NTime(NodeInfo.of(FUNCTOR), value);
    }

    @Override
    public String toString(TokenType[] previous) {
        Object value = getVal(0);
        return String.valueOf(value);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing && get(0) instanceof String) {
            try {
                return setArgs(build());
            } catch (DateTimeException | NumberFormatException e) {
                knowledgeBase.addException(new ParseException("Invalid ISO 8601 time: " + e.getMessage(), this));
            }
        }
        return this;
    }

    private Object build() {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < length(); i++) {
            flatten(get(i), parts);
        }
        int hour = Integer.parseInt(parts.get(0));
        int minute = Integer.parseInt(parts.get(1));
        int second = parts.size() > 2 ? Integer.parseInt(parts.get(2)) : 0;
        int nano = parts.size() > 3 ? nanosOf(parts.get(3)) : 0;
        return LocalTime.of(hour, minute, second, nano);
    }

    // ".500" captures the digit string "500"; its value scales by the number of
    // digits: 3 digits -> *10^6.
    private static int nanosOf(String fraction) {
        long value = Long.parseLong(fraction);
        int scale = 9 - fraction.length();
        for (; scale > 0; scale--) {
            value *= 10;
        }
        for (; scale < 0; scale++) {
            value /= 10;
        }
        return (int) value;
    }

    static void flatten(Object node, List<String> out) {
        if (node instanceof String number) {
            out.add(number);
        } else if (node instanceof Optional<?> optional) {
            optional.ifPresent(value -> flatten(value, out));
        } else if (node instanceof Iterable<?> list) {
            list.forEach(element -> flatten(element, out));
        }
    }

}
