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
import java.time.LocalDate;

// Date 	::= <NUMBER> - <NUMBER> - <NUMBER>
public final class NDate extends Node {
    @Serial
    private static final long serialVersionUID = 4471816666027178737L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    @NelumboConstructor
    public NDate(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public static NDate of(Object value) {
        return new NDate(NodeInfo.of(FUNCTOR), value);
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
                return setArgs(new Object[] { LocalDate.of(intAt(0), intAt(1), intAt(2)) });
            } catch (DateTimeException | NumberFormatException e) {
                knowledgeBase.addException(new ParseException("Invalid ISO 8601 date: " + e.getMessage(), this));
            }
        }
        return this;
    }

    private int intAt(int i) {
        return Integer.parseInt((String) get(i));
    }

}
