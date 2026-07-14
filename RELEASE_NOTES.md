This release delivers the following artifacts. All jars require Java 21 or higher.

## nelumbo-server-${version-num}.jar - lean executor

The smallest way to run **nelumbo** specifications: a self-contained HTTP server (~2 MB) that
loads `.nl` files into a knowledge base and evaluates posted documents against it. No UI, no
editor support - just execution. It runs on the JDK's built-in HTTP server and has no
third-party dependencies.

- run `java -jar nelumbo-server-${version-num}.jar [--port N] [--timeout MS] [<file-or-dir>...]`,
  or double-click the jar: a small status window shows the URL with Open in Browser / Stop buttons
- `POST /eval` evaluates a posted **nelumbo** document (raw text, or a JSON envelope
  `{"document": "...", "limit": N}`) and returns the query results as JSON
- `POST /eval/trace` is `/eval` with a (currently stubbed) trace field
- `GET /metadata` describes the loaded knowledge base (types, functors, rules, facts)
- `GET /health` is a liveness check

## nelumbo-http-server-${version-num}.jar - website server

The full **nelumbo** website server, as it runs on [nelumbo.nl](https://nelumbo.nl): the same
REST endpoints as the lean executor, plus an LSP editor service over WebSocket at `/lsp` and
the public pages (landing page, feature tour, and playground with browser-based editors).

- run `java -jar nelumbo-http-server-${version-num}.jar [--port N] [<file-or-dir>...]`,
  or double-click the jar: a small status window shows the URL with Open in Browser / Stop buttons

## nelumbo-${version-num}-editor.jar - standalone editor

A simple desktop editor for **nelumbo** specifications; query results are calculated and shown
on the fly. This editor is not an LSP-based editor, and useful for educational and demo purposes.

- run `java -jar nelumbo-${version-num}-editor.jar` (or double-click the jar)

Always start a **nelumbo** specification with an import-statement. For example:

```
import    nelumbo.strings

String  a

"foo"+"bar"=a  ?    [(a="foobar")][..]
```

## nelumbo-mcp-server-${version-num}.jar - MCP server

A [Model Context Protocol](https://modelcontextprotocol.io) stdio server with LLM authoring
tools for `.nl` decision models (evaluate, search docs, get examples, new model skeleton).

- register it with your MCP client, e.g. `claude mcp add nelumbo -- java -jar nelumbo-mcp-server-${version-num}.jar`
  (double-clicking the jar shows these registration instructions)

## IDE plugins

- `intellij-nelumbo-plugin-${version-num}.zip` - IntelliJ IDEA plugin (LSP-based); install via
  Settings > Plugins > Install Plugin from Disk
- `eclipse-nelumbo-plugin-${version-num}.jar` - Eclipse plugin; drop it into the `dropins`
  folder of your Eclipse installation

## nelumbo-slides.zip

The **nelumbo** presentation slides as a static website; unzip and open `index.html`.
