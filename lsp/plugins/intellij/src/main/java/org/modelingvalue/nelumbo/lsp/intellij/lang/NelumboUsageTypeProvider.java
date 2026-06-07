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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;

/**
 * Maps each nelumbo token under the cursor to a human-readable usage label
 * ("type", "variable", "keyword", ...). IntelliJ uses this in Find Usages and
 * in the Cmd-hover navigation preview, replacing LSP4IJ's generic "LSP Symbol"
 * fallback.
 */
public class NelumboUsageTypeProvider implements UsageTypeProvider {
    private static final UsageType TYPE     = new UsageType(() -> "type");
    private static final UsageType VARIABLE = new UsageType(() -> "variable");
    private static final UsageType KEYWORD  = new UsageType(() -> "keyword");
    private static final UsageType NAME     = new UsageType(() -> "name");
    private static final UsageType NUMBER   = new UsageType(() -> "number");
    private static final UsageType STRING   = new UsageType(() -> "string");
    private static final UsageType OPERATOR = new UsageType(() -> "operator");
    private static final UsageType COMMENT  = new UsageType(() -> "comment");

    @Override
    public @Nullable UsageType getUsageType(@NotNull PsiElement element) {
        ASTNode node = element.getNode();
        if (node == null) {
            return null;
        }
        IElementType type = node.getElementType();
        org.modelingvalue.nelumbo.syntax.TokenType nl = NelumboTokenType.toNelumbo(type);
        if (nl == null) {
            return null;
        }
        return switch (nl) {
        case TYPE                              -> TYPE;
        case VARIABLE                          -> VARIABLE;
        case KEYWORD                           -> KEYWORD;
        case NAME                              -> NAME;
        case NUMBER                            -> NUMBER;
        case STRING                            -> STRING;
        case OPERATOR, META_OPERATOR           -> OPERATOR;
        case IN_LINE_COMMENT, END_LINE_COMMENT -> COMMENT;
        default                                -> null;
        };
    }
}
