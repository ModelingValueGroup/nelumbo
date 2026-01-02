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

package org.modelingvalue.nelumbo.logic;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class Boolean extends Predicate {
    @Serial
    private static final long serialVersionUID = -8515171118744898263L;
    //
    public static Boolean     TRUE;
    public static Boolean     FALSE;
    public static Boolean     UNKNOWN;
    //
    private InferResult       result;

    public Boolean(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, parse(functor.name()));
        if (TRUE == null && isTrue()) {
            TRUE = this;
        } else if (FALSE == null && isFalse()) {
            FALSE = this;
        } else if (UNKNOWN == null && isUnknown()) {
            UNKNOWN = this;
        }
    }

    @Override
    public List<Object> args() {
        return List.of();
    }

    private static java.lang.Boolean parse(String arg) {
        return "true".equalsIgnoreCase(arg) ? java.lang.Boolean.TRUE : //
                "false".equalsIgnoreCase(arg) ? java.lang.Boolean.FALSE : null;
    }

    private Boolean(Object[] args, Boolean declaration) {
        super(args, declaration);
    }

    public boolean isTrue() {
        java.lang.Boolean b = (java.lang.Boolean) get(0);
        return b != null && b;
    }

    public boolean isFalse() {
        java.lang.Boolean b = (java.lang.Boolean) get(0);
        return b != null && !b;
    }

    public boolean isUnknown() {
        java.lang.Boolean b = (java.lang.Boolean) get(0);
        return b == null;
    }

    public InferResult result() {
        return infer(null);
    }

    @Override
    protected Boolean struct(Object[] array, Node declaration) {
        return new Boolean(array, (Boolean) declaration);
    }

    @Override
    public InferResult resolve(InferContext context) {
        return infer(context);
    }

    @Override
    protected InferResult infer(InferContext context) {
        if (context != null && context.shallow()) {
            return unresolvable();
        }
        if (result == null) {
            result = isTrue() ? factCC() : isFalse() ? falsehoodCC() : unknown();
        }
        return result;
    }

    @Override
    public Boolean set(int i, Object... a) {
        return (Boolean) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        String string = isUnknown() ? "unknown" : toString(0);
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER || previous[0] == TokenType.DECIMAL) {
            previous[0] = TokenType.NAME;
            return " " + string;
        }
        previous[0] = TokenType.NAME;
        return string;
    }

}
