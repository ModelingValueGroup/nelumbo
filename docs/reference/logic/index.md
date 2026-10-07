# `nelumbo.logic`

> **Level:** `nelumbo.logic`. Available after `import nelumbo.logic`, or transitively through any package (`nelumbo.integers` and up).

The logic module. A file gets it with `import nelumbo.logic`, or transitively by importing any package. It declares the `Boolean` type, the three Boolean values, the connectives, [lambdas](lambdas.md), the quantifiers (which are lambdas), equality, and the three top-level forms (`fact`, `<=>`, `?`).

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/logic/logic.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/logic/logic.nl) — 62 lines.

**Import:**

```nelumbo
import nelumbo.logic
```

`nelumbo.logic` itself imports `nelumbo.lang`, which contributes the meta-syntax — `import`, `::`, `::=`, `::>`, type-variable declarations, `<NAME>`, `<STRING>`, etc. (see [`lang.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/lang/lang.nl)).

---

## Types

```nelumbo
Boolean   :: Object
FactType  :: Boolean
Function  :: Object
Literal   :: Object
Struct    :: Object
```
- `Boolean` — the type of truth-valued expressions.
- `FactType` — marker subtype of `Boolean` used by the language transformation machinery (see [`belasting.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/belasting.nl) for usage in a DSL).
- `Literal` — values that are their own canonical form: named constants such as `T1`, `Hendrik`, `true`, integers, strings, …
- `Function` — values produced by functional patterns (e.g., `a+b`, `fib(n)`, `r(x/y)`) that reduce by rules.
- `Struct` — values that are constructors with **structural equality**: two `Struct` values are equal when their parts are (collection literals such as `{1,2}` and `[1,2]` are `Struct`s; a user type declared `:: Struct` gets the same). They take the `eq` route like a `Literal`.

The `Literal` / `Function` split is what makes the rules in *Equality* (below) sufficient: every operand of `=` is either a literal, a function, or a variable, and the rules cover the relevant cases.

---

## Boolean values

```nelumbo
Boolean ::= true       @nelumbo.logic.NBoolean,
            false      @nelumbo.logic.NBoolean,
            unknown    @nelumbo.logic.NBoolean
```

Three Boolean values, all bound to the native class `NBoolean`. `unknown` is a first-class value of type `Boolean`, not an absence of one. See [`three-valued-logic.md`](three-valued-logic.md) for the truth tables.

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

```nelumbo
Boolean p1, p2

p1 -> p2   <=>  !p1 | p2
p1 <-> p2  <=>  (p1 -> p2) & (p2 -> p1)
```

This is the meta-language working on itself: `<->` is a user-level rule built from `->`, which is itself a user-level rule built from native `|` and `!`.

`logicTest.nl` is the executable truth-table specification for all five connectives, including the `unknown` rows. For example:

```nelumbo
unknown & true   ? [..][..]
unknown & false  ? [][()]
true -> unknown  ? [..][..]
false -> unknown ? [()][]
```

---

## Quantifiers

