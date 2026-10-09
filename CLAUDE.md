# CLAUDE.md

Guidance for Claude Code in this repository.

## Project Overview

Nelumbo is a declarative logic programming meta-language in Java: custom syntax and semantics, IDE integration via LSP. Early stage - incompatible changes are likely.

## Build Commands

Java 21+, Gradle 8.14.3 (Kotlin DSL) via wrapper.

```sh
./gradlew build                          # everything (core + LSP server + tests)
./gradlew test                           # test task of EVERY (sub)project
./gradlew :test                          # core tests only
./gradlew jar                            # core library only
./gradlew :cli:cliJar                    # CLI + HTTP eval server (nelumbo-cli-<version>.jar)
./gradlew :lsp:server:serverJar          # LSP server
./gradlew :website:serverJar             # website server (needs node/npm: bundles the Monaco frontend)
./gradlew :mcp:mcpJar                    # MCP server (stdio)
./gradlew :lsp:plugins:eclipse:jar       # Eclipse plugin
./gradlew :lsp:plugins:intellij:build    # IntelliJ plugin
./gradlew editorJar                      # standalone editor
./gradlew test --tests "org.modelingvalue.nelumbo.test.NelumboTest.initTest"   # one class/method
```

Test task wiring (root `build.gradle.kts`):
- NO `dependsOn` between test tasks: CI runs gradle with `--continue` so all test tasks run even when one fails (a task whose dependency failed would be skipped).
- `allTestsReport` (finalizer of root `test`) uses `binaryResultsDirectory.locationOnly` + `mustRunAfter`, and reports only the test tasks in the current graph.
- `mvgtagger` `dependsOn` every `Test` task, so a red build is never tagged (the tagger itself ignores failures). CI upload/e2e steps have no `if: always()`.
- An `allprojects` test listener prints a red `####` banner per failed test, a per-task summary, and a `::error` annotation on GitHub Actions. Use backslash-u001B escapes, never raw ESC bytes.

### Dependency versions

- Shared versions in `gradle/libs.versions.toml` (`libs.mvg.json` etc.); bump there only. Module-unique deps stay literals in their build script.
- immutable-collections is declared ONCE, in the root, as `api(libs.immutable.collections)`; do NOT re-declare it in submodules.
- immutable-collections and mvg-json are plain versions from Maven Central: no ALLREP_TOKEN needed. Do not reintroduce `-BRANCHED` versions (they resolve per git branch and silently diverge in the CI master build). Tokenless check: `./gradlew -DALLREP_TOKEN=bogus build`.
- Local immutable-collections checkout: `includeBuild` substitution in `settings.gradle.kts`.
- IntelliJ plugin: sandbox in `lsp/plugins/intellij/build/idea-sandbox`. The plugin recreates `.intellijPlatform/` at the repo root on every build; its build script deletes it again (`taskGraph.whenReady` + `removeEmptyPlatformCacheDir`), only the coroutines javaagent keeps it alive. Its location can only be set as an absolute path. `selfUpdateCheck=false` in `gradle.properties` avoids a lock file and a network call.
- Changing `gradle.properties` with a daemon up trips mvgplugin's "changed in mid air" guard: `./gradlew --stop`.

## Architecture

