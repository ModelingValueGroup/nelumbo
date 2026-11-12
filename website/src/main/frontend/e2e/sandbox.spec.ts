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

// every example opened from the sidebar is a history entry, so Back and Forward walk them, back to the starter
test('Back and Forward return to the examples opened from the sidebar and to the starter', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    const starter: string = await editorText(page);
    const fibonacci: string = await exampleSource(page, 'fibonacci');
    const family: string = await exampleSource(page, 'family');
    await page.locator('aside nav a[data-example="fibonacci"]').click();
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(fibonacci);
    await page.locator('aside nav a[data-example="family"]').click();
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(family);
    await page.goBack();
    await expect(page).toHaveURL(/#fibonacci$/);
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(fibonacci);
    await expect(page.locator('aside nav a[data-example="fibonacci"]')).toHaveClass(/\bactive\b/);
    await page.goBack();
    await expect(page).toHaveURL(/\/sandbox\.html$/);
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(starter);
    await expect(page.locator('aside nav a.active')).toHaveCount(0);
    await page.goForward();
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(fibonacci);
});

test('Cmd/Ctrl+click on an example opens it in a new tab and leaves this one alone', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html#family');
    const source: string = await exampleSource(page, 'family');
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(source);
    const opened: Promise<Page> = page.context().waitForEvent('page');
    await page.locator('aside nav a[data-example="fibonacci"]').click({ modifiers: ['ControlOrMeta'] });
    const tab: Page = await opened;
    await tab.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    await expect.poll(async (): Promise<string> => editorText(tab), { timeout: 20_000 }).toBe(await exampleSource(page, 'fibonacci'));
    await tab.close();
    expect(await editorText(page)).toBe(source);
    await expect(page).toHaveURL(/#family$/);
});

test('a #name link opens the sandbox with that example loaded', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html#family');
    const source: string = await exampleSource(page, 'family');
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(source);
    await expect(page.locator('aside nav a[data-example="family"]')).toHaveClass(/\bactive\b/);
});

test('a sidebar group collapses and opens again when one of its examples is shown', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    const answer: Locator = page.locator('aside nav a[data-example="family"]');
    await expect(answer).toBeVisible();
    await page.locator('aside nav .group', { hasText: 'Exercises' }).click();
    await expect(answer).toBeHidden();
    await page.evaluate((): void => { location.hash = 'family'; });
    await expect(answer).toBeVisible();
    await expect(answer).toHaveClass(/\bactive\b/);
});
