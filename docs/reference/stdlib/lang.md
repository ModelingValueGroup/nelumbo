# `nelumbo.lang`

The bootstrap layer. Every other `.nl` file — including `logic.nl` itself — is written in the syntax that `lang.nl` declares. It is the meta-language for the meta-language.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/lang/lang.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/lang/lang.nl) — 52 lines.

**Import:**

```
import nelumbo.lang
```

You rarely import `lang` explicitly: `nelumbo.logic` already imports it, so every program that imports any of the higher stdlib modules gets `lang` transitively. Importing `lang` directly is only useful when you want the bare meta-language — no `Boolean`, no `=`, no `&` — for example, to define your own logic from scratch.

---

## What this module is

`lang.nl` is unique among the stdlib modules in two ways:

1. **Self-bootstrap.** It uses the very constructs it declares (`::`, `::=`, `<...>`, `<(>...<)+>`, etc.) to describe itself. The Java parser has just enough hard-coded knowledge to read `lang.nl`; from there on, every other file is parsed using the patterns this file installs.
2. **No rules, no facts, no queries.** It contains no `<=>`, no `fact`, no `?`. Those forms are themselves declared here — they cannot be used until `nelumbo.logic` adds them. `lang.nl` only declares types and patterns.

Everything in this module is either a `NATIVE` declaration (the tokenizer / runtime supplies the implementation) or a `::=` pattern bound to a class under `nelumbo.lang.*` or `nelumbo.patterns.*`.

---

## Token types

```
SINGLEQUOTE   :: NATIVE     // '
SEMICOLON     :: NATIVE     // ;
COMMA         :: NATIVE     // ,
LEFT          :: NATIVE     // [\(\[\{]
RIGHT         :: NATIVE     // [\)\]\}]
STRING        :: NATIVE     // "([^"\\]|\\[\s\S])*"
DECIMAL       :: NATIVE     // -?[0-9]+\.[0-9]+
NUMBER        :: NATIVE     // -?[0-9]+(#[0-9a-zA-Z]+)?
NAME          :: NATIVE     // [a-zA-Z_][0-9a-zA-Z_]*
OPERATOR      :: NATIVE     // (?!//)[~!@#$%^&*=+|:<>.?/-]+
NEWLINE       :: NATIVE     // \R
BEGINOFFILE   :: NATIVE
ENDOFFILE     :: NATIVE
```

Each `:: NATIVE` declares a token type produced by the tokenizer. The comment that follows is the regex (or short description) of what the lexer matches:

| Token         | Matches                                                  |
|---|---|
| `SINGLEQUOTE` | `'`                                                       |
| `SEMICOLON`   | `;`                                                       |
| `COMMA`       | `,`                                                       |
| `LEFT`        | `(`, `[`, or `{`                                          |
| `RIGHT`       | `)`, `]`, or `}`                                          |
| `STRING`      | a double-quoted string with backslash escapes             |
| `DECIMAL`     | a signed decimal-point number (used by `<Rational>`)      |
| `NUMBER`      | a signed integer, optionally with a `#`-prefixed base form |
| `NAME`        | an identifier — letter / underscore, then alphanumerics    |
| `OPERATOR`    | one or more operator characters (`! @ # $ % ^ & * = + | : < > . ? / -`), but not starting with `//` |
| `NEWLINE`     | a line terminator                                         |
| `BEGINOFFILE` | synthetic token at the start of input                     |
| `ENDOFFILE`   | synthetic token at the end of input                       |

These names are visible to user code wherever a lexical-token hole is expected: `<NUMBER>`, `<DECIMAL>`, `<STRING>`, `<NAME>`, `<OPERATOR>`, `<LEFT>`, `<RIGHT>`, `<COMMA>`, etc. See [`built-in-tokens.md`](../built-in-tokens.md) for how they are used in user-facing pattern declarations.

---

## Object types

```
Object        :: NATIVE
Type          :: Object
Variable      :: Object
Root          :: Object             // top of the statement hierarchy
Functor       :: Root                // language pattern with a type, e.g. a function or operator
Pattern       :: Object #PATTERN     // syntactic pattern
Namespace     :: Object              // local scope
RootNamespace :: Root, Namespace
```

The seven core types of the object model:

| Type            | Role                                                                                            |
|---|---|
| `Object`        | Native root of the type hierarchy. Everything is an `Object`.                                    |
| `Type`          | A type itself. Used wherever the meta-language needs to talk about types — e.g., `Type T`.       |
| `Variable`      | A logical variable binding site. Quantifiers and rules use `<Variable>` holes.                   |
| `Root`          | A top-level statement of a `.nl` file — an `import`, a `::=` declaration, a `fact`, a query, …  |
| `Functor`       | A `::=`-declared pattern with a type. Subtype of `Root` because `::=` is itself a top-level form. |
| `Pattern`       | A syntactic pattern fragment (a literal, a hole, an alternation, etc.). The `#PATTERN` annotation tags it for internal handling. |
| `Namespace`     | A local scope (the contents of `{ ... }`).                                                       |
| `RootNamespace` | A `Root` that is also a `Namespace`. The scope-block top-level form `{ ... }` belongs here.      |

