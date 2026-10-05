// Load test for the website: simulates tour-page visitors in steps of 1, 2, 4, ... clients.
// Every client loads the page, opens an LSP session with all tour documents and keeps editing
// queries the way the Monaco client does (one didChange per keystroke, completion + semantic
// tokens while typing, all inlay hints re-pulled on every workspace/inlayHint/refresh).
// Clients stay connected, so every step adds load on top of the previous one. Per step it
// reports page-load time, LSP round-trip time, and edit-to-result time (last keystroke until
// the edited document's inlay hints have changed). Stops after the first step outside the limits.
//
//   node load/loadtest.mts [--url URL] [--max-clients N] [--step-seconds S] [--limit-ms MS]
//                          [--ssh USER@HOST] [--container NAME]
//
// --ssh samples `docker stats` and the load average on the server during the run.

import { spawn } from 'node:child_process';

const ASSETS:          string[] = ['/assets/nelumbo-fields.css', '/assets/nelumbo-fields.js'];
const JOIN_MS:         number   = 250;
const OPEN_MS:         number   = 300;
const KEY_MS:          number   = 100;
const REQUEST_TIMEOUT: number   = 30_000;
const RESULT_TIMEOUT:  number   = 40_000;
const CLOSE_REJECTED:  number   = 1013;

interface Options {
    url:         string;
    maxClients:  number;
    stepSeconds: number;
    limitMs:     number;
    ssh:         string | null;
    container:   string;
}

interface Doc {
    uri:     string;
    text:    string;
    version: number;
    query:   number | null;  // line of the query to vary, null for read-only solution viewers
    hints:   string;         // last pulled inlay hints as JSON, '[]' until evaluated (a broken document stays so)
}

interface Pending {
    method:  string;
    start:   number;
    resolve: (result: unknown) => void;
    timer:   ReturnType<typeof setTimeout>;
}

class Stats {
    page:     number[] = [];
    lsp:      number[] = [];
    result:   number[] = [];
    rejected: number   = 0;
    errors:   string[] = [];
    cpu:      number[] = [];
    mem:      number[] = [];
    load:     number[] = [];
}

let stats:    Stats  = new Stats();
let sessions: number = 0;

function now(): number {
    return performance.now();
}

function sleep(ms: number): Promise<void> {
    return new Promise((resolve: () => void) => setTimeout(resolve, ms));
}

function rand(min: number, max: number): number {
    return min + Math.random() * (max - min);
}

function percentile(values: number[], p: number): number {
    if (values.length === 0) {
        return NaN;
    }
    const sorted: number[] = [...values].sort((a: number, b: number) => a - b);
    return sorted[Math.max(0, Math.ceil(p * sorted.length) - 1)];
}

function parseArgs(args: string[]): Options {
    const options: Options = { url: 'http://localhost:8899', maxClients: 64, stepSeconds: 60, limitMs: 1000, ssh: null, container: 'nelumbo' };
    for (let i: number = 0; i < args.length; i++) {
        const value: string = args[++i];
        switch (args[i - 1]) {
            case '--url':
                options.url = value.replace(/\/$/, '');
                break;
            case '--max-clients':
                options.maxClients = Number(value);
                break;
            case '--step-seconds':
                options.stepSeconds = Number(value);
                break;
            case '--limit-ms':
                options.limitMs = Number(value);
                break;
            case '--ssh':
                options.ssh = value;
                break;
            case '--container':
                options.container = value;
                break;
            default:
                console.error('unknown argument: ' + args[i - 1]);
                process.exit(2);
        }
    }
    return options;
}

