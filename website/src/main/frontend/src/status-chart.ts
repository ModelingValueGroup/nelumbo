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
    solutions:     { count: number; bytes: number; maxBytes: number; saves: number };
}

interface Point {
    t:           number;
    sessionsMax: number;
    runningMax:  number;
    cpuAvg:      number;
    cpuMax:      number;
    heapMaxMb:   number;
    evaluations:      number | null;
    overloads:        number | null;
    solutionsMax:     number;
    solutionBytesMax: number;
    saves:            number | null;
}

interface History {
    range:             string;
    resolutionSeconds: number;
    points:            Point[];
}

// one lane of the graph: its own scale and unit; every lane starts at 0
interface Lane {
    scale: string;
    title: string;
    top:   (dataMax: number) => number;
    label: (v: number) => string;
}

const HISTORY_REFRESH_MS: number = 60_000;
const POLL_MS:            number = 5_000;
const RECONNECT_MS:       number = 60_000;
const WINDOW_S:           Record<string, number> = { hour: 3600, day: 86400, week: 604800 };
const MB:                 number                 = 1024 * 1024;
const LANE_PX:            number                 = 90;
const X_AXIS_PX:          number                 = 50;
const Y_AXIS_PX:          number                 = 64;
// share of a lane's height above its data: room for the lane title
const TITLE:              number                 = 0.24;
const DAY_MS:             number                 = 86_400_000;
const RAD:                number                 = Math.PI / 180;
// Waardenburg; the sun is below the horizon at a zenith angle of 90.833 degrees (refraction and the sun's radius included)
const LATITUDE:           number                 = 51.83;
const LONGITUDE:          number                 = 5.26;
const ZENITH:             number                 = 90.833;
const SUNRISE:            number                 = 1;
const SUNSET:             number                 = -1;

const LANES: Lane[] = [
    { scale: 'cpu',       title: 'CPU (%)',                  top: (): number => 100,
      label: (v: number): string => round(v) + '%' },
    { scale: 'evals',     title: 'Evaluations/s',            top: (max: number): number => Math.max(0.1, max),
      label: (v: number): string => v.toFixed(2) + '/s' },
    { scale: 'activity',  title: 'Activity (count)',         top: (max: number): number => Math.max(4, max),
      label: (v: number): string => round(v) },
    { scale: 'solutions', title: 'Shared solutions (count)', top: (max: number): number => Math.max(4, max),
      label: (v: number): string => round(v) },
    { scale: 'disk',      title: 'On disk (MB)',             top: (): number => limitMb(),
      label: (v: number): string => round(v) + ' MB' },
];

let range:       string             = 'hour';
let resolution:  number             = 60;
let history:     Point[]            = [];
let latest:      Stats | null       = null;
let chart:       uPlot | null       = null;
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

// all times on the page use the 24-hour clock, whatever the browser locale
function clock(time: number): string {
    const d: Date = new Date(time);
    return two(d.getHours()) + ':' + two(d.getMinutes()) + ':' + two(d.getSeconds());
}

function hourMinute(time: number): string {
    const d: Date = new Date(time);
    return two(d.getHours()) + ':' + two(d.getMinutes());
}

// axis tick: the date at midnight, else the time - short enough for every range's tick spacing
function tick(time: number): string {
    const d: Date = new Date(time);
    if (d.getHours() === 0 && d.getMinutes() === 0) {
        return two(d.getDate()) + '-' + two(d.getMonth() + 1);
    }
    return hourMinute(time);
}

function dateTime(time: number): string {
    const d: Date = new Date(time);
    return d.getFullYear() + '-' + two(d.getMonth() + 1) + '-' + two(d.getDate()) + ' ' + hourMinute(time);
}

