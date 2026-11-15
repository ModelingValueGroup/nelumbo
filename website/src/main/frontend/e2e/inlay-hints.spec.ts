import { test, expect, Page, Locator } from '@playwright/test';

// Query results are added by the editor, not typed: every result hint has the same light dashed border
// (--nf-hint-border in fields.css) and the same dim purple background, whatever its outcome (dim enough
// that the red cross stays visible).
const DOC: string = [
    'import nelumbo.logic',
    'true ?',
    'false ? [][()]',
    'false ? [()][..]',
].join('\n');

const HINTS:  string[]                          = ['[()][]', '✅', '❌ [][()]']; // result, matched and failed expectation
const BORDER: { light: string; dark: string } = { light: '#5a4f6b', dark: '#9aa0ac' };
const FILL:   { light: string; dark: string } = { light: '#e3cdee', dark: '#4b2f59' };
const TEXT:   { light: string; dark: string } = { light: '#4a1d5e', dark: '#ecdcf4' };

function rgb(hex: string): string {
    const n: number = parseInt(hex.slice(1), 16);
    return 'rgb(' + (n >> 16) + ', ' + ((n >> 8) & 255) + ', ' + (n & 255) + ')';
}

// border, and for the result chip its colours, of the first rendered span whose text is exactly `text`
async function hintStyle(page: Page, text: string): Promise<string> {
    return await page.evaluate((t: string): string => {
        for (const span of Array.from(document.querySelectorAll('.monaco-editor .view-line span'))) {
            // Monaco renders spaces as non-breaking spaces
            if ((span.textContent ?? '').replace(/ /g, ' ') === t && span.children.length === 0) {
                const style: CSSStyleDeclaration = getComputedStyle(span);
                return style.borderTopStyle + ' ' + style.borderTopColor + ' | ' + style.backgroundColor + ' ' + style.color;
            }
        }
        return 'not rendered';
    }, text);
}

for (const scheme of ['light', 'dark'] as const) {
    test('the ' + scheme + ' theme draws a dashed border around every query result', async ({ page }: { page: Page }): Promise<void> => {
        await page.emulateMedia({ colorScheme: scheme });
        await page.goto('/sandbox.html');
        await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
        await page.evaluate((doc: string): void => {
            (window as any).NelumboFields.__editors[0].model.setValue(doc);
        }, DOC);
        for (const hint of HINTS) {
            await expect.poll(async (): Promise<string> => (await hintStyle(page, hint)).split(' | ')[0], { timeout: 20_000, message: hint })
                .toBe('dashed ' + rgb(BORDER[scheme]));
            await expect.poll(async (): Promise<string> => (await hintStyle(page, hint)).split(' | ')[1].split(' rgb')[0], { message: hint + ' background' })
                .toBe(rgb(FILL[scheme]));
        }
        // a plain and a failed result have the same text colour: the red cross tells the outcome
        const color: (hint: string) => Promise<string> = async (hint: string): Promise<string> => (await hintStyle(page, hint)).split(' | ')[1].replace(/^rgb\([^)]*\) /, '');
        expect(await color('❌ [][()]')).toBe(await color('[()][]'));
        expect(await color('[()][]')).toBe(rgb(TEXT[scheme]));
    });
}

