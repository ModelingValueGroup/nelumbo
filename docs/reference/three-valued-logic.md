# Three-valued logic

Nelumbo is a three-valued logic language. This page describes the values, their behaviour under the logical operators, and why the design was made this way. It is the semantic foundation for [`test-expression-semantics.md`](test-expression-semantics.md) and for most of [`writing-rules.md`](writing-rules.md).

If you have not yet read [`../getting-started/reading-a-test.md`](../getting-started/reading-a-test.md), read it first. This page treats the same material at reference depth.

---

## The three values

An expression in Nelumbo, under a given binding of its free variables, is one of:

- **Fact** — proven true
- **Falsehood** — proven false
- **Unknown** — neither proven true nor proven false

These are not merely *"we haven't checked yet"* values. They represent the epistemic state after reasoning has run. An expression is unknown only when the reasoner has genuinely been unable to place it on either side.

Crucially, a binding can never be both a fact and a falsehood simultaneously. If the rules in a program would force such a situation, the program is **inconsistent** (see [`writing-rules.md`](writing-rules.md)).

---

## Why three values instead of two

Two-valued logic languages have a choice when the reasoner can't prove a predicate:

- Refuse to answer (sound but often useless)
- Treat "not proven" as "false" (useful but unsound — this is Prolog's *negation as failure*)

Nelumbo takes neither option. It treats **facts and falsehoods as separate things to prove**. `!p` is a fact precisely when `p` is a falsehood, not when `p` has merely not been proved true. This means:

- Logical laws that fail under negation-as-failure (like `!!p = p`) hold in Nelumbo
- A program can distinguish "X is not a parent of Y" (a proven negative) from "we have no information about whether X is a parent of Y" (unknown)
- Incompleteness of the knowledge base is represented explicitly rather than silently miscast as falsity

The price is that every operator has to be defined on three inputs, not two. Fortunately most of them extend in the natural way.

---

## The truth tables

All tables below are the ones exercised in [`logicTest.nl`](../../src/main/resources/org/modelingvalue/nelumbo/tests/logicTest.nl). You can run that file to verify them.

### Negation — `!`

| `p`       | `!p`      |
|-----------|-----------|
| `true`    | `false`   |
| `false`   | `true`    |
| `unknown` | `unknown` |

Negation is its own inverse: `!!p` is equivalent to `p` in all cases.

### Conjunction — `&`

|           | `true`    | `false`   | `unknown` |
|-----------|-----------|-----------|-----------|
| `true`    | `true`    | `false`   | `unknown` |
| `false`   | `false`   | `false`   | `false`   |
| `unknown` | `unknown` | `false`   | `unknown` |

Key row: `unknown & false` is **`false`**, not unknown. Because `false` dominates conjunction, the other operand's value cannot change the result. Nelumbo recognises this and does not pessimistically degrade to unknown.

### Disjunction — `|`

|           | `true`    | `false`   | `unknown` |
|-----------|-----------|-----------|-----------|
| `true`    | `true`    | `true`    | `true`    |
| `false`   | `true`    | `false`   | `unknown` |
| `unknown` | `true`    | `unknown` | `unknown` |

Dual to `&`: `unknown | true` is **`true`**, because `true` dominates disjunction.

### Implication — `->`

Defined in `logic.nl` as `!p | q`, so the table follows from negation and disjunction:

|           | `q = true` | `q = false` | `q = unknown` |
|-----------|------------|-------------|---------------|
| `p = true`    | `true`  | `false`  | `unknown` |
| `p = false`   | `true`  | `true`   | `true`    |
| `p = unknown` | `true`  | `unknown`| `unknown` |

Notable: `false -> unknown` is **`true`**, and `unknown -> true` is **`true`**. A false premise proves anything; a true conclusion is proved by anything.

### Bi-implication — `<->`

Defined as `(p -> q) & (q -> p)`. The full table is in `logicTest.nl`; the important rows are that `<->` is `true` exactly when both sides have the same two-valued value, `false` when they disagree, and `unknown` whenever either side is unknown.

---

## How the three values relate to query results

A query result has two sides, `[facts][falsehoods]`. For a given binding of the free variables:

| Facts side contains the binding? | Falsehoods side contains the binding? | Interpretation |
|---|---|---|
| Yes | No | The expression is a **fact** under that binding |
| No | Yes | The expression is a **falsehood** under that binding |
| No | No | The expression is **unknown** under that binding |
| Yes | Yes | — contradiction, never occurs; would indicate an inconsistent program |

So the two sides are not two independent Boolean outputs; together they classify each binding into one of three states.

The **incompleteness marker** `..` adds another dimension: even when a binding is not listed on a side, that side may have been reported as non-exhaustive (`..`), in which case the reasoner has not claimed the binding's absence is meaningful. Taken together:

| On facts side | On falsehoods side | Meaning for a given binding |
|---|---|---|
| Listed | not listed, side closed | fact |
| not listed, side closed | Listed | falsehood |
| not listed, side closed | not listed, side closed | impossible — contradicts the query's own completeness claim on at least one side |
| not listed, side has `..` | not listed, side closed | unknown but reasoner ruled out falsehood |
| not listed, side closed | not listed, side has `..` | unknown but reasoner ruled out fact |
| not listed, side has `..` | not listed, side has `..` | unknown, reasoner made no completeness claim |

The common cases are rows 1, 2, and 6 (the last of which is what `[..][..]` represents globally).

---

## Key operator identities

Because Nelumbo's `!` is genuine negation, De Morgan's laws hold:

```
!(a != T1 & a != T2)  ≡  (a = T1 | a = T2)
!(a != T1 | a != T2)  ≡  (a = T1 & a = T2)
```

Both are verified in `logicTest.nl`:

```
!(a!=T1 & a!=T2)  ? [(a=T1),(a=T2)][..]
!(a!=T1 | a!=T2)  ? [][(a=T1),(a=T2),..]
```

Similarly, quantifier duality holds:

```
!A[x](p)  ≡  E[x](!p)
!E[x](p)  ≡  A[x](!p)
```

This is the tangible payoff of three-valued logic: the laws of classical logic remain usable as identities for program transformation and reasoning, without the caveats that Prolog-style systems require.

---

## See also

- [`operators.md`](operators.md) — catalogue of logical operators
- [`test-expression-semantics.md`](test-expression-semantics.md) — formal rules for when a test passes
- [`writing-rules.md`](writing-rules.md) — how rules interact with the three-valued model
- [`logicTest.nl`](../../src/main/resources/org/modelingvalue/nelumbo/tests/logicTest.nl) — the executable specification
