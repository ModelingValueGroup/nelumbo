package org.modelingvalue.nelumbo.lsp;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.CompletionItemOptions;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.DocumentSymbolOptions;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SetTraceParams;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.WorkspaceFoldersOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.modelingvalue.nelumbo.NelumboConstants;

public class NelumboLanguageServer implements LanguageServer {
    private final Workspace          workspace = new Workspace();
    private       ClientCapabilities capabilities;

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        capabilities = params.getCapabilities();

        if (params.getWorkspaceFolders() != null) {
            for (WorkspaceFolder folder : params.getWorkspaceFolders()) {
                workspace.getFolders().add(folder.getUri());
            }
        }
        workspace.resolve();

        ServerCapabilities serverCapabilities = new ServerCapabilities();
        serverCapabilities.setWorkspace(makeWorkspaceCapabilities());
        serverCapabilities.setExecuteCommandProvider(makeExecCommandCapabilities());
        serverCapabilities.setTextDocumentSync(makeDocSyncCapabilities());
        serverCapabilities.setSemanticTokensProvider(makeSemanticTokensCapabilities());
        serverCapabilities.setFoldingRangeProvider(true);
        serverCapabilities.setCompletionProvider(makeCompletionCapabilities());
        serverCapabilities.setDocumentSymbolProvider(makeDocSymbolCapabilities());
        serverCapabilities.setDocumentFormattingProvider(true);
        serverCapabilities.setHoverProvider(true);
        serverCapabilities.setDefinitionProvider(true);
        serverCapabilities.setWorkspaceSymbolProvider(true);
        serverCapabilities.setCodeLensProvider(makeCodeLensCapabilities());
        serverCapabilities.setCodeActionProvider(true);
        serverCapabilities.setTypeDefinitionProvider(true);
        serverCapabilities.setSelectionRangeProvider(true);

        return CompletableFuture.completedFuture(new InitializeResult(serverCapabilities));
    }

    private static WorkspaceServerCapabilities makeWorkspaceCapabilities() {
        WorkspaceFoldersOptions workspaceFoldersOptions = new WorkspaceFoldersOptions();
        workspaceFoldersOptions.setSupported(true);
        workspaceFoldersOptions.setChangeNotifications(true);
        return new WorkspaceServerCapabilities(workspaceFoldersOptions);
    }

    private static ExecuteCommandOptions makeExecCommandCapabilities() {
        return new ExecuteCommandOptions(CommandType.commandList());
    }

    private static TextDocumentSyncOptions makeDocSyncCapabilities() {
        TextDocumentSyncOptions textDocumentSyncOptions = new TextDocumentSyncOptions();
        textDocumentSyncOptions.setOpenClose(true);
        textDocumentSyncOptions.setChange(TextDocumentSyncKind.Full);
        textDocumentSyncOptions.setSave(new SaveOptions(true));
        return textDocumentSyncOptions;
    }

    private static SemanticTokensWithRegistrationOptions makeSemanticTokensCapabilities() {
        SemanticTokensLegend legend = new SemanticTokensLegend(SemanticMapping.allSemanticTypes(), SemanticMapping.allSemanticModifiers());
        return new SemanticTokensWithRegistrationOptions(legend, true);
    }

    private static CompletionOptions makeCompletionCapabilities() {
        CompletionOptions completionOptions = new CompletionOptions(null, Arrays.asList("*", "@"));
        completionOptions.setCompletionItem(new CompletionItemOptions(true));
        return completionOptions;
    }

    private static DocumentSymbolOptions makeDocSymbolCapabilities() {
        return new DocumentSymbolOptions(NelumboConstants.NAME);
    }

    private static CodeLensOptions makeCodeLensCapabilities() {
        return new CodeLensOptions(false);
    }

    //==========================================================================================================================================================
    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void exit() {
        // a clean and quick shutdown:
        System.err.println("~~~ exit");
        System.exit(0);
    }

    //==========================================================================================================
    @Override
    public TextDocumentService getTextDocumentService() {
        return new NlTextDocumentService(workspace);
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new NlWorkspaceService(workspace);
    }

    //==========================================================================================================
    @Override
    public void setTrace(SetTraceParams params) {
        Main.LOGGER.info("@@@ Trace: " + params.getValue());
    }
}