// The tour page's editor contents, in page order, with the URIs the Monaco client gives them.
async function loadDocs(url: string): Promise<Doc[]> {
    const html:   string   = await (await fetch(url + '/tour.html')).text();
    const docs:   Doc[]    = [];
    const regex:  RegExp   = /<(div|pre) class="(nelumbo-field|nelumbo-solution)"[^>]*>([\s\S]*?)<\/\1>/g;
    let   fields: number   = 0;
    let   match:  RegExpExecArray | null;
    while ((match = regex.exec(html)) !== null) {
        const text:  string  = match[3].replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&amp;/g, '&');
        const field: boolean = match[2] === 'nelumbo-field';
        const uri:   string  = field ? 'inmemory://field-' + fields + '.nl' : 'inmemory://solution-' + (fields - 1) + '.nl';
        const query: number  = text.split('\n').findIndex((line: string) => /^\s*[^/\s].*\?(\s*\[.*\])?\s*$/.test(line));
        docs.push({ uri: uri, text: text, version: 1, query: field && query >= 0 ? query : null, hints: '[]' });
        if (field) {
            fields++;
        }
    }
    return docs;
}

class Client {
    private readonly url:      string;
    private readonly docs:     Doc[];

    private readonly pending:  Map<number, Pending>            = new Map();
    private ws:                WebSocket | null                = null;
    private nextId:            number                          = 1;
    private stopped:           boolean                         = false;
    private connected:         boolean                         = false;
    private lastChange:        number                          = 0;
    private editDoc:           Doc | null                      = null;
    private editCheck:         (hints: string) => boolean      = () => false;
    private editStart:         number                          = 0;
    private editDone:          ((ok: boolean) => void) | null  = null;

    constructor(url: string, docs: Doc[]) {
        this.url  = url;
        this.docs = docs.map((d: Doc) => ({ ...d }));
    }

    async run(): Promise<void> {
        await this.loadPage();
        if (!await this.connect()) {
            return;
        }
        await this.openDocs();
        while (!this.stopped) {
            await sleep(rand(5000, 15000));
            if (!this.stopped) {
                await this.editQuery();
            }
        }
    }

    stop(): void {
        this.stopped = true;
        this.ws?.close(1000);
        this.finishEdit(false);
    }

    private async loadPage(): Promise<void> {
        const start: number = now();
        try {
            const urls:      string[]   = ['/tour.html', ...ASSETS].map((path: string) => this.url + path);
            const responses: Response[] = await Promise.all(urls.map((u: string) => fetch(u)));
            for (const response of responses) {
                await response.arrayBuffer();
                if (response.status !== 200) {
                    stats.errors.push('HTTP ' + response.status + ' ' + response.url);
                }
            }
            stats.page.push(now() - start);
        } catch (e) {
            stats.errors.push('page load: ' + e);
        }
    }

    private connect(): Promise<boolean> {
        return new Promise((resolve: (ok: boolean) => void) => {
            const ws: WebSocket = new WebSocket(this.url.replace(/^http/, 'ws') + '/lsp');
            this.ws = ws;
            ws.addEventListener('open', () => {
                this.request('initialize', { processId: null, clientInfo: { name: 'loadtest' }, rootUri: null, capabilities: {} }).then((result: unknown) => {
                    if (result === undefined) {
                        resolve(false);
                        return;
                    }
                    sessions++;
                    this.connected = true;
                    this.notify('initialized', {});
                    resolve(true);
                });
            });
            ws.addEventListener('message', (event: MessageEvent) => this.onMessage(String(event.data)));
            ws.addEventListener('close', (event: CloseEvent) => {
                for (const p of this.pending.values()) {
                    clearTimeout(p.timer);
                    p.resolve(undefined);
                }
                this.pending.clear();
                if (this.connected) {
                    sessions--;
                }
                if (event.code === CLOSE_REJECTED) {
                    stats.rejected++;
                } else if (!this.stopped) {
                    stats.errors.push('socket closed ' + event.code + ' ' + event.reason);
                }
                this.stopped = true;
                this.finishEdit(false);
                resolve(false);
            });
            ws.addEventListener('error', () => {
                if (!this.stopped) {
                    stats.errors.push('socket error');
                }
            });
        });
    }

