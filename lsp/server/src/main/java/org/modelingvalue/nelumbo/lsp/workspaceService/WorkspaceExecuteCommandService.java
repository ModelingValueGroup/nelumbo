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

package org.modelingvalue.nelumbo.lsp.workspaceService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.modelingvalue.nelumbo.lsp.CommandType;
import org.modelingvalue.nelumbo.lsp.Workspace;

public class WorkspaceExecuteCommandService extends WorkspaceServiceAdapter {

    public WorkspaceExecuteCommandService(Workspace workspace) {
        super(workspace);
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        CommandType command = CommandType.of(params.getCommand());
        switch (command) {
            case COMMAND_X -> execute_COMMAND_X(params.getArguments());
            case DEMO_COMMAND -> execute_DEMO_COMMAND(params.getArguments());
            default -> System.err.println("    execute command: " + params.getCommand() + " not implemented");
        }
        return CompletableFuture.completedFuture(null);
    }

    private static void execute_DEMO_COMMAND(List<?> args) {
        System.err.println("    execute demo command: " + args.stream().map(o -> o.getClass().getSimpleName() + ":" + o).toList());
    }

    private static void execute_COMMAND_X(List<?> args) {
        System.err.println("    execute X command: " + args.stream().map(o -> o.getClass().getSimpleName() + ":" + o).toList());
    }
}
