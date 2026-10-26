import { test, expect, Page } from '@playwright/test';

// The comment shortcuts are client-side (LSP has no comment request): they work only when the
// nelumbo language configuration tells Monaco the comment syntax, `//` and `/* */` as in TokenType.
const LINE: string = 'Integer n, f';

async function openField(page: Page): Promise<void> {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    await page.evaluate((line: string): void => {
        const e: any = (window as any).NelumboFields.__editors[0];
        e.model.setValue(line);
        e.editor.focus();
    }, LINE);
}

function text(page: Page): Promise<string> {
    return page.evaluate((): string => (window as any).NelumboFields.__editors[0].model.getValue());
}

// Control, not Meta, even on a Mac: the Desktop Chrome device has a Windows user agent, so Monaco binds Ctrl+/
test('Cmd/Ctrl+/ comments out the whole line and back', async ({ page }: { page: Page }): Promise<void> => {
    await openField(page);
    await page.keyboard.press('Control+Slash');
    expect(await text(page)).toBe('// ' + LINE);
    await page.keyboard.press('Control+Slash');
    expect(await text(page)).toBe(LINE);
});

test('Shift+Alt+A comments out the selected part of a line and back', async ({ page }: { page: Page }): Promise<void> => {
    await openField(page);
    // select ", f"
    await page.evaluate((): void => (window as any).NelumboFields.__editors[0].editor.setSelection(
        { startLineNumber: 1, startColumn: 10, endLineNumber: 1, endColumn: 13 }));
    await page.keyboard.press('Shift+Alt+KeyA');
    expect(await text(page)).toBe('Integer n/* , f */');
    await page.keyboard.press('Shift+Alt+KeyA');
    expect(await text(page)).toBe(LINE);
});
