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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// DateTime ::= <[> <Date> T <Time#50> <]>
public final class NDateTime extends Node {
    @Serial
    private static final long serialVersionUID = 6807816666027178736L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    private static Functor FUNCTOR_LITERAL;

    private static Functor literalFunctor() {
        if (FUNCTOR_LITERAL == null) {
            FUNCTOR_LITERAL = FUNCTOR.setResultType(FUNCTOR.resultType().toLiteral());
        }
        return FUNCTOR_LITERAL;
    }

    @NelumboConstructor
    public NDateTime(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public static NDateTime of(Object value) {
        return new NDateTime(NodeInfo.of(literalFunctor()), value);
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

            return setArgs(new Object[]{LocalDateTime.of(date, time)}).setFunctorOrType(literalFunctor());
        }

        return this;
    }
}