```
nelumbo (root)   -> core: syntax, semantics, patterns, knowledge base
|-- cli          -> NelumboCli (files, -n sources, --json, -i REPL, --server REST) + eval server (package ...server)
|-- website      -> website server: cli's EvalService + LSP WebSocket /lsp + public pages
|-- mcp          -> MCP stdio server: eval_nl, search_docs, get_example, new_model
|-- lsp/server   -> LSP server (LSP4J, Jackson, Tyrus WebSocket)
`-- lsp/plugins  -> eclipse (dropins), intellij (LSP4IJ), vscode (npm/esbuild, NOT a Gradle subproject)
```

### Core (root `src/`, package `org.modelingvalue.nelumbo`)

- `syntax/`: `Tokenizer` -> `Parser` -> AST.
- `patterns/`: `Pattern` + Sequence/Alternation/Optional/Repetition/TokenText/TokenType/NodeType patterns, `Functor`.
- `logic/`: `And`, `Or`, `Not`, `Equal`, `When`, quantifiers, `Predicate`, `BinaryPredicate`, `CompoundPredicate`.
- `integers/`, `strings/`, `collections/`: built-in types.
- `tools/`: Swing `NelumboEditor`, AST/KB viewers, `NelumboEvaluator` (shared by CLI and MCP).
- Core classes: `KnowledgeBase` (engine), `Node`, `Type`, `Variable`, `Fact`, `Rule`, `Transform`, `Query`, `InferResult`, `InferContext`.

Entry points: `KnowledgeBase`, `lsp.Main` (stdio, or `Main ws [port]`), `mcp.Main`, `tools.NelumboEditor`.

### LSP server (`lsp/server/`, package `org.modelingvalue.nelumbo.lsp`)

- `NelumboLanguageServer` (entry), `NlDocument`, `documentService/` (completion, hover, definition, semantic tokens, formatting, folding, code lens, code actions, selection ranges), `workspaceService/`.
- Embeddable: `NelumboLanguageServer(baseKb, evalDeadlineMs, exitHandler)` + `connect(client)`: per-instance `LanguageClient` (on `Workspace`, not static `Main.client`), injectable base KB, no `System.exit`; client folder resolution and filesystem scan skipped.
- `QueryEvaluator.evaluate(base, deadlineMs, content, uri)`; call sites pass `workspace.getBaseKnowledgeBase()`/`getEvalDeadlineMs()`. `NlDocument.of` parses against the workspace KB.
- Every request handler in `NlTextDocumentService` runs in `inKb(...)` = a fresh CHILD of the base KB: `Type.getAssigned` reads `KnowledgeBase.CURRENT`, which lsp4j threads lack (NPE in semantic tokens/hover; pinned by `LspRequestContextTest`). A child, so document types never land in the shared base KB's type cache.
- Completion: core `Token.completions(cursor)` returns LSP-free `Token.Completion(replaceStart, replaceEnd, text, kind, documentation)` (token-relative offsets). Its body is a stub - Wim implements it; do not change other core code for it. `DocumentCompletionService` owns all LSP mapping.

Overload budget (`EvalGate.GLOBAL` counts evaluations JVM-wide):
- System properties `NELUMBO_OVERLOAD_THRESHOLD` (default `Collection.PARALLELISM`, the width of the shared pool `KnowledgeBase.POOL`) and `NELUMBO_OVERLOAD_BUDGET_MS` (default 2000, min 1).
- An evaluation starting while >= threshold others run gets the budget as deadline, if shorter than the workspace deadline (or that is 0).
- The budget counts from the start on a pool worker (set at the start of the `invoke` runnable on `KnowledgeBase.current()`), not from queueing; pinned by `EmbeddedServerTest.lightEvaluationQueuedBehindAFullPoolIsNotStopped`. The normal deadline includes the queue wait.
- Backstop `future.get` in `QueryResultCache`: workspace deadline + 2 s (none when 0).
- A timeout under the budget -> `QueryResult.Kind.OVERLOADED` + Warning diagnostic code `server-overloaded` (`QueryResult.OVERLOAD_CODE`, the client contract) on the query, or at 0:0.
- Such a document is retried server-side every 5 s (`OVERLOAD_RETRY_MS`, same `pending` slot) once `running < threshold`; an edit supersedes it, remove/shutdown cancel it.

## Testing

- Core tests: `src/test/java/org/modelingvalue/nelumbo/test/` (`NelumboTestBase.testString()`/`testFile()`); LSP: `lsp/server/src/test/`.
- Many tests are `@RepeatedTest(10)` with randomized order. System properties: `PARALLEL_COLLECTIONS`, `REVERSE_NELUMBO`, `RANDOM_NELUMBO`, `TRACE_NELUMBO`, `TRACE_SYNTATIC`, `VERBOSE_TESTS`.
- `.nl` resources: `src/main/resources/org/modelingvalue/nelumbo/` (`examples/`, `tests/`, `bugs/`). JUnit over resources is Gradle-cached: pass `--rerun-tasks` when only a resource changed.
- Fast loop: `./gradlew :cli:cliJar`, then `java -jar cli/build/libs/nelumbo-cli-*.jar file.nl` (exit 0 = ok, errors as `file:line:col`, `-q` silences query output). Always rebuild the jar after engine changes - a stale jar once faked an "environment-dependent" bug.

Sudoku examples (`examples/sudoku-*.nl`, NOT in `ExampleCatalog`/`ExamplesTest`):
- Run via CLI with `-DPARALLEL_COLLECTIONS=false`: under parallel collections `map` results are nondeterministic (unfixed engine bug). The flag does NOT serialize inference.
- `sudoku-9x9.nl`: naive brute force; its third puzzle runs 2+ hours (baseline). `sudoku-9x9-smart.nl`: singles-first (`scanF` forced moves, `solveBX` guesses; entry `sudokuSmart(g)`), all four puzzles ~45 s.
- `sudoku-4x4*.nl`: line-for-line 4x4 debug copies (solution grid `[[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]]`), sub-second.
- Brute-force files report the result set open (`[(s=...),..][..]`), singles-first files closed; the smart files keep only the "real" puzzle active (neighbor-query effects).
- `SudokuExamplesTest`: all `@Disabled` (4x4: timing-dependent `speculative-guard-index-crash.nl` without the flag; 9x9: too slow).
- `sudoku-4x4-smart.nl` fails with `Inconsistent results`: `scanB`'s forced branch lacks an `if` (program defect), which the engine reports in one rule order only (`bugs/inconsistency-missed-by-rule-order.nl`; `-DREVERSE_NELUMBO=true` flips it). `sudoku-9x9-smart.nl` has the same `scanB` shape.
- `sudoku-4x4-csp.nl`: CSP solver, Task 6 (`elim`) blocked on `bugs/in-on-function-call-undecided.nl`.

## Nelumbo Language Syntax (.nl files)

- Types: `Person :: Object`
- Patterns: `Integer ::= fib(<Integer>)`
- Variables: `Integer n, f`
- Rules: `fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1`
- Facts: `pc(Hendrik, Juliana)`
- Queries with expected result: `fib(5)=f ? [(f=5)][..]`
- Operators: `&`, `|`, `E[x]`, `A[y]` (up to 6 quantified variables)
- Statements are only `fact`, `<=>`, `?`: a bare `f(i)=[1,2]` is rejected ("unexpected type Boolean, expected Root or List<Root>"); write `f(i)=l <=> l=[1,2]`.

## Authoring .nl files (DSL extension)

- A DSL: `MyType :: Root` + functor bodies `MyType ::= <pattern>`; instances become top-level statements.
- Pattern syntax catalogue: `lang/lang.nl` lines 34-47 (`Pattern ::=`); one example per Pattern subtype in `tests/langOnly.nl`; named patterns `pattern N ::= ...` used as `<N>` (`docs/reference/lang/index.md#named-patterns`).
- Root-extending functors need an explicit precedence per alternative (`MyStmt ::= keyword <(> <Arg> <,> , <)+> #0`), else instances with repetition/optional/alternation fail with `Unexpected token '\n'`. `Object`-based functors don't.
- Functors on `:: Object` are Function-typed (`=` undecided); declare `:: Struct` for structural equality.
- A rule head whose result variable has the WRONG type is accepted silently and makes UNRELATED queries undecided: when everything goes undecided at once, check variable types first.

