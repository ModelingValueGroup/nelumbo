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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.*;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

import java.io.Serial;
import java.time.*;

// DateTime ::= <[> <Date> T <Time#50> <(> <(> Z <|> <(> <(> + <|> - <)> <NUMBER> : <NUMBER> <)> <)> <)?> <]>
public final class NDateTime extends Node {
    @Serial
    private static final long serialVersionUID = 6807816666027178736L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    private static Functor FUNCTOR_LITERAL;

    @NelumboConstructor
    public NDateTime(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
        // TODO: Make it nicer
        if (FUNCTOR == null) {
            FUNCTOR = functor();
        }
        if (FUNCTOR_LITERAL == null) {
            FUNCTOR_LITERAL = FUNCTOR.setResultType(FUNCTOR.resultType().toLiteral());
        }
    }

    public static NDateTime of(Object value) {
        return new NDateTime(NodeInfo.of(FUNCTOR_LITERAL), value);
    }

    @Override
    public String toString(TokenType[] previous) {
        Object value = getVal(0);
        return String.valueOf(value);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing && get(0) instanceof Node) {
            LocalDate date = getVal(0, 0);
            LocalTime time = getVal(1, 0);

            if (get(2) == null) {
                return setArgs(new Object[]{LocalDateTime.of(date, time)}).setFunctorOrType(FUNCTOR_LITERAL);
            }

            try {
                return setArgs(new Object[]{OffsetDateTime.of(date, time, foldTimezone())}).setFunctorOrType(FUNCTOR_LITERAL);
            } catch (DateTimeException e) {
                knowledgeBase.addException(new ParseException("Invalid timezone offset: " + e.getMessage()));
            }
        }

        return this;
    }

    private ZoneOffset foldTimezone() {
        Object offset = get(2);
        if (offset instanceof String) {
            return ZoneOffset.of((String) offset);
        }
        if (offset instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object part : list) {
                if (!(part instanceof CharSequence cs)) {
                    throw new DateTimeException("Unexpected offset element: " + part);
                }
                sb.append(cs);
            }
            return ZoneOffset.of(sb.toString());
        }
        throw new DateTimeException("Unexpected offset value: " + offset);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NDateTime date && date.getVal(0) instanceof OffsetDateTime odt
                && getVal(0) instanceof OffsetDateTime val) {
            return odt.isEqual(val);
        }

        return super.equals(obj);
    }
}
