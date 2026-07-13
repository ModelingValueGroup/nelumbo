import { test, expect, Page } from '@playwright/test';

test('landing page introduces Nelumbo and links to tour and playground', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    await expect(page).toHaveTitle(/Nelumbo/);
    await expect(page.locator('h1')).toContainText('Logic');
    await expect(page.locator('a[href="/tour.html"]').first()).toBeVisible();
    await expect(page.locator('a[href="/playground.html"]').first()).toBeVisible();
});

test('landing page tour button navigates to the tour', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    await page.locator('.cta a[href="/tour.html"]').first().click();
    await expect(page.locator('aside nav a[data-section="logic"]')).toBeVisible();
});
