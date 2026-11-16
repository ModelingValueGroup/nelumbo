import * as monaco from 'monaco-editor/esm/vs/editor/edcore.main';
// esbuild tree-shakes this side-effect contribution out of edcore.main; without it inlay hints never render
import 'monaco-editor/esm/vs/editor/contrib/inlayHints/browser/inlayHintsContribution.js';
import { InlayHintsController } from 'monaco-editor/esm/vs/editor/contrib/inlayHints/browser/inlayHintsController.js';
import { InlayHintsHover } from 'monaco-editor/esm/vs/editor/contrib/inlayHints/browser/inlayHintsHover.js';
import { SparseMultilineTokens } from 'monaco-editor/esm/vs/editor/common/tokens/sparseMultilineTokens.js';
import {
    MonacoLanguageClient,
    MonacoServices,
    MessageTransports,
    CloseAction,
    ErrorAction,
    ProtocolToMonacoConverter,
    ProtocolInlayHint,
} from 'monaco-languageclient';
import type { AnnotatedTextEdit, InlayHint, TextEdit } from 'vscode-languageserver-protocol';
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
            settled       = true;
            connected     = true;
            currentSocket = ws;
            // toSocket takes over ws.onclose, so listen next to it
            ws.addEventListener('close', (event: CloseEvent): void => {
                connected = false;
                // closed by the server on purpose (session cap, error): a failed attempt, so that not every
                // mouse move reconnects
                if (!LOST_CODES.includes(event.code)) {
                    connectionFailed();
                }
            });
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

// Close test hook: drops the LSP connection the way an idle timeout or a sleeping phone does.
export function __closeConnection(): void {
    currentSocket?.close(1000);
}

let servicesReady: boolean                                     = false;
let clientPromise: Promise<MonacoLanguageClient | null> | null = null;
let fieldIndex:    number                                      = 0;

// normal close, going away (server idle timeout, shutdown), abnormal (network, sleeping phone)
const LOST_CODES:    number[]              = [1000, 1001, 1006];
const RETRY_MS:      number                = 30_000;
let   connected:     boolean               = false;
let   connecting:    boolean               = false;
let   lastFailure:   number                = 0;
let   currentSocket: WebSocket | null      = null;
let   lspBanner:     HTMLDivElement | null = null;

const OVERLOAD_CODE:  string                = 'server-overloaded';
let   overloadBanner: HTMLDivElement | null = null;

const STORAGE_PREFIX: string = 'nelumbo-field:';
const BROWSER_ID_KEY: string = 'nelumbo-browser-id';
const SHARE_NAME_KEY: string = 'nelumbo-share-name';
let   pageBrowserId:  string | null = null;

// A mounted .nelumbo-field. Edits are kept in localStorage under `key` as {original, text}, and only
// restored while the field's original text is still `original` (an exercise changed by a deploy
// starts fresh). In the sandbox each example has its own key and original (setFieldText).
interface Field {
    model:    monaco.editor.ITextModel;
    baseKey:  string;
    key:      string;
    original: string;
    pageText: string; // the original from the page itself, under baseKey (restoreFieldText)
    reset:    HTMLButtonElement;
}

// the Field behind each mounted .nelumbo-field (the wrap div)
const fields: WeakMap<HTMLElement, Field> = new WeakMap();

// Syntax colors per LSP semantic token type (LspTokenMapping on the server), mirroring the standalone
// NelumboEditor's DEFAULT_TOKEN_COLORS: light = its colors verbatim, dark = the same hues lightened.
// Keep both sides in sync when a color changes.
const TOKEN_STYLES: Array<{ token: string; light: string; dark: string; fontStyle?: string }> = [
    { token: 'modifier',  light: '0000ff', dark: '6ea8ff', fontStyle: 'bold' }, // KEYWORD
    { token: 'property',  light: '0000ff', dark: '6ea8ff' },                    // NAME
    { token: 'type',      light: '880088', dark: 'd07ad6' },                    // TYPE
    { token: 'variable',  light: '339900', dark: '8fd14f' },                    // VARIABLE
    { token: 'string',    light: '006633', dark: '5fc98f' },                    // STRING
    { token: 'number',    light: '000077', dark: '9aa5ff' },                    // NUMBER
    { token: 'operator',  light: '333333', dark: 'd4d7de', fontStyle: 'bold' }, // OPERATOR
    { token: 'decorator', light: '00cccc', dark: '3f9494' },                    // META_OPERATOR
    { token: 'comment',   light: 'a0a0a0', dark: '6b7080' },                    // END_LINE_COMMENT, IN_LINE_COMMENT
];