    private async openDocs(): Promise<void> {
        for (const doc of this.docs) {
            if (this.stopped) {
                return;
            }
            const textDocument: { uri: string } = { uri: doc.uri };
            this.notify('textDocument/didOpen', { textDocument: { uri: doc.uri, languageId: 'nelumbo', version: doc.version, text: doc.text } });
            this.request('textDocument/codeAction', { textDocument: textDocument, range: range(0, 0), context: { diagnostics: [] } });
            this.pullHint(doc);
            this.request('textDocument/semanticTokens/full', { textDocument: textDocument });
            this.request('textDocument/foldingRange', { textDocument: textDocument });
            await sleep(OPEN_MS);
        }
    }

    // Types a variant of one of the document's queries on a new line below it, waits until the
    // document's inlay hints change, then reverts the document and waits until they are back.
    // An exact copy could leave the hints unchanged (equal queries share one result), so the
    // variant differs in its expectation.
    private async editQuery(): Promise<void> {
        const fields:   Doc[]    = this.docs.filter((d: Doc) => d.query !== null && d.hints !== '[]');
        if (fields.length === 0) {
            return;
        }
        const doc:      Doc      = fields[Math.floor(Math.random() * fields.length)];
        const before:   string   = doc.hints;
        const line:     number   = (doc.query as number) + 1;
        const original: string   = doc.text;
        const all:      string[] = original.split('\n');
        const query:    string   = all[line - 1].trimEnd();
        const stripped: string   = query.replace(/\?.*$/, '?');
        const typed:    string   = stripped !== query ? stripped : query + ' [..][..]';
        const head:     string   = all.slice(0, line).join('\n') + '\n';
        const tail:     string   = '\n' + all.slice(line).join('\n');
        for (let i: number = 1; i <= typed.length && !this.stopped; i++) {
            this.change(doc, head + typed.slice(0, i) + tail);
            if (i % 2 === 0) {
                this.request('textDocument/completion', { textDocument: { uri: doc.uri }, position: { line: line, character: i }, context: { triggerKind: 1 } });
                this.request('textDocument/semanticTokens/full', { textDocument: { uri: doc.uri } });
            }
            await sleep(KEY_MS);
        }
        const shown: boolean = await this.awaitResult(doc, (hints: string) => hints !== before);
        await sleep(rand(2000, 5000));
        if (!this.stopped) {
            this.change(doc, original);
            if (shown) {
                await this.awaitResult(doc, (hints: string) => hints === before);
            }
        }
    }

    private change(doc: Doc, text: string): void {
        doc.text = text;
        doc.version++;
        this.lastChange = now();
        this.notify('textDocument/didChange', { textDocument: { uri: doc.uri, version: doc.version }, contentChanges: [{ text: text }] });
    }

    // Resolves true once a pull of the edited document's hints passes check, false on timeout or stop.
    private awaitResult(doc: Doc, check: (hints: string) => boolean): Promise<boolean> {
        return new Promise((resolve: (ok: boolean) => void) => {
            if (this.stopped) {
                resolve(false);
                return;
            }
            const timer: ReturnType<typeof setTimeout> = setTimeout(() => {
                stats.errors.push('no result ' + RESULT_TIMEOUT / 1000 + 's after edit of ' + doc.uri);
                this.finishEdit(false);
            }, RESULT_TIMEOUT);
            this.editDoc   = doc;
            this.editCheck = check;
            this.editStart = this.lastChange;
            this.editDone  = (ok: boolean) => {
                clearTimeout(timer);
                resolve(ok);
            };
        });
    }

    private finishEdit(ok: boolean): void {
        const done: ((ok: boolean) => void) | null = this.editDone;
        this.editDoc  = null;
        this.editDone = null;
        done?.(ok);
    }

    private onMessage(data: string): void {
        const message: { id?: number; method?: string; result?: unknown; error?: { message: string } } = JSON.parse(data);
        if (message.method === undefined && message.id !== undefined) {
            const p: Pending | undefined = this.pending.get(message.id);
            if (p !== undefined) {
                this.pending.delete(message.id);
                clearTimeout(p.timer);
                stats.lsp.push(now() - p.start);
                if (message.error !== undefined) {
                    stats.errors.push(p.method + ': ' + message.error.message);
                }
                p.resolve(message.error === undefined ? message.result : null);
            }
        } else if (message.method !== undefined && message.id !== undefined) {
            this.send({ jsonrpc: '2.0', id: message.id, result: null });
            if (message.method === 'workspace/inlayHint/refresh') {
                this.pullHints();
            }
        }
    }

