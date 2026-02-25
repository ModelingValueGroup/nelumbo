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

package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.lsp.Main;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentHoverService extends DocumentServiceAdapter {
    public DocumentHoverService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        Position pos   = params.getPosition();
        Token    token = document.tokenAt(pos);
        if (token == null) {
            return CompletableFuture.completedFuture(null);
        }
        StringBuilder text = new StringBuilder();

        if (Main.debugging()) {
            List<Node> debugNodes = document.nodesAt(pos);
            U.DEBUG("    hover [%s]:", U.render(pos));
            text.append(String.format("under [%s]:", U.render(pos)));
            U.DEBUG("     - token %s", token);
            text.append(String.format("\n<br> - [%s] TOKEN %s", U.renderSpan(token), U.escapeMarkdown(token.textTraced())));
            if (debugNodes.isEmpty()) {
                U.DEBUG("        <no nodes found>");
                text.append("\n<br> - <i>no nodes found</i>");
            } else {
                for (Node node : debugNodes) {
                    U.DEBUG("        - [%s} %s", U.renderSpan(node), node);
                    text.append(String.format("\n<br> - [%s] NODE %s", U.renderSpan(node), U.escapeMarkdown(node.toString())));
                }
            }
            text.append("\n\n---\n\n");
        }

        TokenType colorType = token.colorType();
        switch (colorType) {
            case TYPE -> {
                Node node = token.getNode();
                if (node instanceof Type type) {
                    text.append("**type** `").append(U.escapeMarkdown(type.name())).append("`");
                    StringBuilder supersStr = new StringBuilder();
                    for (Type sup : type.supers()) {
                        if (!supersStr.isEmpty()) supersStr.append(", ");
                        supersStr.append("`").append(U.escapeMarkdown(sup.name())).append("`");
                    }
                    if (!supersStr.isEmpty()) {
                        text.append("\n\nSupertypes: ").append(supersStr);
                    }
                } else {
                    text.append("**type** `").append(U.escapeMarkdown(token.text())).append("`");
                }
            }
            case VARIABLE -> {
                Variable var = token.variable();
                if (var != null) {
                    text.append("**variable** `").append(U.escapeMarkdown(var.name()));
                    text.append("` : `").append(U.escapeMarkdown(var.type().name())).append("`");
                } else {
                    text.append("`").append(U.escapeMarkdown(token.text())).append("`");
                }
            }
            case KEYWORD -> {
                text.append("**keyword** `").append(U.escapeMarkdown(token.text())).append("`");
            }
            default -> {
                text.append("`").append(U.escapeMarkdown(token.text())).append("`");
                List<Node> nodes = document.nodesAt(pos);
                if (!nodes.isEmpty()) {
                    Node node = nodes.getFirst();
                    text.append(" \u2014 ").append(node.type().name());
                    if (node.functor() != null) {
                        text.append(" `").append(U.escapeMarkdown(node.functor().name())).append("`");
                    }
                }
            }
        }

        MarkupContent mc    = new MarkupContent(MarkupKind.MARKDOWN, text.toString());
        Range         range = U.range(token);
        Hover         hover = new Hover(mc, range);
        return CompletableFuture.completedFuture(hover);
    }
}
