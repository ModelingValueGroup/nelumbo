import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';

interface Stats {
    status:        string;
    time:          number;
    sessions:      { open: number; max: number };
    evaluations:   { running: number; threshold: number; total: number; overloaded: number };
    cpu:           { load: number; cpus: number };
    heap:          { usedMb: number; maxMb: number };
    uptimeSeconds: number;
}

interface Point {
    t:           number;
    sessionsMax: number;
    runningMax:  number;
    cpuAvg:      number;
    cpuMax:      number;
    heapMaxMb:   number;
    evaluations: number;
    overloads:   number | null;
}

interface History {
    range:             string;
    resolutionSeconds: number;
    points:            Point[];
}

const HISTORY_REFRESH_MS: number = 60_000;
const POLL_MS:            number = 5_000;
const RECONNECT_MS:       number = 60_000;
const CHART_HEIGHT:       number = 200;
const WINDOW_S:           Record<string, number> = { hour: 3600, day: 86400, week: 604800 };

let range:       string             = 'hour';
let resolution:  number             = 60;
let history:     Point[]            = [];
let latest:      Stats | null       = null;
let cpuChart:    uPlot | null       = null;
let actChart:    uPlot | null       = null;
let pollTimer:   number | undefined = undefined;

function el(id: string): HTMLElement {
    return document.getElementById(id) as HTMLElement;
}

function set(id: string, text: string): void {
    el(id).textContent = text;
}

