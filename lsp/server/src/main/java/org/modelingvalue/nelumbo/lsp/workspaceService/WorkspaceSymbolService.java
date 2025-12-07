package org.modelingvalue.nelumbo.lsp.workspaceService;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.lsp.Workspace;

public class WorkspaceSymbolService extends WorkspaceServiceAdapter {

    private final List<WorkspaceSymbol> workspaceSymbols = new ArrayList<>();

    public WorkspaceSymbolService(Workspace workspace) {
        super(workspace);
    }

    @Override
    public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params) {
        workspaceSymbols.clear();

        for (String folder : getWorkspace().getFolders()) {
            for (Path projectDir : U.findProjects(Paths.get(URI.create(folder)))) {
                for (String source : Arrays.asList("main", "test")) {
                    System.err.println("    search " + source + " for " + params.getQuery());
                }
            }
        }

        return CompletableFuture.completedFuture(Either.forRight(workspaceSymbols));
    }
}
