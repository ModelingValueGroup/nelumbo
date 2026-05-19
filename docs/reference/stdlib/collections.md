# `nelumbo.collections`

Generic sets and lists. The smallest stdlib module — and the only one that uses Nelumbo's generic-type parameter mechanism.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl) — 11 lines.

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

- `Type E` introduces `E` as a generic type parameter — the same mechanism is available in user code (see also `Type T` in `lang.nl` for the generic parenthesization rule `T ::= (<T>)`).
- `Collection<E>` is the common supertype.
- `Set<E>` and `List<E>` are both subtypes of `Collection<E>`. A variable of type `Collection<E>` can hold either.

---

## Literals

```
Set<E>  ::= { <(> <E> <,> , <)*> }   @nelumbo.collections.NSet
List<E> ::= [ <(> <E> <,> , <)*> ]   @nelumbo.collections.NList
```

| Syntax       | Type     | Notes                                  |
|---|---|---|
| `{}`         | `Set<E>`  | empty set                              |
| `{x, y, z}`  | `Set<E>`  | unordered, no duplicates               |
| `[]`         | `List<E>` | empty list                             |
| `[x, y, z]`  | `List<E>` | ordered, duplicates preserved          |

The `<(> ... <,> , <)*>` fragment is the zero-or-more comma-separated repetition (see [`built-in-tokens.md`](../built-in-tokens.md#structural-markers--repetition-and-grouping)). The element type `E` is inferred from the surrounding context — the declared type of the receiving variable or pattern hole.

---

## Usage

The whole of `collectionsTest.nl`:

```
import nelumbo.collections

List<Integer>       l
Set<Integer>        s
Collection<Integer> c

s = {1,2,3}   ?   [(s={1,2,3})][..]
s = {}        ?   [(s={})][..]
```

Collection values print back in their literal form on the facts side.

---

## Status

`collections` provides type declarations and literal constructors. There are no built-in operations in the module itself — no membership predicate, no union, no map, no fold. The two native classes `NSet` and `NList` only realise the literal forms.

This is also the only stdlib module that demonstrates generic-type parameters in action. The mechanism is general: a user module can declare `Type T` and parameterise its own types and patterns the same way.

---

## Exports summary

Added to what `nelumbo.integers` and `nelumbo.logic` already export:

| Kind          | Names                                |
|---|---|
| Types         | `Collection<E>`, `Set<E>`, `List<E>` |
| Literals      | `{...}` for sets, `[...]` for lists  |

(The `Type E` declaration introduces the parameter binding inside `collections.nl`; the *mechanism* of generic-type parameters is supplied by `nelumbo.lang` and is available to importers regardless of `collections`.)

---

## See also

- [`integers.md`](integers.md) — the module `collections` imports
- [`built-in-tokens.md`](../built-in-tokens.md#structural-markers--repetition-and-grouping) — the repetition markers `<(>`, `<)*>`, `<,>` used in the literal declarations
- [`collectionsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/collectionsTest.nl) — executable specification