## cli Module

- `NelumboCli` (package `...cli`): runner, `--server` mode, double-click window. In `--server` mode main returns without `System.exit` (the HTTP dispatcher keeps the JVM alive).
- Package `...server`: `EvalService` (transport-independent: document + content type in, `Response(status, json)` out; `metadata()`/`health()`; per-request child KB; engine deadline + future backstop; owns the eval pool, `AutoCloseable`), `NelumboServer` (JDK `com.sun.net.httpserver`, virtual threads), `KnowledgeBaseLoader`, `NamedSource`, `ServerGui`.
- `NelumboServer` uses ONE root context with exact-path dispatch: JDK contexts match by prefix (`/eval` would match `/evaluate`).
- Deliberately no Javalin/Jetty. JSON via MVG's `mvg-json`: `Json.fromJson` gives Map/List with integers as Long and throws IllegalArgumentException on bad JSON (-> 400); `Json.toJson` sorts keys. Only third-party dep: JLine (REPL). Jackson is test-only. Jar ~4 MB.
- REST parity with the website: both use `EvalService`, pinned by identical `NelumboServerTest`s.
- Double-click detection: `NelumboCli` window and `ServerGui` (website Main) when `System.console()==null && !headless`, unless `--no-gui` (playwright passes it). The mcp `Main` peeks the first stdin byte; EOF first -> show registration instructions.
- `NelumboCli` window tabs: usage, prep (stdlib modules; CLI `-p/--prep`, loaded as a separate `<prep>` source so line numbers stay), nelumbo (examples picker, Eval, Trace = `--trace`), output, json, server (start/stop, 1 s stats). Shared look: `NelumboLaf.setup()` (FlatLaf).
- `ServerGui` "Load .nl files" builds the new KB BEFORE stopping the old server.
- The Gradle project name must differ from `:lsp:server`'s "server": equal group:name makes Gradle silently drop one from the classpath.
- Plain jar classifier "plain" (for the website); the CI upload glob matches only the shaded jar.

