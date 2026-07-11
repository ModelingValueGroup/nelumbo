# Playwright Tests for the Feature Tour

Date: 2026-07-11
Status: approved design, pending implementation plan

## Goal

Add end-to-end browser tests (Playwright) for the Nelumbo feature tour, covering both
structural/interaction behavior and the LSP-dependent features (diagnostics squiggle,
hover, completion, Cmd/Ctrl+Click go-to-definition) that the existing JUnit tests cannot
reach. Wire them into CI.

## Decisions

- Server launch: Playwright's `webServer` spawns the built `nelumbo-http-server` jar on a
  fixed test port and tears it down; self-contained (`reuseExistingServer` locally).
- Scope: full - structure + Run->/eval + Show-solution toggle, AND the LSP features
  (mismatch marker, hover, completion, go-to-definition).
- CI: wired in (build the serverJar, install Chromium, run the suite on push).
- Editor driving: a minimal test hook on the component (`window.NelumboFields.__editors`
  + `.__monaco`) makes LSP assertions deterministic rather than flaky black-box DOM work.

## Location and harness

- Playwright lives in the existing frontend npm project `http/src/main/frontend/`
  (`@playwright/test` as a devDependency), specs under `e2e/`.
- `playwright.config.ts`:
  - Resolves the built jar path in Node (glob `../../../build/libs/nelumbo-http-server-*.jar`
    from the frontend dir - i.e. `http/build/libs/`; throw a clear "run ./gradlew :http:serverJar
    first" error if absent).
  - `webServer: { command: 'java -jar <jar> --port 8899', url: 'http://localhost:8899',
    reuseExistingServer: !process.env.CI, timeout: 60_000 }`.
  - Single `chromium` project; `use.baseURL = 'http://localhost:8899'`; retries 1 in CI.
- The jar bundles the frontend (including the test hook), so building `:http:serverJar`
  is the single source of the site under test.

## Component change - test hook

In `http/src/main/frontend/src/nelumbo-fields.ts`, expose a namespaced, read-only test hook
populated as fields mount:

- `export const __editors: Array<{ editor: monaco.editor.IStandaloneCodeEditor; model: monaco.editor.ITextModel }> = [];`
  pushed to in `buildField`.
- `export { monaco as __monaco };` (or `export const __monaco = monaco;`).

These surface as `window.NelumboFields.__editors` / `.__monaco` via the existing esbuild IIFE
global. Harmless in production (a couple of references); clearly test-oriented.

## Spec files

### e2e/tour.spec.ts (structure + interaction, no LSP)

- Page loads at `/`; the sidebar lists all 8 sections; the `logic` section is active by default.
- Clicking each sidebar link activates its `.tour-section` (`.active`) and de-activates others.
- Each section, once shown, mounts a Monaco editor (`.monaco-editor` present within the section).
- Clicking **Run** on the logic demo field posts to `/eval` and renders a result in
  `.nelumbo-field-results` (assert it becomes visible and contains a query result).
- **Show solution** on an exercise toggles the `.nelumbo-solution` block visible/hidden and the
  button label flips Show/Hide.
- The Playground sidebar link navigates to `/playground.html`.

### e2e/lsp.spec.ts (LSP-dependent, uses the hook)

A `waitForLsp(page)` helper waits until the language client is connected and diagnostics are
available (poll `__monaco.editor.getModelMarkers` on a known field or a readiness signal), with
a generous timeout.

- **Diagnostics/squiggle**: on the logic exercise scaffold (`false & true ? [()][]`, which
  mismatches), assert an error marker appears (`getModelMarkers({resource})` has length > 0 for
  that field's model). Then set the model value to the solved form (`false | true ? [()][]`) via
  the hook and assert the markers clear.
- **Hover**: position the cursor over a known token in a demo field and run
  `editor.getAction('editor.action.showHover').run()`; assert `.monaco-hover` becomes visible with
  non-empty content.
- **Completion**: focus a field, trigger `editor.action.triggerSuggest`; assert the suggest widget
  (`.suggest-widget`) becomes visible with at least one row.
- **Go-to-definition**: in a self-contained field, position on a use of a name defined in the same
  document (e.g. `fib` in `fib(7)`), run `editor.action.revealDefinition`, and assert the selection
  moved to the declaration line. Plus one real Cmd/Ctrl+modifier-click smoke to exercise the gesture
  binding enabled by `edcore.main`.

Assertions use Playwright auto-waiting (`expect.poll` / `toPass` / per-assertion timeouts ~15s), no
fixed sleeps.

## npm scripts

- `test:e2e` - `../../../../gradlew :http:serverJar && playwright test` (build the jar, then run).
- `test:e2e:install` - `playwright install --with-deps chromium`.

(Keep existing `check`/`build`/`dist`.)

## CI

In `.github/workflows/build.yaml`, after the main build, add a step (or job) that:
- installs Chromium (`npx playwright install --with-deps chromium` in the frontend dir),
- builds `./gradlew :http:serverJar`,
- runs `npx playwright test` in the frontend dir (CI=true, so no reuseExistingServer, retries on).

Node is already set up in CI. Cache the Playwright browser download if convenient.

## Testing / verification

- `npm run test:e2e` green locally (Chromium).
- Deliberately break an exercise scaffold to confirm the diagnostics test would catch a regression
  (sanity of the assertion), then revert.

## Out of scope

- Cross-browser (Firefox/WebKit) - Chromium only for now.
- Visual regression / screenshot diffing.
- Cross-field go-to-definition (each field is self-contained).

## Sizing

- Playwright install + config + test hook: small.
- tour.spec.ts (structure/interaction): small-medium.
- lsp.spec.ts (LSP-dependent, hook-driven): medium (the fiddly part).
- CI wiring: small.