function showStats(s: Stats): void {
    latest = s;
    el('status').className = s.status;
    set('status', s.status);
    set('sessions', s.sessions.open + ' / ' + s.sessions.max);
    set('running', s.evaluations.running + ' (overloaded at ' + s.evaluations.threshold + ')');
    set('total', String(s.evaluations.total));
    set('overloaded', String(s.evaluations.overloaded));
    set('solutions', String(s.solutions.count));
    set('solution-disk', (s.solutions.bytes / MB).toFixed(1) + ' MB of ' + Math.round(s.solutions.maxBytes / MB) + ' MB');
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
                      cpuMax: latest.cpu.load, heapMaxMb: latest.heap.usedMb, evaluations: null, overloads: null,
                      solutionsMax: latest.solutions.count, solutionBytesMax: latest.solutions.bytes, saves: null });
    }
    return points;
}

// legend swatches follow the series line style, so the dashed CPU max is dashed there too
const LEGEND: uPlot.Legend = { markers: { dash: (u: uPlot, i: number): string => u.series[i].dash ? 'dashed' : 'solid' } };

function round(v: number): string {
    return String(Math.round(v * 100) / 100);
}

// the y axis scale runs from 0 (bottom) to LANES.length (top); lane i (0 = top) is the band [n - 1 - i, n - i],
// its data from the band's bottom up to TITLE below its top
function bandBottom(i: number): number {
    return (LANES.length - 1 - i) / LANES.length;
}

function bandTop(i: number): number {
    return (LANES.length - i - TITLE) / LANES.length;
}

// a lane's scale range that puts its data range [0, top] in its band of the plot height
function laneRange(i: number, top: number): [number, number] {
    const span: number = top / (bandTop(i) - bandBottom(i));
    const min:  number = -span * bandBottom(i);
    return [min, min + span];
}

// the smallest 1, 2 or 5 times a power of ten that gives at most three ticks from 0 to top (so at least two)
function tickStep(top: number): number {
    const third: number = top / 3 * (1 + 1e-9);
    const power: number = Math.pow(10, Math.floor(Math.log10(third)));
    const times: number = [1, 2, 5, 10].find((m: number): boolean => m * power > third) as number;
    return times * power;
}

// the y axis ticks of every lane, as positions on the y scale and labels in the lane's unit
function laneTicks(u: uPlot): { at: number[]; labels: string[] } {
    const at:     number[] = [];
    const labels: string[] = [];
    LANES.forEach((lane: Lane, i: number): void => {
        const scale: uPlot.Scale = u.scales[lane.scale];
        const min:   number      = scale.min as number;
        const span:  number      = (scale.max as number) - min;
        const top:   number      = min + span * bandTop(i);
        const step:  number      = tickStep(top);
        for (let k: number = 0; k * step <= top * (1 + 1e-9); k++) {
            at.push(LANES.length * (k * step - min) / span);
            labels.push(lane.label(k * step));
        }
    });
    return { at, labels };
}

// NOAA's General Solar Position Calculations, the fractional year taken at noon UTC: the sunrise or sunset of a day
// (days since 1970-01-01 UTC) in minutes after its midnight UTC
function sunMinutes(day: number, event: number): number {
    const year:   number = new Date(day * DAY_MS).getUTCFullYear();
    const first:  number = Date.UTC(year, 0, 1) / DAY_MS;
    const g:      number = 2 * Math.PI * (day - first) / (Date.UTC(year + 1, 0, 1) / DAY_MS - first);
    const eqtime: number = 229.18 * (0.000075 + 0.001868 * Math.cos(g) - 0.032077 * Math.sin(g) - 0.014615 * Math.cos(2 * g) - 0.040849 * Math.sin(2 * g));
    const decl:   number = 0.006918 - 0.399912 * Math.cos(g) + 0.070257 * Math.sin(g) - 0.006758 * Math.cos(2 * g) + 0.000907 * Math.sin(2 * g)
                           - 0.002697 * Math.cos(3 * g) + 0.00148 * Math.sin(3 * g);
    const lat:    number = LATITUDE * RAD;
    const ha:     number = Math.acos(Math.cos(ZENITH * RAD) / (Math.cos(lat) * Math.cos(decl)) - Math.tan(lat) * Math.tan(decl)) / RAD;
    return 720 - 4 * (LONGITUDE + event * ha) - eqtime;
}

function sun(day: number, event: number): number {
    return day * DAY_MS + sunMinutes(day, event) * 60_000;
}

