# Nelumbo Feature Tour Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the thin demo page with a sidebar-navigated feature tour at `/` that explains each major Nelumbo capability and gives editable demo fields plus self-checking exercises; move the free-form playground to `/playground.html`.

**Architecture:** Content lives in a single authored `tour.html` (sidebar + one-section-at-a-time content pane, driven by a small inline script). The existing Monaco/LSP frontend bundle gains a `connect()` / `mountFields(container)` split so sections mount their editors lazily, plus a `.nelumbo-solution` "Show solution" reveal. Exercises self-check via Nelumbo's expected-result query notation (`... ? [(r=120)][..]`) and the LSP mismatch squiggle; no grading code.

**Tech Stack:** TypeScript + monaco-editor + monaco-languageclient (esbuild IIFE), Java 21 / Javalin, Gradle. Static HTML/CSS/JS for the tour shell.

**Spec:** `docs/superpowers/specs/2026-07-11-nelumbo-feature-tour-design.md`

**Conventions for every task:** run from repo root `/Users/tom/projects/mvg-nelumbo/nelumbo`. New Java carries the LGPL header. ASCII only. Field content is HTML: escape Nelumbo operators inside `.nelumbo-field`/`.nelumbo-solution` (`<`->`&lt;`, `>`->`&gt;`, `&`->`&amp;`). The frontend build needs Node; `./gradlew :http:serverJar` runs `npm run dist` automatically.

## File structure

- `http/src/main/frontend/src/nelumbo-fields.ts` (modify) - split into `connect()` + `mountFields()`, keep `initNelumboFields()`; add solution reveal.
- `http/src/main/frontend/src/fields.css` (modify) - add `.nelumbo-exercise`, `.challenge`, `.nelumbo-solution`, secondary-button styles.
- `http/src/main/resources/public/tour.html` (create) - the tour shell + 8 sections + nav script.
- `http/src/main/resources/public/playground.html` (keep) - now served at `/playground.html`.
- `http/src/main/resources/public/demo.html` (delete).
- `http/src/main/java/org/modelingvalue/nelumbo/http/NelumboHttpServer.java` (modify) - route `/` to tour, add `/playground.html`, drop `/demo.html`.
- `http/src/test/java/org/modelingvalue/nelumbo/http/NelumboHttpServerTest.java` (modify) - update page-serving tests.
- `CLAUDE.md` (modify) - note the tour.

---

### Task 1: Frontend component - connect/mountFields split + solution reveal

**Files:**
- Modify: `http/src/main/frontend/src/nelumbo-fields.ts`
- Modify: `http/src/main/frontend/src/fields.css`

- [ ] **Step 1: Replace the tail of nelumbo-fields.ts**

Replace everything from `function buildField(` to end of file with:

```ts
let servicesReady: boolean                              = false;
let clientPromise:  Promise<MonacoLanguageClient | null> | null = null;
let fieldIndex:     number                              = 0;

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
```

- [ ] **Step 2: Add component styles to fields.css**

Append to `http/src/main/frontend/src/fields.css`:

```css
.nelumbo-exercise { border-left: 3px solid #6ea8fe; padding-left: 12px; margin: 16px 0; }
.nelumbo-exercise .challenge { color: #c6cad4; margin: 0 0 6px; }
.nelumbo-solution { display: none; margin: 8px 0 0; padding: 10px 12px; background: #1b1d23; border: 1px solid #343843; border-radius: 8px; font: 12px/1.5 ui-monospace, Menlo, Consolas, monospace; color: #e6e8ee; white-space: pre-wrap; }
.nelumbo-solution.visible { display: block; }
.nelumbo-field-toolbar button.secondary { background: transparent; color: #9aa0ac; border: 1px solid #343843; }
.nelumbo-field-toolbar button.secondary:hover { color: #e6e8ee; }
```

- [ ] **Step 3: Typecheck**

Run: `cd http/src/main/frontend && npm run check`
Expected: exit 0, no tsc errors.

- [ ] **Step 4: Build the bundle**

Run: `cd http/src/main/frontend && npm run build && ls dist/`
Expected: `nelumbo-fields.js` and `nelumbo-fields.css` present; exit 0.

- [ ] **Step 5: Commit**

```bash
git add http/src/main/frontend/src/nelumbo-fields.ts http/src/main/frontend/src/fields.css
git commit -m "feat(http): split field component into connect/mountFields + solution reveal"
```