    private pullHints(): void {
        for (const doc of this.docs) {
            this.pullHint(doc);
        }
    }

    private pullHint(doc: Doc): void {
        this.request('textDocument/inlayHint', { textDocument: { uri: doc.uri }, range: range(0, lines(doc.text)) }).then((result: unknown) => {
            if (!Array.isArray(result)) {
                return;
            }
            doc.hints = JSON.stringify(result);
            if (doc === this.editDoc && this.editCheck(doc.hints)) {
                stats.result.push(now() - this.editStart);
                this.finishEdit(true);
            }
        });
    }

    // Resolves with the result, null on an error response, undefined when the socket closed or timed out.
    private request(method: string, params: unknown): Promise<unknown> {
        return new Promise((resolve: (result: unknown) => void) => {
            if (this.ws === null || this.ws.readyState !== WebSocket.OPEN) {
                resolve(undefined);
                return;
            }
            const id:    number                        = this.nextId++;
            const timer: ReturnType<typeof setTimeout> = setTimeout(() => {
                this.pending.delete(id);
                stats.errors.push('timeout ' + method);
                resolve(undefined);
            }, REQUEST_TIMEOUT);
            this.pending.set(id, { method: method, start: now(), resolve: resolve, timer: timer });
            this.send({ jsonrpc: '2.0', id: id, method: method, params: params });
        });
    }

    private notify(method: string, params: unknown): void {
        this.send({ jsonrpc: '2.0', method: method, params: params });
    }

    private send(message: unknown): void {
        if (this.ws !== null && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        }
    }
}

function lines(text: string): number {
    return text.split('\n').length - 1;
}

function range(startLine: number, endLine: number): { start: { line: number; character: number }; end: { line: number; character: number } } {
    return { start: { line: startLine, character: 0 }, end: { line: endLine, character: 0 } };
}

// Samples container CPU/memory and the host load average over one long-lived ssh connection.
function startMonitor(options: Options): ReturnType<typeof spawn> {
    const remote: string = 'echo "H $(nproc) $(free -m | awk \'/^Mem:/{print $2}\')"; '
                         + 'while true; do echo "S $(cut -d" " -f1 /proc/loadavg) $(docker stats --no-stream --format \'{{.CPUPerc}} {{.MemUsage}}\' ' + options.container + ')"; sleep 2; done';
    const child:  ReturnType<typeof spawn> = spawn('ssh', ['-o', 'BatchMode=yes', options.ssh as string, remote], { stdio: ['ignore', 'pipe', 'pipe'] });
    let   buffer: string = '';
    child.stdout?.on('data', (chunk: Buffer) => {
        buffer += chunk.toString();
        let newline: number;
        while ((newline = buffer.indexOf('\n')) >= 0) {
            onMonitorLine(buffer.slice(0, newline));
            buffer = buffer.slice(newline + 1);
        }
    });
    child.stderr?.on('data', (chunk: Buffer) => process.stderr.write('ssh: ' + chunk.toString()));
    return child;
}

function onMonitorLine(line: string): void {
    const parts: string[] = line.trim().split(/\s+/);
    if (parts[0] === 'H') {
        console.log('server: ' + parts[1] + ' cores, ' + (Number(parts[2]) / 1024).toFixed(1) + ' GiB RAM');
    } else if (parts[0] === 'S') {
        stats.load.push(Number(parts[1]));
        if (parts.length >= 4) {
            stats.cpu.push(parseFloat(parts[2]));
            stats.mem.push(toMiB(parts[3]));
        }
    }
}

