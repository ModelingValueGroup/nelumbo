package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.List;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;

public class DocumentSyncService extends DocumentServiceAdapter {
    public DocumentSyncService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem textDocument = params.getTextDocument();
        add(textDocument.getText(), textDocument.getUri(), textDocument.getVersion());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        TextDocumentIdentifier               textDocument   = params.getTextDocument();
        List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
        if (!contentChanges.isEmpty()) {
            if (1 < contentChanges.size()) {
                System.err.println("    multiple content changes not supported: " + contentChanges.size());
            }
            update(contentChanges.getFirst().getText(), textDocument.getUri());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        TextDocumentIdentifier textDocument = params.getTextDocument();
        documentManager.closeDocument(textDocument.getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        TextDocumentIdentifier textDocument = params.getTextDocument();
        update(params.getText(), textDocument.getUri());
    }

    private void add(String content, String uri, int version) {
        if (content != null && !content.isBlank()) {
            documentManager.addDocument(uri, content, version);
        }
    }

    private void update(String content, String uri) {
        if (content != null && !content.isBlank()) {
            documentManager.updateDocument(uri, content);
        }
    }
}