// Without a client tokenizer Monaco anchored a result's own hover to its whole line: it showed at the line
// start, over the lines above, with the whole query highlighted. A result now has no hover of its own
// (resultsHoverAtTheirQuery): the hover is the server's, at the end of the query.
test('the hover of a query result shows at the result and highlights nothing', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    await page.evaluate((doc: string): void => {
        (window as any).NelumboFields.__editors[0].model.setValue(doc);
    }, DOC);
    const chip: Locator = page.getByText('[()][]', { exact: true }).first();
    await expect(chip).toBeVisible({ timeout: 20_000 });
    await chip.hover();
    const hover: Locator = page.locator('.monaco-hover').first();
    await expect(hover).toBeVisible({ timeout: 10_000 });
    await expect(hover).toContainText(/\d[\d,]* \u03bcs/);
    const chipBox:  { x: number; y: number; height: number } | null = await chip.boundingBox();
    const hoverBox: { x: number; y: number; height: number } | null = await hover.boundingBox();
    expect(Math.abs(hoverBox!.x - chipBox!.x), 'the hover starts at the result, not at the line start').toBeLessThan(20);
    // the chip's padding and border stick out a little above its line, the hover sits on that line
    expect(hoverBox!.y + hoverBox!.height, 'the hover is right above the result').toBeLessThanOrEqual(chipBox!.y + chipBox!.height / 2);
    const highlighted: number = await page.evaluate((): number => Array.from(document.querySelectorAll('.monaco-editor .hoverHighlight'))
        .reduce((sum: number, e: Element): number => sum + e.getBoundingClientRect().width, 0));
    expect(highlighted, 'width of the hover highlight').toBeLessThan(2);
});

// A double-click on a result writes it into the query as its expected result (the hint's text edit):
// after a plain query, or over a wrong expected result; both then match.
test('double-clicking a query result writes it as the expected result', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    await page.evaluate((doc: string): void => {
        (window as any).NelumboFields.__editors[0].model.setValue(doc);
    }, DOC);
    const text: () => Promise<string> = async (): Promise<string> =>
        await page.evaluate((): string => (window as any).NelumboFields.__editors[0].model.getValue());
    const result: Locator = page.getByText('[()][]', { exact: true }).first();
    await expect(result).toBeVisible({ timeout: 20_000 });
    await result.dblclick();
    await expect.poll(text).toContain('true ? [()][]\n');
    const mismatch: Locator = page.getByText('❌ [][()]').first();
    await expect(mismatch).toBeVisible({ timeout: 20_000 });
    await mismatch.dblclick();
    await expect.poll(text).toContain('false ? [][()]\nfalse ? [][()]');
    await expect(page.getByText('✅')).toHaveCount(3, { timeout: 20_000 });
});

// Monaco's default hover background hardly differs from the editor's (1.09:1 dark, 1.11:1 light)
const HOVER: { light: string; dark: string } = { light: '#e6d9f1 #8a78d0', dark: '#4a4160 #a99ae8' };

for (const scheme of ['light', 'dark'] as const) {
    test('the ' + scheme + ' theme gives a hover its own background and a border', async ({ page }: { page: Page }): Promise<void> => {
        await page.emulateMedia({ colorScheme: scheme });
        await page.goto('/sandbox.html');
        await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
        await page.evaluate((doc: string): void => {
            (window as any).NelumboFields.__editors[0].model.setValue(doc);
        }, DOC);
        const chip: Locator = page.getByText('[()][]', { exact: true }).first();
        await expect(chip).toBeVisible({ timeout: 20_000 });
        await chip.hover();
        const hover: Locator = page.locator('.monaco-hover').first();
        await expect(hover).toBeVisible({ timeout: 10_000 });
        const [background, border]: string[] = HOVER[scheme].split(' ');
        await expect(hover).toHaveCSS('background-color', rgb(background));
        await expect(hover).toHaveCSS('border-top-color', rgb(border));
    });
}

// The quick fix does what the double-click does. monaco-languageclient 1.0.1 built its edit in a shape
// Monaco 0.34 rejects ("Unsupported edit"), so choosing it did nothing (fixWorkspaceTextEdits). The
// lightbulb stays off: without a glyph margin Monaco drew it over the text of the line above.
test('the quick fix writes the result as the expected result, without a lightbulb', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    await page.evaluate((doc: string): void => {
        (window as any).NelumboFields.__editors[0].model.setValue(doc);
    }, DOC);
    await expect(page.getByText('[()][]', { exact: true }).first()).toBeVisible({ timeout: 20_000 });
    await page.evaluate((): void => {
        const editor: any = (window as any).NelumboFields.__editors[0].editor;
        editor.setPosition({ lineNumber: 2, column: 3 });
        editor.focus();
    });
    await page.waitForTimeout(1000);
    await expect(page.locator('.monaco-editor .codicon-light-bulb, .monaco-editor .codicon-lightbulb-autofix').locator('visible=true')).toHaveCount(0);
    await page.evaluate((): void => {
        (window as any).NelumboFields.__editors[0].editor.trigger('e2e', 'editor.action.codeAction', { kind: 'quickfix', apply: 'first' });
    });
    await expect.poll(async (): Promise<string> =>
        await page.evaluate((): string => (window as any).NelumboFields.__editors[0].model.getValue())).toContain('true ? [()][]\n');
});

