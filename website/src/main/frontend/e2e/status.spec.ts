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
    await expect(page.locator('#chart-stats .uplot')).toHaveCount(1);
    await page.locator('button[data-range="week"]').click();
    await expect(page.locator('button[data-range="week"]')).toHaveAttribute('aria-pressed', 'true');
});

test('the status page fits a phone screen', async ({ page }: { page: Page }): Promise<void> => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/admin');
    await expect(page.locator('#chart-stats .uplot')).toHaveCount(1, { timeout: 10_000 });
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
    await expect(page.locator('#chart-stats .u-legend')).toContainText('Shares', { timeout: 10_000 });
    await expect(page.locator('#history-table thead')).toContainText('Disk MB');
});

test('the stats are one graph with five lanes', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/admin');
    await expect(page.locator('#chart-stats .uplot')).toHaveCount(1, { timeout: 10_000 });
    await expect(page.locator('#panel-stats .uplot')).toHaveCount(1);
    const ids: string[] = await page.locator('#panel-stats [id^="chart-"]').evaluateAll((els: Element[]): string[] => els.map((e: Element): string => e.id));
    expect(ids).toEqual(['chart-stats']);
    const legend: Locator = page.locator('#chart-stats .u-legend');
    for (const label of ['CPU avg', 'Evaluations/s', 'Sessions', 'Shared solutions', 'On disk']) {
        await expect(legend).toContainText(label);
    }
    await expect(page.locator('#history-table thead')).toContainText('Eval/s');
});

type Night = [number, number];

const TEN_MINUTES: number = 10 * 60_000;

async function nightsBetween(page: Page, from: string, to: string): Promise<Night[]> {
    return page.evaluate((range: Night): Night[] =>
        (window as unknown as { NelumboAdmin: { nights: (fromMs: number, toMs: number) => Night[] } }).NelumboAdmin.nights(range[0], range[1]),
        [Date.parse(from), Date.parse(to)] as Night);
}

function expectNight(nights: Night[], sunset: string, sunrise: string): void {
    const shown: string           = nights.map((n: Night): string => new Date(n[0]).toISOString() + ' - ' + new Date(n[1]).toISOString()).join(', ');
    const night: Night | undefined = nights.find((n: Night): boolean => Math.abs(n[0] - Date.parse(sunset)) < TEN_MINUTES);
    expect(night, 'a night starting near ' + sunset + ' in ' + shown).toBeDefined();
    expect(Math.abs(night![1] - Date.parse(sunrise)), 'its sunrise in ' + shown).toBeLessThan(TEN_MINUTES);
}

test('nights run from sunset to sunrise in Waardenburg', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/admin');
    const june:     Night[] = await nightsBetween(page, '2026-06-21T00:00:00Z', '2026-06-22T00:00:00Z');
    const december: Night[] = await nightsBetween(page, '2026-12-21T00:00:00Z', '2026-12-22T00:00:00Z');
    expectNight(june, '2026-06-21T20:00:00Z', '2026-06-22T03:20:00Z');
    expectNight(december, '2026-12-21T15:32:00Z', '2026-12-22T07:44:00Z');
    for (const n of june) {
        expect(n[0] < Date.parse('2026-06-22T00:00:00Z') && n[1] > Date.parse('2026-06-21T00:00:00Z')).toBe(true);
    }
});

test('the stats graph uses the window width and follows its resizes', async ({ page }: { page: Page }): Promise<void> => {
    await page.setViewportSize({ width: 1600, height: 900 });
    await page.goto('/admin');
    const graph: Locator = page.locator('#chart-stats .uplot');
    await expect(graph).toHaveCount(1, { timeout: 10_000 });
    expect((await graph.boundingBox())!.width).toBeGreaterThan(1400);
    await page.setViewportSize({ width: 400, height: 800 });
    await expect.poll(async (): Promise<number> => (await graph.boundingBox())!.width).toBeLessThanOrEqual(400 - 2 * 16);
    const overflow: boolean = await page.evaluate((): boolean => document.documentElement.scrollWidth > document.documentElement.clientWidth);
    expect(overflow).toBe(false);
    await page.setViewportSize({ width: 1600, height: 900 });
    await expect.poll(async (): Promise<number> => (await graph.boundingBox())!.width).toBeGreaterThan(1400);
});