function tokenRules(scheme: 'light' | 'dark'): monaco.editor.ITokenThemeRule[] {
    return TOKEN_STYLES.map((s): monaco.editor.ITokenThemeRule => ({ token: s.token, foreground: s[scheme], fontStyle: s.fontStyle ?? '' }));
}

function monacoTheme(): string {
    return document.documentElement.dataset.theme === 'light' ? 'nelumbo-light' : 'nelumbo-dark';
}

// Users could not tell the query results the editor adds from the text they typed: every inlay hint
// gets a light dashed border, whatever its outcome (--nf-hint-border in fields.css). Monaco 0.34 gives a
// hint no CSS class of its own, its style is a generated rule that _fillInColors fills in, so extend that.
function borderInlayHints(): void {
    const fill: (props: Record<string, unknown>, hint: { kind?: number }) => void = InlayHintsController.prototype._fillInColors;
    InlayHintsController.prototype._fillInColors = function (this: InlayHintsController, props: Record<string, unknown>, hint: { kind?: number }): void {
        fill.call(this, props, hint);
        props.border = '1px dashed var(--nf-hint-border)';
    };
}

// A double-click on a query result writes it into the query as its expected result: the server puts that
// edit on the hint (textEdits), but monaco-languageclient 1.0.1 drops textEdits on the way to Monaco, so
// carry them over.
function keepInlayHintTextEdits(): void {
    const asInlayHint: (hint: InlayHint) => ProtocolInlayHint = ProtocolToMonacoConverter.prototype.asInlayHint;
    ProtocolToMonacoConverter.prototype.asInlayHint = function (this: ProtocolToMonacoConverter, hint: InlayHint): ProtocolInlayHint {
        const result: ProtocolInlayHint = asInlayHint.call(this, hint);
        result.textEdits = hint.textEdits?.map((edit: TextEdit): monaco.languages.TextEdit => this.asTextEdit(edit));
        return result;
    };
}

// Monaco's tap on a touch screen (base/browser/touch.js)
interface GestureTap extends Event {
    pageX: number;
    pageY: number;
}

// the longest time between the two taps of a double tap, as Monaco's Gesture.CLEAR_TAP_COUNT_TIME
const DOUBLE_TAP_MS: number = 400;

// A phone never fires the double-click: Monaco cancels the mouse events of a touch (Android) or gets only
// pointer events, whose detail is always 0 (iOS). Monaco sends its taps, without bubbling, to the lines of
// the editor: a double tap on a result does what a double-click does. Not by Monaco's tapCount: any
// touchmove resets it, and on Android a finger moves a little during almost every tap.
function acceptResultsByDoubleTap(): void {
    const install: (updateInlayHints: () => void) => monaco.IDisposable = InlayHintsController.prototype._installDblClickGesture;
    InlayHintsController.prototype._installDblClickGesture = function (this: InlayHintsController, updateInlayHints: () => void): monaco.IDisposable {
        const click:   monaco.IDisposable = install.call(this, updateInlayHints);
        const lines:   Element | null     = this._editor.getDomNode()!.querySelector('.lines-content');
        let   lastTap: number             = 0;
        const onTap:   (e: Event) => void = (e: Event): void => {
            const tap:    GestureTap = e as GestureTap;
            const now:    number     = Date.now();
            const double: boolean    = now - lastTap <= DOUBLE_TAP_MS;
            // a third tap starts a new double tap
            lastTap = double ? 0 : now;
            if (!double) {
                return;
            }
            const target: monaco.editor.IMouseTarget | null = this._editor.getTargetAtClientPoint(tap.pageX - window.scrollX, tap.pageY - window.scrollY);
            const edits:  monaco.languages.TextEdit[] | undefined = target ? this._getInlayHintLabelPart({ target: target })?.item.hint.textEdits : undefined;
            if (edits && edits.length > 0) {
                this._editor.executeEdits('inlayHint.default', edits);
                updateInlayHints();
            }
        };
        lines!.addEventListener('-monaco-gesturetap', onTap);
        return {
            dispose: (): void => {
                click.dispose();
                lines!.removeEventListener('-monaco-gesturetap', onTap);
            },
        };
    };
}

// monaco-languageclient 1.0.1 builds a workspace text edit in the old Monaco shape ({resource, edit,
// modelVersionId}); Monaco 0.34 wants {resource, textEdit, versionId} and threw "Unsupported edit", so a
// code action with an edit ("Insert the result as expected result") did nothing.
type ResourceEdits = (this: ProtocolToMonacoConverter, resource: monaco.Uri, edits: TextEdit[],
    asMetadata: (annotation: string | undefined) => monaco.languages.WorkspaceEditMetadata | undefined,
    version?: number) => monaco.languages.IWorkspaceTextEdit[];

