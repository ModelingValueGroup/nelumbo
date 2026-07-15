# Nelumbo

<p align="center"><img src="docs/nelumbo.svg" alt="Nelumbo" width="50" height="50"/></p>

Nelumbo aims to be a powerful and extensible declarative logic programming language, designed for defining and executing
custom syntax and semantics. As a meta-language, Nelumbo will be easily extensible, making it suitable for a wide range
of applications. The goal is to integrate it with any IDE using the Language Server Protocol, allowing Nelumbo to serve
as a language development platform. The language is currently developed in Java for seamless integration and
performance. Please note that Nelumbo is in a very early stage of development, and incompatible changes are likely to
occur.

---

## Table of Contents

- [Features](#features)
- [Documentation](#documentation)
- [Building](#building)
- [IDE Plugins](#ide-plugins)
- [Command-Line Interface](#command-line-interface)
- [HTTP Server](#http-server)
- [MCP Server](#mcp-server)
- [Examples](#examples)
- [Releasing](#releasing)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

---

## Features

* Define and parse syntaxes
* Define and execute semantics
* Purely declarative semantics
* Define and run tests
* Easily extensible
* Easily integrable
* IDE integration via LSP, including auto-formatting
* Written in Java

## Documentation

Detailed documentation.

- [Detailed documentation](docs/documentation.md)

## Building

Requires Java 21 or later.

Build everything (core library, LSP server, and all IDE plugins):

```sh
./gradlew build
```

Build individual components:

```sh
./gradlew jar                          # core library
./gradlew :nelumbo-server:serverJar    # HTTP eval server (shaded jar)
./gradlew :lsp:server:serverJar        # LSP server (shaded jar)
./gradlew :lsp:plugins:eclipse:jar     # Eclipse plugin (includes LSP server)
./gradlew :lsp:plugins:intellij:build  # IntelliJ plugin
./gradlew cliJar                       # command-line runner (shaded jar)
```

Run tests:

```sh
./gradlew test
```

## IDE Plugins

Nelumbo has LSP-based editor plugins for multiple IDEs:

| IDE      | Path                                           | Details                                         |
|----------|------------------------------------------------|-------------------------------------------------|
| Eclipse  | [`lsp/plugins/eclipse`](lsp/plugins/eclipse)   | Dropins-based plugin with semantic highlighting |
| IntelliJ | [`lsp/plugins/intellij`](lsp/plugins/intellij) | IntelliJ platform plugin                        |
| VS Code  | [`lsp/plugins/vscode`](lsp/plugins/vscode)     | VS Code extension                               |
| Neovim   | [`lsp/plugins/neovim`](lsp/plugins/neovim)     | Neovim LSP configuration                        |

See the README in each plugin directory for installation instructions.

## Command-Line Interface

`NelumboCli` parses and evaluates one or more `.nl` files from the terminal, printing each query together with its inferred result. Queries that declare expected results are compared and mismatches are reported as errors. It is attached to every [release](https://github.com/ModelingValueGroup/nelumbo/releases) as `nelumbo-cli-<version>.jar`, or build it with `./gradlew cliJar` (output in `build/libs/nelumbo-cli-<version>.jar`) and run it with `java`:

```sh
java -jar build/libs/nelumbo-cli-<version>.jar [options] <file>...
```

Pass `-` in place of a filename to read from stdin. Use `-q` / `--quiet` to suppress query output (errors are still reported) and `-h` / `--help` for the full option list. The process exits with `0` on success, `1` on parse/evaluation/comparison errors, and `2` on usage errors — suitable for scripting and CI.

The jar is also double-clickable: launched without a console and without arguments it opens a file chooser for a `.nl` file and shows the evaluation results in a window.

## HTTP Server

The `nelumbo-server` module is a lean HTTP executor for `.nl` specifications: it loads the given files (directories are scanned for `*.nl`) into a knowledge base and evaluates posted documents against it. It runs on the JDK's built-in HTTP server and has no third-party dependencies — the shaded jar is about 2 MB. It is attached to every [release](https://github.com/ModelingValueGroup/nelumbo/releases) as `nelumbo-cli-server-<version>.jar`, or can be built from source:

```sh
./gradlew :nelumbo-server:serverJar
java -jar nelumbo-server/build/libs/nelumbo-cli-server-<version>.jar [--port N] [--timeout MS] [<file-or-dir>...]
```

| Endpoint           | Purpose                                                                                              |
|--------------------|------------------------------------------------------------------------------------------------------|
| `POST /eval`       | Evaluate a posted `.nl` document (raw text, or a JSON envelope `{"document": "...", "limit": N}`); returns query results as JSON |
| `POST /eval/trace` | Like `/eval`, with a (currently stubbed) trace field                                                  |
| `GET /metadata`    | Knowledge base metadata: declared types, functors, rules, and facts                                   |
| `GET /health`      | Liveness check                                                                                        |

Each request is evaluated against a throwaway child of the loaded knowledge base, so requests are isolated from each other and never mutate shared state. The per-request inference budget defaults to 30 seconds (`--timeout`, 0 disables).

The jar is also double-clickable: when launched without a console it shows a small status window with the server URL and Open in Browser / Stop buttons (`--no-gui` suppresses it).

The website server (`nelumbo-web-server-<version>.jar`, the `website` module) serves the same REST endpoints plus an LSP editor service over WebSocket and the public pages of [nelumbo.nl](https://nelumbo.nl).

## MCP Server

The `mcp` module is a [Model Context Protocol](https://modelcontextprotocol.io) stdio server that lets LLM agents author and verify self-contained `.nl` decision models. It exposes four tools:

| Tool          | Purpose                                                                                                                      |
|---------------|------------------------------------------------------------------------------------------------------------------------------|
| `eval_nl`     | Parse and evaluate a `.nl` model; returns structured diagnostics and per-query results, including whether embedded expected results matched |
| `search_docs` | Keyword search over the bundled language documentation                                                                       |
| `get_example` | List or fetch the bundled working `.nl` examples                                                                             |
| `new_model`   | Return a commented, self-verifying skeleton model to start from                                                              |

Build the shaded jar:

```sh
./gradlew :mcp:mcpJar
```

Register it with an MCP client, for example Claude Code:

```sh
claude mcp add nelumbo -- java -jar $(pwd)/mcp/build/libs/nelumbo-mcp-server-<version>.jar
```

The server communicates over stdio (stdout is the JSON-RPC channel, logging goes to stderr). The query evaluation deadline defaults to 10 seconds and can be changed with `--eval-deadline-ms <ms>`.

The intended agent workflow: start from `new_model`, look up syntax with `search_docs` and `get_example`, then iterate with `eval_nl` until it reports `ok=true` with all expected query results matched. The bundled example [`clubFees.nl`](src/main/resources/org/modelingvalue/nelumbo/examples/clubFees.nl) was authored end-to-end this way.

## Examples

### Family Relations Example

```text
Person    :: Object
Male      :: Person
Female    :: Person

FactType  ::= pc(<Person>,<Person>)   // parent-child

Person    ::= p(<Person>),   // parent
              c(<Person>),   // child
              a(<Person>),   // ancestor
              d(<Person>),   // descendant
              m(<Person>),   // mother
              f(<Person>)    // father

Person a, b, c
Male   y
Female x
           
c(a)=b  <=>  pc(a,b)
p(a)=b  <=>  pc(b,a)
m(a)=b  <=>  E[x](c(x)=a & b=x)
f(a)=b  <=>  E[y](c(y)=a & b=y)

a(a)=b  <=>  d(b)=a
d(a)=c  <=>  c(a)=c |
             E[b](d(a)=b & c(b)=c)

Male   ::= Hendrik, Bernhard, Claus, Willem
Female ::= Wilhelmina, Juliana, Beatrix, Maxima, Amalia

pc(Hendrik, Juliana)
pc(Wilhelmina, Juliana)
pc(Juliana, Beatrix)
pc(Bernhard, Beatrix)
pc(Beatrix, Willem)
pc(Claus, Willem)
pc(Willem, Amalia)
pc(Maxima, Amalia)

a(Amalia)=a         ? [(a=Beatrix),(a=Maxima),(a=Hendrik),(a=Bernhard),(a=Juliana),(a=Claus),(a=Willem),(a=Wilhelmina)][..]    
m(Amalia)=Maxima    ? [()][]
m(Amalia)=Willem    ? [][()]
m(Amalia)=a         ? [(a=Maxima)][..]
f(Amalia)=a         ? [(a=Willem)][..]
f(m(f(Amalia)))=a   ? [(a=Bernhard)][..]
```

### Fibonacci Example

```text
Integer ::= fib(<Integer>)

Integer n, f

fib(n)=f <=> f=n                 if n<=1,
             f=fib(n-1)+fib(n-2) if n>1  

fib(0)=f       ? [(f=0)][..]
fib(1)=f       ? [(f=1)][..]
fib(5)=f       ? [(f=5)][..]
fib(100)=f     ? [(f=36#22r8fozas3n8w3)][..]
fib(1000)=f    ? [(f=36#18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf)][..]

    
```

## Releasing

Releases are created automatically by CI. The process works as follows:

1. You edit `RELEASE_NOTES.md` in the repository root with the release notes for the upcoming version.
   You can use `${version}` and `${version-num}` for the version tag and version number respectively.
2. You merge your changes to `master`, that's it!
3. The build workflow runs. On success, the Gradle `mvgTagger` plugin creates and pushes a version tag (e.g. `v1.2.3`).
4. The tag push triggers the release workflow, which downloads the build artifacts and creates a GitHub release with the
   contents of `RELEASE_NOTES.md`.

The release includes the following artifacts:

- **Editor** — standalone Swing-based editor (`nelumbo-ide-${version-num}.jar`)
- **Eclipse plugin** — dropins-based plugin (`nelumbo-eclipse-plugin-${version-num}.jar`)
- **IntelliJ plugin** — LSP4IJ-based plugin (`nelumbo-intellij-plugin-${version-num}.zip`)
- **Slides** — presentation slides (`nelumbo-slides-${version-num}.zip`)

If `RELEASE_NOTES.md` is absent, GitHub will auto-generate release notes from the commit log.

## Contributing

Contributions and feedback are welcome! Please open issues or pull requests on GitHub.

## License

This project is licensed under the terms of the LICENSE file provided in the repository.

## Support

For questions or support, please use the GitHub Issues page.
