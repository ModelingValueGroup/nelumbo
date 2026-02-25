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

## Architecture

### Module Structure

```
nelumbo (root)              → Core language: syntax, semantics, pattern matching, knowledge base
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
- `WsServer` — WebSocket transport support.
- Main class: `org.modelingvalue.nelumbo.lsp.Main`

### Key Entry Points

- `org.modelingvalue.nelumbo.KnowledgeBase` — Core language execution
- `org.modelingvalue.nelumbo.lsp.Main` — LSP server
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

## Code Conventions

- All Java source files carry an LGPL 3.0 header (auto-corrected by `mvgCorrector` Gradle task using `header-template.txt`).
- Git branches: `master` (release), `develop` (active development).
- CI runs on GitHub Actions (`.github/workflows/build.yaml`). Skip CI with `[no-ci]` in commit message.
