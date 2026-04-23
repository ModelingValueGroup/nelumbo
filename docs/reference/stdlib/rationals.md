# `nelumbo.rationals`

Exact rational arithmetic — no floating-point rounding. Layered on top of `nelumbo.integers`.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/rationals/rationals.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/rationals/rationals.nl) — 47 lines.

**Import:**

```
import nelumbo.rationals
```

`nelumbo.rationals` imports `nelumbo.integers` (and thus transitively `nelumbo.logic`). You get integers, logic, and rationals with a single import.

---

## Type

```
Rational :: Object
```

A value of type `Rational` is an exact rational number — an integer numerator over an integer denominator, held in reduced form. No floating-point, no rounding.

---

## Literals

Rationals have a decimal-literal form and a constructor form:

```
Rational ::= <DECIMAL>              @...Rational
Rational ::= r(<Integer>)
Rational ::= r(<Integer>/<Integer>)
```

- `<DECIMAL>` — literals with a decimal point: `0.0`, `-1.5`, `3.14`, `1.0`
- `r(n)` — promote an integer `n` to a rational
- `r(n/d)` — the rational `n/d` from two integer components

The two `r(...)` forms are defined in Nelumbo on top of a private native primitive:

```
private Boolean ::= iir(<Integer>, <Integer>, <Rational>)  @...IntegersRational

r(x)   = a  <=>  iir(x, 1, a)
r(x/y) = a  <=>  iir(x, y, a)
```

---

## Arithmetic

The same shape as `integers`, over `Rational`:

| Pattern | `#N` | Meaning |
|---|---|---|
| `<Rational> + <Rational>` | 40 | addition |
| `<Rational> - <Rational>` | 40 | subtraction |
| `<Rational> * <Rational>` | 50 | multiplication |
| `<Rational> / <Rational>` | 50 | **exact** division |
| `- <Rational>`            | 80 | unary negation |
| `| <Rational> |`          | 35 | absolute value |

Defined the same way as integers, in terms of two private native primitives:

```
private Boolean ::= add(<Rational>,<Rational>,<Rational>)   @...Add
private Boolean ::= mult(<Rational>,<Rational>,<Rational>)  @...Multiply

a + b = c  <=>  add(a, b, c)
a - b = c  <=>  add(c, b, a)
a * b = c  <=>  mult(a, b, c)
a / b = c  <=>  mult(c, b, a)

-a = b  <=>  0.0 - a = b

|a| = b  <=>  b = a   if a >= 0.0,
              b = -a  if a <  0.0
```

Note the literal `0.0` (not `0`) in the rules. Rationals and integers are **distinct types**; the rules are typed, and you cannot silently mix them.

### Division is exact

Unlike `Integer` division, rational division does not truncate:

```
21.0 / 10.0 = a   ? [(a=2.1)][..]
21.0 / 10.0 = 2.0 ? [][()]
```

The first query produces the exact result `2.1`. The second asserts `21.0 / 10.0 = 2.0` and correctly receives a falsehood.

---

## Comparison

```
<Rational>  >  <Rational>   #30   @...GreaterThan
<Rational> "<" <Rational>   #30
<Rational> "<=" <Rational>  #30
<Rational> >=  <Rational>   #30
```

As in `integers`, only `>` is native; `<`, `<=`, `>=` are defined in Nelumbo:

```
a <  b  <=>  b > a
a <= b  <=>  a < b | a = b
a >= b  <=>  a > b | a = b
```

---

## Integer-to-rational conversion

```
private Boolean ::= iir(<Integer>, <Integer>, <Rational>)  @...IntegersRational

r(x)   = a  <=>  iir(x, 1, a)
r(x/y) = a  <=>  iir(x, y, a)
```

`r(5) = a` yields `a = 5.0`. `r(1/3) = a` yields the exact rational `1/3`. `iir` works in multiple directions like other primitives: given a rational, you can ask for compatible integer numerator/denominator pairs — subject to the usual constraints around the fact/falsehood/unknown trichotomy.

There is no silent promotion from `Integer` to `Rational`. If you want to mix them in an expression, you must call `r(...)` explicitly, by design — it keeps the type system honest and surfaces conversions where they happen.

---

## Exports summary

After `import nelumbo.rationals`, in addition to everything from `integers` and `logic`:

- Type: `Rational`
- Literals: `<DECIMAL>`
- Constructors: `r(x)`, `r(x/y)`
- Operators: `+`, `-` (binary and unary), `*`, `/`, `|x|`, `<`, `<=`, `>`, `>=` — all on `Rational`

`add`, `mult`, `iir` are `private`.

---

## See also

- [`integers.md`](integers.md) — the module `rationals` builds on, and whose structure it mirrors
- [`rationalsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/rationalsTest.nl) — executable specification
