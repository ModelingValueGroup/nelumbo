package org.modelingvalue.nelumbo.lsp.workspaceService;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.modelingvalue.nelumbo.lsp.Workspace;

public abstract class WorkspaceServiceAdapter implements WorkspaceService {
    private final Workspace workspace;

    public WorkspaceServiceAdapter(Workspace workspace) {
        this.workspace = workspace;
    }

    protected Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        // Empty default implementation
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        // Empty default implementation
    }
}
