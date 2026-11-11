import { test, expect, Page, Locator, Browser, Dialog } from '@playwright/test';

// Save as... / Load...: a text saved under a name in one browser loads by that name in any other.
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

async function share(wrap: Locator, action: 'Save' | 'Load', name: string): Promise<Locator> {
    await wrap.getByRole('button', { name: action === 'Save' ? 'Save as...' : 'Load...' }).click();
    await wrap.getByLabel('Solution name').fill(name);
    await wrap.getByLabel('Solution name').press('Enter');
    return wrap.locator('.nelumbo-share-message');
}

test('a saved solution loads in another browser, padded name and all', async ({ browser }: { browser: Browser }): Promise<void> => {
    const name: string = 'a' + RUN;
    const a:    Page   = await (await browser.newContext()).newPage();
    await a.goto('/tutorial.html#logic');
    await waitForEditors(a);
    await typeAtEnd(a, 0, '\n// by frits\n');
    await expect(await share(firstField(a), 'Save', '  ' + name + ' ')).toHaveText('Saved as "' + name + '" (kept 7 days)');
    const b: Page = await (await browser.newContext()).newPage();
    await b.goto('/sandbox.html');
    await waitForEditors(b);
    await expect(await share(firstField(b), 'Load', name)).toHaveText('Loaded "' + name + '"');
    expect(await editorText(b, 0)).toBe(await editorText(a, 0));
    await expect(await share(firstField(b), 'Save', name)).toHaveText('"' + name + '" is taken');
});

test('Load asks first only when the field holds own work', async ({ page }: { page: Page }): Promise<void> => {
    const name:    string   = 'b' + RUN;
    const dialogs: string[] = [];
    await page.goto('/tutorial.html#logic');
    await waitForEditors(page);
    const original: string = await editorText(page, 0);
    await typeAtEnd(page, 0, '\n// saved\n');
    await expect(await share(firstField(page), 'Save', name)).toHaveText('Saved as "' + name + '" (kept 7 days)');
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
    await expect(await share(firstField(page), 'Save', name)).toHaveText('Saved as "' + name + '" (kept 7 days)');
    await firstField(page).getByRole('button', { name: 'Reset' }).click();
    await expect(await share(firstField(page), 'Load', name)).toHaveText('Loaded "' + name + '"');
    await firstField(page).getByRole('button', { name: 'Reset' }).click();
    expect(await editorText(page, 0)).toBe(original);
});

test('the name field remembers the last name', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'd' + RUN;
    await page.goto('/sandbox.html');
    await waitForEditors(page);
    await expect(await share(firstField(page), 'Save', name)).toHaveText('Saved as "' + name + '" (kept 7 days)');
    await firstField(page).getByRole('button', { name: 'Load...' }).click();
    await expect(firstField(page).getByLabel('Solution name')).toHaveValue(name);
});

test('the landing page and the library have no share buttons', async ({ page }: { page: Page }): Promise<void> => {
    for (const path of ['/', '/library.html']) {
        await page.goto(path);
        await waitForEditors(page);
        await expect(page.getByRole('button', { name: 'Save as...' })).toHaveCount(0);
    }
});

// crypto.randomUUID exists only in secure contexts: a server jar run on a classroom LAN over plain HTTP has none
test('saving works without crypto.randomUUID', async ({ page }: { page: Page }): Promise<void> => {
    const name: string = 'e' + RUN;
    await page.addInitScript((): void => {
        delete (Crypto.prototype as any).randomUUID;
    });
    await page.goto('/sandbox.html');
    await waitForEditors(page);
    await expect(await share(firstField(page), 'Save', name)).toHaveText('Saved as "' + name + '" (kept 7 days)');
});
