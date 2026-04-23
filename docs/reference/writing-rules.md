# Writing rules

This page is the reference for the `<=>` rule operator: how rules are written, how multiple rules for the same relation combine, how guards work, and what constitutes an inconsistency.

For the semantic background, read [`three-valued-logic.md`](three-valued-logic.md) first.

---

## The rule form

```
L  <=>  R
```

A rule asserts that `L` holds exactly when `R` holds. `<=>` is **bi-implication** — the two sides have the same truth value under every binding of their free variables.

Both `L` and `R` are expressions. Typically `L` is either:

- A **relation application** defining a function-like production, such as `fib(n)=f` or `c(a)=b`
- A **Boolean expression** such as `even(x)` or `p1 -> p2`

The free variables on each side must have been declared with a variable declaration (`T v`) somewhere in scope.

---

## A single rule — the simple case

```
even(x) <=> E[y](y = x/2)
```

Read as: *"`even(x)` is true exactly when there exists a `y` such that `y = x/2`."*

For any `x`, the engine treats `even(x)` and `E[y](y=x/2)` as interchangeable. Queries against `even(x)` are answered by reasoning about the RHS; queries against the RHS can be answered using the LHS. `<=>` is symmetric.

---

## Guarded rules — `if`

A right-hand side may be qualified by an `if`-guard:

```
fib(n) = f  <=>  f = n                  if n >= 0 & n <= 1
```

The rule **only contributes** under bindings for which the guard holds. Under bindings where the guard is false, this particular rule says nothing; other rules may still apply.

Guards are not conditions on the LHS — they are conditions on *this particular rule applying*. Under an unsatisfied guard, the rule is silent, not false.

---

## Multiple rules for the same LHS

Nelumbo lets you write many rules with the same left-hand side. They accumulate. Every rule bi-implies the LHS; none supersedes another.

### The comma shorthand

A common idiom compresses several rules onto adjacent lines using a comma:

```
fib(n) = f  <=>  f = n                    if n >= 0 & n <= 1,
                 f = fib(n-1) + fib(n-2)  if n > 1
```

The comma is **purely syntactic shorthand** for repeating the left-hand side. The above is exactly equivalent to:

```
fib(n) = f  <=>  f = n                    if n >= 0 & n <= 1
fib(n) = f  <=>  f = fib(n-1) + fib(n-2)  if n > 1
```

Two separate rules, both bi-implying `fib(n) = f`.

### How results merge

For a query against the LHS, the engine runs every applicable rule and **merges** the results:

- All facts produced by every rule go into the facts side of the combined result.
- All falsehoods produced by every rule go into the falsehoods side.
- A side of the combined result is closed iff *every* contributing rule produced a closed side (i.e. had no `..`).

Because every rule is a bi-implication, all the rules are making claims about the same relation, and the combined result is a straightforward union.

### Why this is not "or"

It is tempting to read multiple rules as `L <=> R1 | R2 | ...`. That reading is wrong. Under disjunction, a binding that makes `R1` true is allowed to make `R2` irrelevant. Under merging-via-bi-implication, every rule's claim must hold. A binding that `R1` declares a fact and `R2` declares a falsehood is a **contradiction**, not a disjunction resolved to true.

---

## Contradictions

A contradiction arises when two rules with the same LHS make incompatible closed claims about the same binding:

- Rule A places binding `b` on the facts side with a closed claim
- Rule B places binding `b` on the falsehoods side with a closed claim

Because both rules are bi-implications of `L`, both claims have to hold simultaneously — but `b` cannot be both a fact and a falsehood. The program is inconsistent under binding `b`.

Inconsistency is a program defect. The engine detects it and raises an error (`InconsistencyException`).

### Why `..` matters for safety

A rule that ends its relevant side with `..` is declaring that side **open** — "there may be more bindings I have not accounted for." Open claims do not fix the membership of their side; they only require that the listed bindings be present.

Two rules can safely disagree on the *membership* of an open side, because neither has claimed the side is complete. Contradiction only occurs when both rules have made **closed claims that conflict**.

In practice the simplest way to avoid contradictions is to give rules **mutually exclusive guards**. The `fib` rule does this: one guard is `n >= 0 & n <= 1`, the other is `n > 1`. For any `n` at most one rule contributes, so the closed claims never collide.

### When contradictions are helpful

Not all contradictions are bugs. Asserting that two rules *must* agree is a form of integrity constraint. If a user adds a fact that causes two previously non-overlapping rules to produce incompatible closed claims, Nelumbo will surface that as an inconsistency — a signal that the user has added a fact that contradicts the rule base.

---

## Reading real rules

### From `integers.nl`

```
a < b  <=>  b > a
a <= b <=>  a < b | a = b
a >= b <=>  a > b | a = b

a + b = c  <=>  add(a, b, c)
a - b = c  <=>  add(c, b, a)
a * b = c  <=>  mult(a, b, c)
a / b = c  <=>  mult(c, b, a)

-a = b  <=>  0 - a = b

|a| = b  <=>  b = a   if a >= 0,
              b = -a  if a < 0
```

Notice the rewriting idiom. Subtraction `a - b = c` is defined as addition with arguments swapped: `add(c, b, a)`. This is possible because `add` is a three-argument relation, not a function — it can be asked for any missing argument given the others. Division and negation use the same trick.

Notice also the absolute-value rule: two clauses, `if a >= 0` and `if a < 0`, covering the full domain with mutually exclusive guards. Clean and contradiction-free.

### From `family.nl`

```
c(a) = b  <=>  pc(a, b)
p(a) = b  <=>  pc(b, a)
m(a) = b  <=>  E[x](c(x) = a & b = x)
f(a) = b  <=>  E[y](c(y) = a & b = y)

a(a) = b  <=>  d(b) = a
d(a) = c  <=>  c(a) = c |
               E[b](d(a) = b & c(b) = c)
```

The `d` (descendant) rule uses `|` on the right to combine two cases into one clause — a direct child relationship, or an indirect descendance through an intermediate `b`. Either case can make the rule's RHS true.

The `a` (ancestor) rule is defined in terms of `d`: ancestor is just descendant with arguments swapped. No recursion in `a` itself; it inherits recursion from `d`.

---

## Guidelines

- **Prefer mutually exclusive guards** over overlapping ones. Mutually exclusive guards make rules compositional: you can add a new case without auditing the existing ones for contradictions.
- **Use `|` on the RHS** when multiple patterns of the *same* binding can make the rule hold — for example, `child-or-descendant-of-child`.
- **Use multiple rules (or comma shorthand)** when the cases are genuinely distinct and selected by different guards.
- **Close a side with an exhaustive case when you can.** A rule whose facts side is closed for all relevant bindings is stronger than one that leaves `..` everywhere, and enables stronger falsehood reasoning downstream.
- **Let negation do its work.** Because Nelumbo has genuine `!`, you rarely need to hand-enumerate falsehood cases. The reasoner will derive them from rule structure.

---

## See also

- [`three-valued-logic.md`](three-valued-logic.md) — the semantic model rules produce results within
- [`test-expression-semantics.md`](test-expression-semantics.md) — how tests observe the results rules produce
- [`../guides/writing-tests.md`](../guides/writing-tests.md) — pragmatic tips for test design (Phase 4)
- [`../getting-started/first-program.md`](../getting-started/first-program.md) — `fib` explained line by line