function fixWorkspaceTextEdits(): void {
    (ProtocolToMonacoConverter.prototype as unknown as { asResourceEdits: ResourceEdits }).asResourceEdits = function (
        this: ProtocolToMonacoConverter, resource: monaco.Uri, edits: TextEdit[],
        asMetadata: (annotation: string | undefined) => monaco.languages.WorkspaceEditMetadata | undefined,
        version?: number): monaco.languages.IWorkspaceTextEdit[] {
        return edits.map((edit: TextEdit): monaco.languages.IWorkspaceTextEdit => ({
            resource:  resource,
            textEdit:  this.asTextEdit(edit),
            versionId: version,
            metadata:  asMetadata((edit as AnnotatedTextEdit).annotationId),
        }));
    };
}

// Around a result Monaco showed the hover at the end of its query (with the problem marker of a failed
// expected result), on it the result's own tooltip: one or the other, depending on where the mouse was.
// Give a result no hover of its own: the server's hover at the end of the query carries everything
// (QueryResult.hover(): the time, a long result, how to write it as the expected result).
function resultsHoverAtTheirQuery(): void {
    InlayHintsHover.prototype.suggestHoverAnchor = (): null => null;
}

// Monaco's store of the semantic tokens of some lines (not exported by Monaco 0.34)
interface TokenStorage {
    _tokens:     Uint32Array; // 4 numbers per token: delta line, start, end, metadata
    _tokenCount: number;
    acceptInsertText(deltaLine: number, character: number, eolCount: number, firstLineLength: number, lastLineLength: number, firstCharCode: number): void;
}

// Monaco moves the semantic tokens along with an edit until the server's next tokens arrive. One word
// character typed where a token ends grows that token; a token starting there should grow instead or move,
// but Monaco 0.34 leaves it in place, so both tokens cover the new character. On a line with an inlay hint
// Monaco builds the shown text from the token offsets (LineTokens.withInserted), and the overlap repeats
// characters: 1|+2 ?, typing 0, showed 1000+2 until the server's next tokens. Grow such a token, as Monaco
// intends, unless a token ends there: then move it.
function moveTokenAtTypedCharacter(): void {
    const storage: TokenStorage = Object.getPrototypeOf(SparseMultilineTokens.create(1, new Uint32Array(0))._tokens);
    const accept:  TokenStorage['acceptInsertText'] = storage.acceptInsertText;
    storage.acceptInsertText = function (this: TokenStorage, deltaLine: number, character: number, eolCount: number,
                                         firstLineLength: number, lastLineLength: number, firstCharCode: number): void {
        const tokens:   Uint32Array = this._tokens;
        const starting: number[]    = [];
        let   ending:   boolean     = false;
        for (let i: number = 0; i < 4 * this._tokenCount; i += 4) {
            if (tokens[i] === deltaLine && tokens[i + 1] === character && tokens[i + 2] > character) {
                starting.push(i);
            }
            if (tokens[i] === deltaLine && tokens[i + 2] === character) {
                ending = true;
            }
        }
        accept.call(this, deltaLine, character, eolCount, firstLineLength, lastLineLength, firstCharCode);
        for (const i of starting) {
            // still in place: Monaco skipped it
            if (tokens[i] === deltaLine && tokens[i + 1] === character) {
                tokens[i + 1] += ending ? 1 : 0;
                tokens[i + 2] += 1;
            }
        }
    };
}

