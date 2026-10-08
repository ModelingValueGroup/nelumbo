import { test, expect, Page } from '@playwright/test';

// The web editors must color every semantic token type the LSP server sends (LspTokenMapping), with the
// palette of the standalone NelumboEditor (DEFAULT_TOKEN_COLORS): verbatim in the light theme, the same
// hues lightened for the dark theme. Without explicit theme rules Monaco colored only a few of them.
const DOC: string = [
    'import nelumbo.integers',
    'import nelumbo.strings',
    '// a comment',
    'Integer ::= fib(<Integer>)',
    'Integer n, f',
    'fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1',
    'String s',
    's="hi" ?',
    '',
].join('\n');

interface Expectation { text: string; light: string; dark: string; bold: boolean }

const TOKENS: Expectation[] = [
    { text: 'import',       light: '#0000ff', dark: '#6ea8ff', bold: true  }, // keyword  -> modifier
    { text: 'fib',          light: '#0000ff', dark: '#6ea8ff', bold: false }, // name     -> property
    { text: 'Integer',      light: '#880088', dark: '#d07ad6', bold: false }, // type     -> type
    { text: 'n',            light: '#339900', dark: '#8fd14f', bold: false }, // variable -> variable
    { text: '"hi"',         light: '#006633', dark: '#5fc98f', bold: false }, // string   -> string
    { text: '2',            light: '#000077', dark: '#9aa5ff', bold: false }, // number   -> number
    { text: '+',            light: '#333333', dark: '#d4d7de', bold: true  }, // operator -> operator
    { text: '<',            light: '#00cccc', dark: '#3f9494', bold: false }, // meta-op  -> decorator (the pattern hole brackets in fib(<Integer>))
    { text: '// a comment', light: '#a0a0a0', dark: '#6b7080', bold: false }, // comment  -> comment
];

function rgb(hex: string): string {
    const n: number = parseInt(hex.slice(1), 16);
    return 'rgb(' + (n >> 16) + ', ' + ((n >> 8) & 255) + ', ' + (n & 255) + ')';
}

// color + weight of the first rendered token span whose text is exactly `text`
async function tokenStyle(page: Page, text: string): Promise<string> {
    return await page.evaluate((t: string): string => {
        for (const span of Array.from(document.querySelectorAll('.monaco-editor .view-line span span'))) {
            // Monaco renders spaces as non-breaking spaces
            if ((span.textContent ?? '').replace(/ /g, ' ') === t) {
                const style: CSSStyleDeclaration = getComputedStyle(span);
                return style.color + ' ' + (parseInt(style.fontWeight, 10) >= 600 ? 'bold' : 'normal');
            }
        }
        return 'not rendered';
    }, text);
}

for (const scheme of ['light', 'dark'] as const) {
    test('the ' + scheme + ' editor theme colors every token type like the NelumboEditor', async ({ page }: { page: Page }): Promise<void> => {
        await page.emulateMedia({ colorScheme: scheme });
        await page.goto('/sandbox.html');
        await page.waitForFunction((): boolean => ((window as any).NelumboFields?.__editors?.length ?? 0) > 0);
        await page.evaluate((doc: string): void => {
            (window as any).NelumboFields.__editors[0].model.setValue(doc);
        }, DOC);
        for (const token of TOKENS) {
            const expected: string = rgb(token[scheme]) + ' ' + (token.bold ? 'bold' : 'normal');
            await expect.poll(async (): Promise<string> => tokenStyle(page, token.text), { timeout: 20_000, message: token.text })
                .toBe(expected);
        }
    });
}
