package org.modelingvalue.nelumbo.lsp;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.Command;

public enum CommandType {
    COMMAND_X, // not a real command, to be used later
    DEMO_COMMAND,
    ;

    private static final String  COMMAND_PRE = "nelumbo.";
    private final        String  commandId;
    private final        String  commandTitle;
    private final        Command command;

    CommandType() {
        this.commandId    = COMMAND_PRE + name().toLowerCase();
        this.commandTitle = name().toLowerCase()//
                                  .replaceAll("_", " ")//
                                  .replaceAll(" command ", " ")//
                                  .replaceAll("^command ", "")//
                                  .replaceAll(" command$", "");
        this.command      = new Command(commandTitle, commandId, List.of());
    }

    public static CommandType of(String commandId) {
        return CommandType.valueOf(commandId.replaceAll("^" + Pattern.quote(COMMAND_PRE), "").toUpperCase());
    }

    public String commandId() {
        return commandId;
    }

    public String commandTitle() {
        return commandTitle;
    }

    public Command command() {
        return command;
    }

    public Command command(List<Object> args) {
        return new Command(commandId, commandTitle, args);
    }

    public static List<String> commandList() {
        return Arrays.stream(CommandType.values()).map(CommandType::commandId).toList();
    }
}
