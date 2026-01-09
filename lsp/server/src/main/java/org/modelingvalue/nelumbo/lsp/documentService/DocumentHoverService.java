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
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.syntax.Token;

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
        List<Node>    nodes = document.nodesAt(pos);
        StringBuilder text  = new StringBuilder();
        U.DEBUG("    hover [%s]:", U.render(pos));
        text.append(String.format("under [%s]:", U.render(pos)));
        U.DEBUG("     - token %s", token);
        text.append(String.format("\n<br> - [%s] TOKEN %s", U.renderSpan(token), U.escapeMarkdown(token.textTraced())));
        if (nodes.isEmpty()) {
            U.DEBUG("        <no nodes found>");
            text.append("\n<br> - <i>no nodes found</i>");
        } else {
            for (Node node : nodes) {
                U.DEBUG("        - [%s} %s", U.renderSpan(node), node);
                text.append(String.format("\n<br> - [%s] NODE %s", U.renderSpan(node), U.escapeMarkdown(node.toString())));
            }
        }

        MarkupContent mc    = new MarkupContent(MarkupKind.MARKDOWN, text.toString());
        Range         range = U.range(token);
        Hover         hover = new Hover(mc, range);
        return CompletableFuture.completedFuture(hover);
    }
}

