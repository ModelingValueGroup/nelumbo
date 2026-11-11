import { test, expect, Page, Locator } from '@playwright/test';

async function editorText(page: Page): Promise<string> {
    return await page.evaluate((): string => (window as any).NelumboFields.__editors[0].model.getValue());
}

async function moduleSource(page: Page, name: string): Promise<string> {
    return await page.evaluate(async (n: string): Promise<string> => (await fetch('/library/' + n)).text(), name);
}

test('the header links the library between the sandbox and the docs', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.locator('.site-header nav a[href="/library.html"]').click();
    await expect(page).toHaveURL(/\/library\.html$/);
    await expect(page.locator('.site-header nav a[href="/library.html"]')).toHaveAttribute('aria-current', 'page');
});

test('the library lists the modules and opens the first one', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/library.html');
    const sidebar: Locator = page.locator('aside nav');
    for (const name of ['lang', 'logic', 'integers', 'rationals', 'strings', 'collections', 'datetime']) {
        await expect(sidebar.locator(`a[data-module="${name}"]`)).toBeVisible();
    }
    const source: string = await moduleSource(page, 'lang');
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(source);
    await expect(page.locator('#module-name')).toHaveText('nelumbo.lang');
});

test('clicking a module loads its source and explanation', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/library.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    await page.locator('aside nav a[data-module="collections"]').click();
    const source: string = await moduleSource(page, 'collections');
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(source);
    await expect(page).toHaveURL(/\/library\.html#collections$/);
    await expect(page.locator('aside nav a[data-module="collections"]')).toHaveClass(/\bactive\b/);
    await expect(page.locator('#module-name')).toHaveText('nelumbo.collections');
    await expect(page.locator('#module-text')).toContainText('sets and lists');
    await expect(page.locator('#module-import')).toHaveText('import nelumbo.collections');
    await expect(page.locator('#module-docs')).toHaveAttribute('href', '/docs/reference/packages/collections.html');
    // the new source is not left selected as a whole
    expect(await page.evaluate((): boolean => (window as any).NelumboFields.__editors[0].editor.getSelection().isEmpty())).toBe(true);
});

test('a #name link opens the library with that module loaded', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/library.html#datetime');
    const source: string = await moduleSource(page, 'datetime');
    await expect.poll(async (): Promise<string> => editorText(page)).toBe(source);
    await expect(page.locator('aside nav a[data-module="datetime"]')).toHaveClass(/\bactive\b/);
});

// The module source arrives after the editor opened (empty) on the LSP server; that open was once dropped, and the
// page then mostly stayed uncolored. Colors come only from semantic tokens: plain text has one color.
test('the module source is syntax colored', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/library.html');
    await expect.poll(async (): Promise<number> => page.evaluate((): number => {
        const colors: Set<string> = new Set();
        document.querySelectorAll('.nelumbo-field .view-lines span span').forEach((s: Element): void => {
            if ((s.textContent ?? '').trim()) { colors.add(getComputedStyle(s).color); }
        });
        return colors.size;
    }), { timeout: 10_000 }).toBeGreaterThan(1);
});

test('a sidebar group collapses and opens again when one of its modules is shown', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/library.html');
    const link: Locator = page.locator('aside nav a[data-module="datetime"]');
    await expect(link).toBeVisible();
    await page.locator('aside nav .group', { hasText: 'Packages' }).click();
    await expect(link).toBeHidden();
    await page.evaluate((): void => { location.hash = 'datetime'; });
    await expect(link).toBeVisible();
    await expect(link).toHaveClass(/\bactive\b/);
});