---

### Task 2: Routing + tour shell + Propositional-logic section + tests

**Files:**
- Create: `http/src/main/resources/public/tour.html`
- Delete: `http/src/main/resources/public/demo.html`
- Modify: `http/src/main/java/org/modelingvalue/nelumbo/http/NelumboHttpServer.java`
- Modify: `http/src/test/java/org/modelingvalue/nelumbo/http/NelumboHttpServerTest.java`

- [ ] **Step 1: Create tour.html shell with the first section**

Create `http/src/main/resources/public/tour.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Nelumbo - Feature Tour</title>
<style>
  * { box-sizing: border-box; }
  html, body { height: 100%; margin: 0; }
  body { background: #1b1d23; color: #e6e8ee; font: 15px/1.6 system-ui, -apple-system, Segoe UI, Roboto, sans-serif; display: flex; }
  aside { width: 210px; flex: 0 0 210px; border-right: 1px solid #343843; padding: 16px 0; overflow: auto; }
  aside h1 { font-size: 15px; margin: 0 16px 14px; }
  aside nav a { display: block; padding: 6px 16px; color: #9aa0ac; text-decoration: none; font-size: 13px; cursor: pointer; }
  aside nav a:hover { color: #e6e8ee; }
  aside nav a.active { color: #6ea8fe; border-left: 2px solid #6ea8fe; padding-left: 14px; }
  aside .sep { border-top: 1px solid #343843; margin: 12px 0; }
  main { flex: 1; overflow: auto; }
  .tour-section { display: none; max-width: 860px; margin: 0 auto; padding: 24px 24px 80px; }
  .tour-section.active { display: block; }
  h2 { font-size: 20px; margin-top: 8px; }
  p { color: #c6cad4; }
  code { background: #23262e; border-radius: 4px; padding: 1px 5px; font: 13px ui-monospace, Menlo, Consolas, monospace; }
</style>
</head>
<body>
<aside>
  <h1>Nelumbo</h1>
  <nav>
    <a data-section="logic" class="active">Propositional logic</a>
    <a data-section="facts">Facts &amp; queries</a>
    <a data-section="rules">Rules &amp; recursion</a>
    <a data-section="types">Data types</a>
    <a data-section="relations">Relational KB</a>
    <a data-section="dsl">Custom DSL</a>
    <a data-section="transform">Transformations</a>
    <a data-section="scoping">Scoping</a>
    <div class="sep"></div>
    <a href="/playground.html">Playground -&gt;</a>
  </nav>
</aside>
<main>
  <section class="tour-section active" id="logic">
    <h2>Propositional logic</h2>
    <p>Nelumbo is three-valued: a query is <code>true</code>, <code>false</code>, or
       <code>unknown</code>. The result notation <code>[facts][falsehoods]</code> shows the two
       sides; <code>[()][]</code> means "true", <code>[][()]</code> means "false", and
       <code>[..][..]</code> means "unknown". Edit and press Run (or Cmd/Ctrl+Enter).</p>
    <div class="nelumbo-field" data-height="200px">import nelumbo.logic

true &amp; true ?
true | false ?
!false ?
unknown &amp; true ?
</div>
    <div class="nelumbo-exercise">
      <p class="challenge">Make the query below come out <strong>true</strong> by changing only the
         operator (the expected-result annotation <code>[()][]</code> means the editor will underline
         a mismatch until it is true).</p>
      <div class="nelumbo-field" data-height="90px">import nelumbo.logic

false | true ? [()][]
</div>
      <pre class="nelumbo-solution">import nelumbo.logic

false | true ? [()][]
</pre>
    </div>
  </section>
</main>
<link rel="stylesheet" href="/assets/nelumbo-fields.css">
<script src="/assets/nelumbo-fields.js"></script>
<script>
  (function () {
    var sections = document.querySelectorAll('.tour-section');
    var links = document.querySelectorAll('aside nav a[data-section]');
    function show(id) {
      var found = document.getElementById(id);
      if (!found) { return; }
      sections.forEach(function (s) { s.classList.toggle('active', s.id === id); });
      links.forEach(function (a) { a.classList.toggle('active', a.getAttribute('data-section') === id); });
      NelumboFields.mountFields(found);
      if (location.hash !== '#' + id) { history.replaceState(null, '', '#' + id); }
    }
    links.forEach(function (a) {
      a.addEventListener('click', function (e) { e.preventDefault(); show(a.getAttribute('data-section')); });
    });
    NelumboFields.connect();
    var initial = (location.hash || '').replace('#', '');
    show(document.getElementById(initial) ? initial : sections[0].id);
  })();
</script>
</body>
</html>
```

