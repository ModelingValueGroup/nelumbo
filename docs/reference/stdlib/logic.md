# `nelumbo.logic`

The foundation module. Every other Nelumbo program imports this — either directly, or transitively through one of the other stdlib modules. It declares the `Boolean` type, the three Boolean values, the connectives, the quantifiers, equality, and the three top-level forms (`fact`, `<=>`, `?`).

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/logic/logic.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/logic/logic.nl) — 41 lines.

**Import:**

```
import nelumbo.logic
```

`nelumbo.logic` itself imports `nelumbo.lang`, which contributes the meta-syntax — `import`, `::`, `::=`, `::>`, type-variable declarations, `<NAME>`, `<STRING>`, etc. (see [`lang.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/lang/lang.nl)).

---

## Types

```
Boolean   :: Object
FactType  :: Boolean
Function  :: Object
Literal   :: Object
```

- `Boolean` — the type of truth-valued expressions.
- `FactType` — marker subtype of `Boolean` used by the language transformation machinery (see [`belasting.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/belasting.nl) for usage in a DSL).
- `Literal` — values that are their own canonical form: named constants such as `T1`, `Hendrik`, `true`, integers, strings, …
- `Function` — values produced by functional patterns (e.g., `a+b`, `fib(n)`, `r(x/y)`) that reduce by rules.

The `Literal` / `Function` split is what makes the three rules in *Equality* (below) sufficient: every operand of `=` is either a literal, a function, or a variable, and the rules cover the relevant cases.

---

## Boolean values

```
Boolean ::= true       @nelumbo.logic.NBoolean,
            false      @nelumbo.logic.NBoolean,
            unknown    @nelumbo.logic.NBoolean
```

Three Boolean values, all bound to the native class `NBoolean`. `unknown` is a first-class value of type `Boolean`, not an absence of one. See [`three-valued-logic.md`](../three-valued-logic.md) for the truth tables.

---

## Connectives

| Pattern              | `#N` | Native / definition          |
|---|---|---|
| `! <Boolean>`        | 25   | `nelumbo.logic.Not`          |
| `<Boolean> & <Boolean>` | 22 | `nelumbo.logic.And`         |
| `<Boolean> \| <Boolean>` | 20 | `nelumbo.logic.Or`          |
| `<Boolean> -> <Boolean>` | 18 | defined in `logic.nl`       |
| `<Boolean> <-> <Boolean>` | 16 | defined in `logic.nl`      |

`->` and `<->` are not native. They are defined in Nelumbo on top of `!` and `|`:

```
Boolean p1, p2

p1 -> p2   <=>  !p1 | p2
p1 <-> p2  <=>  (p1 -> p2) & (p2 -> p1)
```

This is the meta-language working on itself: `<->` is a user-level rule built from `->`, which is itself a user-level rule built from native `|` and `!`.

`logicTest.nl` is the executable truth-table specification for all five connectives, including the `unknown` rows. For example:

```
unknown & true   ? [..][..]
unknown & false  ? [][()]
true -> unknown  ? [..][..]
false -> unknown ? [()][]
```

---

## Quantifiers

```
Boolean ::= E[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @nelumbo.logic.ExistentialQuantifier,
            A[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @nelumbo.logic.UniversalQuantifier
```

- `E[x](p)` — there exists `x` such that `p`.
- `A[x](p)` — for all `x`, `p`.
- Multiple bound variables are allowed: `E[x,y,z](p)`, `A[x,y,z](p)`.

The pattern fragment `<(> <Variable#100> <,> , <)+>` is the one-or-more, comma-separated repetition that admits the variable list. The body `<Boolean#0>` is at the lowest precedence, so the entire expression inside the parentheses is consumed.

Test examples from `logicTest.nl`:

```
Test :: Object
Test ::= T1, T2
Test a

E[a](a=T1 | a=T2)      ? [()][]
A[a](a=T1 & a=T2)      ? [][()]
!A[a](a!=T1 & a!=T2)   ? [()][]
!E[a](a!=T1 | a!=T2)   ? [][()]
```

