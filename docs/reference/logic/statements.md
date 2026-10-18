# Statements

> **Level:** `nelumbo.logic`. Available after `import nelumbo.logic`, or transitively through any package (`nelumbo.integers` and up).

The top-level statements that drive execution: asserting facts and asking queries. Rules (`<=>`) have their own page, [Writing rules](writing-rules.md). All of these are declared in `logic.nl`. The declarations that shape syntax (types, patterns, variables) are in the [`nelumbo.lang` grammar](../lang/grammar.md).

---

## Facts

Any `Boolean` expression can be asserted with the `fact` keyword: a relation declared as a `Boolean` pattern, or the value of a function (`f(x)=v`):

```nelumbo
Boolean ::= friends(<Person>,<Person>)                     // friends.nl
Boolean ::= het inkomen van <Person> is <one Integer> euro  // belasting.nl
Female  ::= m(<Person>)                                    // family.nl

fact friends(Piet, Jan),
     friends(Jan, Klaas)

fact het inkomen van Piet is 50000 euro

fact m(Beatrix)=Juliana
```

`fact` introduces one or more comma-separated assertions. Once asserted, they are available to the query engine as proven facts. Facts and rules are aligned: the same relation can have both facts and a defining rule (see [`friends.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/friends.nl)).

### Open facts and `one`

Facts are open: what is not asserted is not false, it is unknown. A query over facts therefore ends its answers in `..`:

```nelumbo
friends(Piet, x) ? [(x=Jan),..][..]
```

A hole marked `<one T>` is unique: for each combination of the other arguments there is at most one value. Asserting `het inkomen van Piet is 50000 euro` then also decides that every other amount is false, so `het inkomen van Piet is i euro` gives `[(i=50000)][..]`. Functions work the same way: a function has a single result, so `fact m(Beatrix)=Juliana` makes `m(Beatrix)=Beatrix` false.

A unique result must therefore be decided by what is known: a function whose defining rule derives its value from open facts (`m(a)=b <=> E[x](pc(x,a) & b=x)`, with `pc` asserted) would answer `[(b=Juliana),..][..]`, one value plus possibly others, which contradicts uniqueness. The engine reports it as `Inconsistent result: ... has non unique bindings`. Model such a derived relation as a relation (`m(a,b) <=> E[x](pc(x,a) & b=x)`, answering `[(b=Juliana),..][..]`), or as a set-valued function built with a set-builder, whose single result is an open set such as `{Juliana,..}` (see [`collections.md`](../packages/collections.md#open-collections--) and [`family.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/family.nl)).

---

## Queries and tests

Three forms drive execution:

```nelumbo
fact E             // assertion — see Facts above
E ?                // query — run the reasoner, print the result
E ? [F][N]         // test — query and compare; pass iff result matches
```

A bare expression without `fact` is rejected (`unexpected type Boolean, expected Root or List<Root>`).

See [`test-expression-semantics.md`](test-expression-semantics.md) for the precise comparison rules.

---

## See also

- [Operators](operators.md) — `fact`, `<=>`, `?` and the logical operators
- [Writing rules](writing-rules.md) — `<=>`, guards, merging, contradictions
- [Reading a query and test](../../getting-started/reading-a-test.md) — the `[facts][falsehoods]` notation
