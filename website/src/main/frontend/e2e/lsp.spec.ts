import { test, expect, Page } from '@playwright/test';

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

async function errorMarkerCount(page: Page, index: number): Promise<number> {
    return await page.evaluate((i: number): number => {
        const nf:      any = (window as any).NelumboFields;
        const model:   any = nf.__editors[i].model;
        const markers: Array<{ severity: number }> = nf.__monaco.editor.getModelMarkers({ resource: model.uri });
        return markers.filter((m: { severity: number }): boolean => m.severity === 8).length;
    }, index);
}

async function awaitLspConnected(page: Page): Promise<void> {
    const logicIdx: number = await editorIndexContaining(page, 'false & true');
    expect(logicIdx).toBeGreaterThanOrEqual(0);
    await expect.poll(async (): Promise<number> => errorMarkerCount(page, logicIdx), { timeout: 20_000, intervals: [500] })
        .toBeGreaterThan(0);
}

test('an unsolved exercise shows a diagnostic that clears when solved', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    const idx: number = await editorIndexContaining(page, 'false & true');
    expect(idx).toBeGreaterThanOrEqual(0);
    await expect.poll(async (): Promise<number> => errorMarkerCount(page, idx), { timeout: 20_000, intervals: [500] })
        .toBeGreaterThan(0);
    await page.evaluate((i: number): void => {
        (window as any).NelumboFields.__editors[i].model.setValue('import nelumbo.logic\n\nfalse | true ? [()][]\n');
    }, idx);
    await expect.poll(async (): Promise<number> => errorMarkerCount(page, idx), { timeout: 20_000, intervals: [500] })
        .toBe(0);
});

test('go-to-definition moves the selection to the declaration', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    await page.locator('aside nav a[data-section="rules"]').click();
    await expect(page.locator('#rules')).toHaveClass(/active/);
    await awaitLspConnected(page);
    const result: { before: number; after: number } = await page.evaluate(async (): Promise<{ before: number; after: number }> => {
        const nf:  any = (window as any).NelumboFields;
        const fib: any = nf.__editors.find((e: any): boolean => e.model.getValue().indexOf('fib(n)=f') >= 0);
        const use: any = fib.model.findMatches('fib(7)', true, false, false, null, false)[0].range;
        fib.editor.setPosition({ lineNumber: use.startLineNumber, column: use.startColumn + 1 });
        const before: number = fib.editor.getPosition().lineNumber;
        await fib.editor.getAction('editor.action.revealDefinition').run();
        const after:  number = fib.editor.getPosition().lineNumber;
        return { before, after };
    });
    // fib(7) is on the query line; its declaration Integer ::= fib(<Integer>) is earlier in the field.
    expect(result.after).toBeLessThan(result.before);
    expect(result.after).toBeGreaterThan(0);
});

test('hover shows information for a known symbol', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    await page.locator('aside nav a[data-section="rules"]').click();
    await expect(page.locator('#rules .monaco-editor').first()).toBeVisible();
    await awaitLspConnected(page);
    // The Integer type reference in the declaration returns a documented hover ("type Integer ...").
    await page.evaluate((): void => {
        const nf:  any = (window as any).NelumboFields;
        const fib: any = nf.__editors.find((e: any): boolean => e.model.getValue().indexOf('fib(n)=f') >= 0);
        const m:   any = fib.model.findMatches('Integer ::=', true, false, false, null, false)[0].range;
        fib.editor.focus();
        fib.editor.setPosition({ lineNumber: m.startLineNumber, column: m.startColumn + 1 });
        fib.editor.getAction('editor.action.showHover').run();
    });
    const hover = page.locator('.monaco-hover').first();
    await expect(hover).toBeVisible({ timeout: 10_000 });
    await expect(hover).toContainText('Integer');
});

test('completion offers suggestions', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    await page.locator('aside nav a[data-section="rules"]').click();
    await expect(page.locator('#rules .monaco-editor').first()).toBeVisible();
    await awaitLspConnected(page);
    // At the end of the fib field on a fresh line the LSP offers identifier completions (fib, factorial, ...).
    await page.evaluate((): void => {
        const nf:  any = (window as any).NelumboFields;
        const fib: any = nf.__editors.find((e: any): boolean => e.model.getValue().indexOf('fib(n)=f') >= 0);
        const monaco: any = nf.__monaco;
        fib.editor.focus();
        const last: number = fib.model.getLineCount();
        const col:  number = fib.model.getLineMaxColumn(last);
        fib.editor.executeEdits('e2e', [{ range: new monaco.Range(last, col, last, col), text: '\n' }]);
        const nl: number = fib.model.getLineCount();
        fib.editor.setPosition({ lineNumber: nl, column: 1 });
        fib.editor.getAction('editor.action.triggerSuggest').run();
    });
    const widget = page.locator('.suggest-widget').first();
    await expect(widget).toBeVisible({ timeout: 10_000 });
    await expect(widget.locator('.monaco-list-row').first()).toBeVisible();
});