- [ ] **Step 2: Delete demo.html**

Run: `git rm http/src/main/resources/public/demo.html`

- [ ] **Step 3: Update the server routes**

In `NelumboHttpServer.java` `start(int port)`, replace the resource loads and the `/` and `/demo.html` routes.

Change:
```java
        String playground = loadResource("/public/playground.html");
        String demo       = loadResource("/public/demo.html");
```
to:
```java
        String tour       = loadResource("/public/tour.html");
        String playground = loadResource("/public/playground.html");
```

Change:
```java
        app.get("/", ctx -> ctx.html(playground));
        app.get("/demo.html", ctx -> ctx.html(demo));
```
to:
```java
        app.get("/", ctx -> ctx.html(tour));
        app.get("/playground.html", ctx -> ctx.html(playground));
```

- [ ] **Step 4: Update the page-serving tests**

In `NelumboHttpServerTest.java`, replace the `playgroundIsServedAtRoot` and `demoPageIsServed` tests with:

```java
    @Test
    void tourIsServedAtRoot() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"),
                "tour should be served as HTML");
        String html = response.body();
        assertTrue(html.contains("data-section=\"logic\""), "tour should render the sidebar navigation");
        assertTrue(html.contains("nelumbo-field"), "tour should mount Nelumbo editor fields");
        assertTrue(html.contains("/assets/nelumbo-fields.js"), "tour should load the frontend bundle");
    }

    @Test
    void playgroundIsServedAtItsPath() throws Exception {
        HttpResponse<String> response = get("/playground.html");
        assertEquals(200, response.statusCode());
        String html = response.body();
        assertTrue(html.contains("nelumbo-field"), "playground should mount a Nelumbo editor field");
        assertTrue(html.contains("initNelumboFields"), "playground should initialize the editor fields");
    }
```

(Leave `frontendBundleIsServed` and every other test unchanged.)

- [ ] **Step 5: Verify the CLI-checkable snippets in this section**

Build the CLI and check each `.nelumbo-field`/`.nelumbo-solution` snippet in tour.html, unescaping entities (`&amp;`->`&`, `&lt;`->`<`, `&gt;`->`>`). The demo field and both exercise snippets must evaluate:

Run: `./gradlew cliJar`
Then for the logic demo (save unescaped to /tmp/s.nl):
```
import nelumbo.logic

true & true ?
true | false ?
!false ?
unknown & true ?
```
Run: `java -jar build/libs/nelumbo-*-cli.jar /tmp/s.nl` -> exit 0.
For the exercise solution (`false | true ? [()][]`): exit 0 (it is already the solved form).

- [ ] **Step 6: Build and run the http tests**

Run: `./gradlew :http:test`
Expected: PASS (tourIsServedAtRoot, playgroundIsServedAtItsPath, frontendBundleIsServed, LspWebSocketTest, and the rest).

- [ ] **Step 7: Smoke-serve**

Run: `./gradlew :http:serverJar && java -jar http/build/libs/nelumbo-http-server-*.jar --port 0`
Grep the log for the port, then `curl -s localhost:PORT/ | grep -c data-section` (>0), `curl -s -o /dev/null -w "%{http_code}" localhost:PORT/playground.html` (200), `curl -s -o /dev/null -w "%{http_code}" localhost:PORT/demo.html` (404). Kill the server.

- [ ] **Step 8: Commit**

```bash
git add http/src/main/resources/public http/src/main/java/org/modelingvalue/nelumbo/http/NelumboHttpServer.java http/src/test/java/org/modelingvalue/nelumbo/http/NelumboHttpServerTest.java
git commit -m "feat(http): serve feature tour at / and playground at /playground.html"
```

---

### Content-task conventions (Tasks 3-5)

Each content task adds `<section class="tour-section" id="...">` blocks to `tour.html` (before the closing `</main>`), in sidebar order. Follow the exact markup contract:

