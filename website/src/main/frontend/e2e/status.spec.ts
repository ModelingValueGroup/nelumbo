import { test, expect, Locator, Page, Route } from '@playwright/test';

test('the status dot shows the server status; only shift+click on it opens the status page', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    const dot: Locator = page.locator('.status-dot[data-status-link]');
    await expect(dot).toHaveClass(/\b(ok|busy|overloaded)\b/, { timeout: 10_000 });
    await expect(page.locator('a[href="/status.html"]')).toHaveCount(0);
    await dot.click();
    await expect(page).toHaveURL(/\/$/);
    await dot.click({ modifiers: ['Shift'] });
    await expect(page).toHaveURL(/\/status\.html$/);
    await expect(page.locator('#sessions')).toHaveText(/^\d+ \/ \d+$/, { timeout: 10_000 });
});

test('the status page updates live and shows the history charts', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/status.html');
    const updated: Locator = page.locator('#updated');
    await expect(updated).toHaveText(/^\d{2}:\d{2}:\d{2}$/, { timeout: 10_000 });
    const first: string = await updated.innerText();
    await expect(updated).not.toHaveText(first, { timeout: 15_000 });
    await expect(page.locator('#chart-cpu .uplot')).toHaveCount(1);
    await expect(page.locator('#chart-activity .uplot')).toHaveCount(1);
    await page.locator('button[data-range="week"]').click();
    await expect(page.locator('button[data-range="week"]')).toHaveAttribute('aria-pressed', 'true');
});

test('the status page fits a phone screen', async ({ page }: { page: Page }): Promise<void> => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/status.html');
    await expect(page.locator('#chart-cpu .uplot')).toHaveCount(1, { timeout: 10_000 });
    const overflow: boolean = await page.evaluate((): boolean => document.documentElement.scrollWidth > document.documentElement.clientWidth);
    expect(overflow).toBe(false);
});

test('the status page updates from the stream when plain /stats is blocked', async ({ page }: { page: Page }): Promise<void> => {
    await page.route((url: URL): boolean => url.pathname === '/stats', (route: Route): Promise<void> => route.abort());
    await page.goto('/status.html');
    const updated: Locator = page.locator('#updated');
    await expect(updated).toHaveText(/^\d{2}:\d{2}:\d{2}$/, { timeout: 10_000 });
    const first: string = await updated.innerText();
    await expect(updated).not.toHaveText(first, { timeout: 15_000 });
});

test('the status page falls back to polling when the stream is blocked', async ({ page }: { page: Page }): Promise<void> => {
    await page.route((url: URL): boolean => url.pathname === '/stats/stream', (route: Route): Promise<void> => route.abort());
    await page.goto('/status.html');
    const updated: Locator = page.locator('#updated');
    await expect(updated).toHaveText(/^\d{2}:\d{2}:\d{2}$/, { timeout: 10_000 });
    const first: string = await updated.innerText();
    await expect(updated).not.toHaveText(first, { timeout: 15_000 });
});
