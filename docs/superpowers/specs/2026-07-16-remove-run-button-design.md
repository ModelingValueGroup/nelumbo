# Remove the Run button: LSP-only evaluation feedback

Date: 2026-07-16
Status: approved

## Problem

The website editors (tour, playground) show a Run button that POSTs the document to
`/eval` and renders a results panel, while the marketing claim is *immediate* execution.
The LSP already evaluates every document on a 300 ms debounce after each edit and shows
per-query inlay hints (`result` / checkmark / warning) plus mismatch squiggles. The Run
button therefore contradicts the story and duplicates the eval path.

## Decision

Remove the Run path from the frontend entirely. The LSP inlay hints become the single
feedback channel, enriched so nothing readable is lost:

- Full results move into an inlay-hint tooltip; the inline label is capped.
- When the LSP connection fails, the banner states that evaluation is unavailable.
  No fallback eval path (the `/eval` REST endpoints remain for API users, the pages
  just never call them).

Rejected alternatives: keeping the results panel fed by a custom LSP notification
(protocol extension for little gain) or by a debounced client-side `/eval` (evaluates
every document twice per edit on a public server).

## Changes

### 1. LSP server (`lsp/server`)

- `QueryResult` gains `tooltip()`: markdown with the full inferred result, one binding
  per line; for `MATCH` the inferred result behind the checkmark; for `ERROR` the full
  message.
- `inlineLabel()` caps the label at ~60 chars with an ellipsis; the tooltip always
  carries the full text.
- `QueryResultCache.evaluate` sets the tooltip (`MarkupContent`) on each `InlayHint`.
- **Risk to verify first:** the site bundles `monaco-editor/esm/vs/editor/edcore.main`;
  confirm that build renders inlay-hint tooltips on hover. If it does not, fall back to
  raising/removing the label cap (no tooltip).

### 2. Frontend (`website/src/main/frontend/src/nelumbo-fields.ts`, `fields.css`)

- Delete `runEval`, `renderResults`, `esc`, the `Eval*` interfaces, the Run button, the
  status span, the results div, and the Cmd/Ctrl+Enter binding.
- Create the toolbar only when a `.nelumbo-solution` sibling exists (its sole remaining
  content is the Show-solution toggle); other fields get just the editor.
- Drop the dead `.nelumbo-field-results` / `.badge` / `.bindings` CSS.
- Banner text: "Language features and evaluation are unavailable (LSP connection
  failed)."

### 3. Pages

- `tour.html`: replace "Edit and press Run (or Cmd/Ctrl+Enter)" with wording that
  results appear inline as you type.
- `playground.html`: subtitle ("edit and run ...") and intro ("press Run") reworded the
  same way.

### 4. e2e tests (`website/src/main/frontend/e2e`)

- Replace the "Run on the logic demo evaluates against /eval" test with one that waits
  for the logic demo's query line to show an inlay hint and asserts its content
  (Monaco renders hints as DOM spans; fall back to the `NelumboFields.__editors` hook
  if DOM assertion is brittle).
- Show-solution and LSP feature tests unchanged.

## Untouched

- `/eval`, `/eval/trace`, `/metadata`, `/health` REST endpoints, `EvalService`, CLI
  window and server GUI.
- Exercise self-checking (expected-result mismatch squiggles) already LSP-based.
