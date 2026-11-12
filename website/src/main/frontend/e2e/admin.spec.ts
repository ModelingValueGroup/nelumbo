import { test, expect, APIResponse, Browser, BrowserContext, Dialog, Locator, Page, Response } from '@playwright/test';

test.use({ baseURL: 'http://localhost:8897' });

const RUN: string = String(Date.now());

test('localhost sees the admin link and the page opens on Stats', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tutorial.html');
    const link: Locator = page.getByRole('link', { name: 'Admin' });
    await expect(link).toBeVisible();
    await link.click();
    await expect(page).toHaveURL(/\/admin$/);
    await expect(page.getByRole('tab', { name: 'Stats' })).toHaveAttribute('aria-selected', 'true');
    await expect(page.locator('#sessions')).toHaveText(/^\d+ \/ \d+$/, { timeout: 10_000 });
});

test('a non-admin sees no link and gets 404', async ({ browser }: { browser: Browser }): Promise<void> => {
    const context: BrowserContext = await browser.newContext({
        baseURL:          'http://localhost:8897',
        extraHTTPHeaders: { 'X-Nelumbo-Proxy': 'e2e-secret', 'X-Forwarded-For': '203.0.113.5' },
    });
    const page: Page              = await context.newPage();
    const me:   Promise<Response> = page.waitForResponse((r: Response): boolean => r.url().endsWith('/admin/me'));
    await page.goto('/tutorial.html');
    expect((await me).status()).toBe(404);
    await expect(page.locator('a.admin-link')).toHaveCount(0);
    expect((await context.request.get('/admin')).status()).toBe(404);
    await context.close();
});

test('snippets: list, open, delete one, delete all', async ({ page }: { page: Page }): Promise<void> => {
    const one: string = 'e2e-a-' + RUN;
    const two: string = 'e2e-b-' + RUN;
    await Promise.all([one, two].map(async (name: string): Promise<void> => {
        const put: APIResponse = await page.request.put('/solutions/' + name, { headers: { 'X-Nelumbo-Browser': 'e2e-1' }, data: 'text of ' + name });
        expect(put.status()).toBe(200);
    }));
    await page.goto('/admin#snippets');
    const rowOne: Locator = page.locator('#snippets tbody tr', { has: page.getByRole('button', { name: one, exact: true }) });
    const rowTwo: Locator = page.locator('#snippets tbody tr', { has: page.getByRole('button', { name: two, exact: true }) });
    await expect(rowOne).toHaveCount(1);
    await expect(rowTwo).toHaveCount(1);
    await expect(page.locator('#snippets th')).toHaveText(['Name', 'Saved', 'Size', 'Owner', 'IP', '']);
    await expect(rowOne.locator('td').nth(4)).not.toHaveText('-');
    await expect(rowOne.locator('td').nth(1)).toHaveText(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/);

    const name: Locator = rowOne.locator('.snippet-name');
    await name.evaluate((b: HTMLElement): void => {
        b.click();
        b.click();
    });
    await expect(name).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('tr.snippet-text pre')).toHaveText('text of ' + one);
    await expect(page.locator('tr.snippet-text')).toHaveCount(1);

    page.once('dialog', (d: Dialog): void => {
        expect(d.message()).toBe('Delete "' + one + '"?');
        d.accept();
    });
    await rowOne.getByRole('button', { name: 'Delete' }).click();
    await expect(page.getByRole('button', { name: one, exact: true })).toHaveCount(0);
    expect((await page.request.get('/solutions/' + one)).status()).toBe(404);

    page.once('dialog', (d: Dialog): void => {
        expect(d.message()).toMatch(/^Delete all \d+ shared snippets\?$/);
        d.accept();
    });
    await page.getByRole('button', { name: 'Delete all...' }).click();
    await expect(page.locator('#snippets tbody tr')).toHaveCount(0);
    await expect(page.locator('#snippets-status')).toHaveText(/^Deleted \d+ snippets\.$/);
});

test('a snippet text shows as text', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'e2e-xss-' + RUN;
    const text: string = '<img src=x onerror="window.__xss=1">';
    await page.request.put('/solutions/' + name, { headers: { 'X-Nelumbo-Browser': 'e2e-1' }, data: text });
    await page.goto('/admin#snippets');
    await page.getByRole('button', { name, exact: true }).click();
    await expect(page.locator('tr.snippet-text pre')).toHaveText(text);
    expect(await page.locator('tr.snippet-text pre').evaluate((e: HTMLElement): string => e.innerText)).toBe(text);
    await expect(page.locator('#snippets img')).toHaveCount(0);
    expect(await page.evaluate((): unknown => (window as unknown as { __xss?: number }).__xss)).toBeUndefined();
    await page.request.delete('/admin/solutions/' + name);
});

test('stats drawn after the snippets tab have their full width', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/admin#snippets');
    await page.getByRole('tab', { name: 'Stats' }).click();
    await expect(page.locator('#chart-cpu .uplot')).toHaveCount(1, { timeout: 10_000 });
    const box: { x: number; y: number; width: number; height: number } | null = await page.locator('#chart-cpu .uplot').boundingBox();
    expect(box!.width).toBeGreaterThan(200);
});

test('the header with the admin link never overflows', async ({ page }: { page: Page }): Promise<void> => {
    test.setTimeout(240_000);
    for (const path of ['/tutorial.html', '/sandbox.html', '/library.html', '/docs/', '/admin']) {
        await page.setViewportSize({ width: 699, height: 700 });
        await page.goto(path);
        await expect(page.locator('a.admin-link')).toBeHidden();
        // below 700 px the link is hidden: the header there is not this test's business
        for (let width: number = 700; width <= 1000; width += 5) {
            await page.setViewportSize({ width, height: 700 });
            const over: number = await page.evaluate((): number => document.documentElement.scrollWidth - document.documentElement.clientWidth);
            expect(over, path + ' at ' + width + ' px').toBeLessThanOrEqual(0);
        }
    }
});

test('switching sharing off hides the globe and refuses shares', async ({ page, context }: { page: Page; context: BrowserContext }): Promise<void> => {
    const name:  string  = 'e2e-off-' + RUN;
    const globe: Locator = page.getByRole('button', { name: 'Share or load by name' });
    const admin: Page    = await context.newPage();
    try {
        await page.goto('/tutorial.html#logic');
        await expect(globe.first()).toBeVisible();
        await admin.goto('/admin#snippets');
        await admin.locator('#sharing').click();
        await expect(admin.locator('#sharing')).not.toBeChecked();
        await expect.poll(async (): Promise<unknown> => (await (await page.request.get('/solutions')).json()).enabled).toBe(false);

        await globe.first().click();
        await page.getByRole('menuitem', { name: 'Share...' }).first().click();
        await page.getByLabel('Solution name').first().fill(name);
        await page.getByLabel('Solution name').first().press('Enter');
        await expect(page.locator('.nelumbo-share-message').first()).toHaveText('Sharing is turned off');

        const answered: Promise<Response> = page.waitForResponse((r: Response): boolean => new URL(r.url()).pathname === '/solutions');
        await page.reload();
        expect(await (await answered).json()).toEqual({ enabled: false });
        await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
        await expect(globe).toHaveCount(0);
    } finally {
        const restored: APIResponse = await page.request.put('/admin/sharing', { data: { enabled: true } });
        expect(restored.status()).toBe(200);
    }
});
