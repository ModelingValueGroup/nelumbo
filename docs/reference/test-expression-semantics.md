# Test expression semantics

This page formally defines what a **query** produces, what a **test** compares, and under what conditions a test passes or fails. It is the reference counterpart to [`../getting-started/reading-a-test.md`](../getting-started/reading-a-test.md), which introduces the same material gently.

---

## What a query produces

A query is written as an expression followed by `?`:

```
E ?
```

Running a query on expression `E` produces a **query result**: a pair

```
( FactsSet , FactsIsClosed , FalsehoodsSet , FalsehoodsIsClosed )
```

where:

- `FactsSet` is the set of bindings of `E`'s free variables for which `E` has been proven to be a fact
- `FalsehoodsSet` is the set of bindings for which `E` has been proven to be a falsehood
- `FactsIsClosed` is `true` iff the reasoner certifies that `FactsSet` contains *every* binding that makes `E` a fact
- `FalsehoodsIsClosed` is defined analogously for falsehoods

These two "closed" flags are what the **incompleteness marker** `..` reports in textual form. A side is closed iff its printed form omits `..`; a side is open iff its printed form includes `..`.

A binding that is in neither set, on a side that is closed, has been **affirmatively excluded** from that side. A binding that is in neither set, on a side that is open, has no claim made about it.

---

## Printed form

Query results are printed as:

```
[ facts-list ][ falsehoods-list ]
```

where each list is a comma-separated sequence of bindings, optionally ending in `..` to indicate that side is open.

A binding is written as `(v1=value1, v2=value2, ...)` or as `()` for the empty binding (a solution with no free variables). An empty list `[]` is the closed claim "no bindings on this side."

### Examples of printed forms

| Printed form | FactsSet | Facts closed? | FalsehoodsSet | Falsehoods closed? |
|---|---|---|---|---|
| `[()][]` | `{()}` | yes | `{}` | yes |
| `[][()]` | `{}` | yes | `{()}` | yes |
| `[..][..]` | `{}` | no | `{}` | no |
| `[(f=5)][..]` | `{(f=5)}` | yes | `{}` | no |
| `[(a=T1),(a=T2)][..]` | `{(a=T1),(a=T2)}` | yes | `{}` | no |
| `[][..]` | `{}` | yes | `{}` | no |
| `[(a=0),..][..]` | `{(a=0)}` | no | `{}` | no |

---

## What a test compares

A test is written as:

```
E ? [ expected-facts ][ expected-falsehoods ]
```

The engine runs the query `E ?` to get an **actual result**, and compares the actual result to the **expected result** expressed by the two bracket literals.

The test **passes** iff both of the following hold:

1. `expected-facts` matches `actualFactsSet` and its closure flag
2. `expected-falsehoods` matches `actualFalsehoodsSet` and its closure flag

"Matches" is defined per-side as follows.

### Matching a side

Let `expected` be one bracket literal and `actual` be the corresponding actual side of the query result.

- If `expected` is closed (does not contain `..`), it matches iff `expected` is the same set of bindings as `actual`, and `actual` is also closed.
- If `expected` is open (contains `..`), it matches iff:
  - every binding listed explicitly in `expected` is present in `actual`, and
  - any remaining bindings in `actual` are allowed (no enumeration constraint beyond the listed ones).

In short: a closed bracket asserts an **exact** set; an open bracket asserts a **lower bound** (the listed bindings must be present; more are permitted).

### Why both sides are checked

A test that only asserts the facts side (by using `[..]` on the falsehoods side) is not checking falsehoods at all. Conversely, `[][(a=T1)]` asserts a specific falsehood and an empty, closed facts side. Both sides are first-class; a test expresses whatever facts-and-falsehoods claims the author intends.

---

## Worked examples

### `fib(5)=f ? [(f=5)][..]`

Actual result: `FactsSet = {(f=5)}`, facts closed, `FalsehoodsSet = {}`, falsehoods open.

Checking:

- Expected facts `[(f=5)]` is closed. Equals `{(f=5)}`, and actual facts is closed. ✓
- Expected falsehoods `[..]` is open with no listed bindings. Every listed binding (none) is in actual. ✓

**Passes.**

### `m(Amalia)=Maxima ? [()][]`

Actual result: `FactsSet = {()}`, facts closed, `FalsehoodsSet = {}`, falsehoods closed.

Checking:

- Expected facts `[()]` is closed. Equals `{()}`, and actual is closed. ✓
- Expected falsehoods `[]` is closed. Equals `{}`, and actual is closed. ✓

**Passes.**

### `unknown ? [..][..]`

Actual result: `FactsSet = {}`, facts open, `FalsehoodsSet = {}`, falsehoods open.

Checking:

- Expected facts `[..]` is open with no bindings. Actual has no bindings. ✓
- Expected falsehoods `[..]` is open with no bindings. Actual has no bindings. ✓

**Passes.**

### `a>0 ? [..][(a=0),..]`

From `integersTest.nl`. Actual result: `FactsSet = {}` but open (there are infinitely many positive integers, so the reasoner cannot close the side); `FalsehoodsSet = {(a=0)}` open (there are other falsifying bindings too: `(a=-1), (a=-2), ...`).

Checking:

- Expected facts `[..]` open with no listed bindings. ✓
- Expected falsehoods `[(a=0),..]` open with `(a=0)` listed. `(a=0)` is in actual. ✓

**Passes.**

### A failing test (hypothetical)

If we changed the last test to `a>0 ? [..][(a=1),..]`, it would fail: `(a=1)` is a *fact* side binding of `a>0`, not a falsehood. The expected falsehoods list asserts `(a=1)` is a falsehood, but the actual falsehoods side does not contain it.

---

## The relationship to the three-valued model

Given a binding `b` of the free variables, the query result classifies `b` as follows:

- `b` in actual facts ⇒ `b` is a **fact**
- `b` in actual falsehoods ⇒ `b` is a **falsehood**
- `b` in neither, both sides closed ⇒ impossible (would mean the reasoner asserts `b` is neither, but has certified completeness, yet `b` is a syntactically valid binding — treated as a programming error)
- `b` in neither, at least one side open ⇒ `b`'s status is **unknown** (the open side makes no claim)

See [`three-valued-logic.md`](three-valued-logic.md) for the semantic model behind this.

---

## Bare queries

A query without expected brackets (`E ?`) does not pass or fail; it just prints its result. Bare queries are useful during development. A file of bare queries (like [`queryOnly.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/queryOnly.nl)) runs and produces a transcript, which you can read, compare to your expectations, and then turn into tests by adding brackets.

---

## See also

- [`../getting-started/reading-a-test.md`](../getting-started/reading-a-test.md) — gentle introduction to the same material
- [`three-valued-logic.md`](three-valued-logic.md) — what facts, falsehoods, and unknown mean
- [`writing-rules.md`](writing-rules.md) — how rules produce the facts and falsehoods that tests observe
