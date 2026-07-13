# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Nelumbo is a declarative logic programming meta-language written in Java. It supports defining custom syntax and semantics, with IDE integration via the Language Server Protocol. Early stage — incompatible changes are likely.

## Build Commands

Requires **Java 21+**. Uses Gradle 8.14.3 (Kotlin DSL) via wrapper.

```sh
./gradlew build                          # Build everything (core + LSP server + tests)
./gradlew test                           # Run all tests (core + LSP server)
./gradlew jar                            # Core library only
./gradlew :lsp:server:serverJar          # LSP server shaded JAR
./gradlew :website:serverJar             # Website HTTP server shaded JAR (needs node/npm: bundles the Monaco/LSP frontend)
./gradlew :mcp:mcpJar                    # MCP server shaded JAR (stdio; register as: java -jar nelumbo-mcp-server-<version>.jar)
./gradlew :lsp:plugins:eclipse:jar       # Eclipse plugin
./gradlew :lsp:plugins:intellij:build    # IntelliJ plugin
./gradlew editorJar                      # Standalone editor (ShadowJar)
```

Run a single test class:
```sh
./gradlew test --tests "org.modelingvalue.nelumbo.test.NelumboTest"
```

Run a single test method:
```sh
./gradlew test --tests "org.modelingvalue.nelumbo.test.NelumboTest.initTest"
```

The root `test` task depends on `:lsp:server:test`, so `./gradlew test` runs both.

The IntelliJ plugin's IDE sandbox is redirected to `lsp/plugins/intellij/build/idea-sandbox` (`sandboxContainer` in its build script), so it is wiped by `clean`. The `.intellijPlatform/` dir at the repo root (gitignored) is IntelliJ Platform Gradle Plugin cache; the plugin unconditionally recreates it (at configuration time of every build, plus during intellij task execution), so the intellij build script rmdirs it when it stays empty (a `taskGraph.whenReady` hook plus a `removeEmptyPlatformCacheDir` finalizer) (content appears on demand: ivy descriptors on cold dependency resolution, coroutines javaagent for runIde/test - then it is left alone). Its location is not project-relative configurable (the `org.jetbrains.intellij.platform.intellijPlatformCache` property resolves relative paths against the daemon cwd, so only absolute paths work there). `org.jetbrains.intellij.platform.selfUpdateCheck=false` in `gradle.properties` stops the plugin's update check from writing a lock file there (and avoids a network call). Note: changing `gradle.properties` while a daemon is up trips mvgplugin's "changed in mid air" guard - run `./gradlew --stop`.

## Architecture

### Module Structure

```
nelumbo (root)              → Core language: syntax, semantics, pattern matching, knowledge base
├── website                 → HTTP server (Javalin + Jackson): serves a KB over REST (/eval, /metadata, /health), an LSP WebSocket at /lsp, and the public pages from src/main/resources/public/: landing (/), Monaco-based feature tour (/tour.html) and free-form playground (/playground.html)
├── mcp                     → MCP stdio server (official MCP Java SDK): tools eval_nl, search_docs, get_example, new_model for LLM authoring of .nl decision models
├── lsp/server              → LSP server (depends on root + LSP4J + Jackson + Tyrus WebSocket)
└── lsp/plugins/
    ├── eclipse             → Eclipse IDE plugin (Java, dropins-based)
    ├── intellij            → IntelliJ IDEA plugin (Java, LSP4IJ)
    └── vscode              → VS Code extension (TypeScript, separate build)
```

The VS Code plugin is **not** a Gradle subproject — it has its own `package.json` and builds with npm/esbuild.

### Core Language Layers (root `src/`)

Package: `org.modelingvalue.nelumbo`