```html
<section class="tour-section" id="ID">
  <h2>TITLE</h2>
  <p>PROSE (may use &lt;code&gt; and escaped operators)</p>
  <div class="nelumbo-field" data-height="200px">DEMO SNIPPET (operators HTML-escaped)</div>
  <div class="nelumbo-exercise">
    <p class="challenge">CHALLENGE TEXT</p>
    <div class="nelumbo-field" data-height="150px">SCAFFOLD SNIPPET (parses cleanly, query has expected annotation, mismatches until solved)</div>
    <pre class="nelumbo-solution">FULL SOLUTION SNIPPET</pre>
  </div>
</section>
```

**Mandatory verification for every snippet in the task** (do NOT commit until all pass):
1. `./gradlew cliJar` once.
2. For each demo and each `.nelumbo-solution`: unescape entities, write to a temp `.nl`, run `java -jar build/libs/nelumbo-*-cli.jar /tmp/x.nl`; require exit 0.
3. For each exercise **scaffold**: it must parse (no `file:line:col` parse error) but its checked query must NOT already be satisfied - i.e. the CLI reports a query mismatch (so the browser shows the self-check squiggle). If the scaffold instead throws a hard eval error, adjust it to parse cleanly (declare functors/vars, leave only the rule missing). Confirm the corresponding solution turns it green.
4. Sidebar links for these ids already exist in Task 2's shell - no nav edits needed.

Source known-good syntax from `src/main/resources/org/modelingvalue/nelumbo/examples/*.nl` and `tests/*.nl`; adjust snippets that fail rather than inventing syntax.

---

### Task 3: Sections "Facts & queries" and "Rules & recursion"

**Files:**
- Modify: `http/src/main/resources/public/tour.html`

- [ ] **Step 1: Add the `facts` section**

Insert after the `logic` section. Prose: facts are asserted with `fact`; objects/types are declared (`Person :: Object`), and queries bind variables. Demo (verified against `examples/friends.nl`):

```
import nelumbo.logic
Person :: Object
FactType ::= friends(<Person>,<Person>)
Person ::= Piet, Jan, Klaas
fact friends(Piet, Jan),
     friends(Jan,  Klaas)
Person X
friends(Piet, X) ?
```

Exercise - challenge: "Add a fact so that `friends(Klaas, Kees)` holds, then confirm the query is true." Scaffold (query has expected `[()][]`, fails until the fact is added):

```
import nelumbo.logic
Person :: Object
FactType ::= friends(<Person>,<Person>)
Person ::= Piet, Jan, Klaas, Kees
// add a fact here
friends(Klaas, Kees) ? [()][]
```

Solution: same with `fact friends(Klaas, Kees)` added before the query.

- [ ] **Step 2: Add the `rules` section**

Prose: `<=>` defines a rule; `if` adds guards; rules may recurse. Demo (verified fib, the same one used in the LSP tests):

```
import nelumbo.integers
Integer ::= fib(<Integer>)
Integer n, f
fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1
Integer r
fib(7)=r ?
```

Exercise - challenge: "Define `factorial(n)` so that `factorial(5)=120`." Scaffold (parses; factorial declared but undefined, so the checked query mismatches):

```
import nelumbo.integers
Integer ::= factorial(&lt;Integer&gt;)
Integer n, r
// define factorial(n)=r here (hint: 1 if n&lt;=0, else n*factorial(n-1))
factorial(5)=r ? [(r=120)][..]
```

Solution:

```
import nelumbo.integers
Integer ::= factorial(<Integer>)
Integer n, r
factorial(n)=r <=> r=1 if n<=0, r=n*factorial(n-1) if n>0
factorial(5)=r ? [(r=120)][..]
```

- [ ] **Step 3: Verify every snippet per the Content-task conventions**

Run `./gradlew cliJar` and check all four demo/solution snippets (exit 0) and both scaffolds (parse; query mismatches until solved). If the `factorial` scaffold's undefined functor causes a hard error rather than a clean mismatch, give it a deliberately-wrong stub rule (e.g. `factorial(n)=r <=> r=n`) so it parses and mismatches cleanly.

- [ ] **Step 4: Build and smoke**

Run: `./gradlew :http:serverJar` then serve argless and `curl -s localhost:PORT/ | grep -c 'id="rules"'` (>0). Kill server.

- [ ] **Step 5: Commit**

```bash
git add http/src/main/resources/public/tour.html
git commit -m "feat(http): tour sections for facts/queries and rules/recursion"
```

