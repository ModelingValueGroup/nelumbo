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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.usageView.UsageViewTypeLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.modelingvalue.nelumbo.lsp.intellij.Constants;

/**
 * Replaces LSP4IJ's generic "LSP Symbol" label in navigation tooltips and Find Usages
 * titles with a nelumbo-specific token kind ("type", "variable", "keyword", ...).
 * <p>
 * The navigation target is usually LSP4IJ's {@code LSPPsiElement} (a {@code FakePsiElement}
 * with no AST node), so we descend into the containing {@link com.intellij.psi.PsiFile} to
 * find the real leaf element at the LSP-targeted offset and read its element type from
 * there.
 */
public class NelumboElementDescriptionProvider implements ElementDescriptionProvider {

    @Override
    public @Nullable String getElementDescription(@NotNull PsiElement element, @NotNull ElementDescriptionLocation location) {
        if (!(location instanceof UsageViewTypeLocation)) {
            return null;
        }
        PsiFile file = element.getContainingFile();
        if (file == null || file.getLanguage() != Constants.NELUMBO) {
            return null;
        }
        IElementType type = elementTypeFor(element, file);
        if (type == null) {
            return null;
        }
        org.modelingvalue.nelumbo.syntax.TokenType nl = NelumboTokenType.toNelumbo(type);
        if (nl == null) {
            return null;
        }
        return switch (nl) {
            case TYPE -> "type";
            case VARIABLE -> "variable";
            case KEYWORD -> "keyword";
            case NAME -> "name";
            case NUMBER, DECIMAL -> "number";
            case STRING -> "string";
            case OPERATOR, META_OPERATOR -> "operator";
            case IN_LINE_COMMENT, END_LINE_COMMENT -> "comment";
            default -> null;
        };
    }

    private static @Nullable IElementType elementTypeFor(@NotNull PsiElement element, @NotNull PsiFile file) {
        ASTNode node = element.getNode();
        if (node != null) {
            return node.getElementType();
        }
        // Fall back: find the leaf at the LSP-targeted offset (LSPPsiElement has no AST node).
        TextRange range = element.getTextRange();
        if (range == null) {
            return null;
        }
        PsiElement leaf = file.findElementAt(range.getStartOffset());
        if (leaf == null || leaf.getNode() == null) {
            return null;
        }
        return leaf.getNode().getElementType();
    }
}