- **`syntax/`** — Tokenizer → Parser pipeline. `Tokenizer` does lexical analysis, `Parser` builds AST nodes.
- **`patterns/`** — Pattern matching system: `Pattern` base class with `SequencePattern`, `AlternationPattern`, `OptionalPattern`, `RepetitionPattern`, `TokenTextPattern`, `TokenTypePattern`, `NodeTypePattern`, `Functor`.
- **`logic/`** — Logical operators and predicates: `And`, `Or`, `Not`, `Equal`, `When`, quantifiers (`ExistentialQuantifier`, `UniversalQuantifier`), `Predicate`, `BinaryPredicate`, `CompoundPredicate`.
- **`integers/`**, **`strings/`**, **`collections/`** — Built-in data type operations.
- **`tools/`** — Standalone Swing-based editor (`NelumboEditor`) and AST/KB viewer dialogs.
- **Core classes** — `KnowledgeBase` (main execution engine), `Node` (AST), `Type`, `Variable`, `Fact`, `Rule`, `Transform`, `Query`, `InferResult`, `InferContext`.

### LSP Server (`lsp/server/`)

Package: `org.modelingvalue.nelumbo.lsp`

- `NelumboLanguageServer` — Main LSP entry point.
- `NlDocument` — Represents an open document.
- `documentService/` — Individual LSP features (completion, hover, definition, semantic tokens, formatting, folding, code lens, code actions, selection ranges).
- `workspaceService/` — Workspace-level features (symbols, execute command).
- Main class: `org.modelingvalue.nelumbo.lsp.Main` (stdio, or a Tyrus WebSocket via `Main ws [port]`)

The server is **embeddable**: `NelumboLanguageServer(baseKb, evalDeadlineMs, exitHandler)` + `connect(client)` runs it with a per-instance `LanguageClient` (stored on `Workspace`, not the static `Main.client`), an injectable base KB, a query-eval deadline, and no `System.exit`. The website module embeds it behind `/lsp` (see below). Client folder resolution and filesystem scanning in `initialize` are skipped in embedded mode.

### Key Entry Points

- `org.modelingvalue.nelumbo.KnowledgeBase` — Core language execution
- `org.modelingvalue.nelumbo.lsp.Main` — LSP server
- `org.modelingvalue.nelumbo.mcp.Main` — MCP server (stdio)
- `org.modelingvalue.nelumbo.tools.NelumboEditor` — Standalone editor

## Testing

Tests are in `src/test/java/org/modelingvalue/nelumbo/test/` (core) and `lsp/server/src/test/` (LSP).

Core tests use `NelumboTestBase` which provides `testString()` and `testFile()` helpers. Many tests use `@RepeatedTest(10)` with randomized execution order (controlled by `RANDOM_NELUMBO` system property). Example `.nl` files live in `src/main/resources/examples/`.

System properties controlling test behavior: `PARALLEL_COLLECTIONS`, `REVERSE_NELUMBO`, `RANDOM_NELUMBO`, `TRACE_NELUMBO`, `TRACE_SYNTATIC`, `VERBOSE_TESTS`.

## Nelumbo Language Syntax (.nl files)

- Type declarations: `Person :: Object`
- Pattern definitions: `Integer ::= fib(<Integer>)`
- Variables: `Integer n, f`
- Rules: `fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1`
- Facts: `pc(Hendrik, Juliana)`
- Queries with expected results: `fib(5)=f ? [(f=5)][..]`
- Logical operators: `&` (AND), `|` (OR), `E[x]` (existential), `A[y]` (universal)

## Authoring .nl files (DSL extension)

Building a DSL in Nelumbo means declaring `MyType :: Root` and giving it functor bodies (`MyType ::= <pattern>`) — instances of those functors then become first-class top-level statements in `.nl` files. The canonical catalogue of pattern syntax lives in `src/main/resources/org/modelingvalue/nelumbo/lang/lang.nl` lines 34-47 (the `Pattern ::=` declaration); `src/main/resources/org/modelingvalue/nelumbo/tests/langOnly.nl` has one worked example per Pattern subtype. Reusable sub-patterns can be factored out with `pattern N ::= …` (named patterns) and referenced as `<N>` — see `docs/reference/stdlib/lang.md#named-patterns`.

