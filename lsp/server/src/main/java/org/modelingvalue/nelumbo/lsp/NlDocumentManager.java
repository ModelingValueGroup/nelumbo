package org.modelingvalue.nelumbo.lsp;

import java.util.concurrent.ConcurrentHashMap;

public class NlDocumentManager {
    private final Workspace                             workspace;
    private final ConcurrentHashMap<String, NlDocument> documentCache = new ConcurrentHashMap<>();

    public NlDocumentManager(Workspace workspace) {
        this.workspace = workspace;
    }

    public Workspace workspace() {
        return workspace;
    }

    public void addDocument(String uri, String content, int version) {
        documentCache.put(uri, NlDocument.of(content, version, uri));
    }

    public void updateDocument(String uri, String content) {
        NlDocument document = getDocument(uri);
        if (document != null) {
            documentCache.put(uri, NlDocument.of(document, content));
        }
    }

    public NlDocument getDocument(String uri) {
        return documentCache.get(uri);
    }

    public void closeDocument(String uri) {
        documentCache.remove(uri);
    }
}
