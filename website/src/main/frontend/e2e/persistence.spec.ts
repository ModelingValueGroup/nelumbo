import { test, expect, Page, Locator } from '@playwright/test';

// Edits are kept in localStorage (a refresh, or a phone that discarded the tab, brings them back) and the
// LSP connection comes back at the next user activity after it was lost (idle timeout, sleeping phone).

async function waitForEditors(page: Page): Promise<void> {
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
}

async function editorText(page: Page, index: number): Promise<string> {
    return await page.evaluate((i: number): string => (window as any).NelumboFields.__editors[i].model.getValue(), index);
}

async function typeAtEnd(page: Page, index: number, text: string): Promise<void> {
    await page.evaluate(({ i, t }: { i: number; t: string }): void => {
        const model: any = (window as any).NelumboFields.__editors[i].model;
        const end:   any = model.getFullModelRange().getEndPosition();
        model.pushEditOperations([], [{ range: { startLineNumber: end.lineNumber, startColumn: end.column, endLineNumber: end.lineNumber, endColumn: end.column }, text: t }], (): null => null);
    }, { i: index, t: text });
}

async function undo(page: Page, index: number): Promise<void> {
    await page.evaluate((i: number): void => (window as any).NelumboFields.__editors[i].editor.trigger('test', 'undo', null), index);
}

async function exampleSource(page: Page, name: string): Promise<string> {
    return await page.evaluate(async (n: string): Promise<string> => (await fetch('/examples/' + n)).text(), name);
}

test('an edited tour field survives a reload', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    await waitForEditors(page);
    const original: string = await editorText(page, 0);
    await typeAtEnd(page, 0, '\n// mine\n');
    await page.reload();
    await waitForEditors(page);
    expect(await editorText(page, 0)).toBe(original + '\n// mine\n');
});

test('Reset shows only for an edited field and brings back the original', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    await waitForEditors(page);
    const wrap:     Locator = page.locator('#logic .nelumbo-field-wrap').first();
    const reset:    Locator = wrap.getByRole('button', { name: 'Reset' });
    const original: string  = await editorText(page, 0);
    // a field without a solution has no toolbar until it is edited
    await expect(wrap.locator('.nelumbo-field-toolbar')).toBeHidden();
    await typeAtEnd(page, 0, '\n// mine\n');
    await expect(reset).toBeVisible();
    await reset.click();
    expect(await editorText(page, 0)).toBe(original);
    await expect(reset).toBeHidden();
    // the reset is one undoable edit
    await undo(page, 0);
    expect(await editorText(page, 0)).toBe(original + '\n// mine\n');
    await reset.click();
    await page.reload();
    await waitForEditors(page);
    expect(await editorText(page, 0)).toBe(original);
});

test('a saved edit of an older version of the field is not restored', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    await waitForEditors(page);
    const original: string = await editorText(page, 0);
    await typeAtEnd(page, 0, '\n// mine\n');
    // as if the field's original text changed with a deploy after the edit was saved
    await page.evaluate((): void => {
        for (const key of Object.keys(localStorage)) {
            const saved: { original: string; text: string } = JSON.parse(localStorage.getItem(key) as string);
            localStorage.setItem(key, JSON.stringify({ original: saved.original + 'old', text: saved.text }));
        }
    });
    await page.reload();
    await waitForEditors(page);
    expect(await editorText(page, 0)).toBe(original);
});

test('the sandbox keeps the edits per example, also across a reload', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html#fibonacci');
    await waitForEditors(page);
    const fibonacci: string = await exampleSource(page, 'fibonacci');
    const family:    string = await exampleSource(page, 'family');
    await expect.poll(async (): Promise<string> => editorText(page, 0)).toBe(fibonacci);
    await typeAtEnd(page, 0, '\n// mine\n');
    await page.locator('aside nav a[data-example="family"]').click();
    await expect.poll(async (): Promise<string> => editorText(page, 0)).toBe(family);
    await page.locator('aside nav a[data-example="fibonacci"]').click();
    await expect.poll(async (): Promise<string> => editorText(page, 0)).toBe(fibonacci + '\n// mine\n');
    await page.reload();
    await waitForEditors(page);
    await expect.poll(async (): Promise<string> => editorText(page, 0)).toBe(fibonacci + '\n// mine\n');
});

test('a lost LSP connection comes back at the next user activity', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    const hint: Locator = page.locator('#logic .nelumbo-field-wrap').first().getByText('[()][]').first();
    await expect(hint).toBeVisible({ timeout: 20_000 });
    await page.evaluate((): void => (window as any).NelumboFields.__closeConnection());
    await expect(hint).toBeHidden({ timeout: 10_000 });
    await page.mouse.move(20, 20);
    await page.mouse.move(40, 40);
    await expect(hint).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('.nelumbo-lsp-banner.visible')).toHaveCount(0);
});