**Precedence gotcha — Root-extending functors need explicit `#N`.** When a functor is declared on a `Root` subtype, append an explicit precedence to each alternative (e.g. `MyStmt ::= keyword <(> <Arg> <,> , <)+> #0`). Without it, instances containing repetition / optional / alternation patterns fail with `Unexpected token '\n', expected ` and the error cascades. `Object`-based functors (`Color ::= mix(<Color>,<Color>)`) don't need this. See `logic.nl`, `examples/deHet.nl`, and the pattern-coverage section of `tests/langOnly.nl` for the convention.

**Fast verify loop — use the CLI, not JUnit, while iterating.** `./gradlew cliJar` produces `build/libs/nelumbo-<version>-cli.jar`; then `java -jar build/libs/nelumbo-*-cli.jar path/to/file.nl` parses and evaluates the file (exit 0 on success; parse/eval errors print `file:line:col`). Add `-q` to silence query output. JUnit tests over `.nl` resources are Gradle-cached, so pass `--rerun-tasks` when only the resource changed; filter with `./gradlew :test --tests "..." --rerun-tasks` (the `:` targets root project, since the root `test` task chains `:lsp:server:test`).

## LSP Server - Injectable Base KB

`QueryEvaluator` now has a 4-arg overload `evaluate(KnowledgeBase base, long deadlineMs, String content, String uri)`. The 2-arg overload delegates to it with `(BASE, 0, ...)`. Call sites in `QueryResultCache` and `WorkspaceExecuteCommandService` pass `workspace.getBaseKnowledgeBase()` and `workspace.getEvalDeadlineMs()`. `NlDocument.of` parses against the workspace KB via `Parser.parse(workspace.getBaseKnowledgeBase(), tokenizerResult)`.

## Website Module - LSP over WebSocket

`NelumboHttpServer` exposes a `/lsp` WebSocket route (in addition to the REST endpoints). Each connection gets its own embedded `NelumboLanguageServer` instance wired via `LspBridge`. Wire format: one plain JSON-RPC message per text frame (no Content-Length framing - vscode-ws-jsonrpc compatible). A session cap (default 32, `--max-lsp-sessions` on the CLI / 4-arg constructor) rejects excess connections with close code 1013; per-session guards: 64 KB text-frame limit, 10-minute idle timeout, 64 documents max, and the eval deadline also bounds LSP parsing/inference. The 3-arg ctor delegates to the 4-arg one.

`lsp:server` publishes a plain jar with classifier `"plain"` (in addition to the shadow jar). `website/build.gradle.kts` depends on it via a normal `implementation(project(":lsp:server"))` dependency.

The frontend is an npm project at `website/src/main/frontend/` (Monaco + monaco-languageclient, esbuild IIFE). It exposes `NelumboFields.connect()` (establish the single page-shared `/lsp` client) + `NelumboFields.mountFields(container)` (mount `.nelumbo-field` editors within a container, idempotent) so the feature tour mounts each section's editors lazily over one shared client; `initNelumboFields()` (the playground) mounts every field at once. An exercise field with a following `.nelumbo-solution` sibling gets a "Show solution" toggle. The `:website:npmBundle` Gradle task runs `npm run dist`; the bundle is registered as a source-set output dir and shipped inside the shaded jar under `public/assets/`, served at `/assets/`. See `website/src/main/frontend/README.md` for the (intentionally old, self-contained) dependency-stack rationale.

The pages are `landing.html` (served at `/`): a static, self-contained intro page (no LSP/eval dependency) linking to tour and playground; `tour.html` (served at `/tour.html`): a sidebar feature tour with 8 sections, each with prose, a demo field, and self-checking exercises (the exercise query carries an expected result like `? [(r=120)][..]`, so the LSP mismatch squiggle confirms a solution); and `playground.html` (served at `/playground.html`): a single free-form field.

