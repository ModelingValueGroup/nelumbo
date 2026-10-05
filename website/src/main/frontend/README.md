# nelumbo-fields

Browser bundle for the Nelumbo demo site. Turns every `<div class="nelumbo-field">` on a page
into a Monaco editor that is an LSP client over a page-shared `/lsp` WebSocket; query results
appear inline as end-of-line inlay hints (full result in the hover tooltip).

## Build

```sh
npm install      # first time (creates package-lock.json)
npm run check    # tsc --noEmit typecheck
npm run build    # esbuild -> dist/nelumbo-fields.js + .css (+ codicon .ttf)
npm run dist     # npm ci + build (used by the Gradle :website:npmBundle task)
```

The whole `dist/` directory must be served together: the CSS references the emitted
`codicon-<hash>.ttf` by a relative URL, so the font must sit next to the css.

## Host page integration

```html
<link rel="stylesheet" href="/assets/nelumbo-fields.css">
<script src="/assets/nelumbo-fields.js"></script>
<script>NelumboFields.initNelumboFields();</script>
```

A field div's text content is its initial document. Optional `data-height` overrides the
220px editor height.

## Load test

```sh
npm run test:load -- --url https://nelumbo.nl --ssh <user>@server1.openwalnoot.com
```

`load/loadtest.mts` (Node 24, no dependencies) simulates tour-page visitors in steps of
1, 2, 4, ... up to `--max-clients` (default 64), `--step-seconds` (default 60) per step;
earlier clients stay connected. Each client loads the page, opens an LSP session with all
tour documents, and keeps editing queries like the Monaco client does. Per step it prints:

- `sessions` / `rejected`: open LSP sessions, and connections refused by the session cap
- `page p95`: tour page + bundle load time
- `lsp p50/p95/max`: LSP request round trips
- `result p50/p95/max`: last keystroke until the inlay hints changed (includes the
  server's 300 ms debounce)
- `cpu max` / `mem max` / `load max` (only with `--ssh`): `docker stats` of the container
  (100% = one core) and the host's 1-minute load average

A step fails on any rejection or error, or a p95 above `--limit-ms` (default 1000); the
run stops there and exits 1. Default target is `http://localhost:8899` (the e2e port).

## Dependency stack (accepted technical debt)

Pinned to an intentionally older, self-contained Monaco stack:

- `monaco-editor` 0.34.1 (vanilla)
- `monaco-languageclient` 1.0.1 (classic `MonacoServices.install` API)
- `vscode-ws-jsonrpc` 2.0.2

The plan first targeted `monaco-languageclient` v8 with the `@codingame/monaco-vscode-*`
service-override shims, but that stack has hard peer-version conflicts and needs VS Code
workbench worker bootstrapping that does not bundle cleanly in a plain esbuild IIFE. The v1
stack typechecks, bundles to a single self-contained IIFE, and has a clean `npm audit`.
Upgrading later is a rewrite of `connectLanguageClient()` in `src/nelumbo-fields.ts`, not a
version bump. No `MonacoEnvironment` worker is configured; monaco falls back to a synchronous
main-thread worker, which is fine because the `/lsp` server supplies all language features.