<<<<<<< HEAD
```
Boolean ::= E<Lambda<Boolean>>   @nelumbo.logic.ExistentialQuantifier,
            A<Lambda<Boolean>>   @nelumbo.logic.UniversalQuantifier
=======
```nelumbo
Boolean ::= E[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @nelumbo.logic.ExistentialQuantifier,
            A[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @nelumbo.logic.UniversalQuantifier
>>>>>>> refs/remotes/origin/develop
```

- `E[x](p)` — there exists `x` such that `p`.
- `A[x](p)` — for all `x`, `p`.
- Up to six bound variables are allowed: `E[x,y,z](p)`, `A[x,y,z](p)`; a seventh is a parse error.

The quantifier is the keyword `E` / `A` followed by a [**lambda**](lambdas.md) whose body is a `Boolean`: `[x,y](p)` is a `Lambda2<…,Boolean>`. The bracketed variables must be declared variables, they are local to the body, and the body is at the lowest precedence (`#0`) so the entire expression inside the parentheses is consumed. (The quantifiers used to carry their own `<(> <Variable#100> <,> , <)+>` variable-list pattern; they now share the lambda syntax.)

Test examples from `logicTest.nl`:

```nelumbo
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

```nelumbo
private Boolean ::= eq(<Literal>, <Literal>)   @nelumbo.logic.Equal

Boolean ::= <Object> =  <Object>   #30   @nelumbo.logic.NIs,
            <Object> != <Object>   #30
```

Two related but distinct primitives:

- `eq(l1, l2)` is the **private** literal-equality predicate. It compares two `Literal` values directly and is bound to the native class `Equal`. Importers never call `eq` themselves.
- `<Object> = <Object>` is the **public** equality operator, bound to `NIs`. Unlike `eq`, it accepts any `Object` on either side, so it works on functions and variables as well as literals.

`!=` has no native binding of its own; it is the negation of `=`. The remaining cases are rules, written with *intersection types* `{A,B}` ("both an `A` and a `B`") over a generic parameter `E`:

<<<<<<< HEAD
```
Type E
E            n1, n2
{E,Literal}  l1, l2
{E,Function} f1, f2
=======
```nelumbo
Literal  l1, l2
Function f1
Object   n1, n2
>>>>>>> refs/remotes/origin/develop

l1 = l2   <=>  eq(l1, l2)
l1 = f1   <=>  f1 = l1
n1 != n2  <=>  !(n1 = n2)
f1 = f2   <=>  E[l1](f1 = l1 & f2 = l1)
```

- `l1 = l2` — two literals (of one type `E`) are compared by `eq`.
- `l1 = f1` swaps a literal-equals-function query into function-equals-literal form, so that user-defined rules of the shape `f1 = literal <=> ...` are reachable from either direction.
- `f1 = f2` — two functions are equal when some literal is equal to both.

`=` is what makes ordinary rule bodies work — `f=fib(n-1)+fib(n-2)` succeeds when both sides reduce to the same value — and is the surface form used in queries to bind variables. Note that a class-less `Object` functor with arguments is `Function`-typed, so `f(1)=f(1)` is undecided unless the result type is declared `:: Struct` (structural equality).

---

## Top-level forms

`nelumbo.logic` also declares the three statement forms that appear at the top level of a `.nl` file:

<<<<<<< HEAD
```
Root ::= "fact" <(> <FactType#0> <,> , <)+>                                       @nelumbo.logic.Fact,
=======
```nelumbo
Root ::= "fact" <(> <Boolean#0> <,> , <)+>                                        @nelumbo.logic.Fact,
>>>>>>> refs/remotes/origin/develop
         <Boolean#0> "<=>" <(> <Boolean#0> <(> "if" <Boolean#0> <)?> <,> , <)+>   @nelumbo.logic.Rule,
         <Boolean#0> ? <(> <BINDING> <BINDING> <)?>                               @nelumbo.logic.Query
```

| Form  | Shape                                                | Native             |
|---|---|---|
| Fact  | `fact <FactType>`, comma-separated lists allowed      | `Fact`             |
| Rule  | `<Boolean> <=> <Boolean> if <Boolean>`, `if` optional | `Rule`             |
| Query | `<Boolean> ?`, optionally followed by `[..][..]`      | `Query`            |

In test files the `fact` keyword is often elided — a bare predicate at top level (such as `pc(Hendrik, Juliana)` in `family.nl`) is sugar for `fact pc(Hendrik, Juliana)`. The `BINDING` fragment that follows `?` is a [named pattern](../lang/index.md#named-patterns), declared here:

<<<<<<< HEAD
```
pattern BINDING ::= [ <(> <(> ( <(> <Variable#100> = <Object#0> <,> , <)*> ) <|> .. <)> <,> , <)*> ]
=======
```nelumbo
pattern BINDING ::= [ <(> <(> ( <(> <Variable#100> = <Object#100> <,> , <)*> ) <|> .. <)> <,> , <)*> ]
>>>>>>> refs/remotes/origin/develop
```

That is the grammar of `[(a=T1), (a=T2)]`, `[..]`, `[]`, and combinations such as `[(a=0),..]`. (It was previously a stand-alone `Binding :: Object` type; it is now a named pattern, so it adds no type — it is pure syntax for the query suffix.) See [`test-expression-semantics.md`](test-expression-semantics.md) for how a `?` test is judged to pass or fail.

---

## Exports summary

After `import nelumbo.logic`, the following are visible to the importer:

| Kind           | Names                                              |
|---|---|
| Types          | `Boolean`, `FactType`, `Literal`, `Function`, `Struct`, `Lambda`, `Lambda1`…`Lambda6` |
| Boolean values | `true`, `false`, `unknown`                          |
| Connectives    | `!`, `&`, `\|`, `->`, `<->`                         |
| Lambdas        | `[x,…](body)` (1–6 variables)                      |
| Quantifiers    | `E[...]`, `A[...]`                                 |
| Equality       | `=`, `!=`                                           |
| Top-level forms| `fact`, `<=>`, `?`                                 |

`eq` is `private` and is not visible to importers.

---

## See also

- [`three-valued-logic.md`](three-valued-logic.md) — the semantic model these operators live in
- [`lambdas.md`](lambdas.md) — the `[x](body)` values the quantifiers and the collection operations take
- [`operators.md`](operators.md) — full operator catalogue
- [`writing-rules.md`](writing-rules.md) — `<=>` and `if` semantics
- [`logicTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/logicTest.nl) — executable specification of every connective and quantifier
