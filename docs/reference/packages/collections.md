# `nelumbo.collections`

> **Level:** optional package `nelumbo.collections`. Write `import nelumbo.collections`; it brings in `nelumbo.integers`, `nelumbo.logic` and `nelumbo.lang` transitively.

Generic sets and lists, with membership, set algebra, list concatenation and indexing, and the higher-order operations `where` (filter), `map` and `sort`. The higher-order operations take [lambdas](../logic/lambdas.md), which are declared in `nelumbo.logic`, not here. This is also the only stdlib module that uses Nelumbo's generic-type parameter mechanism.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/collections/collections.nl) — 79 lines.

**Import:**

```
import nelumbo.collections
```

`nelumbo.collections` imports `nelumbo.integers` (and thus `nelumbo.logic`, which supplies `Boolean`, the quantifiers and the `Lambda` types).

---

## Types

```
Type E, F

Collection<E>  :: Object
Set<E>         :: Collection<E>
List<E>        :: Collection<E>
```

- `Type E, F` introduces `E` and `F` as generic type parameters — the same mechanism is available in user code (see also `Type P` in `lang.nl` for the generic parenthesization rule `P ::= (<P>)`). `F` is only used by `map`, whose input and output element types differ.
- `Collection<E>` is the common supertype.
- `Set<E>` and `List<E>` are both subtypes of `Collection<E>`. A variable of type `Collection<E>` can hold either; cardinality, membership, `map` and `sort` accept any `Collection<E>`.

---

## Literals

```
{Struct,Set<E>}  ::= { <(> <E> <,> , <)*> }  @nelumbo.collections.NSet
{Struct,List<E>} ::= [ <(> <E> <,> , <)*> ]  @nelumbo.collections.NList
```

| Syntax        | Type      | Notes                                       |
|---|---|---|
| `{}`          | `Set<E>`  | empty set                                   |
| `{x, y, z}`   | `Set<E>`  | unordered, no duplicates                    |
| `{[e](c)}`    | `Set<E>`  | set-builder (comprehension) — see below     |
| `[]`          | `List<E>` | empty list                                  |
| `[x, y, z]`   | `List<E>` | ordered, duplicates preserved               |

