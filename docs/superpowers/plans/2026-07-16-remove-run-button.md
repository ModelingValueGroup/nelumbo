# Remove Run Button (LSP-only Evaluation Feedback) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the Run button from the website editors; the LSP inlay hints (with a capped inline label and a full-result tooltip) become the single evaluation feedback channel.

**Architecture:** The LSP server already auto-evaluates every document on a 300 ms debounce and serves per-query inlay hints. We enrich `QueryResult` with a `tooltip()` (full result) and cap `inlineLabel()` at 60 chars, set the tooltip on each `InlayHint` in `QueryResultCache`, then delete the whole Run/`/eval` path from the frontend and update prose + e2e tests. The `/eval` REST endpoints stay (API users); the pages just never call them.

**Tech Stack:** Java 21 / LSP4J (lsp/server), TypeScript + Monaco + monaco-languageclient (website frontend), Playwright (e2e). Verified: the bundled `edcore.main` includes the inlayHints contribution and monaco-languageclient 1.0.1 converts `inlayHint.tooltip`, so tooltips render.

**Spec:** `docs/superpowers/specs/2026-07-16-remove-run-button-design.md`

**Branch:** work on `local/work`. Commit after every task. Never push.

---

### Task 1: QueryResult - capped inline label + full-result tooltip

**Files:**
- Create: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryResultTest.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResult.java`

Note: every Java file needs the LGPL header - copy the exact 15-line comment block from the top of `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EmbeddedServerTest.java` (the `//~~~...` block). The `mvgCorrector` Gradle task enforces it.

- [ ] **Step 1: Write the failing test**

Create `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryResultTest.java` (LGPL header block first, then):

```java
package org.modelingvalue.nelumbo.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class QueryResultTest {

    @Test
    public void shortLabelIsNotCapped() {
        QueryResult r = QueryResult.result("[()][]");
        assertEquals("[()][]", r.inlineLabel());
        assertEquals("[()][]", r.tooltip());
    }

    @Test
    public void longLabelIsCappedButTooltipIsFull() {
        String      full = "[(f=0),(f=1),(f=2),(f=3),(f=4),(f=5),(f=6),(f=7),(f=8),(f=9)][..]";
        QueryResult r    = QueryResult.result(full);
        assertEquals(60, r.inlineLabel().length(), "inline label is capped at 60 chars");
        assertTrue(r.inlineLabel().endsWith("..."), "capped label ends in an ellipsis");
        assertTrue(full.startsWith(r.inlineLabel().substring(0, 57)), "capped label is a prefix of the full result");
        assertEquals(full, r.tooltip(), "tooltip always carries the full result");
    }

    @Test
    public void matchShowsCheckmarkWithResultTooltip() {
        QueryResult r = QueryResult.match("[()][]");
        assertEquals("✓", r.inlineLabel());
        assertEquals("[()][]", r.tooltip(), "the tooltip reveals the result behind the checkmark");
    }

    @Test
    public void errorTooltipCarriesFullMessage() {
        QueryResult r = QueryResult.error("evaluation exceeded the deadline");
        assertEquals("⚠ evaluation exceeded the deadline", r.inlineLabel());
        assertEquals("⚠ evaluation exceeded the deadline", r.tooltip());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.QueryResultTest"`
Expected: FAIL - compilation error, `tooltip()` does not exist on `QueryResult`.

- [ ] **Step 3: Implement in QueryResult**

In `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResult.java`, replace the existing `inlineLabel()` method with:

```java
    private static final int MAX_LABEL_LENGTH = 60;

    /** Short label used for the end-of-line inlay hint; capped so long results don't blow out the line. */
    public String inlineLabel() {
        return switch (kind) {
            case RESULT, MISMATCH -> cap(inferred);
            case MATCH -> "✓";
            case ERROR -> cap("⚠ " + inferred);
        };
    }

    /** Full result for the inlay-hint tooltip; never capped. */
    public String tooltip() {
        return switch (kind) {
            case RESULT, MISMATCH, MATCH -> inferred;
            case ERROR -> "⚠ " + inferred;
        };
    }

    private static String cap(String s) {
        return s.length() <= MAX_LABEL_LENGTH ? s : s.substring(0, MAX_LABEL_LENGTH - 3) + "...";
    }
```

