# Nelumbo LSP Eclipse Plugin

Eclipse IDE plugin for the Nelumbo language, providing syntax highlighting, code completion, hover info, go-to-definition, and more via the Language Server Protocol.

## Prerequisites

- Eclipse IDE 2023-09 or later (with LSP4e included)
- Java 21 or later

LSP4e is included in most Eclipse IDE packages (Java, Enterprise, etc.) since 2023-09. If your Eclipse does not include it, install it from the Eclipse Marketplace: search for "LSP4e".

## Building

From the project root:

```sh
./gradlew :lsp:plugins:eclipse:jar
```

The plugin JAR will be at:
```
lsp/plugins/eclipse/build/libs/eclipse-nelumbo-plugin-<version>.jar
```

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
