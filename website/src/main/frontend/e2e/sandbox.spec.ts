import { test, expect, Page, Locator } from '@playwright/test';

async function editorText(page: Page): Promise<string> {
    return await page.evaluate((): string => (window as any).NelumboFields.__editors[0].model.getValue());
}

async function exampleSource(page: Page, name: string): Promise<string> {
    return await page.evaluate(async (n: string): Promise<string> => (await fetch('/examples/' + n)).text(), name);
}

test('the sandbox sidebar lists the examples and exercises but no sudokus', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    const sidebar: Locator = page.locator('aside nav');
    await expect(sidebar.locator('a[data-example="fibonacci"]')).toBeVisible();
    // an exercise is listed with a link to its answer
    await expect(sidebar.locator('a[data-example="familyAssignment"]')).toBeVisible();
    await expect(sidebar.locator('a[data-example="family"]')).toBeVisible();
    await expect(sidebar.locator('a[data-example^="sudoku"]')).toHaveCount(0);
});

test('clicking an example loads it into the editor and links to it', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    const starter: string = await editorText(page);
    await page.locator('aside nav a[data-example="fibonacci"]').click();
    const source: string = await exampleSource(page, 'fibonacci');
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(source);
    await expect(page).toHaveURL(/\/sandbox\.html#fibonacci$/);
    await expect(page.locator('aside nav a[data-example="fibonacci"]')).toHaveClass(/\bactive\b/);
    // loading is one undoable edit, so whatever was in the editor can be brought back
    await page.evaluate((): void => (window as any).NelumboFields.__editors[0].editor.trigger('test', 'undo', null));
    expect(await editorText(page)).toBe(starter);
});

test('a #name link opens the sandbox with that example loaded', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html#family');
    const source: string = await exampleSource(page, 'family');
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(source);
    await expect(page.locator('aside nav a[data-example="family"]')).toHaveClass(/\bactive\b/);
});

test('the Library menu opens a standard library module in the same editor', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    const sidebar: Locator = page.locator('aside nav');
    await expect(sidebar.locator('.group', { hasText: 'Library' })).toBeVisible();
    await expect(sidebar.locator('a[data-example="library/lang"]')).toHaveText('lang.nl');
    await sidebar.locator('a[data-example="library/integers"]').click();
    const source: string = await page.evaluate(async (): Promise<string> => (await fetch('/library/integers')).text());
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(source);
    await expect(page).toHaveURL(/\/sandbox\.html#library\/integers$/);
    await expect(sidebar.locator('a[data-example="library/integers"]')).toHaveClass(/\bactive\b/);
});

test('a #library/<module> link opens the sandbox with that module loaded', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html#library/collections');
    const source: string = await page.evaluate(async (): Promise<string> => (await fetch('/library/collections')).text());
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(source);
    await expect(page.locator('aside nav a[data-example="library/collections"]')).toHaveClass(/\bactive\b/);
});
