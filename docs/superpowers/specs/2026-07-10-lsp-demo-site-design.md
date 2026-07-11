# Nelumbo Demo Site with LSP-Powered Editable Fields

Date: 2026-07-10
Status: approved design, pending implementation plan

## Goal

Extend the Nelumbo HTTP server into a public demonstration site where pages contain
multiple editable Nelumbo fields. Each field is a full LSP client (completion, hover,
diagnostics, semantic tokens, inlay hints, code actions, formatting, folding,
definition, symbols) and can execute its content on demand against the server's
loaded knowledge base.

## Decisions Made

- Site shape: both an upgraded playground and a multi-field demo page, built on one
  reusable field component.
- KB context: each field sees the server's loaded .nl knowledge base (same context
  POST /eval uses); fields are isolated from each other.
- Editor: Monaco + monaco-languageclient (full LSP feature coverage; accepts the
  ~4-5 MB payload and an npm build step in the http module).
- Transport: the existing Javalin server gains a /lsp WebSocket route; one process,
  one port, same origin.
- Deployment: public internet site behind a TLS reverse proxy; server-side resource
  guards required.

## Architecture

```
Browser page                          nelumbo-http-server (one jar, one port)
+----------------------------+        +--------------------------------------+
| field 1 (Monaco editor) ---+--+     | Javalin                              |
| field 2 (Monaco editor) ---+--+--ws-+--> /lsp  -> LSP4J <-> NelumboLS      |
| Run buttons ---------------+--+-http+--> /eval (existing)                  |
|                            |        |    /     demo pages + JS bundle      |
+----------------------------+        |    KnowledgeBase loaded from .nl     |
                                      +--------------------------------------+
```

- Each page opens one WebSocket to /lsp shared by all fields on that page.
- Each field is one LSP document with URI inmemory://field-<n>.nl.
- Each WebSocket session gets a fresh NelumboLanguageServer instance, seeded with
  the loaded KB; it is disposed when the connection closes.
- Field isolation holds because the LSP server parses every document standalone
  (verified in NlDocument.of: fresh Tokenizer/Parser per document).
- Execution on demand stays on POST /eval (unchanged). The LSP additionally
  auto-evaluates queries (debounced, QueryResultCache) and serves results as inlay
  hints, so results also appear inline while typing.

## Server-Side Changes

### lsp/server: injectable base knowledge base

NelumboLanguageServer and the code paths that tokenize/parse/evaluate documents
(notably QueryEvaluator.evaluate and NlDocument.of) currently run under the
hard-coded KnowledgeBase.BASE. Parsing in Nelumbo is KB-driven (the tokenizer and
parser consult the thread-local KnowledgeBase.CURRENT), so this seeding also makes
DSL syntax defined in loaded .nl files parse correctly inside demo fields.

Change: NelumboLanguageServer accepts a base KnowledgeBase at construction,
defaulting to KnowledgeBase.BASE. All existing entry points (stdio, Tyrus ws mode,
IDE plugins) are untouched by the default.

### http: /lsp WebSocket route and bridge

- The http module gains a dependency on :lsp:server.
- A small adapter (~100 lines) bridges Javalin's ws callbacks (onMessage, onClose,
  onError) to an LSP4J Launcher: incoming ws text frames feed the launcher's
  message consumer; outgoing LSP messages are written back via session send.
- Per-connection lifecycle: connect -> new NelumboLanguageServer(loadedKb) ->
  launcher.startListening; close/error -> dispose server, release resources.

### Public-deployment guards (http module)

- Cap concurrent LSP sessions (default 32, CLI-configurable); reject connections
  beyond the cap with a clear close reason the client surfaces as a banner.
- Idle timeout on ws sessions (default 10 minutes without client messages).
- Cap document size accepted over didOpen/didChange (default 64 KB).
- Existing per-request eval timeout on /eval stays as is.
- TLS/rate limiting remain reverse-proxy concerns, out of scope here.

## Client Side

New npm project at http/src/main/frontend/ (esbuild, no framework; same pattern as
lsp/plugins/vscode). It produces one JS bundle plus CSS into served resources and
exports one entry point:

```js
initNelumboFields(); // upgrades every <div class="nelumbo-field"> on the page
```

Per field div (its text content is the initial document), the component renders:

- a Monaco editor wired via monaco-languageclient + vscode-ws-jsonrpc to the
  page-shared /lsp socket, one document per field;
