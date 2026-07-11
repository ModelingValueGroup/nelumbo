import * as monaco from 'monaco-editor/esm/vs/editor/edcore.main';
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

interface EvalBinding {
    [name: string]: string;
}

interface EvalQuery {
    query:    string;
    status:   string;
    bindings: EvalBinding[];
    result?:  string;
}

interface EvalError {
    line?:    number;
    column?:  number;
    message?: string;
}

interface EvalResponse {
    queries?: EvalQuery[];
    errors?:  EvalError[];
    error?:   string;
    message?: string;
}

function esc(s: unknown): string {
    return String(s).replace(/[&<>]/g, (c: string): string => {
        if (c === '&') {
            return '&amp;';
        }
        if (c === '<') {
            return '&lt;';
        }
        return '&gt;';
    });
}

function renderResults(el: HTMLElement, data: EvalResponse): void {
    let html: string = '';
    if (data.error) {
        html += '<div class="q q-error">' + esc(data.error);
        if (data.message) {
            html += ': ' + esc(data.message);
        }
        html += '</div>';
    }
    const errors: EvalError[] = data.errors || [];
    for (const e of errors) {
        const loc: string = e.line != null ? e.line + ':' + e.column + '  ' : '';
        html += '<div class="q q-error">' + esc(loc + (e.message || '')) + '</div>';
    }
    const queries: EvalQuery[] = data.queries || [];
    for (const q of queries) {
        const cls: string = 'q-' + esc(q.status);
        html += '<div class="q ' + cls + '">' + esc(q.query);
        html += '<span class="badge ' + cls + '">' + esc(q.status) + '</span></div>';
        const bindings: EvalBinding[] = q.bindings || [];
        if (bindings.length) {
            html += '<div class="bindings">';
            for (const b of bindings) {
                const parts: string = Object.entries(b)
                    .map(([k, v]: [string, string]): string => k + '=' + v)
                    .join(', ');
                html += '<span class="b">' + esc(parts || '()') + '</span>';
            }
            html += '</div>';
        }
    }
    if (!html) {
        html = '<div class="placeholder">No queries in the document (declarations evaluated).</div>';
    }
    el.innerHTML = html;
    el.classList.add('visible');
}

async function runEval(content: string, statusEl: HTMLElement, resultsEl: HTMLElement): Promise<void> {
    statusEl.textContent = 'running...';
    let res:  Response;
    let data: EvalResponse;
    try {
        res  = await fetch('/eval', {
            method:  'POST',
            headers: { 'Content-Type': 'text/plain' },
            body:    content,
        });
        data = await res.json() as EvalResponse;
    } catch (err: unknown) {
        statusEl.textContent = 'request failed';
        renderResults(resultsEl, { error: 'request failed', message: String(err) });
        return;
    }
    statusEl.textContent = 'HTTP ' + res.status;
    renderResults(resultsEl, data);
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
            void client.start();
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

let servicesReady: boolean                                     = false;
let clientPromise: Promise<MonacoLanguageClient | null> | null = null;
let fieldIndex:    number                                      = 0;

function ensureServices(): void {
    if (servicesReady) {
        return;
    }
    MonacoServices.install(monaco);
    monaco.languages.register({ id: LANGUAGE_ID, extensions: ['.nl'] });
    servicesReady = true;
}

function showBanner(): void {
    const banner: HTMLDivElement = document.createElement('div');
    banner.className   = 'nelumbo-lsp-banner visible';
    banner.textContent = 'Language features are unavailable (LSP connection failed). Editing and Run still work.';
    document.body.prepend(banner);
}

function addSolutionToggle(field: HTMLElement, toolbar: HTMLElement): void {
    const next: Element | null = field.nextElementSibling;
    if (next === null || !next.classList.contains('nelumbo-solution')) {
        return;
    }
    const solution: HTMLElement = next as HTMLElement;
    const button:   HTMLButtonElement = document.createElement('button');
    button.type        = 'button';
    button.className   = 'secondary';
    button.textContent = 'Show solution';
    button.addEventListener('click', (): void => {
        const visible: boolean = solution.classList.toggle('visible');
        button.textContent = visible ? 'Hide solution' : 'Show solution';
    });
    toolbar.appendChild(button);
}

function buildField(div: HTMLElement, index: number): void {
    let initial: string = div.textContent || '';
    if (initial.startsWith('\n')) {
        initial = initial.slice(1);
    }
    div.textContent = '';
    div.classList.add('nelumbo-field-wrap');

    const toolbar: HTMLDivElement = document.createElement('div');
    toolbar.className = 'nelumbo-field-toolbar';

    const runButton: HTMLButtonElement = document.createElement('button');
    runButton.type        = 'button';
    runButton.textContent = 'Run';

    const status: HTMLSpanElement = document.createElement('span');
    status.className   = 'status';
    status.textContent = 'ready';

    toolbar.appendChild(runButton);
    toolbar.appendChild(status);

    const host: HTMLDivElement = document.createElement('div');
    host.className = 'nelumbo-field-editor';
    if (div.dataset.height) {
        host.style.height = div.dataset.height;
    }

    const results: HTMLDivElement = document.createElement('div');
    results.className = 'nelumbo-field-results';

    div.appendChild(toolbar);
    div.appendChild(host);
    div.appendChild(results);

    const uri:   monaco.Uri                = monaco.Uri.parse('inmemory://field-' + index + '.nl');
    const model: monaco.editor.ITextModel  = monaco.editor.createModel(initial, LANGUAGE_ID, uri);

    const editor: monaco.editor.IStandaloneCodeEditor = monaco.editor.create(host, {
        model:                model,
        theme:                'vs-dark',
        minimap:              { enabled: false },
        automaticLayout:      true,
        fontSize:             13,
        'semanticHighlighting.enabled': true,
        scrollBeyondLastLine: false,
        // Cmd/Ctrl+Click goes to definition (Alt+Click is multi-cursor), the VS Code default made explicit
        multiCursorModifier:  'alt',
    });

    const run = (): void => {
        void runEval(model.getValue(), status, results);
    };
    runButton.addEventListener('click', run);
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, run);

    addSolutionToggle(div, toolbar);
}

// Establish the single page-shared /lsp language client. Idempotent: repeated calls return the
// same promise. On failure resolves null and shows the banner (editing + Run still work).
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
