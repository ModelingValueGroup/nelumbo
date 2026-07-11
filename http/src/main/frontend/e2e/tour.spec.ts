import { test, expect, Page, Locator } from '@playwright/test';

const SECTIONS: string[] = ['logic', 'facts', 'rules', 'types', 'relations', 'dsl', 'transform', 'scoping'];

async function showSection(page: Page, id: string): Promise<Locator> {
    await page.locator('aside nav a[data-section="' + id + '"]').click();
    const section: Locator = page.locator('#' + id);
    await expect(section).toHaveClass(/active/);
    return section;
}

test('tour loads with the logic section active and the full sidebar', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    for (const id of SECTIONS) {
        await expect(page.locator('aside nav a[data-section="' + id + '"]')).toBeVisible();
    }
    await expect(page.locator('#logic')).toHaveClass(/active/);
});

test('each section switches and mounts a Monaco editor', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
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

test('Run on the logic demo evaluates against /eval and renders a result', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    const demo:    Locator = page.locator('#logic .nelumbo-field-wrap').first();
    const results: Locator = demo.locator('.nelumbo-field-results');
    await demo.getByRole('button', { name: 'Run' }).click();
    await expect(results).toBeVisible();
    // assert the actual status badge, not just any "true" text (the query text also contains "true")
    await expect(results.locator('.badge.q-true').first()).toBeVisible();
});

test('Show solution toggles the solution block', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
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
    await page.goto('/');
    await page.locator('aside nav a[href="/playground.html"]').click();
    await expect(page).toHaveURL(/\/playground\.html$/);
    await expect(page.locator('.nelumbo-field-wrap').first()).toBeVisible();
});
