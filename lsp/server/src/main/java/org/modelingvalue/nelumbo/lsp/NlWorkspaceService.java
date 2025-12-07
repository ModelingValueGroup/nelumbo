//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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
