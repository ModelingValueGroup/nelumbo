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
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class NBoolean extends Predicate {
    @Serial
    private static final long serialVersionUID = -8515171118744898263L;
    //
    public static NBoolean    TRUE;
    public static NBoolean    FALSE;
    public static NBoolean    UNKNOWN;
    //
    private InferResult       result;

    public NBoolean(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, parse(functor.name()));
        if (TRUE == null && isTrue()) {
            TRUE = this;
        } else if (FALSE == null && isFalse()) {
            FALSE = this;
        } else if (UNKNOWN == null && isUnknown()) {
            UNKNOWN = this;
        }
    }

    public NBoolean(Variable var) {
        super(UNKNOWN.functor(), List.of(var), var);
    }

    @Override
    public List<Object> args() {
        return List.of();
    }

    private static Boolean parse(String arg) {
        return "true".equalsIgnoreCase(arg) ? Boolean.TRUE : //
                "false".equalsIgnoreCase(arg) ? Boolean.FALSE : null;
    }

    private NBoolean(Object[] args, NBoolean declaration) {
        super(args, declaration);
    }

    private Boolean getBoolean() {
        Object object = get(0);
        return object instanceof Boolean b ? b : null;
    }

    @Override
    public Variable variable() {
        Object object = get(0);
        if (object instanceof Variable v) {
            return v;
        }
        object = declaration().get(0);
        if (object instanceof Variable v) {
            return v;
        }
        return null;
    }

    public boolean isTrue() {
        Boolean b = getBoolean();
        return b != null && b;
    }

    public boolean isFalse() {
        Boolean b = getBoolean();
        return b != null && !b;
    }

    public boolean isUnknown() {
        Boolean b = getBoolean();
        return b == null;
    }

    public InferResult result() {
        return infer(null);
    }

    @Override
    protected NBoolean struct(Object[] array, Node declaration) {
        return new NBoolean(array, (NBoolean) declaration);
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
            Variable var = variable();
            if (var != null) {
                result = InferResult.of(this, Set.of(set(0, Boolean.TRUE)), true, //
                        Set.of(set(0, Boolean.FALSE)), true, Set.of());
            } else {
                result = isTrue() ? factCC() : isFalse() ? falsehoodCC() : unknown();
            }
        }
        return result;
    }

    @Override
    public NBoolean set(int i, Object... a) {
        return (NBoolean) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        Variable var = variable();
        String string = var != null ? var.name() : isUnknown() ? "unknown" : toString(0);
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER || previous[0] == TokenType.DECIMAL) {
            previous[0] = TokenType.NAME;
            return " " + string;
        }
        previous[0] = TokenType.NAME;
        return string;
    }

}