// every night overlapping [fromMs, toMs], as [sunset, next sunrise] in ms
export function nights(fromMs: number, toMs: number): [number, number][] {
    const result: [number, number][] = [];
    for (let day: number = Math.floor(fromMs / DAY_MS) - 1; day * DAY_MS < toMs; day++) {
        const night: [number, number] = [sun(day, SUNSET), sun(day + 1, SUNRISE)];
        if (night[0] < toMs && night[1] > fromMs) {
            result.push(night);
        }
    }
    return result;
}

function drawNights(u: uPlot, fill: string): void {
    const min: number | null | undefined = u.scales.x.min;
    const max: number | null | undefined = u.scales.x.max;
    // the x scale has no range until the graph first gets data
    if (min == null || max == null) {
        return;
    }
    const box: uPlot.BBox = u.bbox;
    u.ctx.fillStyle = fill;
    for (const night of nights(min * 1000, max * 1000)) {
        const from: number = Math.max(box.left, u.valToPos(night[0] / 1000, 'x', true));
        const to:   number = Math.min(box.left + box.width, u.valToPos(night[1] / 1000, 'x', true));
        u.ctx.fillRect(from, box.top, to - from, box.height);
    }
}

// each lane's baseline (its 0, also the line between two lanes) and its title at its top left
function drawLanes(u: uPlot, line: string, ink: string): void {
    const box:   uPlot.BBox = u.bbox;
    const ratio: number     = uPlot.pxRatio;
    u.ctx.save();
    u.ctx.font         = 12 * ratio + 'px "Instrument Sans", system-ui, sans-serif';
    u.ctx.textAlign    = 'left';
    u.ctx.textBaseline = 'top';
    LANES.forEach((lane: Lane, i: number): void => {
        const top:    number = u.valToPos(LANES.length - i, 'y', true);
        const bottom: number = u.valToPos(LANES.length - 1 - i, 'y', true);
        u.ctx.fillStyle = line;
        u.ctx.fillRect(box.left, Math.round(bottom - ratio / 2), box.width, ratio);
        u.ctx.fillStyle = ink;
        u.ctx.fillText(lane.title, box.left + 6 * ratio, top + 4 * ratio);
    });
    u.ctx.restore();
}

function axes(): uPlot.Axis[] {
    const ink:  string = css('--muted');
    const grid: string = css('--grid');
    return [
        { stroke: ink, grid: { stroke: grid, width: 1 }, ticks: { stroke: grid, width: 1 },
          values: (_u: uPlot, vals: number[]): string[] => vals.map((s: number): string => tick(s * 1000)) },
        { scale: 'y', size: Y_AXIS_PX, stroke: ink, grid: { stroke: grid, width: 1 }, ticks: { stroke: grid, width: 1 },
          splits: (u: uPlot): number[] => laneTicks(u).at, values: (u: uPlot): string[] => laneTicks(u).labels },
    ];
}

function scales(): uPlot.Scales {
    const result: uPlot.Scales = { x: { time: true, range: xRange }, y: { range: [0, LANES.length] } };
    LANES.forEach((lane: Lane, i: number): void => {
        result[lane.scale] = { range: (_u: uPlot, _min: number, max: number): [number, number] => laneRange(i, lane.top(max)) };
    });
    return result;
}

// the axis always spans the selected range, so a lone point does not stretch it
function xRange(): [number, number] {
    const now: number = (latest !== null ? latest.time : Date.now()) / 1000;
    return [now - WINDOW_S[range], now];
}

function rate(p: Point): number | null {
    return p.evaluations === null ? null : p.evaluations / resolution;
}

function width(): number {
    return el('chart-stats').clientWidth;
}

function height(): number {
    return LANES.length * LANE_PX + X_AXIS_PX;
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
    const data: uPlot.AlignedData = [
        xs,
        val((p: Point): number | null => p.cpuAvg * 100), val((p: Point): number | null => p.cpuMax * 100),
        val(rate),
        val((p: Point): number | null => p.sessionsMax), val((p: Point): number | null => p.runningMax),
        val((p: Point): number | null => p.overloads), val((p: Point): number | null => p.saves),
        val((p: Point): number | null => p.solutionsMax),
        val((p: Point): number | null => p.solutionBytesMax / MB), val((): number | null => limitMb()),
    ];
    if (chart === null) {
        build(data);
        return;
    }
    chart.setData(data);
}

