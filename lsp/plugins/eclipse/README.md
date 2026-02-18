# Nelumbo LSP Eclipse Plugin

Eclipse IDE plugin for the Nelumbo language, providing syntax highlighting, code completion, hover info, go-to-definition, and more via the Language Server Protocol.

## Prerequisites

- Eclipse IDE 2025-12 or later (with LSP4e included)
- Java 21 or later

LSP4e is included in most Eclipse IDE packages (Java, Enterprise, etc.) since 2023-09. If your Eclipse does not include it, install it from the Eclipse Marketplace: search for "LSP4e".

## Building

Build the Eclipse plugin (includes the LSP server):

```sh
./gradlew :lsp:plugins:eclipse:jar
```

Build all plugins (Eclipse, IntelliJ, VS Code, Neovim):

```sh
./gradlew jar
```

The plugin JAR will be at:
```
lsp/plugins/eclipse/build/libs/eclipse-nelumbo-plugin-<version>.jar
```

If the Eclipse dropins directory exists at `~/Applications/java-2025-12/Eclipse.app/Contents/Eclipse/dropins`, the jar is automatically copied there.

Start Eclipse with `-clean` the first time to pick up the new plugin:

```sh
eclipse -clean
```

## Compile dependencies

All compile dependencies are in the `libs/` directory, copied from the target Eclipse installation. This ensures compile-time and runtime API versions match. To update them, copy the corresponding jars from `~/.p2/pool/plugins/`:

- `org.eclipse.lsp4e_*.jar`
- `org.eclipse.lsp4j_*.jar`
- `org.eclipse.lsp4j.jsonrpc_*.jar`
- `org.eclipse.jface.text_*.jar`
- `org.eclipse.text_*.jar`
- `org.eclipse.swt.*.jar` (any platform variant — only used for compilation)
- `org.eclipse.equinox.common_*.jar`
- `org.eclipse.core.resources_*.jar`

## Installation

### Option 1: dropins (recommended)

1. Build the plugin (see above)
2. Copy the JAR to your Eclipse `dropins/` folder:
   - macOS: `Eclipse.app/Contents/Eclipse/dropins/`
   - Linux: `<eclipse-install>/dropins/`
   - Windows: `<eclipse-install>\dropins\`
3. Restart Eclipse

### Option 2: manual install

1. In Eclipse, go to **Help > Install New Software...**
2. Click **Add... > Archive...**
3. Select the plugin JAR
4. Follow the wizard to install and restart

## Usage

After installation, open or create a file with the `.nl` extension. The Nelumbo language server will start automatically and provide:

- Semantic syntax highlighting
- Code completion
- Hover documentation
- Go to definition
- Document symbols
- Code formatting
- Code folding
- Code actions

## Uninstallation

- **dropins**: Delete the JAR from the `dropins/` folder and restart Eclipse
- **manual install**: Go to **Help > About Eclipse IDE > Installation Details**, select the plugin, and click **Uninstall**
