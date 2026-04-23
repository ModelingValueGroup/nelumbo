# `nelumbo.collections`

Generic sets and lists.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl) — 8 lines.

**Import:**

```
import nelumbo.collections
```

`nelumbo.collections` imports `nelumbo.integers` transitively (and thus `logic`). The module itself is tiny because most of the work is in the native bindings.

---

## Types and literals

```
Type E

Set<E>  ::= { <(> <E> <,> , <)*> }   @...NSet
List<E> ::= [ <(> <E> <,> , <)*> ]   @...NList
```

Two **generic** type constructors, each parameterised by an element type `E`:

- `Set<E>` — curly-braced literal, unordered, no duplicates
- `List<E>` — square-bracketed literal, ordered, duplicates preserved

The `Type E` declaration introduces `E` as a generic type parameter. The two `::=` declarations then define literal syntax for `Set<E>` and `List<E>` using the zero-or-more repetition marker (`<)*>`, comma-separated).

### Collection type

Some examples also reference `Collection<E>` as a super-type of both:

```
Collection<Integer> c
```

From [`collectionsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/collectionsTest.nl). `Collection` is the common supertype when you want a variable that can hold either a `Set` or a `List`.

---

## Literals

```
{}                  // empty Set<E>
{1, 2, 3}           // Set<Integer> with three elements
[]                  // empty List<E>
[1, 2, 3]           // List<Integer> with three elements
```

The element type is inferred from the context (a variable's declared type or the type hole receiving the literal).

---

## Usage

From [`collectionsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/collectionsTest.nl):

```
List<Integer>       l
Set<Integer>        s
Collection<Integer> c

s = {1, 2, 3}   ?  [(s={1,2,3})][..]
s = {}          ?  [(s={})][..]
```

The facts side includes the full literal — `{1,2,3}` and `{}` — showing how collection values are printed in results.

---

## Status

`collections` is the youngest of the stdlib modules and the smallest. At the current stage (v0.9.x) it provides literal constructors and the basic type infrastructure. Richer operations (membership, union, map, fold) are expected but may be added over time; check the module source and [`collectionsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/collectionsTest.nl) for the latest behaviour.

This is also the only stdlib module that showcases Nelumbo's **generic type parameters** (`Type E`, `Set<E>`). The mechanism is general — you can use it in your own modules to define generic containers or parameterised abstractions. See [`../../guides/generics.md`](../../guides/generics.md) when that Phase 4 guide lands.

---

## Exports summary

After `import nelumbo.collections`, in addition to everything from `integers` and `logic`:

- Generic type parameter mechanism: `Type E`
- Types: `Set<E>`, `List<E>` (and `Collection<E>` where exposed)
- Literals: `{...}` for sets, `[...]` for lists

---

## See also

- [`integers.md`](integers.md) — the module `collections` imports
- [`../built-in-tokens.md`](../built-in-tokens.md#structural-markers--repetition-and-grouping) — the repetition markers `<(>`, `<)*>`, `<,>` used in the literal declarations
- [`collectionsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/collectionsTest.nl) — executable specification
