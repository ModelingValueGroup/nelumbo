import { test, expect, Page } from '@playwright/test';

test('docs overview lists the pages in a sidebar and renders the markdown', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/docs/');
    await expect(page).toHaveTitle('Nelumbo documentation');
    await expect(page.locator('article h1')).toHaveText('Nelumbo documentation');
    await expect(page.locator('aside nav a.active')).toHaveText('Overview');
    await expect(page.locator('aside nav a[href="/docs/reference/lang/grammar.html"]')).toBeVisible();
    // the overview's logo is a relative <img>, which only resolves because /docs redirects to /docs/
    await expect(page.locator('article img').first()).toHaveJSProperty('complete', true);
    expect(await page.locator('article img').first().evaluate((img: HTMLImageElement): number => img.naturalWidth)).toBeGreaterThan(0);
});

test('cross-page links in the docs resolve, anchors included', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/docs/reference/lang/index.html');
    await expect(page.locator('article h1')).toContainText('nelumbo.lang');
    await expect(page.locator('aside nav a.active')).toContainText('nelumbo.lang');
    // the lang overview links into the lang grammar page; the docs were written for GitHub, so the anchor must survive rendering
    const link = page.locator('article a[href^="/docs/reference/lang/grammar.html#"]').first();
    await expect(link).toBeVisible();
    const fragment: string = (await link.getAttribute('href') ?? '').split('#')[1];
    await link.click();
    await expect(page).toHaveURL(/\/docs\/reference\/lang\/grammar\.html#/);
    await expect(page.locator('[id="' + fragment + '"]')).toBeVisible();
});

test('links that leave the docs folder go to the GitHub repository', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/docs/getting-started/first-program.html');
    const github = page.locator('article a[href^="https://github.com/ModelingValueGroup/nelumbo/blob/master/src/"]').first();
    await expect(github).toBeVisible();
});

test('landing, tour and sandbox link to the docs', async ({ page }: { page: Page }): Promise<void> => {
    for (const path of ['/', '/tour.html', '/sandbox.html']) {
        await page.goto(path);
        await expect(page.locator('a[href="/docs/"]').first()).toBeAttached();
    }
});

test('nelumbo code blocks are syntax highlighted', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/docs/getting-started/first-program.html');
    const block = page.locator('article pre code.language-nelumbo').first();
    await expect(block.locator('.nl-type').first()).toBeVisible();
    await expect(block.locator('.nl-keyword').first()).toBeVisible();
    // colored, not just wrapped: a type and a keyword do not share a color
    const typeColor: string = await block.locator('.nl-type').first().evaluate((e: Element): string => getComputedStyle(e).color);
    const keywordColor: string = await block.locator('.nl-keyword').first().evaluate((e: Element): string => getComputedStyle(e).color);
    expect(typeColor).not.toBe(keywordColor);
});