REPL (`-i`/`--interactive`, `NelumboRepl`):
- Files/`-n`/`-p` load into a session KB; each input (a line, or backslash-continued lines) runs in a child via `NelumboEvaluator.evaluate(base, ...)` (`SessionResult` overloads) and becomes the session only without ANY diagnostic.
- Sets `KnowledgeBase.setWorldScopedVariables(true)` (variables are otherwise namespace-scoped, useless on the next line).
- JLine with a console (history `~/.nelumbo_history`, Ctrl-C clears, Ctrl-D exits), else a plain reader without prompts (tests use `plainSource`). Commands `:help`, `:quit`/`:exit`.
- `-t` default unlimited. `-i` with `--json`, `--server` or `-` is a usage error (exit 2). Exit 0 even after input errors.

## Website Module

Server (`NelumboHttpServer`, Javalin 7 - needed for the `/lsp` WebSocket):
- Routes are registered inside `Javalin.create(config -> ...)` via `config.routes.get/post/ws/sse` (7.x removed `app.get`).
- REST `/eval`, `/eval/trace`, `/metadata`, `/health` via the cli's `EvalService`. Pages never call `/eval`.
- `/lsp`: one embedded `NelumboLanguageServer` per connection via `LspBridge`; one plain JSON-RPC message per text frame (no Content-Length).
- Guards: session cap (default 32, `--max-lsp-sessions`; Dockerfile 200) closes with 1013; 64 KB frames; 10-minute idle timeout; 64 documents; the eval deadline also bounds parsing.
- Depends on `lsp:server`'s plain jar (classifier "plain").

