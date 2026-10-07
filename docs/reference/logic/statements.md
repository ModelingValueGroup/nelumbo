# Statements

> **Level:** `nelumbo.logic`. Available after `import nelumbo.logic`, or transitively through any package (`nelumbo.integers` and up).

The top-level statements that drive execution: declaring fact types, asserting facts, and asking queries. Rules (`<=>`) have their own page, [Writing rules](writing-rules.md). All of these are declared in `logic.nl`. The declarations that shape syntax (types, patterns, variables) are in the [`nelumbo.lang` grammar](../lang/grammar.md).

---

## FactType declarations

A `FactType` pattern declares a relation whose instances can be asserted as ground truth:

```nelumbo
FactType ::= pc(<Person>,<Person>)                    // family.nl
FactType ::= friends(<Person>,<Person>)               // friends.nl
FactType ::= het inkomen van <Person> is <Integer> euro  // belasting.nl
```

A fact type looks like any other pattern, but values built with it are not computed by rules — they are either asserted directly (see below) or they are not. This separates **ground truth** from **derived relations**.

### Facts

Facts are asserted with the `fact` keyword:

```nelumbo
fact pc(Hendrik, Juliana),
     pc(Wilhelmina, Juliana),
     pc(Juliana, Beatrix)

fact het inkomen van Piet is 50000 euro
```

`fact` introduces one or more comma-separated ground-truth assertions. Once asserted, they are available to the query engine as proven facts.

---

## Queries, tests, and bare facts

Three forms drive execution:

```nelumbo
E                  // bare expression — treated as a fact if E is a FactType instance
E ?                // query — run the reasoner, print the result
E ? [F][N]         // test — query and compare; pass iff result matches
```

See [`test-expression-semantics.md`](test-expression-semantics.md) for the precise comparison rules.

---

## See also

- [Operators](operators.md) — `fact`, `<=>`, `?` and the logical operators
- [Writing rules](writing-rules.md) — `<=>`, guards, merging, contradictions
- [Reading a query and test](../../getting-started/reading-a-test.md) — the `[facts][falsehoods]` notation
