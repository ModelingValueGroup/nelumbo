# Grammar

This page describes the grammar of Nelumbo itself — the syntax you write in a `.nl` file. It is the foundation the rest of the reference refers back to.

Nelumbo is a **meta-language**, and the meta-ness goes all the way down. Even the grammar described on this page is itself defined in `.nl` files:

- The pattern meta-grammar (`<T>`, `<(>...<)+>`, `<(>...<)?>`, `<,>`, `<|>`, …), the top-level declaration forms (`import`, `T :: S`, `T ::= P`, `T v`, `::>`, scope blocks), and the underlying object hierarchy (`Object`, `Type`, `Variable`, `Root`, …) are all declared in [`lang.nl`](../reference/stdlib/lang.md). The Java core contains just enough hand-coded parsing to load `lang.nl`; from that point on, the user's own `::=` patterns are what parse every subsequent file.
- The three execution-driving statement forms (`fact`, `<=>`, `?`), along with the `Boolean` type and every logical operator, are declared in [`logic.nl`](../reference/stdlib/logic.md).

So "the grammar of Nelumbo" is really *the set of patterns that `lang.nl` and `logic.nl` install when they are loaded*. This page documents that surface; the per-module reference pages document where each piece is declared.

---

## A `.nl` file

A Nelumbo file is a sequence of **declarations** and **statements**, read top to bottom. Order matters: a name must be declared before it is used.

The top-level constructs are:

| Form | Meaning | Defined in |
|---|---|---|
| `import M` | Import module `M` | |
| `T :: S` | Declare type `T` as a subtype of `S` | [Types](#type-declarations) below |
| `T ::= P` | Declare pattern `P` producing a value of type `T` | [Patterns](#pattern-declarations) below |
| `T v` | Declare logical variable `v` of type `T` | [Variables](#variable-declarations) below |
| `L <=> R` | Rule: `L` bi-implies `R` | [`writing-rules.md`](writing-rules.md) |
| `fact E` | Assert ground-truth fact `E` | [Facts](#facts) below |
| `E ?` | Query: run `E` and print the result | [`test-expression-semantics.md`](test-expression-semantics.md) |
| `E ? [F][N]` | Test: query `E` and compare to expected facts `F` / falsehoods `N` | [`test-expression-semantics.md`](test-expression-semantics.md) |
| `{ ... }` | Scope block — declarations inside are local | [`visibility.md`](visibility.md) |
| `L ::> { ... }` | Pattern transformation | [`language-transformations.md`](../guides/language-transformations.md) |

Comments are `//` to end of line. Whitespace is not significant.

---

## Type declarations

```
T :: S
T :: S1, S2
```

Declares `T` as a type. Everything to the right of `::` is the list of **supertypes**. `Object` is the root of the type hierarchy and never needs to be declared.

Examples from the codebase:

```
Person :: Object
Male   :: Person
Female :: Person
LLM    :: Smart
Person :: Smart, Living
```

Multiple inheritance is supported: `Person :: Smart, Living` says a `Person` is both a `Smart` and a `Living`.

A bare type can also be introduced for use as a generic parameter:

```
Type E
```

See [`generics.md`](../guides/generics.md) for details.

---

## Pattern declarations

```
T ::= pattern
T ::= pattern1, pattern2, pattern3
```

The `::=` operator adds a new way to produce a value of type `T` to the language's syntax. The left-hand side is the type being produced; the right-hand side is one or more **patterns**, separated by commas.

A pattern is a mix of:

- **Literal tokens** — identifiers, symbols, keywords, quoted operators — that must appear exactly
- **Holes** of the form `<T>` — placeholders filled by an expression of type `T`
- **Built-in token holes** like `<NUMBER>`, `<STRING>`, `<NAME>` — see [`built-in-tokens.md`](built-in-tokens.md)
- **Variable holes** `<Variable>` — used by binding forms like quantifiers
- **Repetition and grouping markers** `<(>`, `<)>`, `<)*>`, `<)+>`, `<)?>`, `<,>`, `<|>` — see [below](#repetition-and-grouping)
- An optional **precedence annotation** `#N` — see [`precedence-and-associativity.md`](precedence-and-associativity.md)
- An optional **native binding** `@fully.qualified.JavaClass` — see [`native-api.md`](native-api.md)

Examples from the standard library:

```
Integer ::= <NUMBER>                                    @org.modelingvalue.nelumbo.integers.NInteger
Integer ::= <Integer> + <Integer>   #40
Integer ::= <Integer> - <Integer>   #40
Integer ::=           - <Integer>   #80
Integer ::=           | <Integer> | #35
Boolean ::= <Integer> "<" <Integer> #30
```

Note:

- `"<"` is an ordinary literal. Operator-like characters that would otherwise confuse the parser are written quoted.
- A pattern may begin with a hole (`<Integer> - <Integer>`), a literal (`- <Integer>`), or a mix.
- The same type may be declared with many `::=` patterns; each adds another alternative. They do not conflict with each other as long as they parse unambiguously.

### Literal enumerations

A pattern may list several literals as alternatives:

```
Male   ::= Hendrik, Bernhard, Claus, Willem
Female ::= Wilhelmina, Juliana, Beatrix, Maxima, Amalia
Lidwoord ::= de, het
```

Each listed name becomes a constant of the declared type. They are literals, not variables.

### Repetition and grouping

Inside a pattern, angle-bracketed operators build repeating or optional sub-structures:

| Marker | Meaning |
|---|---|
| `<(>` ... `<)>`   | Group — pure grouping, no repetition |
| `<(>` ... `<)?>`  | Optional group — zero or one occurrence |
| `<(>` ... `<)*>`  | Zero-or-more with separator |
| `<(>` ... `<)+>`  | One-or-more with separator |
| `<,>`             | Separator inside a repetition |
| `<|>`             | Alternation marker inside a group |
| `<[>` ... `<]>`   | Connected-token group — adjacent tokens, no whitespace allowed between them |

Examples:

```
Repetition  ::= { <(> <Integer> <,> , <)*> }       // {3,5,7}  or {}
Option      ::= <(> super <)?> fast                // "fast" or "super fast"
Alternation ::= <(> A <|> B <|> C <)>              // "A", "B", or "C"
```

`collections.nl` uses this for generic sets and lists:

```
Set<E>  ::= { <(> <E> <,> , <)*> }
List<E> ::= [ <(> <E> <,> , <)*> ]
```

### Connected-token groups: `<[> ... <]>`

By default whitespace between tokens is insignificant, so a pattern like `<NUMBER> - <NUMBER> - <NUMBER>` accepts `2025-01-01` *and* `2025 - 01 - 01`. The `<[>` … `<]>` form switches that off for the tokens it encloses: every token between `<[>` and `<]>` must be **directly adjacent in the source** — no whitespace, no comment, nothing — to the next.

The brackets themselves are pure meta-syntax: like the other `<(>`-style markers, they do not appear in the parsed source.

```
Date     ::= <[> <NUMBER> - <NUMBER> - <NUMBER> <]>                     @nelumbo.datetime.NDate
Rational ::= <(> - <)?> <[> <NUMBER> . <NUMBER> <]>                     @nelumbo.rationals.Rational
Integer  ::= <(> - <)?> <[> <NUMBER> <(> "#" <(> <(> <NUMBER> <|> <NAME> <)> <)+> <)?> <]>  @nelumbo.integers.NInteger
```

These accept `2025-01-01`, `3.14`, and `16#FF`, but reject `2025 - 01 - 01`, `3 . 14`, and `16 # FF`. This is what distinguishes a literal form (a date, a decimal, a radix-prefixed integer) from a structural expression that happens to use the same operators (`<Date> - <Period>`, `<Integer> - <Integer>`).

A practical rule of thumb: use `<[>` … `<]>` whenever the absence of whitespace is the *only* thing telling the parser "this is one literal, not an expression."

---

## FactType declarations

A `FactType` pattern declares a relation whose instances can be asserted as ground truth:

```
FactType ::= pc(<Person>,<Person>)                    // family.nl
FactType ::= friends(<Person>,<Person>)               // friends.nl
FactType ::= het inkomen van <Person> is <Integer> euro  // belasting.nl
```

A fact type looks like any other pattern, but values built with it are not computed by rules — they are either asserted directly (see below) or they are not. This separates **ground truth** from **derived relations**.

### Facts

Facts are asserted with the `fact` keyword:

```
fact pc(Hendrik, Juliana),
     pc(Wilhelmina, Juliana),
     pc(Juliana, Beatrix)

fact het inkomen van Piet is 50000 euro
```

`fact` introduces one or more comma-separated ground-truth assertions. Once asserted, they are available to the query engine as proven facts.

---

## Variable declarations

```
T v
T v1, v2, v3
```

Declares logical variables of type `T`. Variables declared at the top level are global to the file (or scope); variables declared inside `E[...]` or `A[...]` are local to the quantifier.

```
Integer n, f          // fibonacci.nl
Person  a, b, c       // family.nl
Male    y
Female  x
```

A logical variable is a placeholder for whatever value satisfies the surrounding rule or query. It is not a storage cell and does not have a single value.

---

## Expressions

An expression is anything built from:

- **Literals** — `5`, `3.14`, `"hello"`, `true`, `Hendrik`
- **Variables** — `n`, `a`, `p`
- **Pattern applications** — anything built from a `::=` pattern, such as `fib(n)`, `a+b`, `|n|`, `{1,2,3}`, `het inkomen van Piet is 50000 euro`
- **Logical operators from `nelumbo.logic`** — `&`, `|`, `!`, `->`, `<->`, `E[]`, `A[]`, `=`, `!=`

Expressions have types (propagated through pattern declarations) and can appear anywhere the grammar expects an expression of the matching type.

---

## Scope blocks

Curly braces `{ ... }` introduce a lexical scope:

```
{
   Aa :: Object
   Aa ::= XXX
   Aa x
   x=XXX ? [(x=XXX)][..]
}
```

Names declared inside a scope are not visible outside it. Imports, types, patterns, variables, and rules can all be scoped. See [`visibility.md`](visibility.md) for the full rules, including `private` and `hidden`.

---

## Pattern transformations

The `::>` operator defines a **language pattern transformation** — a macro-like expansion from one pattern into a block of declarations and rules.

```
attr OT AN AT ::> {
    AT       ::= <OT>.AN
    Root     ::= <OT>.AN := <AT>
    private FactType ::= AN(<OT>,<AT>)
    ...
}
```

This is Nelumbo's mechanism for building higher-level DSLs on top of the core. It is covered in [`language-transformations.md`](../guides/language-transformations.md) rather than here, because transformations are an advanced feature that deserves a full guide.

---

## Queries, tests, and bare facts

Three forms drive execution:

```
E                  // bare expression — treated as a fact if E is a FactType instance
E ?                // query — run the reasoner, print the result
E ? [F][N]         // test — query and compare; pass iff result matches
```

See [`test-expression-semantics.md`](test-expression-semantics.md) for the precise comparison rules.

---

## See also

- [`operators.md`](operators.md) — the complete list of operators
- [`built-in-tokens.md`](built-in-tokens.md) — `<NUMBER>`, `<STRING>`, `<NAME>`, `<Variable>`
- [`precedence-and-associativity.md`](precedence-and-associativity.md) — how `#N` works
- [`visibility.md`](visibility.md) — `private`, `hidden`, `visible`, and `{ }` scopes
