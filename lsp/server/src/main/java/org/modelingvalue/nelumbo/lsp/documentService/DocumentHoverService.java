package org.modelingvalue.nelumbo.lsp.documentService;

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
        Node          node  = document.nodeAt(pos);
        String        text  = U.escapeMarkdown(token.toString()) + "<br>" + (node == null ? "_no node_" : U.escapeMarkdown(node.toString()));
        MarkupContent mc    = new MarkupContent(MarkupKind.MARKDOWN, text);
        Range         range = U.range(token);
        Hover         hover = new Hover(mc, range);
        System.err.println("    hover " + U.render(pos) + ": token=" + token + ", node=" + node);
        return CompletableFuture.completedFuture(hover);
    }
}
