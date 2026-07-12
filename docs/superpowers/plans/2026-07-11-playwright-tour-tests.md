# Playwright Feature-Tour Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Playwright end-to-end tests for the Nelumbo feature tour (structure, Run->/eval, Show-solution, and the LSP features: diagnostics, hover, completion, go-to-definition) and wire them into CI.

**Architecture:** Playwright lives in the existing frontend npm project (`http/src/main/frontend/`), specs in `e2e/`. `playwright.config.ts` uses `webServer` to spawn the built `nelumbo-http-server` jar on a fixed port and tear it down. A tiny test hook on the component (`NelumboFields.__editors` + `.__monaco`) makes the LSP assertions deterministic via `page.evaluate`.

**Tech Stack:** @playwright/test (Chromium), the existing monaco-editor 0.34 / monaco-languageclient 1.0.1 frontend, the Javalin `nelumbo-http-server` jar, Java 21, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-07-11-playwright-tour-tests-design.md`

**Conventions:** run from repo root `/Users/tom/projects/mvg-nelumbo/nelumbo` unless a step says otherwise. ASCII only. Follow the user's TS style (explicit types on vars/args, braces on all blocks). The frontend dir is `http/src/main/frontend/`; from there the repo-root wrapper is `../../../../gradlew` and the http jar is `../../../build/libs/nelumbo-http-server-*.jar`.

## File structure

- `http/src/main/frontend/src/nelumbo-fields.ts` (modify) - add `__editors` + `__monaco` test hook.
- `http/src/main/frontend/package.json` (modify) - `@playwright/test` devDep + `test:e2e` / `test:e2e:install` scripts.
- `http/src/main/frontend/playwright.config.ts` (create) - webServer spawns the jar; chromium project.
- `http/src/main/frontend/.gitignore` (modify) - ignore playwright output.
- `http/src/main/frontend/e2e/tour.spec.ts` (create) - structure + interaction tests (no LSP).
- `http/src/main/frontend/e2e/lsp.spec.ts` (create) - LSP-dependent tests (hook-driven).
- `.github/workflows/build.yaml` (modify) - e2e step.
- `CLAUDE.md` (modify) - note the e2e tests.

---

### Task 1: Test hook on the component

**Files:**
- Modify: `http/src/main/frontend/src/nelumbo-fields.ts`

- [ ] **Step 1: Add the hook exports and populate them**

Read the current file first. Near the module-level state (the `let servicesReady...` block), add two exported members:

```ts
// Test hook (used by the Playwright e2e suite via window.NelumboFields.__editors / .__monaco).
// Read-only access to the mounted editors and the monaco namespace; harmless in production.
export const __editors: Array<{ editor: monaco.editor.IStandaloneCodeEditor; model: monaco.editor.ITextModel }> = [];
export const __monaco = monaco;
```

In `buildField`, immediately after the `editor` is created (after the `monaco.editor.create(...)` assignment and before/after the run wiring), push the pair:

```ts
    __editors.push({ editor: editor, model: model });
```

- [ ] **Step 2: Typecheck**

Run: `cd http/src/main/frontend && npm run check`
Expected: exit 0.

- [ ] **Step 3: Build and confirm the hook is exported on the global**

Run: `cd http/src/main/frontend && npm run build && grep -c "__editors" dist/nelumbo-fields.js`
Expected: build exit 0; grep count >= 1 (the export descriptor is in the bundle).

- [ ] **Step 4: Commit**

```bash
git add http/src/main/frontend/src/nelumbo-fields.ts
git commit -m "feat(http): expose __editors/__monaco test hook on the field component"
```

---

### Task 2: Playwright harness + structure/interaction tests

**Files:**
- Modify: `http/src/main/frontend/package.json`
- Create: `http/src/main/frontend/playwright.config.ts`
- Modify: `http/src/main/frontend/.gitignore`
- Create: `http/src/main/frontend/e2e/tour.spec.ts`

- [ ] **Step 1: Add the devDependency and scripts**

In `http/src/main/frontend/package.json`, add to `devDependencies`:

```json
    "@playwright/test": "^1.49.0",