Frontend (`website/src/main/frontend/`, Monaco 0.34 + monaco-languageclient 1.0.1, esbuild IIFE; `README.md` explains the old stack):
- API: `NelumboFields.connect()` (one page-shared `/lsp` client), `mountFields(container)` (idempotent, the tour mounts per section), `initNelumboFields()` (sandbox), `setFieldText(field, text, name)`.
- `:website:npmBundle` runs `npm run dist`; the bundle ships in the jar under `public/assets/`.
- No Run button: inlay hints are the only evaluation feedback (`QueryResult.inlineLabel()`, max 60 chars; full result in the hover via `QueryResult.tooltip()`). Until an edit's re-evaluation finishes, `QueryResultCache` serves only the hints on unchanged lines.
- Hint KIND picks the style (Monaco 0.34 has three buckets): none = match (checkmark emoji), Type = result (purple chip), Parameter = mismatch/error (red). Marks are emoji because hint font size cannot exceed the editor's.
- Inlay-hint gotchas: (1) the explicit `import '.../inlayHints/browser/inlayHintsContribution.js'` must stay (esbuild tree-shakes it); (2) Monaco subscribes to `onDidChangeInlayHints` only after a non-empty response, so `wireInlayHintRefresh` answers `workspace/inlayHint/refresh` by registering+disposing a no-op provider.
- `fixedOverflowWidgets: true` on all editors (else hovers are clipped). WebKit places those widgets off by the visual viewport offset: `followVisualViewport` sets `--nf-viewport-x/y`, `fields.css` translates by it (WebKit-only via `CSS.supports('-webkit-backdrop-filter', 'none')`; verified on a real iPhone). On touch, completion opens only on typing.
- Toolbar below the editor, inside the field wrap: "Show solution" (field followed by `.nelumbo-solution`) and Reset; hidden by CSS `:has` while no button is visible.
- Solution = read-only Monaco viewer on the same LSP client (no client tokenizer, so `colorizeElement` renders nothing); green tint via CSS; NOT in `__editors`.
- Banners (`.nelumbo-lsp-banner`, `.nelumbo-overload-banner`) are position:fixed at the bottom: the tour's body is a flex row, an in-flow banner pushes `main` offscreen.
- Font stack (Google Fonts): Bricolage Grotesque, Instrument Sans, JetBrains Mono (also Monaco `fontFamily`, `remeasureFonts()` on `document.fonts.ready`).

Edit persistence and reconnect:
- Every field saves `{original, text}` in localStorage under `nelumbo-field:<pathname>:<position among the page's .nelumbo-field>` (position, not mount index) on every change; removed when equal to the original; restored only while `original` matches the page (a changed exercise starts fresh). Storage errors are swallowed.
- Reset (`.nelumbo-reset`) shows while edited; one undoable edit (`replaceText` pushes a stack element before and after).
- Sandbox: `setFieldText(..., name)` keys edits per example (`<key>#<name>`). Known edge: Cmd+Z right after switching saves the old text under the new example.
- Every connection gets a NEW `MonacoLanguageClient` (vscode-languageclient 8.0.1 keeps a failed `start()` for good). A lost connection (close 1000/1001/1006) reconnects at the next user activity (pointer, key, touch, focus, tab visible) - not right away, or an idle tab holds a session forever. Other close codes (1013, 1011, 1009, 1002) or a failed open: banner, next attempt after `RETRY_MS` (30 s). `toSocket` takes over `ws.onclose`: use `addEventListener`.

Overload banner and stats:
- `.nelumbo-overload-banner` while any marker has code `server-overloaded`; clears after an edit or the server-side retry.
- `GET /stats` (`ServerStats`): `{status, sessions, evaluations, cpu, heap, uptimeSeconds, time}`; status `overloaded` if running >= threshold, `busy` if CPU >= 0.7 or running >= threshold/2, else `ok`. It serves the latest `StatsRecorder` sample: `getProcessCpuLoad` measures since its previous call by anyone, so the recorder must be its only reader.
- `StatsRecorder` samples every 5 s and pushes to `GET /stats/stream` (SSE, event `stats`, max 50 clients, over the cap one `error` event). Javalin 7 streams SSE only with `Accept: text/event-stream` (`curl -N -H 'Accept: text/event-stream' .../stats/stream`).
- `StatsHistory`: per UTC minute, 7 days in memory; with `--stats-dir` (production `/data/stats` = `/data/sites/nelumbo.nl/stats` on server1) one JSON line per minute in `stats-YYYY-MM-DD.jsonl`, last week read at start, files > 8 days deleted. A history failure never stops sampling.
- `GET /stats/history?range=hour|day|week` (week = 10-minute aggregates), else 400.
- `public/status-dot.js` colours every `<span class="status-dot" data-status-link>` (polls 15 s). `/status.html` is internal: no visible link, only shift+click on the dot (`theme.js`).
- `/status.html` (`src/status-chart.ts`, uPlot 1.6.32, global `NelumboStatus`): stream with polling fallback, separate CPU and activity charts (never dual-axis), downtime as gaps, 24-hour clock everywhere (own formatters), legend swatches as lines. Light aqua is < 3:1, so the data table is required.

