# `nelumbo.rationals`

Exact rational arithmetic — no floating-point rounding. Mirrors the shape of `nelumbo.integers` over a separate `Rational` type, plus integer-to-rational conversion.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/rationals/rationals.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/rationals/rationals.nl) — 46 lines.

**Import:**

```
import nelumbo.rationals
```

`nelumbo.rationals` imports `nelumbo.integers` (and thus, transitively, `nelumbo.logic`). One import gets you logic, integers, and rationals.

---

## Type

```
Rational :: Object
```

A `Rational` is an exact rational — integer numerator over integer denominator, held in reduced form. There is no floating-point and no rounding.

`Rational` is **distinct** from `Integer`. The rules below are typed, and there is no silent promotion: to mix an integer with a rational you must call `r(...)` explicitly.

---

## Literals

```
Rational ::= <(> - <)?> <[> <NUMBER> . <NUMBER> <]>  @nelumbo.rationals.Rational,
             r(<Integer>),
             r(<Integer>/<Integer>)
```

- Decimal-point literal — an optional `-`, then two `<NUMBER>` tokens (`[0-9]+`) joined by a `.`. Examples: `0.0`, `1.0`, `-1.5`, `3.14`. There is no separate `<DECIMAL>` lexer token; the literal is assembled at the pattern level, and the mandatory decimal point is what distinguishes it from an `Integer` literal.
- `r(<Integer>)` — promote an integer.
- `r(<Integer>/<Integer>)` — build a rational from numerator/denominator integers.

The two `r(...)` forms reduce to a private native predicate `iir` — a `@NelumboMethod` on `nelumbo.rationals.Rationals` (the same class that carries `add`, `mult`, and `gt`):

```
private Boolean ::= ...,
                    iir(<Integer>,<Integer>,<Rational>)  @nelumbo.rationals.Rationals

Integer x, y
Rational a

r(x)   = a   <=>  iir(x, 1, a)
r(x/y) = a   <=>  iir(x, y, a)
```

`iir(n, d, q)` holds when `q` is the rational `n/d`. Like the other relational primitives, it is bidirectional: with both integers bound it builds (or verifies) the rational; with the rational bound and both integers free it yields the reduced `n/d`; and with one integer plus the rational bound it solves the other integer via the cross-multiplication `n*qd == qn*d` (subject to the usual three-valued constraints).

---

## Arithmetic

```
Rational ::= <Rational> - <Rational>   #40,
             <Rational> + <Rational>   #40,
                        - <Rational>   #80,
             <Rational> * <Rational>   #50,
             <Rational> / <Rational>   #50,
                        | <Rational> | #35
```

| Pattern                   | `#N` | Meaning            |
|---|---|---|
| `<Rational> + <Rational>` | 40   | addition           |
| `<Rational> - <Rational>` | 40   | subtraction        |
| `<Rational> * <Rational>` | 50   | multiplication     |
| `<Rational> / <Rational>` | 50   | **exact** division |
| `- <Rational>`            | 80   | unary negation     |
| `\| <Rational> \|`        | 35   | absolute value     |

Defined exactly like the integer counterparts, in terms of two private natives:

```
private Boolean ::= add(<Rational>,<Rational>,<Rational>)   @nelumbo.rationals.Rationals,
                    mult(<Rational>,<Rational>,<Rational>)  @nelumbo.rationals.Rationals

Rational a, b, c

a + b = c   <=>  add(a, b, c)
a - b = c   <=>  add(c, b, a)
a * b = c   <=>  mult(a, b, c)
a / b = c   <=>  mult(c, b, a)

- a = b     <=>  0.0 - a = b

|a| = b     <=>  b =  a   if a >= 0.0,
                b = -a   if a < 0.0
```

The literal `0.0` (not `0`) in the negation rule keeps the operands in `Rational` — `0` is an `Integer` and the typed `-` would not match.

### Division is exact

Where integer division truncates, rational division does not:

```
20.0 / 10.0 = 2.0   ? [()][]
21.0 / 10.0 = a     ? [(a=2.1)][..]
21.0 / 10.0 = 2.0   ? [][()]
```

The middle query returns the exact result `2.1`. The third asserts the wrong answer and correctly receives a falsehood.

---

## Comparison

```
Boolean ::= <Rational> ">"  <Rational>   #30,
            <Rational> "<"  <Rational>   #30,
            <Rational> "<=" <Rational>   #30,
            <Rational>  >=  <Rational>   #30
```

Same arrangement as `integers`: the operators carry no `@` binding — the single native comparison is the private `gt` helper, and the rest are defined in Nelumbo.

```
a >  b  <=>  gt(a, b)
a <  b  <=>  gt(b, a)
a <= b  <=>  a < b | a = b
a >= b  <=>  a > b | a = b
```

---

## Exports summary

Added to what `nelumbo.integers` (and `nelumbo.logic`) already export:

| Kind        | Names |
|---|---|
| Type        | `Rational`                                                          |
| Literals    | decimal-point literal `<(> - <)?> <[> <NUMBER> . <NUMBER> <]>`      |
| Constructors| `r(x)`, `r(x/y)`                                                    |
| Operators   | `+`, `-` (binary and unary), `*`, `/`, `\|x\|`, `<`, `<=`, `>`, `>=` on `Rational` |
| Constructors| `r(x)`, `r(x/y)` — integer-pair / rational conversion                |

`add`, `mult`, `gt`, and `iir` are all `private`; the public surface is the operators and the `r(...)` constructors.

---

## See also

- [`integers.md`](integers.md) — the module `rationals` builds on, and whose structure it mirrors
- [`rationalsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/rationalsTest.nl) — executable specification
