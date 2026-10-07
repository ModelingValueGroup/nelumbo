# Lambdas

> **Level:** `nelumbo.logic`. Available after `import nelumbo.logic`, or transitively through any package (`nelumbo.integers` and up).

A lambda is an anonymous expression over bound variables: `[x](x>0)`. Lambdas are declared in `logic.nl` (they used to live in `lang.nl` and moved to the logic level on 2026-10-05) because the quantifiers `E[…](…)` and `A[…](…)` are lambdas, and because the collection operations `where`, `map`, `sort` and set-builder `{[e](c)}` take them as arguments.

---

## Syntax

```
Lambda1<A1,R> ::= [ <{Variable,A1}> ] ( <R#0> )                          @nelumbo.logic.Lambda
Lambda2<A1,A2,R> ::= [ <{Variable,A1}> , <{Variable,A2}> ] ( <R#0> )     @nelumbo.logic.Lambda
…                                                                         (up to Lambda6)
```

(abbreviated; see [`logic.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/logic/logic.nl) for all six.)

- `[ … ]` lists the **bound variables**, comma-separated. Each hole is typed `{Variable,A1}`: it must be a *variable* (an already declared one such as `Integer i`) *and* of the matching argument type. Anything else is a parse error.
- `( … )` is the **body**, an expression of result type `R` (precedence `#0`, so everything up to the closing parenthesis belongs to it).
- `R` is the lambda's result type. A lambda whose body is `Boolean` is a *predicate*; one whose body is an `Integer` is a function to `Integer`.

```
Integer i, ib, ia

[i](i>1)           ▸ Lambda1<Integer,Boolean>  — a predicate over one Integer
[i](i*i)           ▸ Lambda1<Integer,Integer>  — a function Integer → Integer
[ib,ia](ib<ia)     ▸ Lambda2<Integer,Integer,Boolean> — a binary relation
```

## Types

```
Type A1, A2, A3, A4, A5, A6, R

Lambda                       :: Object
Lambda<R>                    :: Lambda
Lambda1<A1,R>                :: Lambda<R>
Lambda2<A1,A2,R>             :: Lambda<R>
…
Lambda6<A1,A2,A3,A4,A5,A6,R> :: Lambda<R>
```

The arity-N type is `LambdaN<A1,…,AN,R>`, with N from 1 to 6 (a seventh variable is a parse error). All of them are subtypes of `Lambda<R>`, so a pattern that accepts *any* arity with a given result type can use `<Lambda<R>>` — the quantifiers do exactly that:

```
Boolean ::= E<Lambda<Boolean>>  @nelumbo.logic.ExistentialQuantifier,
            A<Lambda<Boolean>>  @nelumbo.logic.UniversalQuantifier
```

`E[x,y](p)` is therefore the keyword `E` followed by a `Lambda2<…,Boolean>` literal — the quantifiers need no syntax of their own, and take the same one-to-six variables as a lambda.

---

## Scope

The variables in `[ … ]` are **local to the lambda**: inside the body they take on the lambda's role instead of their outer one, and they are not visible outside it. This is the same scoping the quantifiers always had. The variable's *declaration* is still the ordinary one (`Integer i`); a lambda does not declare variables, only binds them.

```
Integer i
{[i](|i|=10)} = s     // i is bound by the lambda; an outer i is untouched
```

---

## Using lambdas

Lambdas are *values consumed by natives*. The language has no call syntax for a lambda: you cannot write `f(3)` for a lambda `f`. A lambda gets applied when it is an argument of a functor whose native implementation evaluates it — `Lambda.test(…)` for predicates (`where`, `sort`) and `Lambda.apply(…)` for functions (`map`). Declaring your own lambda-taking functor works the same as for any argument type:

```
Boolean ::= holds(<Lambda1<Integer,Boolean>>, <Integer>)
```

but its *semantics* must come from a native class (`@nelumbo.…`) that evaluates the lambda; a plain `<=>` rule can only pass the lambda along or ignore it. Library users normally meet lambdas through:

| Use                      | Lambda type              | Defined in                                   |
|---|---|---|
| `E[x](p)`, `A[x,y](p)`   | `Lambda<Boolean>`        | `nelumbo.logic`                              |
| `{[e](c)}`               | `Lambda1<E,Boolean>`     | [`nelumbo.collections`](../packages/collections.md#set-builder--ec) |
| `c where [x](p)`         | `Lambda1<E,Boolean>`     | `nelumbo.collections`                        |
| `c map [x](expr)`        | `Lambda1<F,E>`           | `nelumbo.collections`                        |
| `c sort [a,b](p)`        | `Lambda2<E,E,Boolean>`   | `nelumbo.collections`                        |

### Evaluation

For a predicate lambda, `test` instantiates the body with the argument values and asks the reasoner for a definite answer; for a function lambda, `apply` solves `body = r` for a fresh result variable `r` and takes the value. If the reasoner cannot reach a definite answer for some element (the body depends on something unbound, or recursion overflows), the whole surrounding operation reports an **incomplete** result instead of guessing — see [`three-valued-logic.md`](three-valued-logic.md).

---

## Native class

`nelumbo.logic.Lambda` (`Node`) backs all six functors. It keeps the list of bound variables and the body, and offers `test(Object…)` (a `Boolean`-body lambda; true only when definitely true) and `apply(Object…)` (the value of a function body). See [`native-classes.md`](../core/native-classes.md#lambda).

---

## See also

- [`index.md`](index.md#quantifiers) — `E[…](…)` / `A[…](…)`
- [`collections.md`](../packages/collections.md) — `where`, `map`, `sort`, set-builder
- [`logicTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/logicTest.nl) and [`collectionsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/collectionsTest.nl) — executable specifications