(The `✓`/`⚠` glyphs already exist in this file - keep them exactly as they are.)

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.QueryResultTest" --tests "org.modelingvalue.nelumbo.lsp.workspaceService.QueryExecutionFlowTest"`
Expected: PASS (the existing QueryExecutionFlowTest label assertions use short strings, unaffected by the cap).

- [ ] **Step 5: Commit**

```bash
git add lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResult.java lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/QueryResultTest.java
git commit -m "lsp: QueryResult tooltip() with full result, inlineLabel() capped at 60 chars"
```

---

### Task 2: QueryResultCache - set the tooltip on each inlay hint

**Files:**
- Modify: `lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EmbeddedServerTest.java`
- Modify: `lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResultCache.java:136-150`

- [ ] **Step 1: Write the failing test**

Add to `EmbeddedServerTest.java` (imports needed: `java.util.List`, `org.eclipse.lsp4j.InlayHint`, plus the already-imported assertions):

```java
    @Test
    public void inlayHintsCarryFullResultTooltips() throws InterruptedException {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        server.connect(client);
        try {
            server.getWorkspace().getDocumentManager().addDocument("inmemory://t.nl", "import nelumbo.logic\ntrue ?\n", 1);
            org.junit.jupiter.api.Assertions.assertTrue(client.awaitInlayHintRefresh(15), "expected refreshInlayHints after the debounced evaluation");
            List<InlayHint> hints = server.getWorkspace().getDocumentManager().queryResultCache().hints("inmemory://t.nl");
            assertEquals(1, hints.size(), "one hint for the single query");
            assertNotNull(hints.get(0).getTooltip(), "the hint carries a tooltip");
            assertEquals("[()][]", hints.get(0).getTooltip().getLeft(), "the tooltip is the full inferred result");
        } finally {
            server.getWorkspace().dispose();
        }
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :lsp:server:test --tests "org.modelingvalue.nelumbo.lsp.EmbeddedServerTest"`
Expected: FAIL on `assertNotNull(hints.get(0).getTooltip())` - no tooltip is set yet. (If instead the `[()][]` value assertion fails, the inferred rendering differs - fix the expected string to the actual value printed by the failure, it is the same `InferResult.toString()` the inline label showed before this change.)

- [ ] **Step 3: Set the tooltip in QueryResultCache**

In `QueryResultCache.evaluate`, in the loop building the hint list, after `hint.setPaddingLeft(true);` add:

```java
                    hint.setTooltip(result.tooltip());
```

(`InlayHint.setTooltip(String)` is an LSP4J convenience overload that wraps the string in the Either.)

- [ ] **Step 4: Run the module tests**

Run: `./gradlew :lsp:server:test`
Expected: PASS (all).

- [ ] **Step 5: Commit**

```bash
git add lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp/QueryResultCache.java lsp/server/src/test/java/org/modelingvalue/nelumbo/lsp/EmbeddedServerTest.java
git commit -m "lsp: inlay hints carry the full query result as tooltip"
```

---

### Task 3: Frontend - delete the Run path

**Files:**
- Modify: `website/src/main/frontend/src/nelumbo-fields.ts`
- Modify: `website/src/main/frontend/src/fields.css`

No frontend unit-test infra exists (verification is `npm run check` here, e2e in Task 5).

- [ ] **Step 1: Remove the eval code from nelumbo-fields.ts**

Delete these in full:
- the `EvalBinding`, `EvalQuery`, `EvalError`, `EvalResponse` interfaces
- the `esc()` function
- the `renderResults()` function
- the `runEval()` function

- [ ] **Step 2: Rewrite buildField and addSolutionToggle**

Replace both functions with (toolbar now exists only for exercise fields with a solution, and stays above the editor):

