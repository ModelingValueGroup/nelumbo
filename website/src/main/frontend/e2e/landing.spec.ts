import { test, expect, Page, Locator } from '@playwright/test';

test('landing page introduces Nelumbo and links to tour and playground', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    await expect(page).toHaveTitle(/Nelumbo/);
    await expect(page.locator('h1')).toContainText('language');
    await expect(page.locator('a[href="/tour.html"]').first()).toBeVisible();
    await expect(page.locator('a[href="/playground.html"]').first()).toBeVisible();
});

test('landing page tour button navigates to the tour', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    await page.locator('.cta a[href="/tour.html"]').first().click();
    await expect(page.locator('aside nav a[data-section="logic"]')).toBeVisible();
});

test('the landing showcase is a live editor that verifies its query', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    const card: Locator = page.locator('.codecard');
    await expect(card.locator('.monaco-editor').first()).toBeVisible();
    // the LSP evaluates the snippet; the matched expected result renders as a checkmark hint
    await expect(card.getByText('✅').first()).toBeVisible({ timeout: 20_000 });
});