```

and add to `scripts` (keep the existing check/build/dist):

```json
    "test:e2e": "../../../../gradlew :http:serverJar && playwright test",
    "test:e2e:install": "playwright install --with-deps chromium"
```

Then install: `cd http/src/main/frontend && npm install` (creates the lockfile entry). Expected exit 0.

- [ ] **Step 2: Create playwright.config.ts**

`http/src/main/frontend/playwright.config.ts`:

```ts
import { defineConfig, devices } from '@playwright/test';
import { readdirSync }           from 'node:fs';
import { resolve }               from 'node:path';

const PORT:    number = 8899;
const LIBSDIR: string = resolve(__dirname, '../../../build/libs');

function serverJar(): string {
    let jars: string[] = [];
    try {
        jars = readdirSync(LIBSDIR).filter((f: string): boolean => /^nelumbo-http-server-.*\.jar$/.test(f));
    } catch {
        jars = [];
    }
    if (jars.length === 0) {
        throw new Error('No nelumbo-http-server jar in ' + LIBSDIR + '. Build it first: ./gradlew :http:serverJar');
    }
    return resolve(LIBSDIR, jars[0]);
}

export default defineConfig({
    testDir: './e2e',
    timeout: 30_000,
    retries: process.env.CI ? 1 : 0,
    use:     {
        baseURL: 'http://localhost:' + PORT,
    },
    projects: [
        { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    ],
    webServer: {
        command:             'java -jar "' + serverJar() + '" --port ' + PORT,
        url:                 'http://localhost:' + PORT + '/health',
        reuseExistingServer: !process.env.CI,
        timeout:             60_000,
    },
});
```

- [ ] **Step 3: Ignore Playwright output**

Append to `http/src/main/frontend/.gitignore`:

```
test-results/
playwright-report/
playwright/.cache/
```

- [ ] **Step 4: Write the structure/interaction spec**

`http/src/main/frontend/e2e/tour.spec.ts`:

```ts
import { test, expect, Page, Locator } from '@playwright/test';

const SECTIONS: string[] = ['logic', 'facts', 'rules', 'types', 'relations', 'dsl', 'transform', 'scoping'];

async function showSection(page: Page, id: string): Promise<Locator> {
    await page.locator('aside nav a[data-section="' + id + '"]').click();
    const section: Locator = page.locator('#' + id);
    await expect(section).toHaveClass(/active/);
    return section;
}

test('tour loads with the logic section active and the full sidebar', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    for (const id of SECTIONS) {
        await expect(page.locator('aside nav a[data-section="' + id + '"]')).toBeVisible();
    }
    await expect(page.locator('#logic')).toHaveClass(/active/);
});

test('each section switches and mounts a Monaco editor', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    for (const id of SECTIONS) {
        const section: Locator = await showSection(page, id);
        await expect(section.locator('.monaco-editor').first()).toBeVisible();
        for (const other of SECTIONS) {
            if (other !== id) {
                await expect(page.locator('#' + other)).not.toHaveClass(/active/);
            }
        }
    }
});

test('Run on the logic demo evaluates against /eval and renders a result', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    const demo:    Locator = page.locator('#logic .nelumbo-field-wrap').first();
    const results: Locator = demo.locator('.nelumbo-field-results');
    await demo.getByRole('button', { name: 'Run' }).click();
    await expect(results).toBeVisible();
    await expect(results).toContainText('true');
});

test('Show solution toggles the solution block', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    const exercise: Locator = page.locator('#logic .nelumbo-exercise');
    const solution: Locator = exercise.locator('.nelumbo-solution');
    const button:   Locator = exercise.getByRole('button', { name: 'Show solution' });
    await expect(solution).toBeHidden();
    await button.click();
    await expect(solution).toBeVisible();
    await expect(exercise.getByRole('button', { name: 'Hide solution' })).toBeVisible();
    await exercise.getByRole('button', { name: 'Hide solution' }).click();
    await expect(solution).toBeHidden();
});

