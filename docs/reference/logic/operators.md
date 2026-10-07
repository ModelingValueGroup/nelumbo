# Operators

> **Level:** `nelumbo.logic`. Available after `import nelumbo.logic`, or transitively through any package (`nelumbo.integers` and up).

This page is a catalogue of the operators and statement forms declared in `nelumbo.logic` (the three-valued logic layer). The declarative operators `::`, `::=` and `::>` come from `nelumbo.lang` and are on the [`nelumbo.lang` grammar](../lang/grammar.md) page. Operators from `integers`, `rationals`, `strings`, and `collections` are documented on the per-package pages.

None of these operators are hardcoded in the Java core. Everything below is declared by a `::=` pattern in `logic.nl` and bound to a native class via `@`.

---

## Execution-driving forms (from `nelumbo.logic`)

These are also `Root ::=` patterns, but they are declared in `logic.nl`, not `lang.nl` — they depend on `Boolean`, which `logic.nl` introduces. Without `import nelumbo.logic`, a file cannot use `fact`, `<=>`, or `?`.

### `fact` — ground-truth assertion

```
fact E
fact E1, E2, E3
```

Asserts one or more comma-separated ground-truth facts. See [`statements.md`](statements.md#facts).

### `<=>` — rule (bi-implication)

```
L <=> R
```

Asserts that `L` holds exactly when `R` holds. Multiple rules may share the same `L`; their results merge. See [`writing-rules.md`](writing-rules.md).

### `?` — query / test

```
E ?               // query: run and print
E ? [F][N]        // test: run and compare to expected result
```

See [`test-expression-semantics.md`](test-expression-semantics.md).

---

## Logical operators (from `nelumbo.logic`)

Once you `import nelumbo.logic`, these become available as Boolean-valued operators. Their precedence annotations appear in parentheses.

### `!` — negation (`#25`)

```
!p
```

`!p` is provable as a **fact** when `p` has been proven as a **falsehood**, and vice versa. This is genuine logical negation — not "not provable." See [`three-valued-logic.md`](three-valued-logic.md).

### `&` — conjunction (`#22`)

```
p & q
```

`p & q` is a fact when both `p` and `q` are facts. It is a falsehood when at least one of `p` or `q` is a falsehood — **even if the other is unknown**. Proving `q` as a falsehood is enough to conclude `p & q` is false, regardless of `p`. See [`logicTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/logicTest.nl) for the full truth table.

### `|` — disjunction (`#20`)

```
p | q
```

`p | q` is a fact when at least one of `p` or `q` is a fact — even if the other is unknown. It is a falsehood when both are falsehoods.

### `->` — implication (`#18`)

```
p -> q
```

Defined in `logic.nl` as `!p | q`. Classical material implication.

### `<->` — bi-implication (`#16`)

```
p <-> q
```

Defined in `logic.nl` as `(p -> q) & (q -> p)`.

### `=` — equality (`#30`)

```
a = b
```

Identity comparison. Declared in `logic.nl` as `Boolean ::= <Object> = <Object> #30 @nelumbo.logic.NIs`. The public native `NIs` handles the general case; a separate private native `Equal` backs the private `eq(<Literal>, <Literal>)` predicate, which the rule `l1 = l2 <=> eq(l1, l2)` invokes for literal-to-literal comparison. A second rule, `l1 = f1 <=> f1 = l1`, inverts a literal-equals-function query into function-equals-literal form, so equality is usable in either direction: `fib(5) = f` and `5 = fib(n)` both work.

### `!=` — inequality (`#30`)

```
a != b
```

Defined in `logic.nl` as `!(a = b)`.

### `E[...](...)` — existential quantifier

```
E[x](p)
E[x, y](p)
```

`E[x](p)` is a fact when there exists some binding of `x` for which `p` holds. The bound variables (`x`, `y`, ...) must be declared elsewhere; inside the body they take on the quantifier's role instead of their outer role. Bound variables are not visible outside the quantifier.

Example from `belasting.nl`:

```
E[i, a]((het inkomen van p is i euro) & (p mag a euro aftrekken) & x=(i-a)/2)
```

### `A[...](...)` — universal quantifier

```
A[x](p)
A[x, y](p)
```

`A[x](p)` is a fact when `p` holds for **every** binding of `x`. Dual to `E[]`.

From `logicTest.nl`:

```
E[a](a=T1 | a=T2)   ? [()][]
A[a](a=T1 & a=T2)   ? [][()]
```

---

## Guards — `if`

```
L <=> R if G
L <=> R1 if G1, R2 if G2
```

Not an operator in the same sense, but syntactically significant. `if G` attaches a **guard** to the right-hand side of a rule: the rule only contributes under bindings for which `G` holds. Multiple `if`-guarded clauses can appear separated by commas (see [`writing-rules.md`](writing-rules.md)).

---

## Punctuation

Punctuation from `nelumbo.lang` (supertype and variable lists, `{ }`, `( )`, `//`) is in the [grammar](../lang/grammar.md#punctuation).

| Symbol | Meaning |
|---|---|
| `,` in a rule RHS | Shorthand for repeating the LHS across multiple rule clauses (see [`writing-rules.md`](writing-rules.md)) |
| `,` in a `fact` block | Separates asserted facts |

---

## Special identifiers

The `nelumbo.lang` object hierarchy (`Object`, `Type`, `Root`, ...) is in the [grammar](../lang/grammar.md#special-identifiers).

| Name | Origin | Meaning |
|---|---|---|
| `Boolean`, `FactType`, `Literal`, `Function` | `nelumbo.logic` | The logic-layer types. `Boolean` is the type of truth-valued expressions; `FactType` is a `Boolean` subtype for ground-truth relations (see [`statements.md`](statements.md#facttype-declarations)). |
| `true`, `false`, `unknown` | `nelumbo.logic` | The three Boolean values. |

---

## Precedence summary

Full rules are on [`precedence-and-associativity.md`](../lang/precedence-and-associativity.md). The quick version:

| Operator | Precedence | Notes |
|---|---|---|
| `<->`  | 16 | lowest |
| `->`   | 18 |  |
| `\|`    | 20 |  |
| `&`    | 22 |  |
| `!`    | 25 | prefix |
| `<`, `<=`, `>`, `>=`, `!=` | 30 | comparisons |
| `+`, `-` (binary) | 40 | integer/rational |
| `*`, `/` | 50 |  |
| unary `-` | 80 |  |
| `\|x\|` | 35 | absolute value |

Higher `#N` binds tighter. See the per-module reference pages for the precedence of arithmetic and string operators.