// the server's cap on the stored solutions, in MB; 100 until the first stats arrive
function limitMb(): number {
    return latest !== null ? latest.solutions.maxBytes / MB : 100;
}

// one uPlot: one time axis, cursor and legend over five lanes, each with its own scale and unit
function build(data: uPlot.AlignedData): void {
    destroyChart();
    el('chart-stats').textContent = '';
    const cpu:      string = css('--series-cpu');
    const solution: string = css('--series-4');
    const night:    string = css('--night');
    const line:     string = css('--faint');
    const ink:      string = css('--muted');
    chart = new uPlot({
        width:  width(),
        height: height(),
        scales: scales(),
        legend: LEGEND,
        // a horizontal crosshair would read as a value in one lane only
        cursor: { y: false },
        axes:   axes(),
        hooks:  { drawClear: [(u: uPlot): void => drawNights(u, night)], drawAxes: [(u: uPlot): void => drawLanes(u, line, ink)] },
        series: [
            { value: (_u: uPlot, v: number | null): string => v === null ? '--' : dateTime(v * 1000) },
            { label: 'CPU avg', scale: 'cpu', stroke: cpu, width: 2 },
            { label: 'CPU max', scale: 'cpu', stroke: cpu, width: 1, dash: [4, 4] },
            { label: 'Evaluations/s', scale: 'evals', stroke: css('--series-2'), width: 2,
              value: (_u: uPlot, v: number | null): string => v === null ? '--' : v.toFixed(2) },
            { label: 'Sessions', scale: 'activity', stroke: css('--series-1'), width: 2 },
            { label: 'Evaluations running', scale: 'activity', stroke: css('--series-2'), width: 2 },
            { label: 'Stopped (busy)', scale: 'activity', stroke: css('--series-3'), width: 2 },
            { label: 'Shares', scale: 'activity', stroke: solution, width: 2 },
            { label: 'Shared solutions', scale: 'solutions', stroke: solution, width: 2 },
            { label: 'On disk', scale: 'disk', stroke: solution, width: 2,
              value: (_u: uPlot, v: number | null): string => v === null ? '--' : v.toFixed(1) + ' MB' },
            { label: 'Limit', scale: 'disk', stroke: ink, width: 1, dash: [4, 4],
              value: (_u: uPlot, v: number | null): string => v === null ? '--' : v + ' MB' },
        ],
    }, data, el('chart-stats'));
}

function destroyChart(): void {
    if (chart !== null) {
        chart.destroy();
        chart = null;
    }
}

function fixed(v: number | null): string {
    return v === null ? '-' : v.toFixed(2);
}

function fillTable(): void {
    const body: HTMLElement = el('history-table').querySelector('tbody') as HTMLElement;
    const rows: string[]    = history.slice().reverse().map((p: Point): string =>
        '<tr><td>' + dateTime(p.t) + '</td><td>' + Math.round(p.cpuAvg * 100) + '%</td><td>' + Math.round(p.cpuMax * 100)
        + '%</td><td>' + p.sessionsMax + '</td><td>' + p.runningMax + '</td><td>' + p.overloads + '</td><td>' + p.evaluations
        + '</td><td>' + fixed(rate(p)) + '</td><td>' + p.saves + '</td><td>' + p.solutionsMax + '</td><td>' + (p.solutionBytesMax / MB).toFixed(1) + '</td></tr>');
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
    // colours are read from the theme's CSS variables, so a theme switch rebuilds the graph
    new MutationObserver((): void => {
        destroyChart();
        draw();
    }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    window.addEventListener('resize', (): void => {
        if (chart !== null) {
            chart.setSize({ width: width(), height: height() });
        }
    });
    poll();
    connectStream();
    selectRange('hour');
    window.setInterval(loadHistory, HISTORY_REFRESH_MS);
}