```ts
function addSolutionToggle(field: HTMLElement): void {
    const next: Element | null = field.nextElementSibling;
    if (next === null || !next.classList.contains('nelumbo-solution')) {
        return;
    }
    const solution: HTMLElement       = next as HTMLElement;
    const toolbar:  HTMLDivElement    = document.createElement('div');
    toolbar.className = 'nelumbo-field-toolbar';
    const button:   HTMLButtonElement = document.createElement('button');
    button.type        = 'button';
    button.className   = 'secondary';
    button.textContent = 'Show solution';
    button.addEventListener('click', (): void => {
        const visible: boolean = solution.classList.toggle('visible');
        button.textContent = visible ? 'Hide solution' : 'Show solution';
    });
    toolbar.appendChild(button);
    field.appendChild(toolbar);
}

function buildField(div: HTMLElement, index: number): void {
    let initial: string = div.textContent || '';
    if (initial.startsWith('\n')) {
        initial = initial.slice(1);
    }
    div.textContent = '';
    div.classList.add('nelumbo-field-wrap');

    addSolutionToggle(div);

    const host: HTMLDivElement = document.createElement('div');
    host.className = 'nelumbo-field-editor';
    if (div.dataset.height) {
        host.style.height = div.dataset.height;
    }
    div.appendChild(host);

    const uri:   monaco.Uri               = monaco.Uri.parse('inmemory://field-' + index + '.nl');
    const model: monaco.editor.ITextModel = monaco.editor.createModel(initial, LANGUAGE_ID, uri);

    const editor: monaco.editor.IStandaloneCodeEditor = monaco.editor.create(host, {
        model:                model,
        theme:                'vs-dark',
        minimap:              { enabled: false },
        automaticLayout:      true,
        fontSize:             13,
        fontFamily:           '"JetBrains Mono", ui-monospace, Menlo, Consolas, monospace',
        'semanticHighlighting.enabled': true,
        scrollBeyondLastLine: false,
        // Cmd/Ctrl+Click goes to definition (Alt+Click is multi-cursor), the VS Code default made explicit
        multiCursorModifier:  'alt',
    });
    __editors.push({ editor: editor, model: model });
}
```

(This also drops the Run button, status span, results div, the `run` closure, and the Cmd/Ctrl+Enter binding - auto-eval makes them moot.)

- [ ] **Step 3: Update the banner text and stale comments**

In `showBanner()`:

```ts
    banner.textContent = 'Language features and evaluation are unavailable (LSP connection failed).';
```

In the comment above `connect()`, change `(editing + Run still work)` to `(editing still works)`.

- [ ] **Step 4: Delete dead CSS from fields.css**

Delete these rules entirely:
- `.nelumbo-field-toolbar .status`
- `.nelumbo-field-results` and `.nelumbo-field-results.visible`
- `.nelumbo-field-results .q`, `.q-true`, `.q-false`, `.q-unknown`, `.q-error`
- `.nelumbo-field-results .badge`, `.bindings`, `.b`, `.placeholder`
- the now-unused custom properties `--nf-true`, `--nf-false`, `--nf-unknown` in `.nelumbo-field-wrap` (verify first: `grep -n "nf-true\|nf-false\|nf-unknown" website/src/main/frontend/src/fields.css` must show no other uses)

Keep `.nelumbo-field-toolbar`, its `button`, `button:active` and `button.secondary` rules (the Show-solution toggle uses them).

- [ ] **Step 5: Type-check and bundle**

Run (in `website/src/main/frontend/`): `npm run build`
Expected: `tsc --noEmit` clean (any leftover reference to the deleted code fails here), esbuild bundle written.

- [ ] **Step 6: Commit**

```bash
git add website/src/main/frontend/src/nelumbo-fields.ts website/src/main/frontend/src/fields.css
git commit -m "website: remove the Run button/results panel - LSP inlay hints are the feedback channel"
```

---

### Task 4: Page prose + frontend README

**Files:**
- Modify: `website/src/main/resources/public/tour.html:64`
- Modify: `website/src/main/resources/public/playground.html:45,55`
- Modify: `website/src/main/frontend/README.md:4`

- [ ] **Step 1: tour.html**

Line 64, change

```
       <code>[..][..]</code> means "unknown". Edit and press Run (or Cmd/Ctrl+Enter).</p>
```

to

```
       <code>[..][..]</code> means "unknown". Edit the document; the results update inline as you type.</p>
```

Also run `grep -n -i "press Run\|Run button\|Cmd/Ctrl+Enter" website/src/main/resources/public/*.html` - it must come back empty after the edits in this task.

- [ ] **Step 2: playground.html**

Line 45 subtitle:

