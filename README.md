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
- [Building](#building)
- [IDE Plugins](#ide-plugins)
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
* Written in Java

## Building

Requires Java 21 or later.

Build everything (core library, LSP server, and all IDE plugins):

```sh
./gradlew build
```

Build individual components:

```sh
./gradlew jar                          # core library
./gradlew :lsp:server:serverJar        # LSP server (shaded jar)
./gradlew :lsp:plugins:eclipse:jar     # Eclipse plugin (includes LSP server)
./gradlew :lsp:plugins:intellij:build  # IntelliJ plugin
```

Run tests:

```sh
./gradlew test
```

## IDE Plugins

Nelumbo has LSP-based editor plugins for multiple IDEs:

| IDE | Path | Details |
|---|---|---|
| Eclipse | [`lsp/plugins/eclipse`](lsp/plugins/eclipse) | Dropins-based plugin with semantic highlighting |
| IntelliJ | [`lsp/plugins/intellij`](lsp/plugins/intellij) | IntelliJ platform plugin |
| VS Code | [`lsp/plugins/vscode`](lsp/plugins/vscode) | VS Code extension |
| Neovim | [`lsp/plugins/neovim`](lsp/plugins/neovim) | Neovim LSP configuration |

See the README in each plugin directory for installation instructions.

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

1. Edit `RELEASE_NOTES.md` in the repository root with the release notes for the upcoming version.
2. Merge your changes to `master`.
3. The build workflow runs. On success, the Gradle `mvgTagger` plugin creates and pushes a version tag (e.g. `v1.2.3`).
4. The tag push triggers the release workflow, which downloads the build artifacts and creates a GitHub release with the contents of `RELEASE_NOTES.md`.

The release includes the following artifacts:
- **Editor** — standalone Swing-based editor (`nelumbo-*-editor.jar`)
- **Eclipse plugin** — dropins-based plugin (`eclipse-nelumbo-plugin-*.jar`)
- **IntelliJ plugin** — LSP4IJ-based plugin (`intellij-nelumbo-plugin-*.zip`)
- **Slides** — presentation slides (`nelumbo-slides.zip`)

If `RELEASE_NOTES.md` is absent, GitHub will auto-generate release notes from the commit log.

## Contributing

Contributions and feedback are welcome! Please open issues or pull requests on GitHub.

## License

This project is licensed under the terms of the LICENSE file provided in the repository.

## Support

For questions or support, please use the GitHub Issues page.
