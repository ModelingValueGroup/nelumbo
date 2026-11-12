import { test, expect, Locator, Page, Route } from '@playwright/test';

test('the status dot shows the server status; shift+click does nothing', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    const dot: Locator = page.locator('.status-dot');
    await expect(dot).toHaveClass(/\b(ok|busy|overloaded)\b/, { timeout: 10_000 });
    await dot.click({ modifiers: ['Shift'] });
    await expect(page).toHaveURL(/\/$/);
});

test('the status page updates live and shows the history charts', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/admin');
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
    await page.goto('/admin');
    await expect(page.locator('#chart-cpu .uplot')).toHaveCount(1, { timeout: 10_000 });
    const overflow: boolean = await page.evaluate((): boolean => document.documentElement.scrollWidth > document.documentElement.clientWidth);
    expect(overflow).toBe(false);
});

test('the status page updates from the stream when plain /stats is blocked', async ({ page }: { page: Page }): Promise<void> => {
    await page.route((url: URL): boolean => url.pathname === '/stats', (route: Route): Promise<void> => route.abort());
    await page.goto('/admin');
    const updated: Locator = page.locator('#updated');
    await expect(updated).toHaveText(/^\d{2}:\d{2}:\d{2}$/, { timeout: 10_000 });
    const first: string = await updated.innerText();
    await expect(updated).not.toHaveText(first, { timeout: 15_000 });
});

test('the status page falls back to polling when the stream is blocked', async ({ page }: { page: Page }): Promise<void> => {
    await page.route((url: URL): boolean => url.pathname === '/stats/stream', (route: Route): Promise<void> => route.abort());
    await page.goto('/admin');
    const updated: Locator = page.locator('#updated');
    await expect(updated).toHaveText(/^\d{2}:\d{2}:\d{2}$/, { timeout: 10_000 });
    const first: string = await updated.innerText();
    await expect(updated).not.toHaveText(first, { timeout: 15_000 });
});

test('the status page shows the shared solutions', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/admin');
    await expect(page.locator('#solutions')).toHaveText(/^\d+$/, { timeout: 10_000 });
    await expect(page.locator('#solution-disk')).toHaveText(/^\d+\.\d MB of 100 MB$/);
    await expect(page.locator('#chart-solutions .uplot')).toHaveCount(1);
    await expect(page.locator('#chart-disk .uplot')).toHaveCount(1);
    await expect(page.locator('#chart-activity .u-legend')).toContainText('Shares');
    await expect(page.locator('#history-table thead')).toContainText('Disk MB');
});

test('the status page shows evaluations per second', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/admin');
    await expect(page.locator('#chart-evals .uplot')).toHaveCount(1, { timeout: 10_000 });
    await expect(page.locator('#chart-evals .u-legend')).toContainText('Evaluations/s');
    await expect(page.locator('#history-table thead')).toContainText('Eval/s');
    const order: string[] = await page.locator('#panel-stats .chart > div[id^="chart-"]').evaluateAll((els: Element[]): string[] => els.map((e: Element): string => e.id));
    expect(order.slice(0, 2)).toEqual(['chart-cpu', 'chart-evals']);
});
