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
