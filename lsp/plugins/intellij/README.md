# Nelumbo LSP IntelliJ Plugin

IntelliJ IDEA plugin for the Nelumbo language, providing syntax highlighting, compiler checking, code folding, formatting, go-to-definition, and symbol search via the Language Server Protocol.

The plugin bundles the Nelumbo LSP server (`server.jar`) and uses [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij) (Red Hat's LSP client for IntelliJ) to drive it.

## Prerequisites

- IntelliJ IDEA 2023.2 or later (build `232`–`262.*`)
- Java 21 or later (only needed to build; the plugin runs on the IDE's bundled runtime)
- [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij) — installed automatically as a plugin dependency when you install via Marketplace, or install manually from **Settings > Plugins > Marketplace**

## Building

Build the plugin distribution zip (the LSP server is built and embedded automatically):

```sh
./gradlew :lsp:plugins:intellij:build
```

The distribution zip will be at:

```
lsp/plugins/intellij/build/distributions/intellij-nelumbo-plugin-<version>.zip
```

To launch a sandboxed IDE with the plugin pre-installed for development:

```sh
./gradlew :lsp:plugins:intellij:runIde
```

Auto-reload is enabled in the sandbox, so rebuilding triggers a hot plugin reload without restarting the IDE.

## Installation

1. Build the plugin (see above), or download a release zip.
2. In IntelliJ, open **Settings > Plugins**.
3. Click the gear icon and choose **Install Plugin from Disk...**.
4. Select the `intellij-nelumbo-plugin-<version>.zip` file.
5. Restart the IDE when prompted.

LSP4IJ must be installed alongside the plugin; if it isn't already present, IntelliJ will prompt to install it from Marketplace.


## Usage

After installation, open or create a file with the `.nl` extension. LSP4IJ will start the bundled Nelumbo language server automatically and provide:

- Syntax highlighting
- Compiler checking (diagnostics)
- Code folding
- Code formatting
- Go to definition
- Find symbol

A **Nelumbo** status bar widget shows the server state. Settings live under **Settings > Tools > nelumbo**. To inspect the language server log, run the **View Logs** action (e.g. via **Find Action**: ⌘⇧A / Ctrl+Shift+A).

## Publishing

The Gradle build supports signing and publishing to the JetBrains Marketplace via the following environment variables:

- `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` — used by `signPlugin`
- `PUBLISH_TOKEN` — used by `publishPlugin`

## Uninstallation

Open **Settings > Plugins**, find **nelumbo** in the **Installed** tab, click the gear icon, and choose **Uninstall**. Restart the IDE.
