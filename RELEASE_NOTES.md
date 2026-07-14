To start the **nelumbo** editor:

- install java (version 21 or higher, if not yet installed)
- download `nelumbo-${version-num}-editor.jar`
- in your command line environment run `java -jar nelumbo-${version-num}-editor.jar`

Always start a **nelumbo** specification with an import-statement. For example:

```
import    nelumbo.strings

String  a

"foo"+"bar"=a  ?    [(a="foobar")][..]
```

Query results are calculated and shown on the fly.
This editor is not an LSP-based editor, and useful for educational and demo purposes.

To run the **nelumbo** website server (REST eval + LSP over WebSocket + tour/playground pages):

- download `nelumbo-http-server-${version-num}.jar`
- run `java -jar nelumbo-http-server-${version-num}.jar`

To use the **nelumbo** MCP server (LLM authoring tools for `.nl` decision models):

- download `nelumbo-mcp-server-${version-num}.jar`
- register it with your MCP client, e.g. `claude mcp add nelumbo -- java -jar nelumbo-mcp-server-${version-num}.jar`
