import { test, expect, Page, Locator, Browser, Dialog } from '@playwright/test';

// The globe menu (Share... / Load...): a text shared under a name in one browser loads by that name in any other.
// Names are global on the server and stay taken for a week, so every run uses names of its own.

const RUN: string = String(Date.now());

async function waitForEditors(page: Page): Promise<void> {
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
}

async function editorText(page: Page, index: number): Promise<string> {
    return await page.evaluate((i: number): string => (window as any).NelumboFields.__editors[i].model.getValue(), index);
}

async function typeAtEnd(page: Page, index: number, text: string): Promise<void> {
    await page.evaluate(({ i, t }: { i: number; t: string }): void => {
        const model: any = (window as any).NelumboFields.__editors[i].model;
        const end:   any = model.getFullModelRange().getEndPosition();
        model.pushEditOperations([], [{ range: { startLineNumber: end.lineNumber, startColumn: end.column, endLineNumber: end.lineNumber, endColumn: end.column }, text: t }], (): null => null);
    }, { i: index, t: text });
}

// the field of editor 0: the first one of #logic in the tutorial, the only one in the sandbox
function firstField(page: Page): Locator {
    return page.locator('#logic .nelumbo-field-wrap, main > .nelumbo-field-wrap').first();
}

async function openMenu(wrap: Locator, item: 'Share...' | 'Load...'): Promise<void> {
    await wrap.getByRole('button', { name: 'Share or load by name' }).click();
    await wrap.getByRole('menuitem', { name: item }).click();
}

async function share(wrap: Locator, action: 'Share' | 'Load', name: string): Promise<Locator> {
    await openMenu(wrap, action === 'Share' ? 'Share...' : 'Load...');
    await wrap.getByLabel('Solution name').fill(name);
    await wrap.getByLabel('Solution name').press('Enter');
    return wrap.locator('.nelumbo-share-message');
}

test('a shared solution loads in another browser, padded name and all', async ({ browser }: { browser: Browser }): Promise<void> => {
    const name: string = 'a' + RUN;
    const a:    Page   = await (await browser.newContext()).newPage();
    await a.goto('/tutorial.html#logic');
    await waitForEditors(a);
    await typeAtEnd(a, 0, '\n// by frits\n');
    await expect(await share(firstField(a), 'Share', '  ' + name + ' ')).toHaveText('Shared as "' + name + '" (kept 7 days)');
    const b: Page = await (await browser.newContext()).newPage();
    await b.goto('/sandbox.html');
    await waitForEditors(b);
    await expect(await share(firstField(b), 'Load', name)).toHaveText('Loaded "' + name + '"');
    expect(await editorText(b, 0)).toBe(await editorText(a, 0));
    await expect(await share(firstField(b), 'Share', name)).toHaveText('"' + name + '" is taken');
});

test('Load asks first only when the field holds own work', async ({ page }: { page: Page }): Promise<void> => {
    const name:    string   = 'b' + RUN;
    const dialogs: string[] = [];
    await page.goto('/tutorial.html#logic');
    await waitForEditors(page);
    const original: string = await editorText(page, 0);
    await typeAtEnd(page, 0, '\n// saved\n');
    await expect(await share(firstField(page), 'Share', name)).toHaveText('Shared as "' + name + '" (kept 7 days)');
    await firstField(page).getByRole('button', { name: 'Reset' }).click();
    page.on('dialog', (d: Dialog): void => {
        dialogs.push(d.message());
        void d.dismiss();
    });
    await expect(await share(firstField(page), 'Load', name)).toHaveText('Loaded "' + name + '"');
    expect(dialogs).toEqual([]);
    expect(await editorText(page, 0)).toBe(original + '\n// saved\n');
    await typeAtEnd(page, 0, '// mine\n');
    await share(firstField(page), 'Load', name);
    await expect.poll((): string[] => dialogs).toEqual(['Replace your own text?']);
    expect(await editorText(page, 0)).toBe(original + '\n// saved\n// mine\n');
});

test('Reset after Load brings back the original', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'c' + RUN;
    await page.goto('/tutorial.html#logic');
    await waitForEditors(page);
    const original: string = await editorText(page, 0);
    await typeAtEnd(page, 0, '\n// c\n');
    await expect(await share(firstField(page), 'Share', name)).toHaveText('Shared as "' + name + '" (kept 7 days)');
    await firstField(page).getByRole('button', { name: 'Reset' }).click();
    await expect(await share(firstField(page), 'Load', name)).toHaveText('Loaded "' + name + '"');
    await firstField(page).getByRole('button', { name: 'Reset' }).click();
    expect(await editorText(page, 0)).toBe(original);
});

test('the name field remembers the last name', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'd' + RUN;
    await page.goto('/sandbox.html');
    await waitForEditors(page);
    await expect(await share(firstField(page), 'Share', name)).toHaveText('Shared as "' + name + '" (kept 7 days)');
    await openMenu(firstField(page), 'Load...');
    await expect(firstField(page).getByLabel('Solution name')).toHaveValue(name);
});

test('the landing page and the library have no share buttons', async ({ page }: { page: Page }): Promise<void> => {
    for (const path of ['/', '/library.html']) {
        await page.goto(path);
        await waitForEditors(page);
        await expect(page.getByRole('button', { name: 'Share or load by name' })).toHaveCount(0);
    }
});

// crypto.randomUUID exists only in secure contexts: a server jar run on a classroom LAN over plain HTTP has none
test('sharing works without crypto.randomUUID', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'e' + RUN;
    await page.addInitScript((): void => {
        delete (Crypto.prototype as any).randomUUID;
    });
    await page.goto('/sandbox.html');
    await waitForEditors(page);
    await expect(await share(firstField(page), 'Share', name)).toHaveText('Shared as "' + name + '" (kept 7 days)');
});

test('the globe opens a menu with Share and Load that closes on Escape and outside clicks', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await waitForEditors(page);
    const globe: Locator = firstField(page).getByRole('button', { name: 'Share or load by name' });
    const menu:  Locator = firstField(page).getByRole('menu');
    await expect(menu).toBeHidden();
    await expect(firstField(page).getByRole('button', { name: 'Share...' })).toHaveCount(0);
    await globe.click();
    await expect(menu.getByRole('menuitem')).toHaveText(['Share...', 'Load...']);
    await expect(globe).toHaveAttribute('aria-expanded', 'true');
    await page.keyboard.press('Escape');
    await expect(menu).toBeHidden();
    await expect(globe).toHaveAttribute('aria-expanded', 'false');
    await globe.click();
    await page.locator('main .intro').click();
    await expect(menu).toBeHidden();
});

test('the share message disappears after 10 seconds', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'f' + RUN;
    await page.clock.install();
    await page.goto('/sandbox.html');
    await waitForEditors(page);
    const message: Locator = await share(firstField(page), 'Share', name);
    await expect(message).toHaveText('Shared as "' + name + '" (kept 7 days)');
    await page.clock.fastForward(9_000);
    await expect(message).toHaveText('Shared as "' + name + '" (kept 7 days)');
    await page.clock.fastForward(1_000);
    await expect(message).toBeHidden();
});

test('the share item tells that the IP is kept', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await waitForEditors(page);
    await firstField(page).getByRole('button', { name: 'Share or load by name' }).click();
    await expect(firstField(page).getByRole('menuitem', { name: 'Share...' }))
        .toHaveAttribute('title', 'Anyone who knows the name can load it. Your IP address is kept with it for 7 days.');
});
