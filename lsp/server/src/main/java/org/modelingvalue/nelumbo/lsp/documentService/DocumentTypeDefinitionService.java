package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentTypeDefinitionService extends DocumentServiceAdapter {
    public DocumentTypeDefinitionService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(TypeDefinitionParams params) {
        String     uri      = params.getTextDocument().getUri();
        NlDocument document = documentManager.getDocument(uri);
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        Token t = document.tokenAt(params.getPosition());
        if (t == null || t.type() != TokenType.TYPE) {
            return CompletableFuture.completedFuture(null);
        }
        String typeName = t.text();
        List<Location> locations = document.tokens().stream() //
                                           .filter(tt -> tt.type() == TokenType.TYPE && tt.text().equals(typeName)) //
                                           .map(tt -> new Location(uri, U.range(tt))) //
                                           .toList();
        return CompletableFuture.completedFuture(Either.forLeft(locations));
    }

}
