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
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class NBoolean extends Predicate {
    @Serial
    private static final long serialVersionUID = -8515171118744898263L;
    //
    public static NBoolean TRUE;
    public static NBoolean FALSE;
    public static NBoolean UNKNOWN;
    //
    private InferResult result;

    @NelumboConstructor
    public NBoolean(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected NBoolean set(NodeInfo nodeInfo, Object[] args) {
        return new NBoolean(nodeInfo, args);
    }

    @Override
    public List<Object> args() {
        return List.of();
    }

    private static Boolean parse(String arg) {
        return "true".equalsIgnoreCase(arg) ? Boolean.TRUE : //
                "false".equalsIgnoreCase(arg) ? Boolean.FALSE : null;
    }

    private Boolean getBoolean() {
        Object object = get(0);
        return object instanceof Boolean b ? b : null;
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
    public NBoolean set(int i, Object... a) {
        return (NBoolean) super.set(i, a);
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

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing && length() == 0) {
            Boolean bool = parse(functor().name());
            NBoolean result = set(nodeInfo().resetDeclaration(), new Object[] { bool });
            if (TRUE == null && result.isTrue()) {
                TRUE = result;
            } else if (FALSE == null && result.isFalse()) {
                FALSE = result;
            } else if (UNKNOWN == null && result.isUnknown()) {
                UNKNOWN = result;
            }
            return result;
        }
        return this;
    }

}
