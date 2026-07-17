import * as monaco from 'monaco-editor/esm/vs/editor/edcore.main';
// esbuild tree-shakes this side-effect contribution out of edcore.main; without it inlay hints never render
import 'monaco-editor/esm/vs/editor/contrib/inlayHints/browser/inlayHintsContribution.js';
import {
    MonacoLanguageClient,
    MonacoServices,
    MessageTransports,
    CloseAction,
    ErrorAction,
} from 'monaco-languageclient';
import {
    toSocket,
    WebSocketMessageReader,
    WebSocketMessageWriter,
    IWebSocket,
} from 'vscode-ws-jsonrpc';

import './fields.css';

const LANGUAGE_ID: string = 'nelumbo';

// Monaco 0.34 subscribes to a provider's onDidChangeInlayHints only once that provider has returned
// a non-empty result, but the first pull always races the server's initial evaluation (empty), so the
// server's workspace/inlayHint/refresh request has no subscriber and the first results would only
// appear on the next edit. Registry changes, however, always re-trigger the InlayHintsController:
// answer the refresh request by briefly registering a no-op provider, which makes every editor
// re-pull all providers, including the real LSP one.
function wireInlayHintRefresh(client: MonacoLanguageClient): void {
    const noopProvider: monaco.languages.InlayHintsProvider = {
        provideInlayHints: (): monaco.languages.InlayHintList => ({ hints: [], dispose: (): void => {} }),
    };
    client.onRequest('workspace/inlayHint/refresh', (): void => {
        monaco.languages.registerInlayHintsProvider(LANGUAGE_ID, noopProvider).dispose();
    });
}

function connectLanguageClient(): Promise<MonacoLanguageClient | null> {
    return new Promise<MonacoLanguageClient | null>((resolve): void => {
        const scheme: string = location.protocol === 'https:' ? 'wss:' : 'ws:';
        const url:    string = scheme + '//' + location.host + '/lsp';
        let   ws:     WebSocket;
        try {
            ws = new WebSocket(url);
        } catch {
            resolve(null);
            return;
        }
        let settled: boolean = false;
        ws.onopen = (): void => {
            if (settled) {
                return;
            }
            settled = true;
            const socket: IWebSocket = toSocket(ws);
            const reader: WebSocketMessageReader = new WebSocketMessageReader(socket);
            const writer: WebSocketMessageWriter = new WebSocketMessageWriter(socket);
            const client: MonacoLanguageClient = new MonacoLanguageClient({
                name: 'Nelumbo Language Client',
                clientOptions: {
                    documentSelector: [LANGUAGE_ID],
                    // never spin-restart: on a public demo a wedged server must not loop
                    errorHandler: {
                        error:  () => ({ action: ErrorAction.Continue }),
                        closed: () => ({ action: CloseAction.DoNotRestart }),
                    },
                },
                connectionProvider: {
                    get: (): Promise<MessageTransports> => Promise.resolve({ reader, writer }),
                },
            });
            void client.start().then((): void => {
                wireInlayHintRefresh(client);
            });
            resolve(client);
        };
        ws.onerror = (): void => {
            if (settled) {
                return;
            }
            settled = true;
            resolve(null);
        };
        ws.onclose = (): void => {
            if (settled) {
                return;
            }
            settled = true;
            resolve(null);
        };
    });
}

// Test hook (used by the Playwright e2e suite via window.NelumboFields.__editors / .__monaco).
// Read-only access to the mounted editors and the monaco namespace; harmless in production.
export const __editors: Array<{ editor: monaco.editor.IStandaloneCodeEditor; model: monaco.editor.ITextModel }> = [];
export const __monaco: typeof monaco = monaco;

let servicesReady: boolean                                     = false;
let clientPromise: Promise<MonacoLanguageClient | null> | null = null;
let fieldIndex:    number                                      = 0;

function ensureServices(): void {
    if (servicesReady) {
        return;
    }
    MonacoServices.install(monaco);
    monaco.languages.register({ id: LANGUAGE_ID, extensions: ['.nl'] });
    // Query-result inlay hints: the server picks the style bucket via the hint kind
    // (QueryResultCache): no kind = matched expectation (green checkmark), Type = plain result
    // value (purple chip), Parameter = failed expectation/error (red on pink chip).
    monaco.editor.defineTheme('nelumbo-dark', {
        base:    'vs-dark',
        inherit: true,
        rules:   [],
        colors:  {
            'editorInlayHint.foreground':          '#46c98b',
            'editorInlayHint.background':          '#00000000',
            'editorInlayHint.typeForeground':      '#edd4e8',
            'editorInlayHint.typeBackground':      '#c184d840',
            'editorInlayHint.parameterForeground': '#f1707b',
            'editorInlayHint.parameterBackground': '#f1707b2b',
        },
    });
    // the webfont may finish loading after the first editor measured its glyphs
    void document.fonts.ready.then((): void => {
        monaco.editor.remeasureFonts();
    });
    servicesReady = true;
}

function showBanner(): void {
    const banner: HTMLDivElement = document.createElement('div');
    banner.className   = 'nelumbo-lsp-banner visible';
    banner.textContent = 'Language features and evaluation are unavailable (LSP connection failed).';
    document.body.prepend(banner);
}

