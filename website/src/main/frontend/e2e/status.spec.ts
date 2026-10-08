import { test, expect, Locator, Page } from '@playwright/test';

test('the status dot shows the server status and opens the status page', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/');
    const dot: Locator = page.locator('a.status-dot');
    await expect(dot).toHaveClass(/\b(ok|busy|overloaded)\b/, { timeout: 10_000 });
    await dot.click();
    await expect(page).toHaveURL(/\/status\.html$/);
    await expect(page.locator('#sessions')).toHaveText(/^\d+ \/ \d+$/, { timeout: 10_000 });
});
