import { test, expect, Page } from '@playwright/test';

// served by the second webServer in playwright.config.ts: every evaluation is "busy" with a 300 ms budget
test.use({ baseURL: 'http://localhost:8898' });

const HEAVY: string = 'import nelumbo.integers\n'
                    + 'Integer ::= factorial(<Integer>)\n'
                    + 'Integer n, r\n'
                    + 'factorial(n)=r <=> r=1 if n<=0, r=n*factorial(n-1) if n>0\n'
                    + 'factorial(5000)=r ?\n';
const LIGHT: string = 'import nelumbo.logic\ntrue ?\n';

async function setSandbox(page: Page, text: string): Promise<void> {
    await page.evaluate((t: string): void => {
        (window as any).NelumboFields.__editors[0].model.setValue(t);
    }, text);
}

test('a stopped evaluation shows the overload banner until an evaluation succeeds', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    // the sandbox may load an example asynchronously; wait for it before overwriting
    await expect.poll(async (): Promise<number> => page.evaluate((): number => (window as any).NelumboFields.__editors?.[0]?.model.getValue().length ?? 0), { timeout: 20_000 })
        .toBeGreaterThan(0);
    await setSandbox(page, HEAVY);
    await expect(page.locator('.nelumbo-overload-banner')).toBeVisible({ timeout: 20_000 });
    await setSandbox(page, LIGHT);
    await expect(page.locator('.nelumbo-overload-banner')).toBeHidden({ timeout: 20_000 });
});
