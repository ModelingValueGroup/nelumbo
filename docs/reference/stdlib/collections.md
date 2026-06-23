# `nelumbo.collections`

Generic sets and lists. The smallest stdlib module — and the only one that uses Nelumbo's generic-type parameter mechanism.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl) — 60 lines.

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

## Operations

The module exposes a set of operations as infix/prefix operators. Every one is a relation, so it works in both directions: you can supply the result and check it, or leave it as a variable and have it computed. The native worker for all of them is the `Collections` class; the operator syntax is the public surface, wired to `private` predicates.

### Cardinality — `|c|`

```
Integer ::= | <Collection<E>> | #35
```

`|c| = n` is the number of elements in any `Collection<E>` (set *or* list). Computes the count from the collection, or checks a given count.

```
|{1,2,3}| = i   ?   [(i=3)][..]      ▸ i = 3
|[1,2,3]| = 1   ?   [][()]           ▸ false: the list has 3 elements
```

### Membership — `e in c`

```
Boolean ::= <E> "in" <Collection<E>> #30
```

`e in c` holds when `e` is an element of the collection (a member of a set, or an element at any index of a list). With an unbound element it **enumerates** the members:

```
1 in {1,2,3}    ?   [()][]                       ▸ true
i in {1,2,3}    ?   [(i=1),(i=2),(i=3)][..]      ▸ enumerates members
1 in [1,2,3]    ?   [()][]                       ▸ true for lists too
```

### Subset / superset — `<` `>` `<=` `>=`

```
Boolean ::= <Set<E>> "<"  <Set<E>> #30,
            <Set<E>> ">"  <Set<E>> #30,
            <Set<E>> "<=" <Set<E>> #30,
            <Set<E>> ">=" <Set<E>> #30
```

`s1 < s2` holds when every element of `s1` is in `s2` — i.e. `s1 ⊆ s2`. Note this is the **non-strict** subset (it is backed by `containsAll`, so a set is a subset of itself); `s1 <= s2` is defined as `s1 < s2 | s1 = s2` and denotes the same relation, kept for symmetry with the integer comparison operators. `>`/`>=` are the mirror (superset).

```
{1,2}   < {1,2,3}   ?   [()][]      ▸ true
{}      < {1,2,3}   ?   [()][]      ▸ the empty set is a subset of anything
{1,2,3} < {}        ?   [][()]      ▸ false
```

### Set algebra — `&&` `||` `-`

```
Set<E> ::= <Set<E>> && <Set<E>> #60,   ▸ intersection
           <Set<E>> || <Set<E>> #60,   ▸ union
           <Set<E>> -  <Set<E>> #50    ▸ difference
```

```
{3,4,5} && {1,2,3} = s   ?   [(s={3})][..]
{3,4,5} || {1,2,3} = s   ?   [(s={1,2,3,4,5})][..]
{3,4,5} -  {1,2,3} = s   ?   [(s={4,5})][..]
```

### List concatenation — `+`

```
List<E> ::= <List<E>> + <List<E>> #50
```

```
[1,2,3] + [4,5] = l   ?   [(l=[1,2,3,4,5])][..]
[1,2,3] + []    = l   ?   [(l=[1,2,3])][..]
```

### List index — `e pos l`

```
Integer ::= <E> "pos" <List<E>> #40
```

`e pos l = i` relates an element `e` to its **0-based** index `i` in list `l`. It runs either way — find the index of an element, or find the element at an index — and a duplicated element yields one solution per occurrence:

```
2 pos [1,2,3] = i   ?   [(i=1)][..]      ▸ 2 sits at index 1
i pos [1,2,3] = 2   ?   [(i=3)][..]      ▸ index 2 holds the element 3
```

---

## Usage

A representative slice of `collectionsTest.nl`:

```
import nelumbo.collections

List<Integer>       l
Set<Integer>        s
Collection<Integer> c
Integer             i, v

s = {1,2,3}                     ?   [(s={1,2,3})][..]
{[i](|i|=10)} = s               ?   [(s={-10,10})][(s={0}),..]

|{1,2,3}| = i                   ?   [(i=3)][..]
i in {1,2,3}                    ?   [(i=1),(i=2),(i=3)][..]
{1,2} < {1,2,3}                 ?   [()][]

{3,4,5} && {1,2,3} = s          ?   [(s={3})][..]
{3,4,5} || {1,2,3} = s          ?   [(s={1,2,3,4,5})][..]
{3,4,5} -  {1,2,3} = s          ?   [(s={4,5})][..]

[1,2,3] + [4,5] = l             ?   [(l=[1,2,3,4,5])][..]
2 pos [1,2,3] = i               ?   [(i=1)][..]
```

Collection values print back in their literal form on the facts side.

---

## Status

`collections` provides type declarations, literal constructors, set-builder comprehension, and a working set of algebraic operations: cardinality (`|c|`), membership (`in`), subset/superset (`<` `>` `<=` `>=`), set intersection/union/difference (`&&` `||` `-`), list concatenation (`+`), and list indexing (`pos`). Still absent are higher-order operations such as `map` or `fold`. The native classes are `NSet` and `NList` (literals), `SetBuilder`/`BuildSet` (the comprehension form), and `Collections` (every operation listed above).

This is also the only stdlib module that demonstrates generic-type parameters in action. The mechanism is general: a user module can declare `Type T` and parameterise its own types and patterns the same way.

---

## Exports summary

Added to what `nelumbo.integers` and `nelumbo.logic` already export:

| Kind          | Names                                                                   |
|---|---|
| Types         | `Collection<E>`, `Set<E>`, `List<E>`                                     |
| Literals      | `{...}` for sets, `[...]` for lists                                      |
| Comprehension | `{[e](c)}` — set-builder notation                                       |
| Cardinality   | `\|c\|` — element count of any collection                               |
| Membership    | `e in c`                                                                 |
| Set relations | `<` `>` `<=` `>=` — subset / superset                                    |
| Set algebra   | `&&` intersection, `\|\|` union, `-` difference                         |
| List ops      | `+` concatenation, `e pos l` — 0-based index                            |

(The native predicates `build`, `size`, `indexOf`, `elementOf`, `subset`, `intersection`, `union`, `diff`, and `concat` are all `private`; the operator syntax above is their public surface.)

(The `Type E` declaration introduces the parameter binding inside `collections.nl`; the *mechanism* of generic-type parameters is supplied by `nelumbo.lang` and is available to importers regardless of `collections`.)

---

## See also

- [`integers.md`](integers.md) — the module `collections` imports
- [`built-in-tokens.md`](../built-in-tokens.md#structural-markers--repetition-and-grouping) — the repetition markers `<(>`, `<)*>`, `<,>` used in the literal declarations
- [`three-valued-logic.md`](../three-valued-logic.md) — the quantifier semantics set-builder notation is built on
- [`logic.md`](logic.md) — the `E[...]`/`A[...]` quantifiers `{[e](c)}` is a cousin of
- [`collectionsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/collectionsTest.nl) — executable specification