---

### Task 4: Sections "Data types" and "Relational KB"

**Files:**
- Modify: `http/src/main/resources/public/tour.html`

- [ ] **Step 1: Add the `types` section**

Prose: Nelumbo ships built-in types - integers, rationals, strings, collections, datetime - each importable. Demo (each line verified against the matching `tests/*Test.nl`):

```
import nelumbo.integers
import nelumbo.strings
Integer i
String  s
2+3=i ?
"foo"+"bar"=s ?
len("hello")=i ?
```

Exercise - challenge: "Bind `a` to the concatenation, and `d` to its length." Scaffold:

```
import nelumbo.strings
String  a
Integer d
"lo"+"tus"=a ? [(a="lotus")][..]
len("lotus")=d ? [(d=5)][..]
```

(This scaffold is already satisfiable by the library, so it doubles as a "read the result" exercise; the solution is identical. If you prefer a fill-in exercise, make the scaffold `"lo"+? =a` style only if it parses - otherwise keep this read-only form.) Solution: identical snippet.

Optionally add a second, collections demo (verified against `collectionsTest.nl`):

```
import nelumbo.collections
Set<Integer> s
Integer i
s={1,2,3} ?
|{1,2,3}|=i ?
```

- [ ] **Step 2: Add the `relations` section**

Prose: a knowledge base is facts + rules over relations; rules and quantifiers (`E[x]`, `A[y]`) derive new relations. Demo (adapted from `examples/family.nl`, trimmed to parent/child/mother):

```
import nelumbo.logic
Person   :: Object
Male     :: Person
Female   :: Person
FactType ::= pc(<Person>,<Person>)
Person   ::= p(<Person>), c(<Person>), m(<Person>)
Person a, b
Female x
c(a)=b <=> pc(a,b)
p(a)=b <=> pc(b,a)
m(a)=b <=> E[x](c(x)=a &amp; b=x)
Male   ::= Hendrik
Female ::= Juliana, Beatrix
fact pc(Juliana, Beatrix)
Person who
m(Beatrix)=who ?
```

Exercise - challenge: "Add a `grandparent` relation `gp` and confirm `gp(Beatrix)=Juliana`'s parent." Provide a scaffold declaring `gp` with the query `gp(...)= ? [(...)][..]` mismatching until the rule `gp(a)=b <=> p(p(a))=b` is added; the solution adds that rule. Verify against the CLI; if the trimmed family KB does not produce a clean grandparent chain, simplify the challenge to "add `f(a)` (father) using `E[y]` like `m`" mirroring `examples/family.nl`, whichever verifies.

- [ ] **Step 3: Verify every snippet per the Content-task conventions**

`./gradlew cliJar`; all demos + solutions exit 0; scaffolds parse and mismatch until solved. Adjust snippets that fail using `examples/family.nl` and the `tests/*Test.nl` files as the source of truth.

- [ ] **Step 4: Commit**

```bash
git add http/src/main/resources/public/tour.html
git commit -m "feat(http): tour sections for data types and relational knowledge bases"
```

---

### Task 5: Sections "Custom DSL", "Transformations", "Scoping"

**Files:**
- Modify: `http/src/main/resources/public/tour.html`

- [ ] **Step 1: Add the `dsl` section**

Prose: Nelumbo is a meta-language - you can declare your own functors/patterns so that natural-language statements and queries become first-class syntax. Demo (adapted from `examples/friends.nl` + `whoIs.nl`, self-contained):

```
import nelumbo.logic
Person   :: Object
FactType ::= friends(<Person>,<Person>)
Person   ::= friend(<Person>)
Person   ::= Piet, Jan, Klaas
Person A, B, Who
friend(A)=B &lt;=&gt; friends(A,B) | friends(B,A) | friend(friend(A))=B
Boolean ::= &lt;Person&gt; is a friend of &lt;Person&gt; #30
Person X, Y
X is a friend of Y &lt;=&gt; friend(Y)=X
fact friends(Piet, Jan), friends(Jan, Klaas)
Who is a friend of Piet ?
```