test('the Playground link navigates to /playground.html', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    await page.locator('aside nav a[href="/playground.html"]').click();
    await expect(page).toHaveURL(/\/playground\.html$/);
    await expect(page.locator('.nelumbo-field-wrap').first()).toBeVisible();
});
```

- [ ] **Step 5: Build the jar and install the browser**

Run: `./gradlew :http:serverJar` (from repo root).
Run: `cd http/src/main/frontend && npm run test:e2e:install`
Expected: both exit 0 (Chromium downloaded).

- [ ] **Step 6: Run the structure spec**

Run: `cd http/src/main/frontend && npx playwright test tour.spec.ts`
Expected: PASS (5 tests). Playwright spawns the jar, runs, tears down. If the "Run" result assertion is too strict (the exact rendered text differs), open the served page's result markup and adjust the `toContainText` to a substring that actually renders (e.g. a status word or the query text) - but keep it a real assertion that the results panel populated.

- [ ] **Step 7: Commit**

```bash
git add http/src/main/frontend/package.json http/src/main/frontend/package-lock.json http/src/main/frontend/playwright.config.ts http/src/main/frontend/.gitignore http/src/main/frontend/e2e/tour.spec.ts
git commit -m "test(http): Playwright harness + tour structure/interaction e2e tests"
```

---

### Task 3: LSP-dependent e2e tests

**Files:**
- Create: `http/src/main/frontend/e2e/lsp.spec.ts`

- [ ] **Step 1: Probe what the server actually returns**

Before writing content assertions, run the site and confirm what the LSP provides, so hover/completion target real content. Build+serve (`./gradlew :http:serverJar && java -jar http/build/libs/nelumbo-http-server-*.jar --port 8899`), open `http://localhost:8899/` in a browser if available, or reason from the server code: the logic exercise scaffold `false & true ? [()][]` mismatches, so it MUST get an error marker once connected (this is the reliable anchor). For hover/completion, identify a token/context that returns content (e.g. hover over a functor/type name in the `rules` fib field; completion at the start of a line or after a partial identifier). Note the exact field text and positions you will target.

- [ ] **Step 2: Write the LSP spec**

`http/src/main/frontend/e2e/lsp.spec.ts`. The diagnostics and go-to-definition tests are concrete; for hover and completion, target the token/context you confirmed in Step 1 (the skeleton below uses the `rules` fib field - adjust positions to what actually returns content):

