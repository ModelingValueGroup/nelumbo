# Nelumbo MCP Server

Date: 2026-07-12
Status: approved design, pending implementation plan

## Goal

Empower LLMs to author Nelumbo decision models. The target artifact is a self-contained
`.nl` file (types, patterns, rules, and queries with embedded expected results), so
verification is objective: run the file, exit 0. An MCP server gives any MCP-capable LLM
the write -> run -> read diagnostics -> adjust loop as tools, plus just-in-time language
knowledge, so the LLM does not need Nelumbo in its training data.

## Decisions

- Role: LLM as author with a verify/adjust loop (not a runtime reasoning backend).
- Delivery: MCP server over stdio, usable from any MCP client (approach "B: authoring
  assistant" - executor plus doc search, examples, skeleton, and enriched diagnostics).
- Model shape: self-contained `.nl` files with embedded expected-result queries; no
  stateful sessions, no per-case fact assertion in v1.
- Out of scope (v2 candidates): inference tracing / "explain this result", LSP-derived
  hover/completion tools, stateful KB sessions.

## Architecture

- New Gradle subproject `mcp/` (sibling of `http`), Java 21, depends on the root project.
- Official MCP Java SDK (`io.modelcontextprotocol.sdk:mcp`), stdio transport.
- `:mcp:mcpJar` ShadowJar task produces `nelumbo-<version>-mcp.jar`; clients register it
  as `java -jar nelumbo-mcp.jar`.
- Core refactor: extract the parse/evaluate logic from `NelumboCli` into a reusable
  `NelumboEvaluator` in the root project that returns structured results (diagnostics
  with line/col/message, per-query results with expectation match status) instead of
  printing. `NelumboCli` is refactored to consume it; the MCP server consumes it too.
- Evaluation runs under a deadline (default ~10s, `--eval-deadline-ms` CLI flag on the
  server) so a runaway query cannot hang a tool call.
- stdio hygiene: nothing may write to stdout during evaluation (it would corrupt the
  JSON-RPC stream). The server redirects `System.out` around evaluation and logs to
  stderr only.

## Tools

Four tools, matching the authoring loop:

- `eval_nl(content, name?)` - the workhorse. Input is `.nl` source text (no filesystem
  involvement). Returns:
  - `ok`: boolean (parse + evaluation + all expectations matched)
  - `diagnostics`: list of { line, col, message, sourceLine (with caret), hint?, docRef? }
  - `queryResults`: list of { query, result, expectationMatched? }
- `search_docs(query)` - keyword search over the bundled documentation tree
  (`docs/**/*.md`), returning top matching sections (heading, snippet, doc id). Plain
  text scoring; no embeddings.
- `get_example(name?)` - without argument: lists the bundled example corpus
  (`examples/*.nl`, `tests/*.nl`, stdlib `.nl`) with one-line descriptions. With a name:
  returns the full file. Working examples are the primary syntax teacher.
- `new_model(title)` - returns a commented skeleton `.nl` file: type declarations,
  pattern definitions (with the `#0` precedence convention for Root-extending functors
  already in place), variable declarations, rules, and a queries-with-expected-results
  section. The LLM edits valid structure instead of generating from a blank page.

## Language knowledge delivery

- The `docs/` markdown tree and the `.nl` example corpus are copied into the mcp jar's
  resources at build time, so the server is self-contained.
- Tool descriptions carry a compressed syntax primer (the essentials: type declarations,
  pattern definitions, variables, rules, facts, queries with expected results, logical
  operators), so a client that never calls `search_docs` still starts from correct
  basics.

## Enriched diagnostics

A small curated hint table in one class: each entry is a predicate over (exception
message + source context) plus hint text and a doc pointer. Initial entries:

1. `Unexpected token '\n', expected ...` where the functor extends a Root type ->
   "append explicit precedence #0" hint (the known precedence gotcha).
2. Cascading errors -> collapse to the first error; note the rest likely follow from it.
3. Expected-result mismatch -> show expected vs. actual side by side.
4. Unknown token/type -> suggest the nearest declared name.

No framework; the table grows as real LLM failure modes are observed.

## Testing

- Core `src/test/`: `NelumboEvaluator` structured output - good file, parse error,
  expectation mismatch, deadline exceeded.
- `mcp/src/test/`: hint-table matching per entry, doc search ranking (query hits the
  expected section), each tool handler invoked directly (the SDK's stdio plumbing is not
  under test).

Existing core/CLI tests guard the `NelumboCli` refactor. The root `test` task gets a
dependency on `:mcp:test` (as it already has on `:lsp:server:test`) so CI picks the new
module up.
