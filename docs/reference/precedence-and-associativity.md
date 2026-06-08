# Precedence and associativity

When you declare a pattern with `::=`, you can attach a **precedence annotation** `#N`:

```
Integer ::= <Integer> + <Integer>  #40
Integer ::= <Integer> * <Integer>  #50
Integer ::=           - <Integer>  #80
```

This page explains what `#N` means, how Nelumbo chooses which parse to use when multiple patterns could apply, and how precedence annotations in holes (`<T#N>`) interact with the pattern's own precedence.

---

## The rule

**Higher `#N` binds tighter.** A pattern with precedence 50 (like `*`) applies before one with precedence 40 (like `+`), so `a + b * c` parses as `a + (b * c)`, not `(a + b) * c`.

The full precedence ladder of the standard arithmetic and logical operators:

| Operator | `#N` | Source |
|---|---|---|
| `<->`               | 16 | `logic.nl` |
| `->`                | 18 | `logic.nl` |
| `\|`                 | 20 | `logic.nl` |
| `&`                 | 22 | `logic.nl` |
| `!`                 | 25 | `logic.nl` |
| `<`, `<=`, `>`, `>=`, `!=` | 30 | `logic.nl` and `integers.nl` |
| `\|x\|` (absolute)    | 35 | `integers.nl` |
| `+`, `-` (binary)   | 40 | `integers.nl` |
| `*`, `/`            | 50 | `integers.nl` |
| unary `-`           | 80 | `integers.nl` |

---

## Associativity

For binary operators, Nelumbo parses left-associatively by default: `a - b - c` parses as `(a - b) - c`. If you want right-associative parsing for a custom operator, you control it via the precedence annotations of the **holes** rather than a separate associativity flag (see [Hole precedence](#hole-precedence) below).

---

## Hole precedence

A type hole can carry its own precedence annotation:

```
T ::= <Boolean#5> ? <T> : <T>
```

From `ternary.nl`. `<Boolean#5>` says "this hole accepts a Boolean expression with precedence at least 5." An expression with lower precedence (like `p | q` at #20, or an unparenthesised conjunction) would need parentheses to appear in the hole.

This is how you express "tighter-binding" requirements on specific hole positions — for example, to say the condition of a ternary must be a primary expression, not a raw disjunction.

Hole precedence also controls associativity for repeated operators. For a standard left-associative binary operator like `+`:

```
Integer ::= <Integer> + <Integer>  #40
```

Nelumbo's default treatment of the holes (with respect to the pattern's own `#40`) produces left-associative parsing. If you want right-associative parsing for a custom operator, you can tune the hole annotations to get the desired grouping.

---

## Why operators can share a precedence level

Multiple operators may have the same `#N`. This is normal:

- `+` and `-` are both at 40 — they parse left-to-right as sibling operators
- `*` and `/` are both at 50 — same
- `<`, `<=`, `>`, `>=`, `!=` are all at 30 — sibling comparisons

Operators at the same precedence combine under the ordinary left-to-right rule: `1 + 2 - 3` is `(1 + 2) - 3`.

---

## Prefix vs binary precedence

Unary operators typically carry a higher `#N` than their binary counterparts:

```
Integer ::= <Integer> - <Integer>  #40       // binary subtraction
Integer ::=           - <Integer>  #80       // unary negation
```

Binary subtraction binds looser (40) than unary negation (80), so `-a - b` parses as `(-a) - b`: the unary `-` grabs `a` first, then binary `-` applies.

---

## Precedence of quantifiers

Quantifiers `E[...]` and `A[...]` use hole precedence on their body rather than a single pattern precedence:

```
Boolean ::= E[<(> <Variable#100> <,> , <)+>](<Boolean#0>)
            @org.modelingvalue.nelumbo.logic.ExistentialQuantifier
```

The body `<Boolean#0>` accepts a Boolean expression at the lowest precedence, so `E[x](a & b | c -> d)` parses with the full expression inside, as intended. The variable list `<Variable#100>` demands precedence 100 — effectively "must be a bare variable," no composite expressions.

---

## When you need to think about this

For ordinary Nelumbo code — writing rules, queries, and tests over the standard library — you almost never need to assign precedences. The stdlib has already done the work.

You will need to assign precedences when:

- **You define a new operator.** Pick a `#N` from the ladder above to slot into. If your operator is arithmetic-like, mirror the arithmetic levels. If it is comparison-like, pick 30.
- **You need to override precedence in a specific hole.** The `#N` on a type hole is how you do it; `<Integer#80>` requires the hole to contain an expression as tight as unary minus.
- **You get ambiguous parses.** The error messages will point at competing patterns; adjusting `#N` on one (or tightening a hole) usually resolves it.

### A pragmatic recipe for picking `#N`

1. Find the existing stdlib operator your new operator most resembles in "how tightly should it bind."
2. Copy its `#N`.
3. If your operator should bind tighter than that reference, add 5 or 10.
4. If looser, subtract 5 or 10.

The gaps in the ladder (16, 18, 20, 22, 25, 30, 35, 40, 50, 80) are intentional — they leave room for new operators without renumbering the world.

---

## See also

- [`operators.md`](operators.md) — complete catalogue with precedences
- [`grammar.md`](grammar.md) — where `#N` fits syntactically
- [`built-in-tokens.md`](built-in-tokens.md) — type holes and hole precedences
