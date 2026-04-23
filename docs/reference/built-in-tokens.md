# Built-in tokens and pattern holes

When you declare a pattern with `::=`, everything in angle brackets (`<...>`) is a **hole** — a placeholder that the parser fills in with a matching fragment of input. This page catalogues the kinds of hole that are built into Nelumbo and used throughout the standard library.

```
Integer ::= <NUMBER>                  @org.modelingvalue.nelumbo.integers.NInteger
Integer ::= <Integer> + <Integer>     #40
Set<E>  ::= { <(> <E> <,> , <)*> }    @org.modelingvalue.nelumbo.collections.NSet
```

A hole is either a **lexical-token hole** (matches a single lexer token class), a **type hole** (matches an expression of some declared type), or a **structural marker** (builds repetition and grouping).

---

## Lexical-token holes

These holes match a single token produced by the lexer, not an expression built from rules. They are Nelumbo's escape hatch to raw input.

### `<NUMBER>`

Matches an integer literal: one or more decimal digits, possibly in bases other than decimal.

```
Integer ::= <NUMBER>   @org.modelingvalue.nelumbo.integers.NInteger
```

Examples that match: `0`, `1`, `42`, `1000000`.

Nelumbo also supports **base-N literals** of the form `N#digits`, where `N` is the base (up to 36) and `digits` are digits in that base. This is how arbitrary-precision integers are printed for readability once they get large:

```
36#22r8fozas3n8w3
36#18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf
```

Both of the above are base-36 integer literals — the values of `fib(100)` and `fib(1000)` respectively.

### `<DECIMAL>`

Matches a decimal-point literal for rationals:

```
Rational ::= <DECIMAL>   @org.modelingvalue.nelumbo.rationals.Rational
```

Examples that match: `0.0`, `-1.5`, `3.14`.

### `<STRING>`

Matches a double-quoted string literal:

```
String ::= <STRING>   @org.modelingvalue.nelumbo.strings.NString
```

Examples that match: `""`, `"foo"`, `"Hello, World!"`.

### `<NAME>`

Matches an identifier token — the kind of lexical fragment used for literal enumerations and parameters in pattern transformations:

```
Root ::= attr <Type> <NAME> <Type>  #100
```

From `transformation.nl`. The `<NAME>` hole captures a raw identifier that the transformation can use as the name of a new attribute.

`<NAME>` is primarily useful inside pattern transformations where you need the user's literal identifier to build new declarations on the fly. See [`../guides/language-transformations.md`](../guides/language-transformations.md).

---

## Type holes — `<T>`

A type hole matches an expression of type `T`. The expression may itself be complex — it can be any pattern declared for type `T`, including the one currently being declared (allowing recursive patterns like `<Integer> + <Integer>`).

```
Integer ::= <Integer> + <Integer>    #40
Integer ::= fib(<Integer>)
Boolean ::= even(<Integer>)
```

A type hole may carry a **precedence annotation**:

```
T ::= <Boolean#5> ? <T> : <T>
```

From `ternary.nl`. The `#5` restricts what can appear in the hole to expressions with precedence at least 5 — used to avoid ambiguity with surrounding operators. See [`precedence-and-associativity.md`](precedence-and-associativity.md).

A type hole may also be marked with a visibility modifier:

```
Integer ::= <hidden Integer>  && <Integer>  #35
Integer ::= <visible Integer> &  <Integer>  #35
```

From `hidden.nl`. These restrict the hole to hidden or visible variables respectively. See [`visibility.md`](visibility.md).

---

## Variable holes — `<Variable>`

`<Variable>` matches a variable binding site, not a general expression. It is what quantifiers use to introduce a bound variable:

```
Boolean ::= E[<(> <Variable#100> <,> , <)+>](<Boolean#0>)
            @org.modelingvalue.nelumbo.logic.ExistentialQuantifier
```

From `logic.nl`. The quantifier `E[x, y, z](body)` expects binding sites, not pre-existing expressions, in the bracketed position.

Binding variables declared with `<Variable>` are scoped to the surrounding pattern — they do not leak outside.

---

## Structural markers — repetition and grouping

Inside a pattern, special angle-bracketed operators build repeating and optional sub-structures. They are markers, not holes — they do not consume input themselves; they shape how the surrounding holes consume input.

| Marker | Role |
|---|---|
| `<(>` ... `<)>`   | Pure grouping — no repetition |
| `<(>` ... `<)?>`  | Optional group — zero or one occurrence |
| `<(>` ... `<)*>`  | Zero-or-more, separator-delimited |
| `<(>` ... `<)+>`  | One-or-more, separator-delimited |
| `<,>`             | Separator placeholder inside a repetition |
| `<|>`             | Alternation inside a group |

### Examples

```
Repetition  ::= { <(> <Integer> <,> , <)*> }     // {}, {5}, {3,5,7}
Option      ::= <(> super <)?> fast              // "fast" or "super fast"
Alternation ::= <(> A <|> B <|> C <)>            // "A", "B", or "C"
```

In a repetition, the literal that follows `<,>` is the actual separator token (`,` in the `Repetition` example above). The marker `<,>` says "here is the separator"; the real character is whatever follows.

### Generic collections

The collections module uses repetition to define `Set` and `List`:

```
Set<E>  ::= { <(> <E> <,> , <)*> }
List<E> ::= [ <(> <E> <,> , <)*> ]
```

Both say "zero or more `<E>`, comma-separated, wrapped in brackets." `{}` and `[]` are empty literals for set and list respectively. `{1,2,3}` matches, as does `[1]`, as does `{}`.

---

## Precedence and native-binding annotations

Two trailing annotations can attach to a pattern declaration (not to individual holes, unless the hole is a type hole):

### `#N` — precedence

```
Integer ::= <Integer> + <Integer>  #40
```

Declares that this pattern has precedence 40. See [`precedence-and-associativity.md`](precedence-and-associativity.md).

### `@ClassName` — native binding

```
Integer ::= <NUMBER>  @org.modelingvalue.nelumbo.integers.NInteger
```

Binds the pattern to a Java class that implements its semantics. See [`native-api.md`](native-api.md) (Phase 4).

---

## See also

- [`grammar.md`](grammar.md) — where pattern declarations fit in the overall grammar
- [`precedence-and-associativity.md`](precedence-and-associativity.md) — the `#N` system
- [`visibility.md`](visibility.md) — the `hidden`/`visible` modifiers
- [`../guides/language-transformations.md`](../guides/language-transformations.md) — how `<NAME>` and other holes are used in transformations