The hierarchy is what gives the rest of the language a place to hang. A user-declared type like `Integer :: Object` slots in under the same `Object` declared here; `Boolean :: Object` (from `logic.nl`) does the same.

---

## Namespace grammar — what a `.nl` file is

```
Namespace     ::= <BEGINOFFILE> <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> <ENDOFFILE>
                  @nelumbo.lang.Namespace

RootNamespace ::= { <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> }
                  @nelumbo.lang.Namespace
```

A `Namespace` (whole file) is `BEGINOFFILE`, then zero or more newline-terminated items, then `ENDOFFILE`. A `RootNamespace` (scope block) is the same shape wrapped in `{ ... }`. Each item is either a single `Root` statement or a `List<Root>` — a comma-separated list of statements, which is how `Integer ::= a, b, c` and `fact f1, f2, f3` work.

Both forms are bound to the same native class `nelumbo.lang.Namespace`.

This is also the file that justifies the existence of `List<Root>` as a parseable value: scope blocks and files use it, and the collections module (`List<E>`) extends the same shape to user data.

---

## Pattern grammar — the meta-grammar of patterns

This is the part of `lang.nl` that describes the syntax of `::=` patterns themselves. Every angle-bracketed construct you write in a pattern declaration is parsed by one of these alternatives.

```
Pattern ::= <NAME>                                                          @nelumbo.patterns.TokenTextPattern,
            <STRING>                                                        @nelumbo.patterns.TokenTextPattern,
            <OPERATOR>                                                      @nelumbo.patterns.TokenTextPattern,
            <SEMICOLON>                                                     @nelumbo.patterns.TokenTextPattern,
            <SINGLEQUOTE>                                                   @nelumbo.patterns.TokenTextPattern,
            <COMMA>                                                         @nelumbo.patterns.TokenTextPattern,
            "<" <Variable#100> ">"                                          @nelumbo.patterns.TokenTextPattern,
            "<" "(" ">" <(> <(> <Pattern#100> <)+> <,> "<" "|" ">" <)+>
                "<" ")" ">"                                                 @nelumbo.patterns.AlternationPattern,
            "<" "(" ">" <(> <Pattern#100> <)+>
                <(> "<" "," ">" <(> <Pattern#100> <)+> <)?>
                "<" ")" <(> * <|> + <)> ">"                                 @nelumbo.patterns.RepetitionPattern,
            "<" "(" ">" <(> <Pattern#100> <)+> "<" ")" "?" ">"              @nelumbo.patterns.OptionalPattern,
            <LEFT> <(> <Pattern#100> <)+> <RIGHT>                           @nelumbo.patterns.SequencePattern,
            "<" <(> <(> "visible" <|> "hidden" <)> <)?> <Type#100>
                <(> # <NUMBER> <)?> ">"                                     @nelumbo.patterns.NodeTypePattern
```

Reading these alternatives in order:

| Alternative                                                  | Native class            | What it matches                                |
|---|---|---|
| `<NAME>`, `<STRING>`, `<OPERATOR>`, `<SEMICOLON>`, `<SINGLEQUOTE>`, `<COMMA>` | `TokenTextPattern` | A literal token — the text appears verbatim. |
| `"<" <Variable#100> ">"`                                     | `TokenTextPattern`      | A `<Variable>` hole — binding site for a quantifier or rule. |
| `"<(" ... "<|>" ... ")>"`                                    | `AlternationPattern`    | An alternation group: `<(> A <\|> B <\|> C <)>`. |
| `"<(" ... "<,>" ... ")>" + or *`                             | `RepetitionPattern`     | A repetition group, with optional separator: `<(> P <,> , <)+>` or `... <)*>`. |
| `"<(" ... ")?>"`                                             | `OptionalPattern`       | An optional group: `<(> super <)?>`.            |
| `<LEFT> ... <RIGHT>`                                         | `SequencePattern`       | A bracketed sequence — any of `(...)`, `[...]`, `{...}`. |
| `"<" (visible\|hidden)? <Type#100> (# <NUMBER>)? ">"`        | `NodeTypePattern`       | A type hole `<T>`, optionally with visibility (`<hidden T>`) and precedence (`<T#5>`). |

The escaping is delicate: `"<"`, `"("`, `"|"`, `","`, `")"`, `"?"`, `">"`, `"+"`, `"*"` all have meaning *inside* a pattern, so when this file wants to write them as literal text it quotes them. This is the meta-syntax describing itself.

Note also the `#100` precedence on the inner `<Pattern#100>` and `<Variable#100>` holes. Precedence 100 is effectively "atomic" — it prevents an inner pattern from being mistaken for a continuing operator expression. See [`precedence-and-associativity.md`](../precedence-and-associativity.md).

