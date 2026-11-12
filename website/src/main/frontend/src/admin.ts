import { start as startStats } from './status-chart';

interface Snippet {
    name:    string;
    savedAt: number;
    bytes:   number;
    owner:   string;
    ip:      string | null;
}

const TABS: string[] = ['stats', 'snippets'];

let statsStarted:   boolean = false;
let snippetsLoaded: boolean = false;

function el(id: string): HTMLElement {
    return document.getElementById(id) as HTMLElement;
}

function two(n: number): string {
    return n < 10 ? '0' + n : String(n);
}

function formatTime(ms: number): string {
    const d: Date = new Date(ms);
    return d.getFullYear() + '-' + two(d.getMonth() + 1) + '-' + two(d.getDate()) + ' ' + two(d.getHours()) + ':' + two(d.getMinutes());
}

function formatSize(bytes: number): string {
    return bytes < 1024 ? bytes + ' B' : (bytes / 1024).toFixed(1) + ' KB';
}

function currentTab(): string {
    const name: string = location.hash.replace('#', '');
    return TABS.indexOf(name) >= 0 ? name : 'stats';
}

function showTab(name: string): void {
    TABS.forEach((t: string): void => {
        el('tab-' + t).setAttribute('aria-selected', String(t === name));
        el('panel-' + t).hidden = t !== name;
    });
    if (name === 'stats') {
        if (statsStarted) {
            // charts resized while hidden have width 0
            window.dispatchEvent(new Event('resize'));
        } else {
            statsStarted = true;
            startStats();
        }
    } else if (!snippetsLoaded) {
        snippetsLoaded = true;
        loadSnippets();
    }
}

function setStatus(id: string, text: string): void {
    el(id).textContent = text;
}

function cell(row: HTMLTableRowElement, text: string, className?: string): HTMLTableCellElement {
    const td: HTMLTableCellElement = row.insertCell();
    td.textContent = text;
    if (className !== undefined) {
        td.className = className;
    }
    return td;
}

function closeText(row: HTMLTableRowElement): void {
    const next: Element | null = row.nextElementSibling;
    if (next !== null && next.classList.contains('snippet-text')) {
        next.remove();
    }
}

async function toggleText(row: HTMLTableRowElement, button: HTMLButtonElement, name: string): Promise<void> {
    if (button.getAttribute('aria-expanded') === 'true') {
        button.setAttribute('aria-expanded', 'false');
        closeText(row);
        return;
    }
    if (button.dataset.busy === 'true') {
        return;
    }
    button.dataset.busy = 'true';
    try {
        const response: Response = await fetch('/admin/solutions/' + encodeURIComponent(name));
        if (!response.ok) {
            setStatus('snippets-status', 'Cannot load "' + name + '": ' + response.status);
            return;
        }
        const text:    string              = await response.text();
        const textRow: HTMLTableRowElement = el('snippets').querySelector('tbody')!.insertRow(row.sectionRowIndex + 1);
        textRow.className = 'snippet-text';
        const td:  HTMLTableCellElement = textRow.insertCell();
        const pre: HTMLPreElement       = document.createElement('pre');
        td.colSpan = 6;
        pre.textContent = text;
        td.appendChild(pre);
        button.setAttribute('aria-expanded', 'true');
    } finally {
        button.dataset.busy = 'false';
    }
}

async function deleteOne(name: string): Promise<void> {
    if (!confirm('Delete "' + name + '"?')) {
        return;
    }
    const response: Response = await fetch('/admin/solutions/' + encodeURIComponent(name), { method: 'DELETE' });
    if (!response.ok && response.status !== 404) {
        setStatus('snippets-status', 'Cannot delete "' + name + '": ' + response.status);
        return;
    }
    loadSnippets();
}

