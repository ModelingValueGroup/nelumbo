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

package org.modelingvalue.nelumbo.lsp.intellij.lang;

import java.util.EnumMap;
import java.util.Map;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.modelingvalue.nelumbo.lsp.intellij.Constants;

/**
 * One IntelliJ {@link IElementType} per nelumbo {@link org.modelingvalue.nelumbo.syntax.TokenType}.
 * Layout tokens (HSPACE/NEWLINE/BEGINOFFILE/ENDOFFILE) are mapped onto IntelliJ's built-in
 * {@link TokenType#WHITE_SPACE}; ERROR tokens onto {@link TokenType#BAD_CHARACTER}.
 */
public final class NelumboTokenType extends IElementType {
    private static final Map<org.modelingvalue.nelumbo.syntax.TokenType, IElementType>                FORWARD = new EnumMap<>(org.modelingvalue.nelumbo.syntax.TokenType.class);
    private static final java.util.IdentityHashMap<IElementType, org.modelingvalue.nelumbo.syntax.TokenType> REVERSE = new java.util.IdentityHashMap<>();

    static {
        for (org.modelingvalue.nelumbo.syntax.TokenType t : org.modelingvalue.nelumbo.syntax.TokenType.values()) {
            IElementType mapped = mapOne(t);
            FORWARD.put(t, mapped);
            if (mapped instanceof NelumboTokenType) {
                REVERSE.put(mapped, t);
            }
        }
    }

    public NelumboTokenType(@NonNls @NotNull String debugName) {
        super(debugName, Constants.NELUMBO);
    }

    public static @NotNull IElementType forNelumbo(@NotNull org.modelingvalue.nelumbo.syntax.TokenType type) {
        IElementType mapped = FORWARD.get(type);
        return mapped != null ? mapped : TokenType.BAD_CHARACTER;
    }

    /** Reverse of {@link #forNelumbo}: returns null for built-in IntelliJ types (WHITE_SPACE etc.). */
    public static org.modelingvalue.nelumbo.syntax.TokenType toNelumbo(IElementType type) {
        return REVERSE.get(type);
    }

    private static IElementType mapOne(org.modelingvalue.nelumbo.syntax.TokenType t) {
        if (t == org.modelingvalue.nelumbo.syntax.TokenType.ERROR) {
            return TokenType.BAD_CHARACTER;
        }
        if (t.isLayout()) {
            return TokenType.WHITE_SPACE;
        }
        return new NelumboTokenType("NL_" + t.name());
    }
}
