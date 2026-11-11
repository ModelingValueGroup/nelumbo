import { test, expect, Page, Locator } from '@playwright/test';

const SECTIONS: string[] = ['syntax', 'patterns', 'precedence', 'parentheses', 'generics', 'connected', 'named-patterns', 'scoping', 'logic', 'facts', 'unique', 'rules', 'combining', 'cycles', 'types', 'equality', 'relations', 'quantifiers', 'dsl', 'transform', 'cnl', 'family-tree', 'club-fees', 'train-network', 'number-toolkit', 'set-algebra', 'list-statistics', 'vectors', 'interpreter', 'room-booking', 'access-policy'];

async function showSection(page: Page, id: string): Promise<Locator> {
    await page.locator('aside nav a[data-section="' + id + '"]').click();
    const section: Locator = page.locator('#' + id);
    await expect(section).toHaveClass(/active/);
    return section;
}

test('tutorial loads with the first syntax section active and the full sidebar', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tutorial.html');
    for (const id of SECTIONS) {
        await expect(page.locator('aside nav a[data-section="' + id + '"]')).toBeVisible();
    }
    await expect(page.locator('aside nav .group')).toHaveText(['Syntax', 'Semantics', 'Transformations', 'Projects']);
    await expect(page.locator('#syntax')).toHaveClass(/active/);
});

test('each section switches and mounts a Monaco editor', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tutorial.html#logic');
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
    await page.goto('/tutorial.html#logic');
    const demo: Locator = page.locator('#logic .nelumbo-field-wrap').first();
    // the LSP evaluates on a debounce; "true & true ?" renders its result [()][] as an inlay hint
    await expect(demo.getByText('[()][]').first()).toBeVisible({ timeout: 20_000 });
});

test('hovering an inline result shows the full-result tooltip', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tutorial.html#logic');
    const demo: Locator = page.locator('#logic .nelumbo-field-wrap').first();
    const hint: Locator = demo.getByText('[()][]').first();
    await expect(hint).toBeVisible({ timeout: 20_000 });
    await hint.hover();
    const hover: Locator = page.locator('.monaco-hover').first();
    await expect(hover).toBeVisible({ timeout: 10_000 });
    await expect(hover).toContainText('[()][]');
});

test('Show solution toggles the solution block', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tutorial.html#logic');
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

test('Next and Previous walk the sections in sidebar order', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tutorial.html');
    await expect(page.locator('#' + SECTIONS[0] + ' .pager a.prev')).toHaveCount(0);
    for (let i = 1; i < SECTIONS.length; i++) {
        await page.locator('#' + SECTIONS[i - 1] + ' .pager a.next').click();
        await expect(page.locator('#' + SECTIONS[i])).toHaveClass(/active/);
        await expect(page.locator('aside nav a[data-section="' + SECTIONS[i] + '"]')).toHaveClass(/active/);
        await expect(page).toHaveURL(new RegExp('#' + SECTIONS[i] + '$'));
    }
    await expect(page.locator('#' + SECTIONS[SECTIONS.length - 1] + ' .pager a.next')).toHaveCount(0);
    for (let i = SECTIONS.length - 1; i > 0; i--) {
        await page.locator('#' + SECTIONS[i] + ' .pager a.prev').click();
        await expect(page.locator('#' + SECTIONS[i - 1])).toHaveClass(/active/);
    }
});

test('a sidebar group collapses and opens again when one of its sections is shown', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tutorial.html#scoping');
    const link: Locator = page.locator('aside nav a[data-section="logic"]');
    await page.locator('aside nav .group', { hasText: 'Semantics' }).click();
    await expect(link).toBeHidden();
    await page.locator('#scoping .pager a.next').click();
    await expect(link).toBeVisible();
    await expect(link).toHaveClass(/active/);
});

test('the Sandbox link navigates to /sandbox.html', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/tutorial.html#logic');
    await page.locator('.site-header nav a[href="/sandbox.html"]').click();
    await expect(page).toHaveURL(/\/sandbox\.html$/);
    await expect(page.locator('.nelumbo-field-wrap').first()).toBeVisible();
});