Pages (`src/main/resources/public/`):
- All pages: `@VERSION@` filled by `processResources`; `/favicon.svg` (explicit route); shared header with theme switch.
- `landing.html` (`/`): live `grandparent.nl` field in a `.codecard` - must stay valid Nelumbo (e2e asserts its checkmark and that h1 contains "language"). The hero animation's end frame must equal the icon paths of `lsp/plugins/*/icons/logo.svg` (stroke 22, fill and transforms tuned to it). Messaging: logic meta-language for specifying DSLs.
- Palette from the lotus logo: accent `#c184d8`, soft `#edd4e8`, violet `#a99ae8`; no browns/golds/blues.
- `tour.html`: 8 sections, exercises carry expected results. `NelumboHttpServerTest.everyTourEditorEvaluates` posts every tour editor to `/eval`: an exercise may only fail on its expectation, a solution not at all. Transformation heads need `{OT,Literal}`/`{AT,Literal}` variables and `.AN` patterns `#30`.
- `sandbox.html`: one field + sidebar from `GET /examples` and `/examples/<name>` (cli examples minus `sudoku*`), grouped Examples/Exercises (`*Assignment` + "answer" link); `#<name>` opens an example. `/playground.html` 301-redirects here.
- Below 760px the tour/sandbox sidebar is a drawer (`#nav-toggle`/`#nav-backdrop`); below 420px the GitHub link shrinks to its logo.

Theme:
- `public/theme.js` (loaded BLOCKING in `<head>`) sets `data-theme` (light/dark) and `data-theme-mode` (auto/light/dark) on `<html>`; switch cycles auto -> light -> dark; override in localStorage `nelumbo-theme`.
- Colours only as CSS variables (`:root` dark, `[data-theme="light"]` override); landing has its own set, `fields.css` the `--nf-*` set. No hardcoded colours.
- Monaco themes `nelumbo-dark`/`nelumbo-light`, switched by a MutationObserver. Token colours: `TOKEN_STYLES` in `nelumbo-fields.ts` = `NelumboEditor.DEFAULT_TOKEN_COLORS` (light verbatim, dark lightened); keep in sync with the Eclipse `NelumboSemanticDamagerRepairer` and the `--nl-*` variables in `docs.html` (e2e `highlighting.spec.ts`). META_OPERATOR is only the `<`/`>` of pattern holes; `::=` is an OPERATOR.

Docs (`/docs/`):
- `copyDocs` (Sync) bundles `docs/**/*.md`, `nelumbo.svg`, root `docs/*.html` + `index.txt`; `DocsSite` renders all pages once at startup (commonmark + GFM tables) into the `docs.html` template.
- URLs: `/docs/` = `documentation.md` (`/docs` 302-redirects there), other pages `/docs/<path>.html`, 404 in the docs layout.
- GitHub rules: GitHub's heading slug algorithm (do NOT use commonmark's heading-anchor extension, its slugs differ); `.md` links -> `/docs/...html`; links outside `docs/` -> GitHub `blob/master` (`tree/` for directories).
- Sidebar: Overview, root `docs/*.html` pages (served as is with `theme.js` injected, titled by `<title>` minus "Nelumbo "), then `DocsSite.GROUPS` in fixed order (folder `index.md` first, rest by H1). Other root `.md` files are served, not listed.
- Every code fence needs a language (`DocsSiteTest` fails otherwise): ` ```nelumbo ` is highlighted by `NelumboHighlighter` (KB with `EvalService.STDLIB_IMPORTS`, 5 s deadline, a block that throws is shown plain); ` ```text ` for diagrams.
- Reference is grouped by defining level (`reference/core|lang|logic|packages`, plus `formatting.md`); every level page opens with a `> **Level:**` banner. `DocsSiteTest` crawls for dead links/anchors and unreachable pages.