function css(name: string): string {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function two(n: number): string {
    return n < 10 ? '0' + n : String(n);
}

function clock(time: number): string {
    const d: Date = new Date(time);
    return two(d.getHours()) + ':' + two(d.getMinutes()) + ':' + two(d.getSeconds());
}

function showStats(s: Stats): void {
    latest = s;
    el('status').className = s.status;
    set('status', s.status);
    set('sessions', s.sessions.open + ' / ' + s.sessions.max);
    set('running', s.evaluations.running + ' (overloaded at ' + s.evaluations.threshold + ')');
    set('total', String(s.evaluations.total));
    set('overloaded', String(s.evaluations.overloaded));
    set('cpu', Math.round(s.cpu.load * 100) + '% of ' + s.cpu.cpus + ' cores');
    set('heap', s.heap.usedMb + ' / ' + s.heap.maxMb + ' MB');
    set('uptime', Math.floor(s.uptimeSeconds / 3600) + 'h ' + Math.floor(s.uptimeSeconds % 3600 / 60) + 'm');
    set('updated', clock(s.time));
    if (range === 'hour') {
        draw();
    }
}

function showUnreachable(): void {
    el('status').className = '';
    set('status', 'unreachable');
    for (const id of ['sessions', 'running', 'total', 'overloaded', 'cpu', 'heap', 'uptime']) {
        set(id, '-');
    }
}

function poll(): void {
    fetch('/stats').then((r: Response): Promise<Stats> => r.json()).then(showStats).catch(showUnreachable);
}

// the stream is the normal path; polling only covers the time the stream is down (proxy timeout, client cap)
function connectStream(): void {
    const source: EventSource = new EventSource('/stats/stream');
    source.addEventListener('stats', (e: MessageEvent): void => {
        if (pollTimer !== undefined) {
            window.clearInterval(pollTimer);
            pollTimer = undefined;
        }
        showStats(JSON.parse(e.data) as Stats);
    });
    source.addEventListener('error', (e: Event): void => {
        startFallback();
        // the server's own error event (client cap) carries data and is followed by a close; do not let EventSource retry every 3 s
        if (e instanceof MessageEvent) {
            source.close();
            window.setTimeout(connectStream, RECONNECT_MS);
        }
    });
}

function startFallback(): void {
    if (pollTimer === undefined) {
        poll();
        pollTimer = window.setInterval(poll, POLL_MS);
    }
}

function loadHistory(): void {
    fetch('/stats/history?range=' + range).then((r: Response): Promise<History> => r.json()).then((h: History): void => {
        if (h.range !== range) {
            return;
        }
        history    = h.points;
        resolution = h.resolutionSeconds;
        draw();
        fillTable();
    }).catch((): void => {
        history = [];
        draw();
    });
}

// a null point where consecutive points are further apart than 1.5 resolutions, so downtime shows as a gap
function series(): (Point | null)[] {
    const points: (Point | null)[] = [];
    let   prev:   Point | null      = null;
    for (const p of history) {
        if (prev !== null && p.t - prev.t > resolution * 1500) {
            points.push(null);
        }
        points.push(p);
        prev = p;
    }
    // in the hour view the latest live sample extends the lines to now
    if (range === 'hour' && latest !== null) {
        points.push({ t: latest.time, sessionsMax: latest.sessions.open, runningMax: latest.evaluations.running, cpuAvg: latest.cpu.load,
                      cpuMax: latest.cpu.load, heapMaxMb: latest.heap.usedMb, evaluations: 0, overloads: null });
    }
    return points;
}

function axes(unit: (v: number) => string): uPlot.Axis[] {
    const ink:  string = css('--muted');
    const grid: string = css('--grid');
    return [
        { stroke: ink, grid: { stroke: grid, width: 1 }, ticks: { stroke: grid, width: 1 } },
        { stroke: ink, grid: { stroke: grid, width: 1 }, ticks: { stroke: grid, width: 1 }, values: (_u: uPlot, vals: number[]): string[] => vals.map(unit) },
    ];
}

// the axis always spans the selected range, so a lone point does not stretch it
function xRange(): [number, number] {
    const now: number = (latest !== null ? latest.time : Date.now()) / 1000;
    return [now - WINDOW_S[range], now];
}

function width(id: string): number {
    return el(id).clientWidth;
}

function draw(): void {
    const rows:    (Point | null)[]  = series();
    const xs:      number[]          = [];
    let   before:  number            = 0;
    for (let i: number = 0; i < rows.length; i++) {
        const row: Point | null = rows[i];
        if (row !== null) {
            before = row.t;
            xs.push(row.t / 1000);
        } else {
            // the gap point sits just after the last real point; only its null values matter
            xs.push((before + 1) / 1000);
        }
    }
    const val: (f: (p: Point) => number | null) => (number | null)[] = (f: (p: Point) => number | null): (number | null)[] =>
        rows.map((p: Point | null): number | null => p === null ? null : f(p));
    const cpuData: uPlot.AlignedData = [xs, val((p: Point): number | null => p.cpuAvg * 100), val((p: Point): number | null => p.cpuMax * 100)];
    const actData: uPlot.AlignedData = [xs, val((p: Point): number | null => p.sessionsMax), val((p: Point): number | null => p.runningMax),
                                        val((p: Point): number | null => p.overloads)];
    if (cpuChart === null || actChart === null) {
        build(cpuData, actData);
        return;
    }
    cpuChart.setData(cpuData);
    actChart.setData(actData);
}

function build(cpuData: uPlot.AlignedData, actData: uPlot.AlignedData): void {
    cpuChart?.destroy();
    actChart?.destroy();
    el('chart-cpu').textContent      = '';
    el('chart-activity').textContent = '';
    const cpu: string = css('--series-cpu');
    cpuChart = new uPlot({
        width:  width('chart-cpu'),
        height: CHART_HEIGHT,
        scales: { x: { time: true, range: xRange }, y: { range: [0, 100] } },
        axes:   axes((v: number): string => v + '%'),
        series: [
            {},
            { label: 'CPU avg', stroke: cpu, width: 2 },
            { label: 'CPU max', stroke: cpu, width: 1, dash: [4, 4] },
        ],
    }, cpuData, el('chart-cpu'));
    actChart = new uPlot({
        width:  width('chart-activity'),
        height: CHART_HEIGHT,
        scales: { x: { time: true, range: xRange }, y: { range: (_u: uPlot, _min: number, max: number): [number, number] => [0, Math.max(4, max)] } },
        axes:   axes((v: number): string => String(v)),
        series: [
            {},
            { label: 'Sessions', stroke: css('--series-1'), width: 2 },
            { label: 'Evaluations running', stroke: css('--series-2'), width: 2 },
            { label: 'Stopped (busy)', stroke: css('--series-3'), width: 2 },
        ],
    }, actData, el('chart-activity'));
}

function fillTable(): void {
    const body: HTMLElement = el('history-table').querySelector('tbody') as HTMLElement;
    const rows: string[]    = history.slice().reverse().map((p: Point): string =>
        '<tr><td>' + new Date(p.t).toLocaleString() + '</td><td>' + Math.round(p.cpuAvg * 100) + '%</td><td>' + Math.round(p.cpuMax * 100)
        + '%</td><td>' + p.sessionsMax + '</td><td>' + p.runningMax + '</td><td>' + p.overloads + '</td><td>' + p.evaluations + '</td></tr>');
    body.innerHTML = rows.join('');
}

function selectRange(next: string): void {
    range = next;
    for (const b of Array.from(document.querySelectorAll<HTMLButtonElement>('button[data-range]'))) {
        b.setAttribute('aria-pressed', String(b.dataset.range === next));
    }
    loadHistory();
}

export function start(): void {
    for (const b of Array.from(document.querySelectorAll<HTMLButtonElement>('button[data-range]'))) {
        b.addEventListener('click', (): void => selectRange(b.dataset.range as string));
    }
    // colours are read from the theme's CSS variables, so a theme switch rebuilds the charts
    new MutationObserver((): void => {
        cpuChart?.destroy();
        actChart?.destroy();
        cpuChart = null;
        actChart = null;
        draw();
    }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    window.addEventListener('resize', (): void => {
        cpuChart?.setSize({ width: width('chart-cpu'), height: CHART_HEIGHT });
        actChart?.setSize({ width: width('chart-activity'), height: CHART_HEIGHT });
    });
    poll();
    connectStream();
    selectRange('hour');
    window.setInterval(loadHistory, HISTORY_REFRESH_MS);
}
