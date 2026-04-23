# `nelumbo.integers`

Arbitrary-precision integer arithmetic and comparison. Most examples in the codebase import this module.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/integers/integers.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/integers/integers.nl) — 37 lines.

**Import:**

```
import nelumbo.integers
```

`nelumbo.integers` imports `nelumbo.logic` itself, so you get the logical connectives transitively. No separate import of `logic` is needed.

---

## Type

```
Integer :: Object
```

A value of type `Integer` is an arbitrary-precision signed integer, backed by Java's `BigInteger`. There is no overflow.

---

## Literals

```
Integer ::= <NUMBER>   @...NInteger
```

`<NUMBER>` accepts ordinary decimal literals (`0`, `42`, `-1`) and base-N literals (`36#22r8fozas3n8w3`) for large values. See [`built-in-tokens.md`](../built-in-tokens.md#number).

---

## Arithmetic

| Pattern | `#N` | Meaning |
|---|---|---|
| `<Integer> + <Integer>` | 40 | addition |
| `<Integer> - <Integer>` | 40 | subtraction |
| `<Integer> * <Integer>` | 50 | multiplication |
| `<Integer> / <Integer>` | 50 | integer division |
| `- <Integer>`           | 80 | unary negation |
| `| <Integer> |`         | 35 | absolute value |

The four binary operators are defined in terms of two private native primitives:

```
private Boolean ::= add(<Integer>,<Integer>,<Integer>)   @...Add
private Boolean ::= mult(<Integer>,<Integer>,<Integer>)  @...Multiply

a + b = c  <=>  add(a, b, c)
a - b = c  <=>  add(c, b, a)
a * b = c  <=>  mult(a, b, c)
a / b = c  <=>  mult(c, b, a)
```

Notice how `a - b = c` is expressed as `add(c, b, a)` — addition with arguments shuffled. This is the payoff of having `add` be a three-argument **relation** rather than a two-argument function: any one of the three can be missing, and the native implementation supplies it if the other two are present. Subtraction is not a separate operation; it is addition viewed from a different angle.

The same trick applies to division: `a / b = c` is `mult(c, b, a)`.

### Unary negation and absolute value

```
-a = b   <=>  0 - a = b

|a| = b  <=>  b = a   if a >= 0,
              b = -a  if a <  0
```

Unary negation reduces to subtraction from zero. Absolute value is defined by two rules with mutually exclusive guards covering the entire integer domain — a clean, contradiction-free split.

### Bidirectional queries

Because arithmetic is relational, every operator works in any direction:

```
10 + 11 = a    ? [(a=21)][..]        // compute the sum
a + 11 = 21    ? [(a=10)][..]        // solve for a
10 + a = 21    ? [(a=11)][..]        // solve for a
```

All three queries run against the same rule and are equally fast. This generalises to the other operators, including `|a|`:

```
|a| = 10   ? [(a=-10),(a=10)][(a=0),..]
```

Two facts (`-10` and `10`), with the falsehoods side listing `a=0` explicitly and leaving `..` for the rest. See [`integersTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/integersTest.nl) for the full coverage.

### Integer division

Integer division truncates toward zero, and a division query with a non-exact dividend produces an empty facts side — no integer makes the equation true:

```
21 / 10 = a   ? [][..]
21 / 10 = 2   ? [][()]
```

---

## Comparison

| Pattern | `#N` | Native |
|---|---|---|
| `<Integer> > <Integer>`  | 30 | `GreaterThan` (native) |
| `<Integer> < <Integer>`  | 30 | defined in Nelumbo |
| `<Integer> <= <Integer>` | 30 | defined in Nelumbo |
| `<Integer> >= <Integer>` | 30 | defined in Nelumbo |

Only `>` is implemented natively. The other three are defined in terms of `>` and `=`:

```
a < b   <=>  b > a
a <= b  <=>  a < b | a = b
a >= b  <=>  a > b | a = b
```

This is another small illustration of the meta-language at work: the stdlib implements one primitive per concept and derives the rest in Nelumbo.

### Comparisons produce both sides

Because Nelumbo's reasoner produces facts and falsehoods:

```
a > 0   ? [..][(a=0),..]        // (a=0) is a proven falsehood of a>0
a >= 0  ? [(a=0),..][..]        // (a=0) is a proven fact of a>=0
```

The reasoner does not enumerate all positive (or non-positive) integers, so both sides remain open — but `a=0` is placed on the correct side in each case, demonstrating that bindings are classified three-valuedly even when the full set is not enumerated.

---

## Exports summary

After `import nelumbo.integers`, the following are added to what `nelumbo.logic` already exports:

- Type: `Integer`
- Literal: `<NUMBER>` recognition producing `Integer` values
- Operators: `+`, `-` (binary and unary), `*`, `/`, `|x|`, `<`, `<=`, `>`, `>=`

`add` and `mult` are `private` — they are not visible to importers. Use `+`, `-`, `*`, `/` instead.

---

## See also

- [`logic.md`](logic.md) — the module `integers` builds on
- [`rationals.md`](rationals.md) — the same shape, but over `Rational`
- [`../writing-rules.md`](../writing-rules.md) — how the rewriting idiom (`a-b=c <=> add(c,b,a)`) works
- [`integersTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/integersTest.nl) — executable specification
- [`fibonacci.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/fibonacci.nl) — non-trivial use
