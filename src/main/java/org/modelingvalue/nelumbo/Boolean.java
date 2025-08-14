//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo;

import java.io.Serial;

import org.modelingvalue.collections.Map;
import org.modelingvalue.nelumbo.syntax.Token;

public final class Boolean extends Predicate {
    @Serial
    private static final long        serialVersionUID = -8515171118744898263L;
    //
    public static        Boolean     TRUE;
    public static        Boolean     FALSE;
    public static        Boolean     UNKNOWN;
    //
    private              InferResult result;

    public Boolean(Functor functor, Token[] tokens, Object[] args) {
        super(functor, tokens, parse((String) args[0]));
        if (TRUE == null && isTrue()) {
            TRUE = this;
        } else if (FALSE == null && isFalse()) {
            FALSE = this;
        } else if (UNKNOWN == null && isUnknown()) {
            UNKNOWN = this;
        }
    }

    private static java.lang.Boolean parse(String arg) {
        return "true".equalsIgnoreCase(arg) ? java.lang.Boolean.TRUE : //
               "false".equalsIgnoreCase(arg) ? java.lang.Boolean.FALSE : null;
    }

    private Boolean(Object[] args, int start, Boolean declaration) {
        super(args, start, declaration);
    }

    @Override
    protected void init(Predicate parent, int idx) {
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
    protected Boolean struct(Object[] array, int start, Predicate declaration) {
        return new Boolean(array, start, (Boolean) declaration);
    }

    @Override
    protected InferResult resolve(InferContext context) {
        return infer(context);
    }

    @Override
    protected InferResult infer(InferContext context) {
        if (result == null) {
            result = isTrue() ? factCC() : isFalse() ? falsehoodCC() : unknown();
        }
        return result;
    }

    @Override
    public Map<Variable, Object> getBinding() {
        return Map.of();
    }

    @Override
    public Boolean set(int i, Object... a) {
        return (Boolean) super.set(i, a);
    }

    @Override
    public String toString() {
        return isUnknown() ? "unknown" : toString(0);
    }

}
