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
