This release delivers the following artifacts. All jars require Java 21 or higher.

## nelumbo-cli-${version-num}.jar - command-line runner and eval server

Parses and evaluates `.nl` files from the terminal, printing each query together with its
inferred result. Queries that declare expected results are compared and mismatches are
reported as errors. Exit codes: `0` success, `1` parse/evaluation/comparison errors,
`2` usage error - suitable for scripting and CI. Self-contained (~2 MB, no third-party
dependencies, HTTP served by the JDK's built-in server).

- run `java -jar nelumbo-cli-${version-num}.jar [options] <file>...` (pass `-` to read stdin,
  `-n '<src>'` to evaluate source given on the command line, `-p logic,integers,...|all` to preload stdlib modules, `-j` for JSON output including
  the parse tree, `-q` to suppress query output, `-h` for all options)
- with `--server <port>` the inputs are instead loaded into a knowledge base that is served
  over HTTP: an info/try-it page at `/`, `POST /eval` (raw text or a JSON envelope
  `{"document": "...", "limit": N, "stdlib": bool}`) returning query results and parse tree,
  `POST /eval/trace`, `GET /metadata`, `GET /examples`, and `GET /health`
- or double-click the jar: an interactive window opens with an editable Nelumbo example, Run
  and output/json tabs, a server tab (start/stop the HTTP server, request counter), and the
  command-line usage as documentation

## nelumbo-web-server-${version-num}.jar - website server

The full **nelumbo** website server, as it runs on [nelumbo.nl](https://nelumbo.nl): the same
REST endpoints as the cli's `--server` mode, plus an LSP editor service over WebSocket at
`/lsp` and the public pages (landing page, feature tour, and playground with browser-based editors).

- run `java -jar nelumbo-web-server-${version-num}.jar [--port N] [<file-or-dir>...]`,
  or double-click the jar: a small status window shows the URL with Open in Browser / Stop buttons

## nelumbo-ide-${version-num}.jar - standalone editor

A simple desktop editor for **nelumbo** specifications; query results are calculated and shown
on the fly. This editor is not an LSP-based editor, and useful for educational and demo purposes.

- run `java -jar nelumbo-ide-${version-num}.jar` (or double-click the jar)

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

- `nelumbo-intellij-plugin-${version-num}.zip` - IntelliJ IDEA plugin (LSP-based); install via
  Settings > Plugins > Install Plugin from Disk
- `nelumbo-eclipse-plugin-${version-num}.jar` - Eclipse plugin; drop it into the `dropins`
  folder of your Eclipse installation

## nelumbo-slides-${version-num}.zip

The **nelumbo** presentation slides as a static website; unzip and open `index.html`.