function addRow(body: HTMLTableSectionElement, s: Snippet): void {
    const row:        HTMLTableRowElement  = body.insertRow();
    const nameCell:   HTMLTableCellElement = row.insertCell();
    const nameButton: HTMLButtonElement    = document.createElement('button');
    nameButton.type = 'button';
    nameButton.className = 'snippet-name';
    nameButton.textContent = s.name;
    nameButton.setAttribute('aria-expanded', 'false');
    nameButton.addEventListener('click', (): void => {
        toggleText(row, nameButton, s.name).catch((e: Error): void => setStatus('snippets-status', e.message));
    });
    nameCell.appendChild(nameButton);
    cell(row, formatTime(s.savedAt));
    cell(row, formatSize(s.bytes), 'num');
    cell(row, s.owner);
    cell(row, s.ip === null ? '-' : s.ip);
    const deleteButton: HTMLButtonElement = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.textContent = 'Delete';
    deleteButton.addEventListener('click', (): void => {
        deleteOne(s.name).catch((e: Error): void => setStatus('snippets-status', e.message));
    });
    row.insertCell().appendChild(deleteButton);
}

async function loadSnippets(): Promise<void> {
    try {
        const response: Response = await fetch('/admin/solutions');
        if (!response.ok) {
            setStatus('snippets-status', 'Cannot load the snippets: ' + response.status);
            return;
        }
        const list: Snippet[]               = await response.json();
        const body: HTMLTableSectionElement = el('snippets').querySelector('tbody')!;
        body.textContent = '';
        list.forEach((s: Snippet): void => addRow(body, s));
        setStatus('snippets-status', list.length === 0 ? 'No shared snippets' : '');
    } catch (e) {
        setStatus('snippets-status', (e as Error).message);
    }
}

async function deleteAll(): Promise<void> {
    const listing: Response = await fetch('/admin/solutions');
    if (!listing.ok) {
        setStatus('snippets-status', 'Cannot load the snippets: ' + listing.status);
        return;
    }
    const count: number = (await listing.json() as Snippet[]).length;
    if (!confirm('Delete all ' + count + ' shared snippets?')) {
        return;
    }
    const response: Response = await fetch('/admin/solutions', { method: 'DELETE' });
    if (!response.ok) {
        setStatus('snippets-status', 'Cannot delete: ' + response.status);
        return;
    }
    const deleted: number = (await response.json() as { deleted: number }).deleted;
    await loadSnippets();
    setStatus('snippets-status', 'Deleted ' + deleted + ' snippets.');
}

async function writeSharing(box: HTMLInputElement): Promise<void> {
    const wanted: boolean = box.checked;
    setStatus('sharing-status', '');
    try {
        const response: Response = await fetch('/admin/sharing', {
            method:  'PUT',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ enabled: wanted })
        });
        if (!response.ok) {
            box.checked = !wanted;
            setStatus('sharing-status', (await response.text()) || 'Error ' + response.status);
        }
    } catch (e) {
        box.checked = !wanted;
        setStatus('sharing-status', (e as Error).message);
    }
}

async function readSharing(box: HTMLInputElement): Promise<void> {
    try {
        const response: Response = await fetch('/admin/sharing');
        if (!response.ok) {
            setStatus('sharing-status', (await response.text()) || 'Error ' + response.status);
            return;
        }
        const state: { enabled: boolean } = await response.json();
        box.checked = state.enabled;
    } catch (e) {
        setStatus('sharing-status', (e as Error).message);
    }
}

export function start(): void {
    TABS.forEach((t: string): void => {
        el('tab-' + t).addEventListener('click', (): void => {
            location.hash = t;
        });
    });
    window.addEventListener('hashchange', (): void => showTab(currentTab()));
    const box: HTMLInputElement = el('sharing') as HTMLInputElement;
    box.addEventListener('change', (): void => {
        writeSharing(box);
    });
    readSharing(box);
    el('refresh').addEventListener('click', (): void => {
        loadSnippets();
    });
    el('delete-all').addEventListener('click', (): void => {
        deleteAll().catch((e: Error): void => setStatus('snippets-status', e.message));
    });
    showTab(currentTab());
}