End-to-end browser tests live in `website/src/main/frontend/e2e/` (Playwright, Chromium). In `website/src/main/frontend/` run `npm run test:e2e:install` once, then `npm run test:e2e` (which rebuilds `:website:serverJar` and runs the suite; `playwright.config.ts` spawns the jar on port 8899). They cover tour structure, Run->/eval, Show-solution, and the LSP features (diagnostics, hover, completion, go-to-definition) via the `NelumboFields.__editors`/`.__monaco` test hook. CI runs them after the build.

**Deployment.** `.github/workflows/deploy.yml` deploys the website on pushes to `master` (and manually via workflow_dispatch): it builds `:website:serverJar`, bakes it into a Docker image (`website/Dockerfile`, arm64), pushes to `registry.openwalnoot.com/openwalnoot/services-docker-images/nelumbo-website` (the self-hosted gitlab.openwalnoot.com registry), then over plain SSH copies `website/docker-compose.yml` to `/data/sites/nelumbo.nl` on the Openwalnoot server and runs `docker compose pull && up -d --force-recreate`, followed by an in-network health check. Traefik routes `nelumbo.nl` to the container (external `web` network). Required repo secrets: `OW_HOST`, `OW_USER`, `OW_PRIVATE_KEY`, `GITLAB_USER`, `GITLAB_DOCKER_TOKEN` (plus the existing `ALLREP_TOKEN`). Note: mvgplugin fails the build if any workflow job lacks the `[no-ci]` guard in an `if:`.

**Public-deployment note.** The per-session guards above are in-process only. For a public site, front `/lsp` with a TLS reverse proxy that enforces per-IP connection limits (32 idle-but-pinging sockets can otherwise hold every session slot) and consider rate-limiting log output (malformed frames and unknown LSP methods each log a line).

## MCP Module - LLM Authoring Tools

`mcp/` is an MCP stdio server (`org.modelingvalue.nelumbo.mcp.Main`, SDK `io.modelcontextprotocol.sdk:mcp`) for LLM authoring of self-contained `.nl` decision models. Tools: `eval_nl` (structured diagnostics via the core `NelumboEvaluator`, enriched by the curated `Hints` table, plus per-query expectation results), `search_docs` (keyword search over `docs/**/*.md`, bundled at build time with an `index.txt`; query terms under 3 chars are ignored), `get_example` (bundled `.nl` corpus with curated descriptions), `new_model` (self-verifying skeleton, `ModelSkeleton` - its test evaluates the skeleton, so it must always stay valid; note: decision functors do not enumerate free variables, queries in the skeleton show the working idiom). Handlers live SDK-free in `NelumboTools`; `Main` owns protocol wiring, the eval deadline (`--eval-deadline-ms`, default 10s) and reroutes `System.out` to stderr (stdout is the JSON-RPC channel). `NelumboCli` now delegates to `NelumboEvaluator` (`tools/NelumboEvaluator.java`). Design: `docs/superpowers/specs/2026-07-12-mcp-server-design.md`.

Adding a `.nl` example (`src/main/resources/org/modelingvalue/nelumbo/examples/`) requires registering it in two explicit lists: `ExampleCatalog.ENTRIES` (mcp, serves `get_example`) and `ExamplesTest` (core, runs it as a test) - neither auto-discovers files. The README has an "MCP Server" section with the install/usage story (`./gradlew :mcp:mcpJar` + `claude mcp add nelumbo -- java -jar ...`).

## Code Conventions

- All Java source files carry an LGPL 3.0 header (auto-corrected by `mvgCorrector` Gradle task using `docs/header-template.txt`).
- Git branches: `master` (release), `develop` (active development).
- CI runs on GitHub Actions (`.github/workflows/build.yaml`). Skip CI with `[no-ci]` in commit message.