Exercise - challenge: "Add the phrase `<Person> knows <Person>` as a synonym for `is a friend of`." Scaffold declares the query line with an expected annotation and leaves the phrase functor + rule to add; solution adds `Boolean ::= <Person> knows <Person> #30` and `X knows Y <=> friend(Y)=X`. Verify via CLI (precedence `#30` matters - see the `whoIs.nl` example and CLAUDE.md's precedence note).

- [ ] **Step 2: Add the `transform` section**

Prose: transformation rules (`::>`) rewrite one construct into others - e.g. an attribute declaration expands into a type, a setter, a fact relation, and a getter rule. Demo (adapted and trimmed from `examples/transformation.nl`); verify it evaluates:

```
import nelumbo.strings
Root ::= attr &lt;Type&gt; &lt;NAME&gt; &lt;Type&gt; #100
Type OT, AT
NAME AN
attr OT AN AT ::&gt; {
    AT   ::= &lt;OT&gt;.AN
    Root ::= &lt;OT&gt;.AN := &lt;AT&gt;
    private FactType ::= AN(&lt;OT&gt;,&lt;AT&gt;)
    OT o
    AT a
    o.AN=a &lt;=&gt; AN(o,a)
    o.AN := a ::&gt; { fact AN(o,a) }
}
Person :: Object
attr Person name String
Person ::= Piet
Piet.name := "Piet"
String n
Piet.name=n ?
```

Exercise - challenge: "Add an `address` attribute to `Person` and set Piet's address." Scaffold has the `attr` machinery + `attr Person address String` but the query `Piet.address=a ? [(a="...")][..]` mismatches until a `Piet.address := "..."` line is added; solution adds it. Verify via CLI.

- [ ] **Step 3: Add the `scoping` section**

Prose: `{ }` blocks introduce a namespace; `private` hides a declaration; the same name can be reused independently in separate scopes. Demo (adapted from `examples/scoping.nl`):

```
import nelumbo.logic
{
    Aa         :: Object
    private Aa ::= XXX
    Aa x
    x=XXX ?
}
{
    Bb         :: Object
    private Bb ::= XXX
    Bb y
    y=XXX ?
}
```

Exercise - challenge: "Add a third scope that reuses the name `XXX` for a new type `Cc` and query it." Scaffold provides two scopes + an empty third `{ }`; solution fills it mirroring the others with a checked query. Verify via CLI.

- [ ] **Step 4: Verify every snippet per the Content-task conventions**

`./gradlew cliJar`; all demos + solutions exit 0; scaffolds parse and mismatch until solved. These sections use advanced syntax (precedence `#N`, `::>`, private) - lean hard on the example files and CLAUDE.md's precedence gotcha; adjust anything that fails.

- [ ] **Step 5: Commit**

```bash
git add http/src/main/resources/public/tour.html
git commit -m "feat(http): tour sections for custom DSL, transformations, and scoping"
```

---

### Task 6: Final build, smoke test, docs

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Full clean build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL (includes the npm bundle and all http tests).

- [ ] **Step 2: Manual browser smoke test**

Run: `./gradlew :http:serverJar && java -jar http/build/libs/nelumbo-http-server-*.jar --port 8080`
Open `http://localhost:8080/`. Verify: sidebar switches sections; each section's editor(s) mount with syntax colors and (if connected) hover/completion; Run shows results; an unsolved exercise shows a red mismatch squiggle on its query and it clears when you apply the solution; "Show solution" toggles the solution block; the Playground link opens `/playground.html` and still works. Note anything broken and fix before committing.

- [ ] **Step 3: Update CLAUDE.md**

In the http module description line, change the pages note to: `Monaco-based feature tour (/) and free-form playground (/playground.html)`. In the "HTTP Module - LSP over WebSocket" section, add one sentence: `The frontend exposes NelumboFields.connect() + mountFields(container) so the tour mounts each section's editors lazily over one shared /lsp client; initNelumboFields() (playground) mounts all fields at once.`

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: describe the Nelumbo feature tour site"
```

---

## Verification checklist (after all tasks)

- [ ] `./gradlew clean build` green (bundle + http tests)
- [ ] `/` serves the sidebar tour; `/playground.html` serves the playground; `/demo.html` is 404
- [ ] Every demo and every `.nelumbo-solution` snippet evaluates (CLI exit 0)
- [ ] Every exercise scaffold parses and shows a mismatch until its solution is applied
- [ ] Section switching mounts editors lazily; one shared `/lsp` client; hover/completion/inline results work when connected
- [ ] "Show solution" reveals/hides each solution; the learner's edits are never overwritten
- [ ] Playground still works at its new path with full LSP features
