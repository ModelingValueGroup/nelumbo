//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.lsp;

import java.util.Arrays;
import java.util.List;
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
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private       ClientCapabilities capabilities;

    public Workspace getWorkspace() {
        return workspace;
    }

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

    @SuppressWarnings("RedundantStringFormatCall")
    private SemanticTokensWithRegistrationOptions makeSemanticTokensCapabilities() {
        List<String> tokenTypes     = LspTokenMapping.lspTypes();
        List<String> tokenModifiers = LspTokenMapping.lspModifiers();
        if (Main.debugging()) {
            System.err.println("LSP TOKEN TYPES:");
            for (int i = 0; i < tokenTypes.size(); i++) {
                String n = tokenTypes.get(i);
                System.err.println(java.lang.String.format("   [%2d] %s", i, n));
            }
            System.err.println("LSP TOKEN MODIFIERS:");
            for (int i = 0; i < tokenModifiers.size(); i++) {
                String n = tokenModifiers.get(i);
                System.err.println(java.lang.String.format("   [%2d] %s", i, n));
            }
        }

        SemanticTokensLegend legend = new SemanticTokensLegend(tokenTypes, tokenModifiers);
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
