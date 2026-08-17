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
