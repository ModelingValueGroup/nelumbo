# `nelumbo.integers`

Arbitrary-precision integer arithmetic and comparison.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/integers/integers.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/integers/integers.nl) — 36 lines.

**Import:**

```
import nelumbo.integers
```

`nelumbo.integers` imports `nelumbo.logic`, so the Boolean type, connectives, and equality come along automatically — there is no need to import `logic` separately.

---

## Type

```
Integer :: Object
```

A value of type `Integer` is an arbitrary-precision signed integer. There is no overflow.

---

## Literals

```
Integer ::= <NUMBER>   @nelumbo.integers.NInteger
```

`<NUMBER>` is the language-level token defined in [`lang.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/lang/lang.nl) as `-?[0-9]+(#[0-9a-zA-Z]+)?`. It admits:

- ordinary signed decimals: `0`, `42`, `-1`
- base-N literals: `<digits>#<digits-in-base>`, where the leading number is the base — e.g., `16#ff`, `36#abc`

The native class `NInteger` parses the matched text into a `BigInteger`-backed value.

---

## Arithmetic

```
Integer ::= <Integer> - <Integer>   #40,
            <Integer> + <Integer>   #40,
                      - <Integer>   #80,
            <Integer> * <Integer>   #50,
            <Integer> / <Integer>   #50,
                      | <Integer> | #35
```

| Pattern               | `#N` | Meaning           |
|---|---|---|
| `<Integer> + <Integer>` | 40 | addition          |
| `<Integer> - <Integer>` | 40 | subtraction       |
| `<Integer> * <Integer>` | 50 | multiplication    |
| `<Integer> / <Integer>` | 50 | integer division  |
| `- <Integer>`           | 80 | unary negation    |
| `\| <Integer> \|`       | 35 | absolute value    |

Six patterns, **two** native primitives. The four binary operators reduce to `add` or `mult`:

```
private Boolean ::= add(<Integer>,<Integer>,<Integer>)   @nelumbo.integers.Add,
                    mult(<Integer>,<Integer>,<Integer>)  @nelumbo.integers.Multiply

Integer a, b, c

a + b = c   <=>  add(a, b, c)
a - b = c   <=>  add(c, b, a)
a * b = c   <=>  mult(a, b, c)
a / b = c   <=>  mult(c, b, a)
```

Subtraction is not a separate native. It is `add` viewed from a different angle: `a - b = c` is the same proposition as `c + b = a`. The same trick gives integer division as `mult(c, b, a)`.

Unary negation and absolute value are defined on top of subtraction:

```
- a = b   <=>  0 - a = b

|a| = b   <=>  b =  a   if a >= 0,
              b = -a   if a < 0
```

The two guarded clauses of `|a|` cover the integer domain without overlap.

### Bidirectional evaluation

Because `add` and `mult` are relational, any one of the three operands can be the unknown. From `integersTest.nl`:

```
10 + 11 = a   ? [(a=21)][..]
a  + 11 = 21  ? [(a=10)][..]
10 + a  = 21  ? [(a=11)][..]

10 - 11 = a   ? [(a=-1)][..]
a  - 11 = -1  ? [(a=10)][..]
10 - a  = -1  ? [(a=11)][..]

|a| = 10      ? [(a=-10),(a=10)][(a=0),..]
|10| = a      ? [(a=10)][..]
```

Absolute value with the result fixed returns both pre-images on the facts side and lists `a=0` as a proven falsehood (with `..` for everything else).

### Integer division

Integer division truncates toward zero. A query with a non-exact dividend gets an empty facts side — no integer makes the equation true:

```
20 / 10 = 2    ? [()][]
20 / 10 = 3    ? [][()]
21 / 10 = a    ? [][..]
21 / 10 = 2    ? [][()]
```

---

## Comparison

```
Boolean ::= <Integer>  >   <Integer>   #30  @nelumbo.integers.GreaterThan,
            <Integer> "<"  <Integer>   #30,
            <Integer> "<=" <Integer>   #30,
            <Integer>  >=  <Integer>   #30
```

| Pattern                  | `#N` | Native / definition          |
|---|---|---|
| `<Integer> > <Integer>`  | 30   | `nelumbo.integers.GreaterThan` |
| `<Integer> < <Integer>`  | 30   | defined in `integers.nl`     |
| `<Integer> <= <Integer>` | 30   | defined in `integers.nl`     |
| `<Integer> >= <Integer>` | 30   | defined in `integers.nl`     |

Only `>` is native. The other three reduce to `>` and `=`:

```
a <  b  <=>  b > a
a <= b  <=>  a < b | a = b
a >= b  <=>  a > b | a = b
```

Comparisons participate in three-valued classification. Asking `a > 0` with `a` unbound does not enumerate the positive integers, but it does place `a = 0` on the correct side:

```
a >  0   ? [..][(a=0),..]    // (a=0) is a proven falsehood of a>0
a >= 0   ? [(a=0),..][..]    // (a=0) is a proven fact of a>=0
```

The angle-bracketed forms `"<"` and `"<="` in the source are quoted because `<` is also the syntax marker for pattern holes (`<Integer>`); the quotes tell the tokenizer to treat `<` and `<=` as ordinary operator text.

---

## Exports summary

Added to what `nelumbo.logic` already exports:

| Kind     | Names |
|---|---|
| Type     | `Integer`                                                       |
| Literal  | `<NUMBER>`                                                      |
| Operators| `+`, `-` (binary and unary), `*`, `/`, `\|x\|`, `<`, `<=`, `>`, `>=` |

`add` and `mult` are `private` and are not visible to importers.

---

## See also

- [`logic.md`](logic.md) — the module `integers` builds on
- [`rationals.md`](rationals.md) — the same shape, lifted to exact rationals
- [`writing-rules.md`](../writing-rules.md) — how the `a-b=c <=> add(c,b,a)` idiom works
- [`integersTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/integersTest.nl) — executable specification
- [`fibonacci.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/fibonacci.nl) — non-trivial use of `+`, `-`, and `<=`