The `<(> ... <,> , <)*>` fragment is the zero-or-more comma-separated repetition (see [`built-in-tokens.md`](../lang/built-in-tokens.md#structural-markers--repetition-and-grouping)). The element type `E` is inferred from the surrounding context — the declared type of the receiving variable or pattern hole.

The result type `{Struct,Set<E>}` is an intersection: a collection literal is a `Set<E>` *and* a `Struct`. `Struct` values have **structural equality** — two literals are equal when their elements are — which is what makes `{1,2}={2,1}` true and `[1,2]=[1,2]` decidable without any rule.

Elements may be expressions; they are evaluated before the collection is formed:

```
[3+4,5+6]=l   ?   [(l=[7,11])][..]
```

---

## Lambdas and set-builder notation

The higher-order operations of this module take a lambda, written `[x](body)` — the bracketed list names the bound variables, the parenthesized body is an expression over them. Lambdas are declared by `nelumbo.logic` (types `Lambda1<A1,R>` … `Lambda6<…>`, each with its `[…](…)` syntax) and are described in [`lambdas.md`](../logic/lambdas.md). This module uses two shapes:

| Type                     | Meaning                                         | Used by                    |
|---|---|---|
| `Lambda1<E,Boolean>`     | a predicate over one element                    | set-builder, `where`       |
| `Lambda1<F,E>`           | a function from one element to a value          | `map`                      |
| `Lambda2<E,E,Boolean>`   | a binary "comes before" relation                | `sort`                     |

The bound variables must be **declared variables** (`Integer i` …) — a lambda binds, it does not declare.

### Set-builder — `{[e](c)}`

```
Set<E> ::= { <Lambda1<E,Boolean>> }
```

`{[e](c)}` denotes *the set of all `e` for which `c` is a fact*. It is a `Lambda1<E,Boolean>` wrapped in `{ }`, and one rule hands it to the native `build` predicate:

```
Lambda1<E,Boolean> leb
Set<E> s

{leb} = s   <=>   build(leb, s)
```

`build` is `private`, backed by `nelumbo.collections.BuildSet`. It is a **quantifier**: like `E[...]` and `A[...]`, it evaluates the lambda body under many bindings of the bound variable and strips that variable from the result, collecting the witnessing values into a set.

```
Integer i   Set<Integer> s

{[i](|i|=10)} = s   ?   [(s={-10,10})][(s={0}),..]
```

The bound variable `i` ranges over the condition `|i| = 10`. Its two solutions, `-10` and `10`, are gathered into the fact `s = {-10, 10}`. The falsehoods side carries `(s={0})`: `i = 0` is a proven *non*-member (`|0| = 10` is false), so the singleton `{0}` is a proven falsehood of the builder, with `..` standing in for the rest of the open domain.

Set-builder takes **exactly one** bound variable (it is a `Lambda1`); `{[i,j](…)}` does not parse. Because it is built on the three-valued quantifier machinery, it inherits the completeness behaviour of `E[...]`/`A[...]` (see [`three-valued-logic.md`](../logic/three-valued-logic.md)). In particular, a condition that cannot be enumerated exhaustively gives an *open* result: `{[i](i in {1,2,3} & i>1)} = s` reports `[(s={2,3}),..][(s={1}),..]`, not a closed `[(s={2,3})][..]`.

---

## Operations

Every operation is a relation, so it works in both directions where that is meaningful: supply the result and check it, or leave it a variable and have it computed. The native worker for all of them is the `Collections` class; the operator syntax is the public surface, wired to `private` predicates:

```
private Boolean ::= build(<Lambda1<E,Boolean>>, <Set<E>>)                    @nelumbo.collections.BuildSet,
                    size(<Collection<E>>, <Integer>)                         @nelumbo.collections.Collections,
                    indexOf(<List<E>>, <E>, <Integer>)                       @nelumbo.collections.Collections,
                    elementOf(<Set<E>>, <E>)                                 @nelumbo.collections.Collections,
                    subset(<Set<E>>, <Set<E>>)                               @nelumbo.collections.Collections,
                    intersection(<Set<E>>, <Set<E>>, <Set<E>>)               @nelumbo.collections.Collections,
                    union(<Set<E>>, <Set<E>>, <Set<E>>)                      @nelumbo.collections.Collections,
                    diff(<Set<E>>, <Set<E>>, <Set<E>>)                       @nelumbo.collections.Collections,
                    concat(<List<E>>, <List<E>>, <List<E>>)                  @nelumbo.collections.Collections,
                    setFilter(<Set<E>>, <Lambda1<E,Boolean>>, <Set<E>>)      @nelumbo.collections.Collections,
                    listFilter(<List<E>>, <Lambda1<E,Boolean>>, <List<E>>)   @nelumbo.collections.Collections,
                    map(<Collection<F>>, <Lambda1<F,E>>, <List<E>>)          @nelumbo.collections.Collections,
                    sort(<Collection<E>>, <Lambda2<E,E,Boolean>>, <List<E>>) @nelumbo.collections.Collections
```

| Operation                         | Pattern                                              | `#N` | Result      |
|---|---|---|---|
| cardinality                       | `\| <Collection<E>> \|`                               | 35   | `Integer`   |
| membership                        | `<E> in <Collection<E>>`                             | 30   | `Boolean`   |
| subset / superset                 | `<Set<E>> < <=  > >= <Set<E>>`                       | 30   | `Boolean`   |
| intersection / union / difference | `<Set<E>> && \|\| - <Set<E>>`                        | 60 / 60 / 50 | `Set<E>` |
| concatenation                     | `<List<E>> + <List<E>>`                              | 50   | `List<E>`   |
| index                             | `<E> pos <List<E>>`                                  | 40   | `Integer`   |
| filter                            | `<Set<E>> where <Lambda1<E,Boolean>>` / the `List` form | 37 | `Set<E>` / `List<E>` |
| map                               | `<Collection<F>> map <Lambda1<F,E>>`                 | 37   | `List<E>`   |
| sort                              | `<Collection<E>> sort <Lambda2<E,E,Boolean>>`        | 37   | `List<E>`   |

### Cardinality — `|c|`

```
Integer ::= | <Collection<E>> | #35
```

`|c| = n` is the number of elements in any `Collection<E>` (set *or* list). Computes the count from the collection, or checks a given count. With the collection itself unknown the answer is unknown.

```
|{1,2,3}| = i   ?   [(i=3)][..]      ▸ i = 3
|[1,2,3]| = 1   ?   [][()]           ▸ false: the list has 3 elements
|{1,1,2}| = i   ?   [(i=2)][..]      ▸ duplicates in a set collapse
|s| = 4          ?   [..][..]        ▸ s is free: unknown
```

### Membership — `e in c`

```
Boolean ::= <E> "in" <Collection<E>> #30
```

`e in c` holds when `e` is an element of the collection (a member of a set, or an element at any index of a list). With an unbound element it **enumerates** the members of a set:

```
1 in {1,2,3}    ?   [()][]                       ▸ true
4 in [1,2,3]    ?   [][()]                       ▸ false, for lists too
i in {1,2,3}    ?   [(i=1),(i=2),(i=3)][..]      ▸ enumerates members
```

For a list, `in` is defined through the index relation (`e in l <=> E[i](e pos l = i)`); for a set it uses the native `elementOf`.

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
{1,2}   >= {1}      ?   [()][]
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

Both operands must be known; the result may be given to check it (`{1,2} - {1,2,3} = {}` holds).

### List concatenation — `+`

```
List<E> ::= <List<E>> + <List<E>> #50
```

```
[1,2,3] + [4,5] = l   ?   [(l=[1,2,3,4,5])][..]
[1,2,3] + []    = l   ?   [(l=[1,2,3])][..]
```

Both operands must be known; `concat` does not split a known result.

### List index — `e pos l`

```
Integer ::= <E> "pos" <List<E>> #40
```

`e pos l = i` relates an element `e` to its **0-based** index `i` in list `l`. It runs either way — find the index of an element, or find the element at an index — and a duplicated element yields one solution per occurrence:

```
2 pos [1,2,3] = i   ?   [(i=1)][..]          ▸ 2 sits at index 1
i pos [1,2,3] = 2   ?   [(i=3)][..]          ▸ index 2 holds the element 3
2 pos [1,2,2] = i   ?   [(i=1),(i=2)][..]    ▸ one solution per occurrence
5 pos [1,2,3] = i   ?   [][..]               ▸ not present
```

(An index outside the list is a known problem — see [`bugs/collections-index-out-of-range.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/bugs/collections-index-out-of-range.nl).)

### Filter — `where`

```
Set<E>  ::= <Set<E>>  where <Lambda1<E,Boolean>> #37
List<E> ::= <List<E>> where <Lambda1<E,Boolean>> #37
```

`c where [x](p)` keeps the elements for which the lambda body holds. The result has the type of the left operand — a set stays a set, a list stays a list (order and duplicates preserved):

```
{1,2,3}   where [i](i>1) = s   ?   [(s={2,3})][..]
[1,2,2,3] where [i](i>1) = l   ?   [(l=[2,2,3])][..]
[1,2,3]   where [i](i>5) = l   ?   [(l=[])][..]
```

### Map — `map`

```
List<E> ::= <Collection<F>> map <Lambda1<F,E>> #37
```

`c map [x](expr)` applies the lambda to every element of any collection and yields a **list** (in the collection's iteration order) of the results. The element type may change — that is what the second type parameter `F` is for:

```
[1,2,3] map [i](i*i) = l   ?   [(l=[1,4,9])][..]
{1,2,3} map [i](i*i) = l   ?   [(l=[1,4,9])][..]    ▸ a set maps to a list
```

### Sort — `sort`

```
List<E> ::= <Collection<E>> sort <Lambda2<E,E,Boolean>> #37
```

`c sort [a,b](before)` orders any collection into a list. The two-variable lambda is the "comes before" test: `a` is placed ahead of `b` when the body holds.

```
[3,1,2] sort [ib,ia](ib<ia) = l   ?   [(l=[1,2,3])][..]    ▸ ascending
[3,1,2] sort [ib,ia](ib>ia) = l   ?   [(l=[3,2,1])][..]    ▸ descending
{3,1,2} sort [ib,ia](ib<ia) = l   ?   [(l=[1,2,3])][..]    ▸ a set sorts to a list
```

The comparison can be any relation over the elements, e.g. sorting sets by size: `[{1,2},{1}] sort [s1,s2](|s1|<|s2|) = ls` gives `[{1},{1,2}]`.

> `where`, `map` and `sort` need the collection and the lambda known; with either unknown the result is unknown. If evaluating the lambda body cannot be completed (for example it depends on an unbound variable), the whole operation reports an incomplete result rather than a wrong one.

---

## Usage

A representative slice of `collectionsTest.nl`:

```
import nelumbo.collections

List<Integer>       l
Set<Integer>        s,   s1,s2
Collection<Integer> c
Integer             i, ib, ia
List<Set<Integer>>  ls

s = {1,2,3}                            ?   [(s={1,2,3})][..]
{[i](|i|=10)} = s                      ?   [(s={-10,10})][(s={0}),..]

|{1,2,3}| = i                          ?   [(i=3)][..]
i in {1,2,3}                           ?   [(i=1),(i=2),(i=3)][..]
{1,2} < {1,2,3}                        ?   [()][]

{3,4,5} && {1,2,3} = s                 ?   [(s={3})][..]
[1,2,3] + [4,5] = l                    ?   [(l=[1,2,3,4,5])][..]
2 pos [1,2,3] = i                      ?   [(i=1)][..]

{1,2} where [i](i>1) = s               ?   [(s={2})][..]
[1,2,2,3] map [i](i+1) = l             ?   [(l=[2,3,3,4])][..]
[3,2,2,1] sort [ib,ia](ib<ia) = l      ?   [(l=[1,2,2,3])][..]
```

Collection values print back in their literal form on the facts side. The CSP-style sudoku solver [`sudoku-4x4-csp.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/sudoku-4x4-csp.nl) is a larger user of `map` over lists of lists.

---

## Status

`collections` provides type declarations, literal constructors, set-builder comprehension, the algebraic operations (cardinality, membership, subset/superset, set intersection/union/difference, list concatenation, list indexing) and the lambda-based higher-order operations `where`, `map` and `sort`. Still absent are folds/reductions (`fold`, `sum`), element access by index in expression position (use `e pos l = i`), `head`/`tail`, and union/intersection for lists. The native classes are `NSet` and `NList` (literals), `BuildSet` (the comprehension form) and `Collections` (every other operation); the lambda value itself is `nelumbo.logic.Lambda`.

---

## Exports summary

Added to what `nelumbo.integers` and `nelumbo.logic` already export:

| Kind          | Names                                                                        |
|---|---|
| Types         | `Collection<E>`, `Set<E>`, `List<E>`; type parameters `E`, `F`                |
| Literals      | `{...}` for sets, `[...]` for lists                                          |
| Comprehension | `{[e](c)}` — set-builder notation                                            |
| Cardinality   | `\|c\|` — element count of any collection                                    |
| Membership    | `e in c`                                                                     |
| Set relations | `<` `>` `<=` `>=` — subset / superset                                        |
| Set algebra   | `&&` intersection, `\|\|` union, `-` difference                              |
| List ops      | `+` concatenation, `e pos l` — 0-based index                                 |
| Higher-order  | `c where [x](p)`, `c map [x](e)`, `c sort [a,b](p)`                          |

(The native predicates `build`, `size`, `indexOf`, `elementOf`, `subset`, `intersection`, `union`, `diff`, `concat`, `setFilter`, `listFilter`, `map` and `sort` are all `private`; the operator syntax above is their public surface.)

(The `Type E, F` declaration introduces the parameter bindings inside `collections.nl`; the *mechanism* of generic-type parameters is supplied by `nelumbo.lang` and is available to importers regardless of `collections`.)

---

## See also

- [`lambdas.md`](../logic/lambdas.md) — the `[x](body)` lambdas that `where`, `map`, `sort` and `{[e](c)}` take
- [`integers.md`](integers.md) — the module `collections` imports
- [`built-in-tokens.md`](../lang/built-in-tokens.md#structural-markers--repetition-and-grouping) — the repetition markers `<(>`, `<)*>`, `<,>` used in the literal declarations
- [`three-valued-logic.md`](../logic/three-valued-logic.md) — the quantifier semantics set-builder notation is built on
- [`logic/index.md`](../logic/index.md) — the `E[...]`/`A[...]` quantifiers `{[e](c)}` is a cousin of
- [`collectionsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/collectionsTest.nl) — executable specification