- a Run button that POSTs the editor content to /eval;
- a results panel reusing the playground's current result rendering (badges,
  bindings, errors).

Monaco is configured for the full feature surface the server implements: semantic
tokens, completion, hover, definition, inlay hints, code actions, formatting,
folding, selection ranges, document symbols.

Fallback: if the ws connection fails or is rejected, fields remain plain Monaco
editors with a working Run button; a banner notes that language features are
unavailable.

## Pages

- playground.html: rebuilt on the field component (single large field; trace and
  raw-JSON options kept).
- demo.html: the demonstration page; authored HTML with prose plus multiple
  nelumbo-field divs seeded with examples (logic, integers, fib, a DSL example).
- Adding a demo page = writing HTML with prose and field divs; no JS changes.

## Build Integration

- Gradle :http gains an npmBundle task (npm ci && npm run build in
  src/main/frontend/) wired before processResources; output lands in
  build/generated-resources/public/ and is included in serverJar.
- Node is a build-time requirement for :http only; the task is up-to-date-checked
  against frontend sources.
- CI (.github/workflows/build.yaml) gains a Node setup step.

## Error Handling

- WS bridge: malformed frames log and close only that session.
- Server-side exceptions during LSP requests surface as LSP error responses
  (LSP4J default behavior).
- /eval error semantics unchanged.

## Testing

- Java (http module): a ws client test connecting to /lsp that runs initialize and
  didOpen, asserts diagnostics and inlay hints arrive, and asserts KB seeding
  (completion offers a functor defined only in a loaded .nl file); a session-cap
  test.
- lsp/server: existing tests must stay green with the defaulted base KB; add a test
  constructing the server with a non-default KB.
- Existing NelumboHttpServerTest guards /eval regressions.
- Frontend: tsc typecheck as part of the bundle build. Browser E2E (Playwright) is
  explicitly out of scope for this effort.

## Out of Scope

- Cross-field visibility of declarations (fields stay isolated).
- Persistence/sharing of field contents.
- TLS, authentication, per-IP rate limiting (reverse proxy concerns).
- Browser E2E test harness.

## Sizing

- lsp/server KB seeding hook: small.
- ws bridge + session management: medium.
- Frontend component + build wiring: the bulk of the work.
- Pages: small.
- Core language module: no changes.

## Implementation Notes (as built)

Discovered during implementation, beyond the original design:

- `NelumboLanguageServer.exit()` called `System.exit(0)`; embedded sessions must not
  kill the host JVM. Fixed with an injectable exit handler (default preserves the old
  behavior for stdio/IDE).
- All client callbacks went through the static `Main.client`; wrong client would win
  with concurrent sessions. Moved the client onto `Workspace` (volatile), threaded it
  through `NlDocument`, `QueryResultCache`, `WorkspaceExecuteCommandService`, and `U`.
- Query auto-eval (`QueryResultCache` -> `QueryEvaluator`) and document parsing had no
  deadline; a public visitor could burn CPU. Added `evalDeadlineMs` on `Workspace`
  (0 = disabled, IDE default), a `QueryResultCache` hard backstop thread, and
  deadline-bounded parsing. Timeouts return partial results with a per-query marker
  rather than discarding everything.
- `QueryResultCache`'s scheduler thread leaked per session; added `Workspace.dispose()`.
- `WsServer.java` (dead code, referenced the static client) was deleted.
- Security hardening for the public target: `initialize` skips client workspace-folder
  resolution / filesystem scanning in embedded mode; documents are capped per session
  (64); ws slot accounting releases the slot if bridge construction fails; explicit
  64 KB text-frame limit and 10-minute idle timeout.
- `lsp:server` re-enabled its plain jar (classifier `"plain"`) so `http` can use a
  normal `implementation(project(":lsp:server"))` dependency; the generated frontend
  bundle is a source-set output dir (not a resources srcDir) so `sourcesJar` stays clean.

Deviations from the design:

- The playground's `trace` and `raw-JSON` toolbar options were dropped; the field
  component owns Run + results. `/eval/trace` is still reachable via curl.
- KB seeding is asserted end-to-end via the inlay-hint query result, not a completion
  item.
- Frontend uses an older, self-contained Monaco stack (monaco-editor 0.34 +
  monaco-languageclient 1.0.1) because the design's monaco-languageclient v8 +
  `@codingame/monaco-vscode-*` service-override stack does not bundle cleanly in a plain
  esbuild IIFE. See `http/src/main/frontend/README.md`.
