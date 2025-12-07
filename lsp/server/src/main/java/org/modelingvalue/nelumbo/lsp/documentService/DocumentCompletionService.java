package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentCompletionService extends DocumentServiceAdapter {
    public DocumentCompletionService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        Position caretPos   = params.getPosition();
        Token    caretToken = document.tokenAt(caretPos);
        if (caretToken == null || caretToken.type() != TokenType.NAME) {
            return CompletableFuture.completedFuture(null);
        }
        String      caretText   = caretToken.text().substring(0, caretPos.getCharacter() - caretToken.position());
        Set<String> completions = new HashSet<>();
        List<CompletionItem> completionItems = document.tokens().stream() //
                                                       .filter(t -> t.type() == TokenType.NAME) //
                                                       .map(Token::text) //
                                                       .filter(text -> text.startsWith(caretText) && !completions.contains(text)) //
                                                       .map(text -> {
                                                           completions.add(text);
                                                           CompletionItem ci = new CompletionItem(text);
                                                           ci.setDetail("nelumbo");
                                                           return ci;
                                                       })//
                                                       .toList();
        return CompletableFuture.completedFuture(Either.forLeft(completionItems));
    }
}
