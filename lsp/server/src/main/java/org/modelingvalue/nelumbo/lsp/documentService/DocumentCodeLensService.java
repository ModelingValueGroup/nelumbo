package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.modelingvalue.nelumbo.lsp.CommandType;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentCodeLensService extends DocumentServiceAdapter {

    public DocumentCodeLensService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        String     docUri   = params.getTextDocument().getUri();
        NlDocument document = documentManager.getDocument(docUri);
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        List<CodeLens> codeLenses = document.tokens().stream() //
                                            .filter(t -> t.type() == TokenType.OPERATOR && t.text().equals("?")) //
                                            .map(t -> {
                                                CodeLens cl = new CodeLens();
                                                cl.setRange(U.range(t));
                                                Command command = new Command();
                                                command.setCommand(CommandType.DEMO_COMMAND.commandId());
                                                command.setTitle("demo");
                                                command.setArguments(List.of(docUri, t.line(), t.position()));
                                                cl.setCommand(command);
                                                return cl;
                                            }).toList();
        return CompletableFuture.completedFuture(codeLenses);
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        return super.resolveCodeLens(unresolved);
    }
}
