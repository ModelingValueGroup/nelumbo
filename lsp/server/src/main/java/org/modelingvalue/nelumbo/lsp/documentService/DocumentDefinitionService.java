package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.syntax.Token;

public class DocumentDefinitionService extends DocumentServiceAdapter {

    public DocumentDefinitionService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        Token t  = document.tokenAt(params.getPosition());
        Token tt = document.next(document.next(t));
        if (tt == null) {
            return CompletableFuture.completedFuture(null);
        }
        List<Location> l = List.of(new Location(params.getTextDocument().getUri(), U.range(tt)));
        return CompletableFuture.completedFuture(Either.forLeft(l));
    }
}
