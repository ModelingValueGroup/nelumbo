package org.modelingvalue.nelumbo.lsp.documentService;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.Workspace;

public abstract class DocumentServiceAdapter implements TextDocumentService {
    protected final NlDocumentManager documentManager;

    public DocumentServiceAdapter(NlDocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    public Workspace workspace() {
        return documentManager.workspace();
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        // Empty default implementation
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        // Empty default implementation
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        // Empty default implementation
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // Empty default implementation
    }
}