```ts
import { test, expect, Page } from '@playwright/test';

// Find the mounted editor whose model text contains `needle`; returns its index in __editors.
async function editorIndexContaining(page: Page, needle: string): Promise<number> {
    return await page.evaluate((n: string): number => {
        const eds: Array<{ model: { getValue(): string } }> = (window as any).NelumboFields.__editors;
        for (let i = 0; i < eds.length; i++) {
            if (eds[i].model.getValue().indexOf(n) >= 0) {
                return i;
            }
        }
        return -1;
    }, needle);
}

// Error-severity marker count for the model of __editors[index].
async function errorMarkerCount(page: Page, index: number): Promise<number> {
    return await page.evaluate((i: number): number => {
        const nf:      any = (window as any).NelumboFields;
        const model:   any = nf.__editors[i].model;
        const markers: Array<{ severity: number }> = nf.__monaco.editor.getModelMarkers({ resource: model.uri });
        // MarkerSeverity.Error === 8
        return markers.filter((m: { severity: number }): boolean => m.severity === 8).length;
    }, index);
}

test('an unsolved exercise shows a diagnostic that clears when solved', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    // logic is active by default; its exercise scaffold contains "false & true".
    const idx: number = await editorIndexContaining(page, 'false & true');
    expect(idx).toBeGreaterThanOrEqual(0);

    // Wait for the LSP to connect and publish the mismatch marker (this doubles as the readiness gate).
    await expect.poll(async (): Promise<number> => errorMarkerCount(page, idx), { timeout: 20_000, intervals: [500] })
        .toBeGreaterThan(0);

    // Fix it: set the model to the solved form; the marker must clear after re-evaluation.
    await page.evaluate((i: number): void => {
        (window as any).NelumboFields.__editors[i].model.setValue('import nelumbo.logic\n\nfalse | true ? [()][]\n');
    }, idx);
    await expect.poll(async (): Promise<number> => errorMarkerCount(page, idx), { timeout: 20_000, intervals: [500] })
        .toBe(0);
});

test('go-to-definition moves the selection to the declaration', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    await page.locator('aside nav a[data-section="rules"]').click();
    await expect(page.locator('#rules')).toHaveClass(/active/);

    // Wait until the LSP is connected (a mismatching field somewhere has a marker), so providers are live.
    const logicIdx: number = await editorIndexContaining(page, 'false & true');
    await expect.poll(async (): Promise<number> => errorMarkerCount(page, logicIdx), { timeout: 20_000, intervals: [500] })
        .toBeGreaterThan(0);

    const movedToDeclaration: boolean = await page.evaluate(async (): Promise<boolean> => {
        const nf:  any = (window as any).NelumboFields;
        const fib: any = nf.__editors.find((e: any): boolean => e.model.getValue().indexOf('fib(n)=f') >= 0);
        // Position on the use "fib(7)" (the query line), then reveal its definition.
        const matches: any[] = fib.model.findMatches('fib(7)', true, false, false, null, false);
        const use:     any = matches[0].range;
        fib.editor.setPosition({ lineNumber: use.startLineNumber, column: use.startColumn + 1 });
        const before: number = fib.editor.getPosition().lineNumber;
        await fib.editor.getAction('editor.action.revealDefinition').run();
        const after:  number = fib.editor.getPosition().lineNumber;
        // The declaration "Integer ::= fib(<Integer>)" / the rule is above the query use.
        return after < before;
    });
    expect(movedToDeclaration).toBe(true);
});

test('hover shows information for a known symbol', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    await page.locator('aside nav a[data-section="rules"]').click();
    await expect(page.locator('#rules .monaco-editor').first()).toBeVisible();

    const logicIdx: number = await editorIndexContaining(page, 'false & true');
    await expect.poll(async (): Promise<number> => errorMarkerCount(page, logicIdx), { timeout: 20_000, intervals: [500] })
        .toBeGreaterThan(0);

    // Position on a token the server provides hover for (adjust to what Step 1 confirmed).
    await page.evaluate((): void => {
        const nf:  any = (window as any).NelumboFields;
        const fib: any = nf.__editors.find((e: any): boolean => e.model.getValue().indexOf('fib(n)=f') >= 0);
        const m:   any = fib.model.findMatches('fib(n)', true, false, false, null, false)[0].range;
        fib.editor.setPosition({ lineNumber: m.startLineNumber, column: m.startColumn + 1 });
        fib.editor.getAction('editor.action.showHover').run();
    });
    await expect(page.locator('.monaco-hover').first()).toBeVisible({ timeout: 10_000 });
});

test('completion offers suggestions', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    await page.locator('aside nav a[data-section="rules"]').click();
    await expect(page.locator('#rules .monaco-editor').first()).toBeVisible();

    const logicIdx: number = await editorIndexContaining(page, 'false & true');
    await expect.poll(async (): Promise<number> => errorMarkerCount(page, logicIdx), { timeout: 20_000, intervals: [500] })
        .toBeGreaterThan(0);

    // Trigger completion at a context that returns items (adjust to what Step 1 confirmed).
    await page.evaluate((): void => {
        const nf:  any = (window as any).NelumboFields;
        const fib: any = nf.__editors.find((e: any): boolean => e.model.getValue().indexOf('fib(n)=f') >= 0);
        fib.editor.focus();
        const last: number = fib.model.getLineCount();
        fib.editor.setPosition({ lineNumber: last, column: fib.model.getLineMaxColumn(last) });
        fib.editor.getAction('editor.action.triggerSuggest').run();
    });
    await expect(page.locator('.suggest-widget').first()).toBeVisible({ timeout: 10_000 });
});
```