---

## Root grammar — top-level statements

```
Root ::= "import" <(> <(> <NAME> <,> . <)+> <,> , <)+>                                     @nelumbo.lang.Import,
         <Root#0> ::> <RootNamespace>                                                       @nelumbo.lang.Transform,
         <(> "hidden" <)?> <Type#100> <(> <NAME> <,> , <)+>                                 @nelumbo.lang.Variable,
         <NAME> <(> < <Type#100> > <)?> :: <(> <Type#100> <,> , <)+> <(> # <NAME> <)?>      @nelumbo.lang.Type,
         <(> "private" <)?> <Type#100> ::= <(> <(> <Pattern#100> <)+>
             <(> # <NUMBER> <)?>
             <(> @ <(> <NAME> <,> . <)+> <)?>
             <,> , <)+>                                                                     @nelumbo.lang.Functor
```

Five top-level statement forms:

| Statement                                                              | Native class           |
|---|---|
| `import M`, `import M1, M2`, `import a.b.c`                            | `nelumbo.lang.Import`   |
| `<Root> ::> { ... }` — pattern transformation                          | `nelumbo.lang.Transform`|
| `(hidden)? T v`, `T v1, v2, v3` — variable declaration                 | `nelumbo.lang.Variable` |
| `T<P>? :: S1, S2 (#tag)?` — type declaration                           | `nelumbo.lang.Type`     |
| `(private)? T ::= P1 (#N)? (@class)?, P2, ...` — pattern declaration   | `nelumbo.lang.Functor`  |

The `Functor` alternative is the dense one: it carries the optional `private` modifier, the optional `#N` precedence annotation, the optional `@`-bound native class, and the comma-separated list of patterns — all in a single declaration. This is the syntax every other stdlib file uses to declare anything.

The `import` alternative permits both comma-separated lists (`import M1, M2`) and dotted module paths (`import a.b.c`).

The `::>` transformation (Transform) takes any `<Root>` shape on the left and a `RootNamespace` (a `{ ... }` block of declarations) on the right. See [`language-transformations.md`](../guides/language-transformations.md). It is declared here, but the mechanism is only useful in conjunction with the rest of the language.

`fact`, `<=>`, and `?` are **not** in this list — they are added by `nelumbo.logic`. Without `logic`, a `lang`-only file can declare types, patterns, variables, imports, and transformations, but it cannot assert facts, write rules, or run queries.

---

## Generic parenthesisation

```
Type T

T ::= (<T>)   @nelumbo.lang.Parenthesized
```

The last two lines of `lang.nl` declare a single generic type parameter `T` and a single generic pattern: parenthesisation. Because `T` is a type variable, this one declaration instantiates for every concrete type that arises later — `(Integer)`, `(Boolean)`, `(Person)`, `(Set<Integer>)`, all of them.

This is also the canonical demonstration of `Type T` (the generic type parameter mechanism), which `collections.nl` reuses to declare `Set<E>` and `List<E>`.

---

## Exports summary

After `import nelumbo.lang`, the following are visible:

| Kind             | Names |
|---|---|
| Token types      | `SINGLEQUOTE`, `SEMICOLON`, `COMMA`, `LEFT`, `RIGHT`, `STRING`, `DECIMAL`, `NUMBER`, `NAME`, `OPERATOR`, `NEWLINE`, `BEGINOFFILE`, `ENDOFFILE` |
| Object types     | `Object`, `Type`, `Variable`, `Root`, `Functor`, `Pattern`, `Namespace`, `RootNamespace` |
| File / scope     | `Namespace`, `RootNamespace` (`{ ... }` blocks) |
| Pattern forms    | literal tokens, `<Variable>`, alternation `<(>...<|>...<)>`, repetition `<(>...<)+>` / `<)*>`, optional `<(>...<)?>`, sequence `<LEFT>...<RIGHT>`, type holes `<T>` / `<hidden T>` / `<T#N>` |
| Top-level forms  | `import`, `::>`, variable declaration, `::` (type), `::=` (pattern) |
| Generic          | `Type T`, parenthesisation `(T)` |

All bindings are native — there is no in-language rule (`<=>`) in this module.

---

## See also

- [`grammar.md`](../grammar.md) — the user-facing view of the same grammar
- [`built-in-tokens.md`](../built-in-tokens.md) — how the token types above appear inside `::=` patterns
- [`precedence-and-associativity.md`](../precedence-and-associativity.md) — the `#N` annotation declared by the `Functor` Root form
- [`visibility.md`](../visibility.md) — the `private` and `hidden` modifiers declared by the `Functor` and `Variable` Root forms
- [`logic.md`](logic.md) — the next layer up, which adds `Boolean`, `fact`, `<=>`, and `?`
- [`langTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/langTest.nl) — minimal smoke test that imports `nelumbo.lang` on its own