`llms.txt`: single source at the repo root, copied by `copyLlms`, served at `/llms.txt`. Its doc links are absolute `https://nelumbo.nl/docs/...` URLs - fix them when renaming a docs page (`everyLinkInLlmsTxtResolves` checks them).

E2E (`website/src/main/frontend/e2e/`, Playwright Chromium): `npm run test:e2e:install` once, then `npm run test:e2e` (rebuilds `:website:serverJar`; server on 8899, a second one on 8898 with `-DNELUMBO_OVERLOAD_THRESHOLD=0 -DNELUMBO_OVERLOAD_BUDGET_MS=300` for `overload.spec.ts`). Test hooks: `NelumboFields.__editors`, `__monaco`, `__closeConnection()`. CI runs them after the build.

Load test (not in CI): `npm run test:load -- --url https://nelumbo.nl [--ssh user@host] [--max-clients 64] [--limit-ms] [--factorial N]` (`load/loadtest.mts`, plain Node 24 with type stripping; `.mts` because package.json has no `"type": "module"`). Replays the observed Monaco client traffic of the tour (didOpen + pulls for 25 documents, one didChange per keystroke, re-pull of all hints on every `workspace/inlayHint/refresh`) with doubling client counts. Edit-to-result is measured by the edited document's hint list changing (equal queries share one result entry, so hint positions do not work). Finding: the hardware is far from a limit; the session cap was (hence the Dockerfile's 200).

Deployment (`.github/workflows/deploy.yml`, on push to `master` or manual):
- Builds `:website:serverJar`, Docker image (`website/Dockerfile`, arm64), pushes to `registry.openwalnoot.com/openwalnoot/services-docker-images/nelumbo-website`, copies `website/docker-compose.yml` over plain SSH to `/data/sites/nelumbo.nl`, `docker compose pull && up -d --force-recreate`, health check.
- Caps: `cpus: "16"`, 16g memory, JVM `-XX:MaxRAMPercentage=75`. `Collection.PARALLELISM` (the one shared inference pool) = cpus limit - 1; uncapped on the 80-core host it mainly wasted CPU.
- Traefik: apex router with HSTS (no preload), `www` 301 to the apex (`$$` escapes compose interpolation). The ACME cert needs DNS incl. AAAA pointing at the server first; after a failure it retries only on a Traefik restart/config change.
- Secrets: `OW_HOST`, `OW_USER`, `OW_PRIVATE_KEY`, `GITLAB_USER`, `GITLAB_DOCKER_TOKEN`, `ALLREP_TOKEN`. mvgplugin fails the build if a workflow job lacks the `[no-ci]` guard in an `if:`.
- Public deployment: the session and SSE caps are in-process only; a proxy should enforce per-IP connection limits on `/lsp` and `/stats/stream`.

## MCP Module

- `mcp.Main` (official MCP Java SDK, stdio): tools `eval_nl` (diagnostics via `NelumboEvaluator` + curated `Hints`, per-query expectation results), `search_docs` (bundled `docs/**/*.md`, terms < 3 chars ignored), `get_example`, `new_model` (`ModelSkeleton`; its test evaluates it, so keep it valid; decision functors do not enumerate free variables).
- Handlers SDK-free in `NelumboTools`; `Main` owns protocol, `--eval-deadline-ms` (10 s) and reroutes `System.out` to stderr (stdout is JSON-RPC).
- Design: `docs/superpowers/specs/2026-07-12-mcp-server-design.md`; install story in the README "MCP Server" section.
- A new example in `examples/` must be registered in `ExampleCatalog.ENTRIES` (mcp) AND `ExamplesTest` (core); nothing auto-discovers.