function toMiB(size: string): number {
    const match: RegExpMatchArray | null = size.match(/^([\d.]+)([KMG]i?B)$/);
    if (match === null) {
        return NaN;
    }
    const factor: Record<string, number> = { KiB: 1 / 1024, MiB: 1, GiB: 1024, KB: 1 / 1000, MB: 1, GB: 1000 };
    return Number(match[1]) * factor[match[2]];
}

function steps(max: number): number[] {
    const result: number[] = [];
    for (let n: number = 1; n < max; n *= 2) {
        result.push(n);
    }
    result.push(max);
    return result;
}

const COLUMNS: string[] = ['clients', 'sessions', 'rejected', 'errors', 'page p95', 'lsp p50', 'lsp p95', 'lsp max', 'result p50', 'result p95', 'result max', 'cpu max', 'mem max', 'load max', 'verdict'];

function row(cells: string[]): string {
    return cells.map((c: string, i: number) => i === cells.length - 1 ? '  ' + c : c.padStart(COLUMNS[i].length + 2)).join('');
}

function ms(value: number): string {
    return isNaN(value) ? '-' : Math.round(value) + '';
}

function max(values: number[]): number {
    return values.length === 0 ? NaN : Math.max(...values);
}

// Prints the step's row; returns whether the step stayed within the limits.
function report(clients: number, s: Stats, options: Options): boolean {
    const problems: string[] = [];
    const page:     number   = percentile(s.page, 0.95);
    const lspP95:   number   = percentile(s.lsp, 0.95);
    const result:   number   = percentile(s.result, 0.95);
    if (s.rejected > 0) {
        problems.push(s.rejected + ' rejected');
    }
    if (s.errors.length > 0) {
        problems.push(s.errors.length + ' errors');
    }
    if (page > options.limitMs) {
        problems.push('page p95 > ' + options.limitMs);
    }
    if (lspP95 > options.limitMs) {
        problems.push('lsp p95 > ' + options.limitMs);
    }
    if (result > options.limitMs) {
        problems.push('result p95 > ' + options.limitMs);
    }
    if (s.result.length === 0) {
        problems.push('no edit results');
    }
    console.log(row([
        clients + '', sessions + '', s.rejected + '', s.errors.length + '',
        ms(page), ms(percentile(s.lsp, 0.5)), ms(lspP95), ms(max(s.lsp)),
        ms(percentile(s.result, 0.5)), ms(result), ms(max(s.result)),
        isNaN(max(s.cpu)) ? '-' : max(s.cpu).toFixed(0) + '%', isNaN(max(s.mem)) ? '-' : ms(max(s.mem)) + 'M', isNaN(max(s.load)) ? '-' : max(s.load).toFixed(1),
        problems.length === 0 ? 'OK' : 'FAIL: ' + problems.join(', '),
    ]));
    for (const e of [...new Set(s.errors)].slice(0, 5)) {
        console.log('    ' + e);
    }
    return problems.length === 0;
}

async function main(): Promise<void> {
    const options: Options  = parseArgs(process.argv.slice(2));
    const docs:    Doc[]    = await loadDocs(options.url);
    const monitor: ReturnType<typeof spawn> | null = options.ssh === null ? null : startMonitor(options);
    const clients: Client[] = [];
    let   best:    number   = 0;
    console.log('target ' + options.url + ': ' + docs.length + ' tour documents, ' + options.stepSeconds + 's per step, limit ' + options.limitMs + ' ms (times in ms)');
    console.log(row(COLUMNS));
    for (const n of steps(options.maxClients)) {
        stats = new Stats();
        while (clients.length < n) {
            const client: Client = new Client(options.url, docs);
            clients.push(client);
            client.run().catch((e: unknown) => stats.errors.push('client: ' + e));
            await sleep(JOIN_MS);
        }
        await sleep(options.stepSeconds * 1000);
        if (!report(n, stats, options)) {
            break;
        }
        best = n;
    }
    for (const client of clients) {
        client.stop();
    }
    monitor?.kill();
    console.log(best === 0 ? 'already 1 client exceeds the limits' : 'highest step within the limits: ' + best + ' clients');
    process.exit(best === options.maxClients ? 0 : 1);
}

main();