---

## Equality

```
private Boolean ::= eq(<Literal>, <Literal>)   @nelumbo.logic.Equal

Boolean ::= <Object> =  <Object>   #30   @nelumbo.logic.NIs,
            <Object> != <Object>   #30
```

Two related but distinct primitives:

- `eq(l1, l2)` is the **private** literal-equality predicate. It compares two `Literal` values directly and is bound to the native class `Equal`. Importers never call `eq` themselves.
- `<Object> = <Object>` is the **public** equality operator, bound to `NIs`. Unlike `eq`, it accepts any `Object` on either side, so it works on functions and variables as well as literals.

`!=` has no native binding of its own; it is the negation of `=`:

```
Literal  l1, l2
Function f1
Object   n1, n2

l1 = l2   <=>  eq(l1, l2)
l1 = f1   <=>  f1 = l1
n1 != n2  <=>  !(n1 = n2)
```

The middle rule (`l1 = f1 <=> f1 = l1`) swaps a literal-equals-function query into function-equals-literal form, so that user-defined rules of the shape `f1 = literal <=> ...` are reachable from either direction.

`=` is what makes ordinary rule bodies work — `f=fib(n-1)+fib(n-2)` succeeds when both sides reduce to the same value — and is the surface form used in queries to bind variables.

---

## Top-level forms

`nelumbo.logic` also declares the three statement forms that appear at the top level of a `.nl` file:

```
Root ::= "fact" <(> <Boolean#0> <,> , <)+>                                        @nelumbo.logic.Fact,
         <Boolean#0> "<=>" <(> <Boolean#0> <(> "if" <Boolean#0> <)?> <,> , <)+>   @nelumbo.logic.Rule,
         <Boolean#0> ? <(> <BINDING> <BINDING> <)?>                               @nelumbo.logic.Query
```

| Form  | Shape                                                | Native             |
|---|---|---|
| Fact  | `fact <Boolean>`, comma-separated lists allowed       | `Fact`             |
| Rule  | `<Boolean> <=> <Boolean> if <Boolean>`, `if` optional | `Rule`             |
| Query | `<Boolean> ?`, optionally followed by `[..][..]`      | `Query`            |

In test files the `fact` keyword is often elided — a bare predicate at top level (such as `pc(Hendrik, Juliana)` in `family.nl`) is sugar for `fact pc(Hendrik, Juliana)`. The `BINDING` fragment that follows `?` is a [named pattern](lang.md#named-patterns), declared here:

```
pattern BINDING ::= [ <(> <(> ( <(> <Variable#100> = <Object#100> <,> , <)*> ) <|> .. <)> <,> , <)*> ]
```

That is the grammar of `[(a=T1), (a=T2)]`, `[..]`, `[]`, and combinations such as `[(a=0),..]`. (It was previously a stand-alone `Binding :: Object` type; it is now a named pattern, so it adds no type — it is pure syntax for the query suffix.) See [`test-expression-semantics.md`](../test-expression-semantics.md) for how a `?` test is judged to pass or fail.

---

## Exports summary

After `import nelumbo.logic`, the following are visible to the importer:

| Kind           | Names                                              |
|---|---|
| Types          | `Boolean`, `FactType`, `Literal`, `Function`       |
| Boolean values | `true`, `false`, `unknown`                          |
| Connectives    | `!`, `&`, `\|`, `->`, `<->`                         |
| Quantifiers    | `E[...]`, `A[...]`                                 |
| Equality       | `=`, `!=`                                           |
| Top-level forms| `fact`, `<=>`, `?`                                 |

`eq` is `private` and is not visible to importers.

---

## See also

- [`three-valued-logic.md`](../three-valued-logic.md) — the semantic model these operators live in
- [`operators.md`](../operators.md) — full operator catalogue
- [`writing-rules.md`](../writing-rules.md) — `<=>` and `if` semantics
- [`logicTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/logicTest.nl) — executable specification of every connective and quantifier
