package org.modelingvalue.nelumbo.lsp.documentService;

import static org.eclipse.lsp4j.SymbolKind.Variable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentSymbolService extends DocumentServiceAdapter {

    public DocumentSymbolService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        List<Either<SymbolInformation, DocumentSymbol>> result = document.tokens().stream() //
                                                                         .filter(t -> t.type() == TokenType.NAME) //
                                                                         .map(t -> {
                                                                             DocumentSymbol docSym = new DocumentSymbol();
                                                                             docSym.setName(t.text());
                                                                             docSym.setKind(Variable);
                                                                             docSym.setRange(U.range(t));
                                                                             docSym.setSelectionRange(U.range(t));
                                                                             docSym.setDetail("hottentot");
                                                                             return Either.<SymbolInformation, DocumentSymbol>forRight(docSym);
                                                                         }) //
                                                                         .toList();
        return CompletableFuture.completedFuture(result);

    }
}