## IntelliJ Plugin

- Coloring comes ONLY from LSP semantic tokens. LSP4IJ's lazy path (`HighlightVisitor`, for languages with a `ParserDefinition`) colors nothing for nelumbo's flat PSI, so `NelumboLanguageServerFactory.createClientFeatures()` overrides `LSPSemanticTokensFeature.shouldVisitPsiElement` -> false (direct path).
- Keep the LSP4IJ pin (0.21.0) at the current marketplace version: the sandbox IDE auto-updates it anyway.
- Debugging: server stderr only in the LSP4IJ console (not `idea.log`); `NlTextDocumentService` prints `~~~ <request>` per request; `"Debugging":true` in `~/nelumbo/settings.json` dumps tokens; the sandbox runs `~/nelumbo/server.jar`, extracted fresh on every start.

## Known-Bug Repros (xfail suite)

- Red-by-design repros in `src/main/resources/org/modelingvalue/nelumbo/bugs/`; every expectation states the CORRECT behavior; headers carry status, code location and attribution.
- Two harnesses, both needed (one bug can show as a crash in one and a wrong result in the other):
  - JUnit `KnownBugsTest`: one `@KnownBug` method per repro (`KnownBugExtension`: a failure shows ABORTED, build green; a pass FAILS with "KNOWN BUG APPEARS FIXED"). `bugResource()` has a preemptive 60 s timeout.
  - CLI `./run-all-tests-with-CLI` (needs `cliJar`, runs with `-ea`): a file taking >= `SLOW_SECONDS` (60) fails.
- Expected errors: a repro whose correct behavior is an error per query starts with `// expect-error-per-query: <text>`; the CLI runner checks every output line, its `KnownBugsTest` method checks diagnostics via `NelumboEvaluator`. Only user: `inconsistency-missed-by-rule-order.nl` (`Rule.biimply` checks earlier rules' facts against the current rule only; rules run in `Set<Rule>` hash order; 32 identical copies because the order depends on what the JVM parsed before).
- Fixed -> promote to `RegressionTest`: move the file to `tests/`, header "Regression test (fixed <date>; was bugs/<name>.nl)", `@RepeatedTest(10)` method (load-time bugs: `@Timeout`).
- Findings without an `.nl` repro (editor/LSP/interactive) are listed in `KnownBugsTest`'s class javadoc.

## Code Conventions

- LGPL 3.0 header on all Java files (auto-corrected by `mvgCorrector`, template `docs/header-template.txt`).
- Branches: `master` (release), `develop` (development).
- `.github/dependabot.yml` is GENERATED by mvgplugin in CI (an npm entry per `package.json` dir); do not edit it by hand. A `#notouch` line disables generation.
- CI: `.github/workflows/build.yaml`; `[no-ci]` in the commit message skips it. build.yaml and deploy.yml set `npm_config_prefer_offline/audit/fund` so every `npm ci` skips registry round trips.
- Versioning: local builds are `dev`; CI uses the patch successor of the highest version tag, unless the `gradle.properties` base version (0.9.0) is higher. The Eclipse `Bundle-Version` falls back to the `gradle.properties` value locally. `playwright.config.ts` picks the newest `nelumbo-web-server-*.jar` by mtime.
- Releases: `.github/workflows/release.yaml` on `v*` tags attaches `nelumbo-<what>-<version>.{jar,zip}` (ide, cli, web-server, mcp-server, eclipse-plugin, intellij-plugin, slides); body from `RELEASE_NOTES.md` (`${version}`/`${version-num}`).
