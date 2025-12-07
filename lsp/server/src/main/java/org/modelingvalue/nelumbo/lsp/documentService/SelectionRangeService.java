package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SelectionRangeParams;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;

public class SelectionRangeService extends DocumentServiceAdapter {
    public SelectionRangeService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        List<SelectionRange> ranges = params.getPositions().stream().map(p -> makeSelectionRange(document, p)).toList();
        ranges.forEach(sr -> System.err.print("   >> " + U.render(sr)));
        return CompletableFuture.completedFuture(ranges);
    }

    private static SelectionRange makeSelectionRange(NlDocument document, Position p) {
        SelectionRange sr = new SelectionRange(U.range(document.tokens()), null);

        List<Node> nodes = document.nodeList();
        while (true) {
            Node node = nodes.stream().filter(n -> U.positionInRange(p, n)).findFirst().orElse(null);
            if (node == null) {
                break;
            }
            Range nodeRange = U.range(node.tokens().toList());
            sr    = new SelectionRange(nodeRange, sr);
            nodes = node.astElements().filter(n -> n instanceof Node).map(n -> (Node) n).toList();
        }
        return sr;
    }

}
