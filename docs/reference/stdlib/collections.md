# `nelumbo.collections`

Generic sets and lists. The smallest stdlib module — and the only one that uses Nelumbo's generic-type parameter mechanism.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl) — 21 lines.

**Import:**

```
import nelumbo.collections
```

`nelumbo.collections` imports `nelumbo.integers` (and thus `nelumbo.logic`).

---

## Types

```
Type E

Collection<E>  :: Object
Set<E>         :: Collection<E>
List<E>        :: Collection<E>
```

- `Type E` introduces `E` as a generic type parameter — the same mechanism is available in user code (see also `Type P` in `lang.nl` for the generic parenthesization rule `P ::= (<P>)`).
- `Collection<E>` is the common supertype.
- `Set<E>` and `List<E>` are both subtypes of `Collection<E>`. A variable of type `Collection<E>` can hold either.

---

## Literals

```
Set<E>  ::= { <(> <E> <,> , <)*> }       @nelumbo.collections.NSet,
            { [ <E> ] ( <Boolean#0> ) }  @nelumbo.collections.SetBuilder
List<E> ::= [ <(> <E> <,> , <)*> ]       @nelumbo.collections.NList
```

| Syntax        | Type     | Notes                                  |
|---|---|---|
| `{}`          | `Set<E>`  | empty set                              |
| `{x, y, z}`   | `Set<E>`  | unordered, no duplicates               |
| `{[e](c)}`    | `Set<E>`  | set-builder (comprehension) — see below |
| `[]`          | `List<E>` | empty list                             |
| `[x, y, z]`   | `List<E>` | ordered, duplicates preserved          |

The `<(> ... <,> , <)*>` fragment is the zero-or-more comma-separated repetition (see [`built-in-tokens.md`](../built-in-tokens.md#structural-markers--repetition-and-grouping)). The element type `E` is inferred from the surrounding context — the declared type of the receiving variable or pattern hole.

---

## Set-builder notation

`Set<E>` has a second literal form — set-builder notation, the logic-programming analogue of mathematical `{ e | c }`:

```
Set<E> ::= { [ <E> ] ( <Boolean#0> ) }   @nelumbo.collections.SetBuilder
```

- `[ <E> ]` names the **bound element variable** — it *must* be a bare variable (anything else is rejected at parse time with `… must be a variable`).
- `( <Boolean#0> )` is the **membership condition** — any Boolean expression, typically constraining the bound variable.

`{[e](c)}` denotes *the set of all `e` for which `c` is a fact*. One native rule wires it up:

```
E e   Boolean c   Set<E> s

{[e](c)} = s   <=>   build(e, c, s)
```

`build` is a `private` predicate backed by `nelumbo.collections.BuildSet`. It is a **quantifier**: like `E[...]` and `A[...]`, it evaluates the condition under many bindings of the local element variable and strips that variable from the result, collecting the witnessing values into a set.

```
Integer i   Set<Integer> s

{[i](|i|=10)} = s   ?   [(s={-10,10})][(s={0}),..]
```

The bound variable `i` ranges over the condition `|i| = 10`. Its two solutions, `-10` and `10`, are gathered into the fact `s = {-10, 10}`. The falsehoods side carries `(s={0})`: `i = 0` is a proven *non*-member (`|0| = 10` is false), so the singleton `{0}` is a proven falsehood of the builder, with `..` standing in for the rest of the open domain.

Because it is built on the three-valued quantifier machinery, set-builder notation inherits the same completeness behaviour as `E[...]`/`A[...]` (see [`three-valued-logic.md`](../three-valued-logic.md) and the quantifier notes in [`native-classes.md`](../native-classes.md)).

---

## Usage

The whole of `collectionsTest.nl`:

```
import nelumbo.collections

List<Integer>       l
Set<Integer>        s
Collection<Integer> c
Integer             i

s = {1,2,3}         ?   [(s={1,2,3})][..]
s = {}              ?   [(s={})][..]
{[i](|i|=10)} = s   ?   [(s={-10,10})][(s={0}),..]
```

Collection values print back in their literal form on the facts side.

---

## Status

`collections` provides type declarations, literal constructors, and set-builder comprehension. There are still no algebraic operations in the module itself — no union, intersection, length, map, or fold — but `{[e](c)}` gives a way to *construct* a set from a predicate. The native classes are `NSet` and `NList` (literals) plus `SetBuilder`/`BuildSet` (the comprehension form).

This is also the only stdlib module that demonstrates generic-type parameters in action. The mechanism is general: a user module can declare `Type T` and parameterise its own types and patterns the same way.

---

## Exports summary

Added to what `nelumbo.integers` and `nelumbo.logic` already export:

| Kind          | Names                                |
|---|---|
| Types         | `Collection<E>`, `Set<E>`, `List<E>` |
| Literals      | `{...}` for sets, `[...]` for lists  |
| Comprehension | `{[e](c)}` — set-builder notation    |

(`build` is `private` and is not visible to importers; the set-builder syntax `{[e](c)}` is its public surface.)

(The `Type E` declaration introduces the parameter binding inside `collections.nl`; the *mechanism* of generic-type parameters is supplied by `nelumbo.lang` and is available to importers regardless of `collections`.)

---

## See also

- [`integers.md`](integers.md) — the module `collections` imports
- [`built-in-tokens.md`](../built-in-tokens.md#structural-markers--repetition-and-grouping) — the repetition markers `<(>`, `<)*>`, `<,>` used in the literal declarations
- [`three-valued-logic.md`](../three-valued-logic.md) — the quantifier semantics set-builder notation is built on
- [`logic.md`](logic.md) — the `E[...]`/`A[...]` quantifiers `{[e](c)}` is a cousin of
- [`collectionsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/collectionsTest.nl) — executable specification
