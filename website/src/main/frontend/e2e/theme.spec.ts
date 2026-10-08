import { test, expect, Page, Locator } from '@playwright/test';

const PAGES: string[] = ['/', '/tour.html', '/sandbox.html', '/docs/'];

function theme(page: Page): Promise<string | null> {
    return page.locator('html').getAttribute('data-theme');
}

// without a stored choice every page must follow the OS preference, in both directions
for (const path of PAGES) {
    test('without an override ' + path + ' follows the OS color scheme', async ({ page }: { page: Page }): Promise<void> => {
        await page.emulateMedia({ colorScheme: 'light' });
        await page.goto(path);
        expect(await theme(page)).toBe('light');
        await page.emulateMedia({ colorScheme: 'dark' });
        await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
        await expect(page.locator('.site-header .theme-toggle, header.top .theme-toggle')).toBeVisible();
    });
}

test('the switch cycles auto -> light -> dark -> auto and the override survives a reload', async ({ page }: { page: Page }): Promise<void> => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.goto('/docs/');
    const toggle: Locator = page.locator('.theme-toggle');
    await expect(page.locator('html')).toHaveAttribute('data-theme-mode', 'auto');

    await toggle.click();
    await expect(page.locator('html')).toHaveAttribute('data-theme-mode', 'light');
    expect(await theme(page)).toBe('light');
    // the choice beats the OS preference, also on another page after navigation
    await page.goto('/tour.html');
    expect(await theme(page)).toBe('light');

    await page.locator('.theme-toggle').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme-mode', 'dark');
    await page.reload();
    await page.emulateMedia({ colorScheme: 'light' });
    expect(await theme(page)).toBe('dark');

    // back to auto: the override is gone and the OS preference (now light) wins again
    await page.locator('.theme-toggle').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme-mode', 'auto');
    expect(await theme(page)).toBe('light');
    expect(await page.evaluate((): string | null => localStorage.getItem('nelumbo-theme'))).toBeNull();
});

test('the switch labels its current mode and the next one for screen readers', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await expect(page.locator('.theme-toggle')).toHaveAttribute('aria-label', 'Theme: auto (switch to light)');
});

test('open Monaco editors switch theme along with the page', async ({ page }: { page: Page }): Promise<void> => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.goto('/sandbox.html');
    const editor: Locator = page.locator('.monaco-editor').first();
    await expect(editor).toHaveClass(/\bvs-dark\b/);
    await page.locator('.theme-toggle').click();
    await expect(editor).toHaveClass(/\bvs\b/);
    await expect(editor).not.toHaveClass(/\bvs-dark\b/);
});
