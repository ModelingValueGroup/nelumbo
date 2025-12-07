import * as path from "path";
import * as fs from "fs";
import * as os from "os";
import {commands, ExtensionContext, languages, QuickPickItem, window,} from "vscode";

import {LanguageClient, LanguageClientOptions, ServerOptions, TransportKind,} from "vscode-languageclient/node";

const serverJar = path.join(os.homedir(), "nelumbo-lsp", "server.jar");
const embedJar = path.join(__dirname, "server.jar");
let client: LanguageClient;

export function activate(context: ExtensionContext) {
  if (fs.existsSync(serverJar)) {
    try {
      fs.unlinkSync(serverJar);
    } catch (e) {}
  }

  fs.mkdirSync(path.join(os.homedir(), "nelumbo-lsp"), { recursive: true });
  fs.copyFileSync(embedJar, serverJar);

  const serverOptions: ServerOptions = {
    command: "java",
    args: ["-cp", serverJar, "org.modelingvalue.nelumbo.lsp.Main"],
    transport: TransportKind.stdio,
  };

  const clientOptions: LanguageClientOptions = {
    documentSelector: [{ scheme: "file", pattern: "**/*.nl" }],
  };

  client = new LanguageClient(
      "nelumbo Language Server",
      "nelumbo Language Server",
    serverOptions,
    clientOptions
  );

  client.start();

  const commandChooseOption = "nelumbo.chooseOption";

  const languageStatusItem = languages.createLanguageStatusItem(
    "nelumbo.languageStatus",
      {language: "nelumbo"}
  );
    languageStatusItem.text = "nelumbo";
  languageStatusItem.command = {
    title: "Choose Option",
    command: commandChooseOption,
  };
  context.subscriptions.push(languageStatusItem);
  const chooseOptionCommand = commands.registerCommand(
    commandChooseOption,
    async () => {
      const viewLogs: QuickPickItem = {
        label: "$(output-view-icon)View Logs",
        description: "View the logs of the language server",
      };

      const resolveDependencies: QuickPickItem = {
        label: "$(debug-restart)Resolve Dependencies",
        description: "Resolve the dependencies of the language server",
      };

      const option = await window.showQuickPick([
        viewLogs,
        resolveDependencies,
      ]);

      if (option == viewLogs) {
        client.outputChannel.show();
      } else if (option == resolveDependencies) {
        client.sendRequest("workspace/executeCommand", {
          command: "nelumbo.resolveDependencies",
        });
      }
    }
  );
  context.subscriptions.push(chooseOptionCommand);
}

export function deactivate(): Thenable<void> | undefined {
  if (!client) {
    return undefined;
  }
  return client.stop();
}
