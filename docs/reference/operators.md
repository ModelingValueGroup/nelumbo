# Operators

This page is a complete catalogue of the operators contributed by `nelumbo.lang` (the syntactic bootstrap) and `nelumbo.logic` (the three-valued logic layer). Operators from `integers`, `rationals`, `strings`, and `collections` are documented on the per-module stdlib pages.

None of these operators are hardcoded in the Java core. Everything below is declared by a `::=` pattern in `lang.nl` or `logic.nl` and bound to a native class via `@`.

---

## Declarative operators (from `nelumbo.lang`)

These shape the program itself. They are not values; you do not compute with them. All are declared as `Root ::=` or `Pattern ::=` patterns in `lang.nl`.

### `::` — type subtyping

```
T :: S
T :: S1, S2
```

Declares `T` as a type whose direct supertypes are listed on the right. Everything ultimately derives from `Object`. See [`grammar.md`](grammar.md#type-declarations).

### `::=` — pattern

```
T ::= pattern
```

Declares a new way to produce a value of type `T`. Extends the language's syntax. Multiple `::=` declarations for the same type are allowed and accumulate. See [`grammar.md`](grammar.md#pattern-declarations).

### `::>` — pattern transformation

```
L ::> { ... declarations ... }
```

Expands an occurrence of pattern `L` into the declarations in the block. A macro-like mechanism for building DSLs on top of Nelumbo. See [`../guides/language-transformations.md`](../guides/language-transformations.md).

---

## Execution-driving forms (from `nelumbo.logic`)

These are also `Root ::=` patterns, but they are declared in `logic.nl`, not `lang.nl` — they depend on `Boolean`, which `logic.nl` introduces. Without `import nelumbo.logic`, a file cannot use `fact`, `<=>`, or `?`.

### `fact` — ground-truth assertion

```
fact E
fact E1, E2, E3
```

Asserts one or more comma-separated ground-truth facts. See [`grammar.md`](grammar.md#facts).

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

`p & q` is a fact when both `p` and `q` are facts. It is a falsehood when at least one of `p` or `q` is a falsehood — **even if the other is unknown**. Proving `q` as a falsehood is enough to conclude `p & q` is false, regardless of `p`. See [`logicTest.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/logicTest.nl) for the full truth table.

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

| Symbol | Meaning |
|---|---|
| `,` in a rule RHS | Shorthand for repeating the LHS across multiple rule clauses (see [`writing-rules.md`](writing-rules.md)) |
| `,` in a `fact` block | Separates asserted facts |
| `,` in a supertype list | Separates supertypes |
| `,` in a variable declaration | Separates variable names |
| `{ }` | Scope block — see [`visibility.md`](visibility.md) |
| `( )` | Grouping inside an expression |
| `//` | Line comment |

---

## Special identifiers

| Name | Origin | Meaning |
|---|---|---|
| `Object`, `Type`, `Variable`, `Root`, `Functor`, `Pattern`, `Namespace`, `RootNamespace` | `nelumbo.lang` | The core object hierarchy. `Root` is the entry-point production for top-level statements; `Type` is used as a generic parameter introducer (`Type T`). |
| `Boolean`, `FactType`, `Literal`, `Function` | `nelumbo.logic` | The logic-layer types. `Boolean` is the type of truth-valued expressions; `FactType` is a `Boolean` subtype for ground-truth relations (see [`grammar.md`](grammar.md#facttype-declarations)). |
| `true`, `false`, `unknown` | `nelumbo.logic` | The three Boolean values. |

---

## Precedence summary

Full rules are on [`precedence-and-associativity.md`](precedence-and-associativity.md). The quick version:

| Operator | Precedence | Notes |
|---|---|---|
| `<->`  | 16 | lowest |
| `->`   | 18 |  |
| `|`    | 20 |  |
| `&`    | 22 |  |
| `!`    | 25 | prefix |
| `<`, `<=`, `>`, `>=`, `!=` | 30 | comparisons |
| `+`, `-` (binary) | 40 | integer/rational |
| `*`, `/` | 50 |  |
| unary `-` | 80 |  |
| `|x|` | 35 | absolute value |

Higher `#N` binds tighter. See the per-module reference pages for the precedence of arithmetic and string operators.