function addSolutionToggle(field: HTMLElement, index: number): void {
    const next: Element | null = field.nextElementSibling;
    if (next === null || !next.classList.contains('nelumbo-solution')) {
        return;
    }
    const solution: HTMLElement       = next as HTMLElement;
    const toolbar:  HTMLDivElement    = document.createElement('div');
    toolbar.className                 = 'nelumbo-field-toolbar';
    const button:   HTMLButtonElement = document.createElement('button');
    button.type                       = 'button';
    button.textContent                = 'Show solution';
    button.addEventListener('click', (): void => {
        const visible: boolean = solution.classList.toggle('visible');
        button.textContent = visible ? 'Hide solution' : 'Show solution';
    });
    toolbar.appendChild(button);
    field.appendChild(toolbar);
    // pull the solution inside the field so it shares the editor's border, right below the toggle
    field.appendChild(solution);
    buildSolutionViewer(solution, index);
}

// Nelumbo has no client-side tokenizer (coloring comes from LSP semantic tokens), so a plain
// colorized block is not possible: the solution becomes a read-only Monaco editor on the same
// LSP client, which also gives it inlay hints (a solved exercise shows its own checkmark).
function buildSolutionViewer(solution: HTMLElement, index: number): void {
    let text: string = solution.textContent || '';
    if (text.startsWith('\n')) {
        text = text.slice(1);
    }
    solution.textContent = '';

    const uri:   monaco.Uri               = monaco.Uri.parse('inmemory://solution-' + index + '.nl');
    const model: monaco.editor.ITextModel = monaco.editor.createModel(text, LANGUAGE_ID, uri);

    const viewer: monaco.editor.IStandaloneCodeEditor = monaco.editor.create(solution, {
        model:                model,
        theme:                'nelumbo-dark',
        readOnly:             true,
        domReadOnly:          true,
        minimap:              { enabled: false },
        automaticLayout:      true,
        fontSize:             13,
        fontFamily:           '"JetBrains Mono", ui-monospace, Menlo, Consolas, monospace',
        'semanticHighlighting.enabled': true,
        scrollBeyondLastLine: false,
        padding:              { top: 12, bottom: 8 },
        // hovers escape the field's overflow:hidden border and overlapping sections
        fixedOverflowWidgets: true,
        renderLineHighlight:  'none',
        occurrencesHighlight: false,
        scrollbar:            { vertical: 'hidden', handleMouseWheel: false },
    });
    // fixed content, so size the (initially display:none) host once from the line count
    const lineHeight: number = viewer.getOption(monaco.editor.EditorOption.lineHeight);
    solution.style.height = (model.getLineCount() * lineHeight + 12 + 8) + 'px';
}

function buildField(div: HTMLElement, index: number): void {
    let initial: string = div.textContent || '';
    if (initial.startsWith('\n')) {
        initial = initial.slice(1);
    }
    div.textContent = '';
    div.classList.add('nelumbo-field-wrap');

    const host: HTMLDivElement = document.createElement('div');
    host.className = 'nelumbo-field-editor';
    if (div.dataset.height) {
        host.style.height = div.dataset.height;
    }
    div.appendChild(host);

    addSolutionToggle(div, index);

    const uri:   monaco.Uri               = monaco.Uri.parse('inmemory://field-' + index + '.nl');
    const model: monaco.editor.ITextModel = monaco.editor.createModel(initial, LANGUAGE_ID, uri);

    const editor: monaco.editor.IStandaloneCodeEditor = monaco.editor.create(host, {
        model:                model,
        theme:                'nelumbo-dark',
        minimap:              { enabled: false },
        automaticLayout:      true,
        fontSize:             13,
        fontFamily:           '"JetBrains Mono", ui-monospace, Menlo, Consolas, monospace',
        'semanticHighlighting.enabled': true,
        scrollBeyondLastLine: false,
        padding:              { top: 12, bottom: 8 },
        // hovers escape the field's overflow:hidden border and overlapping sections
        fixedOverflowWidgets: true,
        // Cmd/Ctrl+Click goes to definition (Alt+Click is multi-cursor), the VS Code default made explicit
        multiCursorModifier:  'alt',
    });
    __editors.push({ editor: editor, model: model });
}

// Establish the single page-shared /lsp language client. Idempotent: repeated calls return the
// same promise. On failure resolves null and shows the banner (editing still works).
export function connect(): Promise<MonacoLanguageClient | null> {
    ensureServices();
    if (clientPromise === null) {
        clientPromise = connectLanguageClient().then((client: MonacoLanguageClient | null): MonacoLanguageClient | null => {
            if (client === null) {
                showBanner();
            }
            return client;
        });
    }
    return clientPromise;
}

// Upgrade every .nelumbo-field in `container` not already mounted into a Monaco editor. Idempotent
// (skips fields already wrapped), so it is safe to call each time a tour section is shown. The shared
// client (via connect()) attaches to models created after it starts, so lazy mounting keeps full LSP.
export function mountFields(container: ParentNode): void {
    ensureServices();
    const divs: NodeListOf<HTMLElement> = container.querySelectorAll<HTMLElement>('.nelumbo-field');
    for (const div of Array.from(divs)) {
        if (div.classList.contains('nelumbo-field-wrap')) {
            continue;
        }
        buildField(div, fieldIndex);
        fieldIndex++;
    }
}

// Playground entry point: mount every field on the page and connect once. Lifecycle is page-scoped
// (no teardown); standalone monaco falls back to a synchronous main-thread worker (one console
// warning) since the /lsp server supplies all language features.
export async function initNelumboFields(): Promise<void> {
    mountFields(document);
    await connect();
}