function ensureServices(): void {
    if (servicesReady) {
        return;
    }
    MonacoServices.install(monaco);
    monaco.languages.register({ id: LANGUAGE_ID, extensions: ['.nl'] });
    // Toggle Line Comment (Cmd/Ctrl+/) and Toggle Block Comment (Shift+Alt+A)
    monaco.languages.setLanguageConfiguration(LANGUAGE_ID, { comments: { lineComment: '//', blockComment: ['/*', '*/'] } });
    borderInlayHints();
    keepInlayHintTextEdits();
    acceptResultsByDoubleTap();
    fixWorkspaceTextEdits();
    resultsHoverAtTheirQuery();
    moveTokenAtTypedCharacter();
    // Query-result inlay hints: the server picks the style bucket via the hint kind (QueryResultCache):
    // no kind = matched expectation (checkmark), Type = plain result, Parameter = failed
    // expectation/error. One dim purple background and one text colour for every outcome, dim enough
    // that the red cross stays visible: the emoji tell the outcome.
    monaco.editor.defineTheme('nelumbo-dark', {
        base:    'vs-dark',
        inherit: true,
        rules:   tokenRules('dark'),
        colors:  {
            // on #4b2f59 (1.47:1 against the editor): lilac text 8.7:1
            'editorInlayHint.foreground':             '#46c98b',
            'editorInlayHint.background':             '#4b2f59',
            'editorInlayHint.typeForeground':         '#ecdcf4',
            'editorInlayHint.typeBackground':         '#4b2f59',
            'editorInlayHint.parameterForeground':    '#ecdcf4',
            'editorInlayHint.parameterBackground':    '#4b2f59',
            // hovers stand out from the editor (the vs-dark default #252526 on #1e1e1e is 1.09:1): 1.76:1, border 6.7:1
            'editorHoverWidget.background':           '#4a4160',
            'editorHoverWidget.statusBarBackground':  '#4a4160',
            'editorHoverWidget.border':               '#a99ae8',
            'editorHoverWidget.foreground':           '#e6e8ee',
        },
    });
    // the same, light: on #e3cdee (1.47:1 against white) purple text 8.7:1
    monaco.editor.defineTheme('nelumbo-light', {
        base:    'vs',
        inherit: true,
        rules:   tokenRules('light'),
        colors:  {
            'editorInlayHint.foreground':             '#1a8a55',
            'editorInlayHint.background':             '#e3cdee',
            'editorInlayHint.typeForeground':         '#4a1d5e',
            'editorInlayHint.typeBackground':         '#e3cdee',
            'editorInlayHint.parameterForeground':    '#4a1d5e',
            'editorInlayHint.parameterBackground':    '#e3cdee',
            // the vs default #f3f3f3 on white is 1.11:1: 1.35:1, border 3.7:1
            'editorHoverWidget.background':           '#e6d9f1',
            'editorHoverWidget.statusBarBackground':  '#e6d9f1',
            'editorHoverWidget.border':               '#8a78d0',
            'editorHoverWidget.foreground':           '#1f1b24',
        },
    });
    // theme.js flips data-theme on <html> (switch click or OS change); the Monaco theme is global
    new MutationObserver((): void => {
        monaco.editor.setTheme(monacoTheme());
    }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    // the webfont may finish loading after the first editor measured its glyphs
    void document.fonts.ready.then((): void => {
        monaco.editor.remeasureFonts();
    });
    watchOverload();
    followVisualViewport();
    servicesReady = true;
}

// WebKit (Safari, and every browser on iOS) reports client rects relative to the visual viewport,
// but position:fixed resolves against the layout viewport. Monaco places the fixedOverflowWidgets
// (suggest, hover) from client rects, so once the visual viewport is panned - iOS zooms in on focus
// and pans for the keyboard - they land off by its offset (floating-ui applies the same correction).
// fields.css shifts them back by the offset; re-place them whenever it changes.
function followVisualViewport(): void {
    const viewport: VisualViewport | null = window.visualViewport;
    if (viewport === null || !CSS.supports('-webkit-backdrop-filter', 'none')) {
        return;
    }
    const update: () => void = (): void => {
        document.documentElement.style.setProperty('--nf-viewport-x', viewport.offsetLeft + 'px');
        document.documentElement.style.setProperty('--nf-viewport-y', viewport.offsetTop + 'px');
        for (const editor of monaco.editor.getEditors()) {
            if (editor.hasTextFocus()) {
                editor.render(true);
            }
        }
    };
    viewport.addEventListener('resize', update);
    viewport.addEventListener('scroll', update);
    update();
}

function showLspBanner(visible: boolean): void {
    if (visible && lspBanner === null) {
        lspBanner             = document.createElement('div');
        lspBanner.className   = 'nelumbo-lsp-banner';
        lspBanner.textContent = 'Language features and evaluation are unavailable (LSP connection failed).';
        document.body.prepend(lspBanner);
    }
    if (lspBanner !== null) {
        lspBanner.classList.toggle('visible', visible);
    }
}

function connectionFailed(): void {
    lastFailure = Date.now();
    showLspBanner(true);
}

// Every connection gets a new client: a stopped client cannot be restarted reliably (vscode-languageclient
// 8 keeps a failed start for good), and a new one opens all models again.
function openConnection(): Promise<MonacoLanguageClient | null> {
    connecting = true;
    return connectLanguageClient().then((client: MonacoLanguageClient | null): MonacoLanguageClient | null => {
        connecting = false;
        if (client === null) {
            connectionFailed();
        } else {
            showLspBanner(false);
        }
        return client;
    });
}

// The server closes a session after 10 idle minutes and a sleeping phone loses it, which leaves the
// editors without colors and results. Reconnect at the next sign of the user, not right away (an idle
// open tab would hold a session for good), and after a failed attempt at most once per RETRY_MS.
function reconnect(): void {
    if (connected || connecting || document.visibilityState !== 'visible' || Date.now() - lastFailure < RETRY_MS) {
        return;
    }
    void openConnection();
}

function watchActivity(): void {
    for (const type of ['pointerdown', 'pointermove', 'keydown', 'touchstart', 'focus']) {
        window.addEventListener(type, reconnect, { capture: true, passive: true });
    }
    document.addEventListener('visibilitychange', reconnect);
}

// The server marks evaluations it stopped to protect other users with this diagnostic code; show a
// banner while any editor carries such a marker (it clears with the next evaluation of that document).
function watchOverload(): void {
    monaco.editor.onDidChangeMarkers((): void => {
        const overloaded: boolean = monaco.editor.getModelMarkers({}).some((m: monaco.editor.IMarker): boolean =>
            m.code === OVERLOAD_CODE || (typeof m.code === 'object' && m.code !== null && m.code.value === OVERLOAD_CODE));
        if (overloaded && overloadBanner === null) {
            overloadBanner             = document.createElement('div');
            overloadBanner.className   = 'nelumbo-lsp-banner nelumbo-overload-banner';
            overloadBanner.textContent = 'The server is busy - an evaluation was stopped. Try again in a moment.';
            document.body.prepend(overloadBanner);
        }
        if (overloadBanner !== null) {
            overloadBanner.classList.toggle('visible', overloaded);
        }
    });
}

function addSolutionToggle(field: HTMLElement, toolbar: HTMLDivElement, index: number): void {
    const next: Element | null = field.nextElementSibling;
    if (next === null || !next.classList.contains('nelumbo-solution')) {
        return;
    }
    const solution: HTMLElement       = next as HTMLElement;
    const button:   HTMLButtonElement = document.createElement('button');
    button.type                       = 'button';
    button.textContent                = 'Show solution';
    button.addEventListener('click', (): void => {
        const visible: boolean = solution.classList.toggle('visible');
        button.textContent = visible ? 'Hide solution' : 'Show solution';
    });
    toolbar.prepend(button);
    // pull the solution inside the field so it shares the editor's border, right below the toggle
    field.appendChild(solution);
    buildSolutionViewer(solution, index);
}

function savedText(key: string, original: string): string {
    try {
        const saved: { original?: unknown; text?: unknown } | null = JSON.parse(localStorage.getItem(key) ?? 'null');
        if (saved !== null && saved.original === original && typeof saved.text === 'string') {
            return saved.text;
        }
    } catch {
        // no storage (private mode, blocked) or a broken entry: the original
    }
    return original;
}

function saveText(field: Field): void {
    const text: string = field.model.getValue();
    try {
        if (text === field.original) {
            localStorage.removeItem(field.key);
        } else {
            localStorage.setItem(field.key, JSON.stringify({ original: field.original, text: text }));
        }
    } catch {
        // no storage (private mode, blocked, full): the edit is just not kept
    }
    field.reset.hidden = text === field.original;
}

// one undoable edit of its own (not merged with the typing before it), so Ctrl/Cmd+Z brings back what was there
function replaceText(model: monaco.editor.ITextModel, text: string): void {
    model.pushStackElement();
    model.pushEditOperations([], [{ range: model.getFullModelRange(), text: text }], (): null => null);
    model.pushStackElement();
}

// replacing the whole text leaves it all selected: start at the top of the new document instead
function revealTop(model: monaco.editor.ITextModel): void {
    for (const editor of monaco.editor.getEditors()) {
        if (editor.getModel() === model) {
            editor.setPosition({ lineNumber: 1, column: 1 });
            editor.revealLine(1);
        }
    }
}

// The random id that makes a shared name this browser's (the server keeps only its hash). Without storage
// (private mode, blocked) it lives for this page only: saving works, updating after a reload does not.
function browserId(): string {
    try {
        const stored: string | null = localStorage.getItem(BROWSER_ID_KEY);
        if (stored !== null) {
            return stored;
        }
    } catch {
        // no storage: the page's own id below
    }
    if (pageBrowserId === null) {
        // not crypto.randomUUID: it exists only in secure contexts, not on a classroom server over plain HTTP
        const bytes: Uint8Array = crypto.getRandomValues(new Uint8Array(16));
        pageBrowserId           = Array.from(bytes, (b: number): string => b.toString(16).padStart(2, '0')).join('');
        try {
            localStorage.setItem(BROWSER_ID_KEY, pageBrowserId);
        } catch {
            // the id lives for this page only
        }
    }
    return pageBrowserId;
}

function lastShareName(): string {
    try {
        return localStorage.getItem(SHARE_NAME_KEY) ?? '';
    } catch {
        return '';
    }
}

function rememberShareName(name: string): void {
    try {
        localStorage.setItem(SHARE_NAME_KEY, name);
    } catch {
        // no storage: the name is just not prefilled next time
    }
}

interface ShareOutcome {
    text:  string;
    error: boolean;
}

// the server's answer is the message to show, success or not
async function saveSolution(field: Field, name: string): Promise<ShareOutcome> {
    try {
        const response: Response = await fetch('/solutions/' + encodeURIComponent(name), {
            method:  'PUT',
            headers: { 'Content-Type': 'text/plain; charset=utf-8', 'X-Nelumbo-Browser': browserId() },
            body:    field.model.getValue(),
        });
        if (response.ok) {
            rememberShareName(name);
        }
        return { text: await response.text(), error: !response.ok };
    } catch {
        return { text: 'Not shared: no connection to the server', error: true };
    }
}

// null when the user keeps their own text. Asking first: the loaded text replaces the field's saved edit
// (saveText), so after a reload neither undo nor Reset brings the own attempt back.
async function loadSolution(field: Field, name: string): Promise<ShareOutcome | null> {
    let response: Response;
    let text:     string;
    try {
        response = await fetch('/solutions/' + encodeURIComponent(name));
        text     = await response.text();
    } catch {
        return { text: 'Not loaded: no connection to the server', error: true };
    }
    if (!response.ok) {
        return { text: text, error: true };
    }
    if (field.model.getValue() !== field.original && !confirm('Replace your own text?')) {
        return null;
    }
    replaceText(field.model, text);
    revealTop(field.model);
    rememberShareName(name);
    return { text: 'Loaded "' + name + '"', error: false };
}

function toolbarButton(text: string): HTMLButtonElement {
    const button: HTMLButtonElement = document.createElement('button');
    button.type                     = 'button';
    button.textContent              = text;
    return button;
}

const SHARE_MESSAGE_MS: number = 10_000;

// a globe drawn with the text colour (outline, meridian, equator)
const GLOBE_SVG: string = '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" '
                        + 'stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M3 12h18'
                        + 'M12 3c2.5 2.7 3.8 5.7 3.8 9s-1.3 6.3-3.8 9c-2.5-2.7-3.8-5.7-3.8-9s1.3-6.3 3.8-9z"/></svg>';

let sharingPromise: Promise<boolean> | null = null;

// One request per page; the globe is only added while the server allows sharing.
function sharingEnabled(): Promise<boolean> {
    if (sharingPromise === null) {
        sharingPromise = fetch('/solutions')
            .then((response: Response): Promise<{ enabled: boolean }> => response.json())
            .then((body: { enabled: boolean }): boolean => body.enabled === true)
            .catch((): boolean => false);
    }
    return sharingPromise;
}

// The globe opens a menu with Share... and Load...; each opens a name row (input, action, Cancel) in place of
// the globe. The outcome shows in the toolbar for SHARE_MESSAGE_MS, or until the next opening.
function addShareButtons(toolbar: HTMLDivElement, field: Field): void {
    const share:        HTMLSpanElement    = document.createElement('span');
    const globe:        HTMLButtonElement  = toolbarButton('');
    const menu:         HTMLDivElement     = document.createElement('div');
    const shareAs:      HTMLButtonElement  = toolbarButton('Share...');
    const loadAs:       HTMLButtonElement  = toolbarButton('Load...');
    const input:        HTMLInputElement   = document.createElement('input');
    const go:           HTMLButtonElement  = toolbarButton('Share');
    const cancel:       HTMLButtonElement  = toolbarButton('Cancel');
    const message:      HTMLSpanElement    = document.createElement('span');
    let   saving:       boolean            = true;
    let   messageTimer: number | undefined = undefined;

    share.className    = 'nelumbo-share';
    globe.className    = 'nelumbo-share-globe';
    globe.innerHTML    = GLOBE_SVG;
    globe.title        = 'Share or load by name';
    globe.setAttribute('aria-label', 'Share or load by name');
    globe.setAttribute('aria-haspopup', 'menu');
    menu.className     = 'nelumbo-share-menu';
    menu.setAttribute('role', 'menu');
    shareAs.title      = 'Anyone who knows the name can load it. Your IP address is kept with it for 7 days.';
    shareAs.setAttribute('role', 'menuitem');
    loadAs.setAttribute('role', 'menuitem');
    input.type         = 'text';
    input.className    = 'nelumbo-share-name';
    input.maxLength    = 40;
    input.placeholder  = 'name';
    input.spellcheck   = false;
    input.autocomplete = 'off';
    input.setAttribute('aria-label', 'Solution name');
    go.className       = 'nelumbo-share-go';
    message.className  = 'nelumbo-share-message';
    message.setAttribute('role', 'status');

    function setMenu(open: boolean): void {
        menu.hidden = !open;
        globe.setAttribute('aria-expanded', String(open));
    }

    // back to the globe; it gets the focus the hidden row had
    function setOpen(open: boolean): void {
        setMenu(false);
        globe.hidden  = open;
        input.hidden  = !open;
        go.hidden     = !open;
        cancel.hidden = !open;
    }

    function close(): void {
        setOpen(false);
        globe.focus();
    }

    function showMessage(text: string, error: boolean): void {
        window.clearTimeout(messageTimer);
        message.textContent = text;
        message.classList.toggle('error', error);
        if (text !== '') {
            messageTimer = window.setTimeout((): void => showMessage('', false), SHARE_MESSAGE_MS);
        }
    }

    function open(save: boolean): void {
        saving              = save;
        go.textContent      = save ? 'Share' : 'Load';
        showMessage('', false);
        input.value         = lastShareName();
        setOpen(true);
        input.focus();
        input.select();
    }

    async function run(): Promise<void> {
        const name: string = input.value.trim();
        if (name === '') {
            input.focus();
            return;
        }
        close();
        const outcome: ShareOutcome | null = saving ? await saveSolution(field, name) : await loadSolution(field, name);
        if (outcome !== null) {
            showMessage(outcome.text, outcome.error);
        }
    }

    globe.addEventListener('click', (): void => {
        setMenu(menu.hidden);
        if (!menu.hidden) {
            shareAs.focus();
        }
    });
    shareAs.addEventListener('click', (): void => {
        open(true);
    });
    loadAs.addEventListener('click', (): void => {
        open(false);
    });
    menu.addEventListener('keydown', (event: KeyboardEvent): void => {
        if (event.key === 'Escape') {
            setMenu(false);
            globe.focus();
        }
    });
    document.addEventListener('pointerdown', (event: PointerEvent): void => {
        if (!menu.hidden && !share.contains(event.target as Node)) {
            setMenu(false);
        }
    });
    go.addEventListener('click', (): void => {
        void run();
    });
    cancel.addEventListener('click', (): void => {
        close();
    });
    input.addEventListener('keydown', (event: KeyboardEvent): void => {
        if (event.key === 'Enter') {
            // else the Enter also clicks the globe, which gets the focus during run()
            event.preventDefault();
            void run();
        } else if (event.key === 'Escape') {
            close();
        }
    });

    menu.append(shareAs, loadAs);
    share.append(globe, menu, input, go, cancel, message);
    setOpen(false);
    toolbar.insertBefore(share, field.reset);
}

// Always LF line endings: a model whose text has none (an empty field) otherwise takes the OS default, CRLF on
// Windows, and its text would never equal an original loaded from the server.
function createModel(text: string, uri: monaco.Uri): monaco.editor.ITextModel {
    const model: monaco.editor.ITextModel = monaco.editor.createModel(text, LANGUAGE_ID, uri);
    model.setEOL(monaco.editor.EndOfLineSequence.LF);
    return model;
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
    const model: monaco.editor.ITextModel = createModel(text, uri);

    const viewer: monaco.editor.IStandaloneCodeEditor = monaco.editor.create(solution, {
        model:                model,
        theme:                monacoTheme(),
        readOnly:             true,
        domReadOnly:          true,
        minimap:              { enabled: false },
        automaticLayout:      true,
        fontSize:             13,
        fontFamily:           '"JetBrains Mono", ui-monospace, Menlo, Consolas, monospace',
        'semanticHighlighting.enabled': true,
        scrollBeyondLastLine: false,
        padding:              { top: 12, bottom: 8 },
        // room between a query result and its dashed border
        inlayHints:           { padding: true },
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
    let original: string = div.textContent || '';
    if (original.startsWith('\n')) {
        original = original.slice(1);
    }
    // the position on the page, not the mount index: the tutorial mounts its sections in any order
    const baseKey: string = STORAGE_PREFIX + location.pathname + ':' + Array.from(document.querySelectorAll('.nelumbo-field')).indexOf(div);
    div.textContent = '';
    div.classList.add('nelumbo-field-wrap');

    const host: HTMLDivElement = document.createElement('div');
    host.className = 'nelumbo-field-editor';
    if (div.dataset.height) {
        host.style.height = div.dataset.height;
    }
    div.appendChild(host);

    const toolbar: HTMLDivElement    = document.createElement('div');
    toolbar.className                = 'nelumbo-field-toolbar';
    const reset:   HTMLButtonElement = document.createElement('button');
    reset.type                       = 'button';
    reset.className                  = 'nelumbo-reset';
    reset.textContent                = 'Reset';
    toolbar.appendChild(reset);
    div.appendChild(toolbar);

    addSolutionToggle(div, toolbar, index);

    const uri:   monaco.Uri               = monaco.Uri.parse('inmemory://field-' + index + '.nl');
    const model: monaco.editor.ITextModel = createModel(savedText(baseKey, original), uri);
    const field: Field                    = { model: model, baseKey: baseKey, key: baseKey, original: original, pageText: original, reset: reset };
    reset.addEventListener('click', (): void => {
        replaceText(model, field.original);
    });
    model.onDidChangeContent((): void => {
        saveText(field);
    });
    reset.hidden = model.getValue() === original;
    if (!div.hasAttribute('data-no-share')) {
        void sharingEnabled().then((enabled: boolean): void => {
            if (enabled) {
                addShareButtons(toolbar, field);
            }
        });
    }

    const editor: monaco.editor.IStandaloneCodeEditor = monaco.editor.create(host, {
        model:                model,
        theme:                monacoTheme(),
        minimap:              { enabled: false },
        automaticLayout:      true,
        fontSize:             13,
        fontFamily:           '"JetBrains Mono", ui-monospace, Menlo, Consolas, monospace',
        'semanticHighlighting.enabled': true,
        scrollBeyondLastLine: false,
        padding:              { top: 12, bottom: 8 },
        // room between a query result and its dashed border
        inlayHints:           { padding: true },
        // hovers escape the field's overflow:hidden border and overlapping sections
        fixedOverflowWidgets: true,
        // Cmd/Ctrl+Click goes to definition (Alt+Click is multi-cursor), the VS Code default made explicit
        multiCursorModifier:  'alt',
        // no code-action lightbulb: without a glyph margin Monaco draws it over the text of the line (or the
        // line above); a double-click on a result does what its one action does, Cmd/Ctrl+. still lists them
        lightbulb:            { enabled: false },
    });
    __editors.push({ editor: editor, model: model });
    fields.set(div, field);
}

// Establish the single page-shared /lsp language client. Idempotent: repeated calls return the
// same promise. On failure resolves null and shows the banner (editing still works); a failed or lost
// connection is retried at the next user activity (reconnect).
export function connect(): Promise<MonacoLanguageClient | null> {
    ensureServices();
    if (clientPromise === null) {
        clientPromise = openConnection();
        watchActivity();
    }
    return clientPromise;
}

// Upgrade every .nelumbo-field in `container` not already mounted into a Monaco editor. Idempotent
// (skips fields already wrapped), so it is safe to call each time a tutorial section is shown. The shared
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

// Load document `name` with original text `text` into a mounted .nelumbo-field (the sandbox loading an
// example): the saved edits of that document when there are any, else `text`. One undoable edit, so
// Ctrl/Cmd+Z brings back what was there; false when the field is not mounted (yet).
export function setFieldText(div: HTMLElement, text: string, name: string): boolean {
    const field: Field | undefined = fields.get(div);
    return field !== undefined && loadText(field, field.baseKey + '#' + name, text);
}

// Back to the field's own document from the page (the sandbox's starter, when Back leaves the examples): its saved
// edits when there are any. One undoable edit; false when the field is not mounted (yet).
export function restoreFieldText(div: HTMLElement): boolean {
    const field: Field | undefined = fields.get(div);
    return field !== undefined && loadText(field, field.baseKey, field.pageText);
}

function loadText(field: Field, key: string, text: string): boolean {
    field.key      = key;
    field.original = text;
    replaceText(field.model, savedText(field.key, text));
    revealTop(field.model);
    // the edit does not fire a change when the text stays the same, but the key and original did change
    saveText(field);
    return true;
}

// Sandbox entry point: mount every field on the page and connect once. Lifecycle is page-scoped
// (no teardown); standalone monaco falls back to a synchronous main-thread worker (one console
// warning) since the /lsp server supplies all language features.
export async function initNelumboFields(): Promise<void> {
    mountFields(document);
    await connect();
}
