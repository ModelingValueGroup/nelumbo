package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.lsp.CommandType;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentCodeActionService extends DocumentServiceAdapter {
    public DocumentCodeActionService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        Token t = document.tokenAt(params.getRange().getStart());
        if (t == null || t.type() != TokenType.OPERATOR || !t.text().equals("?")) {
            return CompletableFuture.completedFuture(null);
        }
        List<Either<Command, CodeAction>> actions = List.of(Either.forLeft(CommandType.DEMO_COMMAND.command()));
        return CompletableFuture.completedFuture(actions);
    }
}
