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

Facts are open: what is not asserted is not false, it is unknown (no closed-world assumption, no negation as failure). A query over facts therefore ends its answers in `..`:

```nelumbo
friends(Piet, x) ? [(x=Jan),..][..]
```

The world of facts is open, with one exception: a hole marked `<one T>` in a `Boolean` pattern is **unique**. For each combination of the other arguments there is at most one value in that hole (a functional dependency). Asserting one value then closes the hole for those other arguments:

```nelumbo
Boolean ::= het inkomen van <Person> is <one Integer> euro

fact het inkomen van Piet is 50000 euro

het inkomen van Piet is i euro      ? [(i=50000)][..]   // facts side closed: the only value
het inkomen van Piet is 4 euro      ? [][()]            // another value is false
het inkomen van Jan is i euro       ? [..][..]          // nothing asserted for Jan: unknown
het inkomen van p is 50000 euro     ? [(p=Piet),..][..] // the other direction stays open
```

**Functions are closed through `=`.** `logic.nl` declares equality as `<one Object> = <one Object>`, so both sides of `=` are unique holes. A function value `f(x)=v` is an `=` fact, so a function has a single result and its asserted value is closed, while everything else about facts stays open:

```nelumbo
Female ::= m(<Person>)
fact m(Beatrix)=Juliana

m(Beatrix)=x        ? [(x=Juliana)][..]    // closed
m(Beatrix)=Beatrix  ? [][()]               // another value is false
m(x)=Juliana        ? [(x=Beatrix),..][..] // open: m need not be injective
m(Juliana)=x        ? [..][..]             // unknown
```

The exact rules:

- Uniqueness constrains the **variable** that fills the hole. Only a top-level argument that is an unbound variable counts; a term such as `m(x)` in the left hole of `=` is not constrained, which is why `m(x)=Juliana` may have several answers.
- The check applies when exactly one unique hole holds an unbound variable. With two (`x=y`, or `married(p,q)` for `married(<one Person>, <one Person>)`) nothing is checked.
- A hole is closed per combination of the **other** arguments, so it needs other arguments that are bound (and literal, for facts). A pattern whose only argument is unique, such as `winner(<one Person>)`, gets no closure.
- Two facts that differ only in a unique hole (`fact m(Beatrix)=Juliana, m(Beatrix)=Wilhelmina`) are an inconsistency.
- The same holds for rules: with the unique hole unbound, a rule may produce no answer or exactly one *closed* answer. Two answers, or one answer with an open facts side, are reported as `Inconsistent result: ... has non unique bindings`.

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
