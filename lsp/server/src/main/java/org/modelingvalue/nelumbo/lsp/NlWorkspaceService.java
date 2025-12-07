package org.modelingvalue.nelumbo.lsp;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.gson.JsonObject;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.modelingvalue.nelumbo.lsp.workspaceService.WorkspaceExecuteCommandService;
import org.modelingvalue.nelumbo.lsp.workspaceService.WorkspaceSymbolService;

public class NlWorkspaceService implements WorkspaceService {
    private final Workspace                      workspace;
    private final WorkspaceExecuteCommandService executeCommand;
    private final WorkspaceSymbolService         symbol;

    public NlWorkspaceService(Workspace workspace) {
        this.workspace      = workspace;
        this.executeCommand = new WorkspaceExecuteCommandService(workspace);
        this.symbol         = new WorkspaceSymbolService(workspace);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        System.err.println("~~~ didChangeConfiguration: " + params.getSettings());
        JsonObject settings = (JsonObject) params.getSettings();
        if (settings != null && !settings.isEmpty()) {
            try {
                ObjectMapper jacksonObjectMapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
                Setting      setting             = jacksonObjectMapper.readValue(settings.toString(), Setting.class);
                workspace.setSetting(setting);
                Path settingFile = U.getLocation(Main.class).getParent().resolve("settings.json");
                workspace.getSetting().save(settingFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        // Empty implementation
        System.err.println("~~~ didChangeWatchedFiles: " + params.getChanges());
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        System.err.println("~~~ executeCommand: " + params.getCommand() + "(" + params.getArguments() + ")");
        return executeCommand.executeCommand(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params) {
        System.err.println("~~~ symbol: " + params.getQuery());
        return symbol.symbol(params);
    }
}