```html
  <span class="sub">edit a Nelumbo document and watch it evaluate as you type</span>
```

Line 55 intro:

```html
  <p class="intro">Edit the document below; results appear inline as you type. The document is self-contained: it imports the stdlib it needs, so it evaluates even against an empty knowledge base.</p>
```

- [ ] **Step 3: frontend README**

In `website/src/main/frontend/README.md`, change the opening paragraph from

```
Browser bundle for the Nelumbo demo site. Turns every `<div class="nelumbo-field">` on a page
into a Monaco editor that is an LSP client over a page-shared `/lsp` WebSocket, plus a Run button
that POSTs the editor content to `/eval` and renders the query results.
```

to

```
Browser bundle for the Nelumbo demo site. Turns every `<div class="nelumbo-field">` on a page
into a Monaco editor that is an LSP client over a page-shared `/lsp` WebSocket; query results
appear inline as end-of-line inlay hints (full result in the hover tooltip).
```

(Leave the root `README.md` alone - its "Run button" is the CLI window, unrelated.)

- [ ] **Step 4: Commit**

```bash
git add website/src/main/resources/public/tour.html website/src/main/resources/public/playground.html website/src/main/frontend/README.md
git commit -m "website: prose reflects inline live results instead of a Run button"
```

---

### Task 5: e2e - replace the Run test with inline-result tests

**Files:**
- Modify: `website/src/main/frontend/e2e/tour.spec.ts:33-40`

- [ ] **Step 1: Replace the Run test**

Delete the test `'Run on the logic demo evaluates against /eval and renders a result'` and add in its place:

```ts
test('query results appear inline without pressing anything', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    const demo: Locator = page.locator('#logic .nelumbo-field-wrap').first();
    // the LSP evaluates on a debounce; "true & true ?" renders its result [()][] as an inlay hint
    await expect(demo.getByText('[()][]').first()).toBeVisible({ timeout: 20_000 });
});

test('hovering an inline result shows the full-result tooltip', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    const demo: Locator = page.locator('#logic .nelumbo-field-wrap').first();
    const hint: Locator = demo.getByText('[()][]').first();
    await expect(hint).toBeVisible({ timeout: 20_000 });
    await hint.hover();
    const hover: Locator = page.locator('.monaco-hover').first();
    await expect(hover).toBeVisible({ timeout: 10_000 });
    await expect(hover).toContainText('[()][]');
});
```

Notes for the implementer:
- The demo field's own text contains no `[()][]` (that literal only appears in prose OUTSIDE the field wrap and in other editors' expected clauses - the `#logic` demo queries have no expected clauses), so the locator can only match the rendered inlay hint.
- If `getByText` does not match (inlay label split across spans), fall back to `demo.locator('.monaco-editor').locator('text=/\\[\\(\\)\\]\\[\\]/')` or assert via a bounding-box hover on `.view-line` - but try the simple form first.

- [ ] **Step 2: Run the e2e suite**

In `website/src/main/frontend/` (Playwright browsers already installed; if not: `npm run test:e2e:install`):

Run: `npm run test:e2e`
Expected: all tests PASS, including the two new ones and the untouched Show-solution + LSP specs. This also proves the removed Run button broke nothing else (the old Run test is gone; nothing else referenced it).

- [ ] **Step 3: Commit**

```bash
git add website/src/main/frontend/e2e/tour.spec.ts
git commit -m "e2e: assert inline query results + tooltip instead of the Run button"
```

---

### Task 6: Docs + full verification

**Files:**
- Modify: `CLAUDE.md` (Website Module section)

- [ ] **Step 1: Update CLAUDE.md**

In the "Website Module - LSP over WebSocket" section:
- In the e2e sentence, replace `Run->/eval` with `inline query results (inlay hints + tooltip)`.
- Add one sentence: editors have no Run button; the LSP inlay hints (label capped at 60 chars, full result in the hover tooltip, set in `QueryResultCache`) are the only evaluation feedback, and the LSP-failed banner says evaluation is unavailable; the `/eval` REST endpoints remain for API users but the pages never call them.

- [ ] **Step 2: Full build + tests**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (core + lsp:server tests, website bundle).

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: CLAUDE.md reflects LSP-only evaluation feedback on the website"
```
