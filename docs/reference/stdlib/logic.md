# `nelumbo.logic`

The foundation module. Every other Nelumbo program (directly or transitively) imports this. It defines the `Boolean` type, the three Boolean values, the logical connectives, the quantifiers, and equality.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/logic/logic.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/logic/logic.nl) — 29 lines.

**Import:**

```
import nelumbo.logic
```

---

## Values

```
Boolean ::= true                                    @...NBoolean,
            false                                   @...NBoolean,
            unknown                                 @...NBoolean
```

Three Boolean values. `unknown` is a first-class citizen: it is a value of type `Boolean`, not an absence of a value. A query that binds a Boolean variable may place `(b=unknown)` in neither the facts nor the falsehoods side — *or* the query `b = unknown` may legitimately produce `[(b=unknown)][..]`. See [`three-valued-logic.md`](three-valued-logic.md).

---

## Connectives

| Operator | Pattern | `#N` | Native |
|---|---|---|---|
| `!p`    | prefix | 25 | `Not` |
| `p & q` | binary | 22 | `And` |
| `p | q` | binary | 20 | `Or` |
| `p -> q`  | binary | 18 | defined in `logic.nl` |
| `p <-> q` | binary | 16 | defined in `logic.nl` |

`->` and `<->` are **defined in Nelumbo**, not natively:

```
p1 -> p2   <=>  !p1 | p2
p1 <-> p2  <=>  (p1 -> p2) & (p2 -> p1)
```

This is a good first worked example of the meta-language working on itself: `<->` is a user-level definition built from `->`, which is itself a user-level definition built from `|` and `!`.

---

## Quantifiers

```
Boolean ::= E[<(> <Variable#100> <,> , <)+>](<Boolean#0>)  @...ExistentialQuantifier
Boolean ::= A[<(> <Variable#100> <,> , <)+>](<Boolean#0>)  @...UniversalQuantifier
```

- `E[x](p)` — there exists `x` such that `p`
- `A[x](p)` — for all `x`, `p`
- Multiple bound variables: `E[x,y,z](p)`, `A[x,y,z](p)`

See [`operators.md`](operators.md#quantifiers) for semantics and duality laws.

---

## Equality

```
<Object> != <Object>  #30
```

The `!=` operator is declared as a pattern with precedence 30. It is defined in terms of `=` and `!`:

```
n1 != n2  <=>  !(n1 = n2)
```

Equality itself (`=`) does not appear as a top-level `::=` pattern because it is built into the engine at a deeper level — it is the fundamental comparison primitive, expressed via the private `eq` predicate:

```
private Boolean ::= eq(<Object>, <Object>)   @...Equal
```

The module wraps `eq` in three rules so that `=` works uniformly regardless of whether a side is a literal, a function, or a variable:

```
l1 = l2  <=>  eq(l1, l2)
l1 = f1  <=>  f1 = l1
```

(Here `l1`, `l2` are `Literal` typed and `f1`, `f2` are `Function` typed.) The second rule inverts a literal-equals-function query to function-equals-literal, so rules that define relations via `f1 = l1` are reachable from either direction.

---

## Exports summary

After `import nelumbo.logic`, the following are visible:

- Types: `Boolean`, `Object`, `Literal`, `Function`
- Values: `true`, `false`, `unknown`
- Operators: `!`, `&`, `|`, `->`, `<->`, `=`, `!=`
- Quantifiers: `E[...]`, `A[...]`
- The `FactType` declaration mechanism (inherited; `FactType` itself is not re-declared here but is in scope for callers)

---

## See also

- [`three-valued-logic.md`](../three-valued-logic.md) — the semantic model these operators live in
- [`operators.md`](../operators.md) — full operator catalogue
- [`logicTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/logicTest.nl) — executable specification of every connective and quantifier
