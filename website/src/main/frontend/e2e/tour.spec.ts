import { test, expect, Page, Locator } from '@playwright/test';

const SECTIONS: string[] = ['logic', 'facts', 'rules', 'types', 'relations', 'dsl', 'transform', 'scoping'];

async function showSection(page: Page, id: string): Promise<Locator> {
    await page.locator('aside nav a[data-section="' + id + '"]').click();
    const section: Locator = page.locator('#' + id);
    await expect(section).toHaveClass(/active/);
    return section;
}

test('tour loads with the logic section active and the full sidebar', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    for (const id of SECTIONS) {
        await expect(page.locator('aside nav a[data-section="' + id + '"]')).toBeVisible();
    }
    await expect(page.locator('#logic')).toHaveClass(/active/);
});

test('each section switches and mounts a Monaco editor', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    for (const id of SECTIONS) {
        const section: Locator = await showSection(page, id);
        await expect(section.locator('.monaco-editor').first()).toBeVisible();
        for (const other of SECTIONS) {
            if (other !== id) {
                await expect(page.locator('#' + other)).not.toHaveClass(/active/);
            }
        }
    }
});

test('query results appear inline without pressing anything', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    const demo: Locator = page.locator('#logic .nelumbo-field-wrap').first();
    // the LSP evaluates on a debounce; "true & true ?" renders its result [()][] as an inlay hint
    await expect(demo.getByText('[()][]').first()).toBeVisible({ timeout: 20_000 });
});

test('hovering an inline result shows the full-result tooltip', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    const demo: Locator = page.locator('#logic .nelumbo-field-wrap').first();
    const hint: Locator = demo.getByText('[()][]').first();
    await expect(hint).toBeVisible({ timeout: 20_000 });
    await hint.hover();
    const hover: Locator = page.locator('.monaco-hover').first();
    await expect(hover).toBeVisible({ timeout: 10_000 });
    await expect(hover).toContainText('[()][]');
});

test('Show solution toggles the solution block', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    const exercise: Locator = page.locator('#logic .nelumbo-exercise');
    const solution: Locator = exercise.locator('.nelumbo-solution');
    const button:   Locator = exercise.getByRole('button', { name: 'Show solution' });
    await expect(solution).toBeHidden();
    await button.click();
    await expect(solution).toBeVisible();
    await expect(exercise.getByRole('button', { name: 'Hide solution' })).toBeVisible();
    await exercise.getByRole('button', { name: 'Hide solution' }).click();
    await expect(solution).toBeHidden();
});

test('the Playground link navigates to /playground.html', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tour.html');
    await page.locator('aside nav a[href="/playground.html"]').click();
    await expect(page).toHaveURL(/\/playground\.html$/);
    await expect(page.locator('.nelumbo-field-wrap').first()).toBeVisible();
});