// A failed expected result is also a problem marker: around its result Monaco showed the marker's hover,
// on it the result's own, one or the other at random to the user. Now a result has no hover of its own
// (resultsHoverAtTheirQuery): everywhere on and around it the same hover, with the time and the error.
test('a failed expected result has one hover, with the time and the error', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    await page.evaluate((doc: string): void => {
        (window as any).NelumboFields.__editors[0].model.setValue(doc);
    }, DOC);
    const chip: Locator = page.getByText('❌ [][()]').first();
    await expect(chip).toBeVisible({ timeout: 20_000 });
    const box:   { x: number; y: number; width: number; height: number } | null = await chip.boundingBox();
    const hover: Locator                                                        = page.locator('.monaco-hover').first();
    const seen:  Set<string>                                                    = new Set<string>();
    for (const dx of [-3, 1, box!.width / 2, box!.width - 1, box!.width + 2]) {
        await page.mouse.move(10, 10);
        await expect(hover).toBeHidden();
        await page.mouse.move(box!.x + dx, box!.y + box!.height / 2);
        await expect(hover).toContainText('Expected result', { timeout: 10_000 });
        await expect(hover).toContainText(/\d[\d,]* μs/);
        seen.add(((await hover.textContent()) ?? '').replace(/\d[\d,]* μs/, 'TIME'));
    }
    expect([...seen], 'the same hover everywhere').toHaveLength(1);
});

// A query whose result is not in yet shows "...calculating" (after 300 ms); the quick queries above it
// keep their results meanwhile.
const SLOW: string = [
    'import nelumbo.integers',
    'Integer ::= factorial(<Integer>)',
    'Integer n, r',
    'factorial(n)=r <=> r=1 if n<=0, r=n*factorial(n-1) if n>0',
    'factorial(5)=r ?',
    'factorial(20000)=r ?',
].join('\n');

test('a slow query shows ...calculating until its result is in', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    await page.evaluate((doc: string): void => {
        (window as any).NelumboFields.__editors[0].model.setValue(doc);
    }, SLOW);
    const calculating: Locator = page.getByText('...calculating');
    await expect(calculating).toBeVisible({ timeout: 20_000 });
    await expect.poll(async (): Promise<boolean> => await page.evaluate((): boolean =>
        document.body.innerText.includes('...calculating') && document.body.innerText.includes('r=120')), { timeout: 20_000 }).toBe(true);
    await expect(calculating).toHaveCount(0, { timeout: 30_000 });
});

test('a quick edit shows no ...calculating', async ({ page }: { page: Page }): Promise<void> => {
    await page.goto('/sandbox.html');
    await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
    await page.evaluate((doc: string): void => {
        (window as any).NelumboFields.__editors[0].model.setValue(doc);
    }, DOC);
    for (const hint of HINTS) {
        await expect(page.getByText(hint, { exact: true }).first()).toBeVisible({ timeout: 20_000 });
    }
    const seen: boolean = await page.evaluate(async (doc: string): Promise<boolean> => {
        (window as any).NelumboFields.__editors[0].model.setValue(doc);
        let   found: boolean = false;
        const end:   number  = Date.now() + 1000;
        while (Date.now() < end) {
            if (document.body.innerText.includes('...calculating')) {
                found = true;
            }
            await new Promise<void>((r: () => void): number => window.setTimeout(r, 10));
        }
        return found;
    }, DOC + '\ntrue ?');
    expect(seen, '...calculating appeared after a quick edit').toBe(false);
});