- [ ] **Step 3: Run the LSP spec and adjust hover/completion targets**

Run: `cd http/src/main/frontend && npx playwright test lsp.spec.ts`
Expected: the diagnostics and go-to-definition tests PASS. If hover or completion fails because the targeted token/context returns no content, use `npx playwright test lsp.spec.ts --debug` or `--headed` and the Step 1 findings to retarget to a token/context that DOES return content. If a given provider genuinely returns nothing usable anywhere in these fields, weaken that one test to assert the action runs without throwing (and record it as a concern) rather than shipping a flaky content assertion - but diagnostics + go-to-definition must be real, passing assertions.

- [ ] **Step 4: Sanity-check the diagnostics assertion actually guards**

Temporarily change the logic exercise scaffold in `tour.html` to the already-solved `false | true` (so no mismatch), rebuild the jar, and confirm the diagnostics test FAILS (proving it detects a real regression). Then revert the scaffold and rebuild. (Do not commit the temporary change.)

- [ ] **Step 5: Commit**

```bash
git add http/src/main/frontend/e2e/lsp.spec.ts
git commit -m "test(http): LSP-dependent e2e tests (diagnostics, hover, completion, go-to-definition)"
```

---

### Task 4: CI wiring

**Files:**
- Modify: `.github/workflows/build.yaml`

- [ ] **Step 1: Add the e2e step after the build step**

In `.github/workflows/build.yaml`, immediately after the `- name: "build"` step (and before the artifact uploads), add (match the file's 4-space/aligned YAML style):

```yaml
            ################################################################################
            -   name: "e2e (playwright)"
                working-directory: http/src/main/frontend
                run : |
                    npm ci
                    npx playwright install --with-deps chromium
                    ../../../../gradlew :http:serverJar
                    npx playwright test
```

(The job already exports `CI: "true"`, so `playwright.config.ts` spawns a fresh server and enables retries; `reuseExistingServer` is off in CI.)

- [ ] **Step 2: Validate the workflow YAML locally**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/build.yaml')); print('yaml ok')"`
Expected: `yaml ok`.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build.yaml
git commit -m "ci: run the Playwright feature-tour e2e tests"
```

---

### Task 5: Docs

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Document the e2e tests**

In `CLAUDE.md`, in the frontend paragraph of the "HTTP Module - LSP over WebSocket" section, append:

`End-to-end browser tests live in http/src/main/frontend/e2e/ (Playwright, Chromium): run ./gradlew :http:serverJar then (in http/src/main/frontend) npm run test:e2e:install once and npm run test:e2e. They cover tour structure, Run->/eval, Show-solution, and the LSP features (diagnostics, hover, completion, go-to-definition) via the NelumboFields.__editors/.__monaco test hook. CI runs them after the build.`

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: describe the Playwright e2e tests"
```

---

## Verification checklist (after all tasks)

- [ ] `./gradlew :http:serverJar` then `cd http/src/main/frontend && npm run test:e2e` is green (all specs)
- [ ] tour.spec.ts: page/nav/section-mount/Run/Show-solution/playground-link all pass
- [ ] lsp.spec.ts: diagnostics (appears + clears) and go-to-definition are real passing assertions; hover/completion pass against a confirmed token (or are documented as weakened)
- [ ] The diagnostics test fails when the exercise is pre-solved (guard sanity), passes when unsolved
- [ ] `.github/workflows/build.yaml` runs the e2e step after build; YAML valid
- [ ] Playwright output dirs are gitignored; only source + config + specs committed
