# Visibility — scopes, `private`, `hidden`

Nelumbo has two mechanisms for controlling what names are visible where: **scope blocks** using `{ }` and **visibility modifiers** (`private`, `hidden`, `visible`). This page describes both.

---

## Scope blocks — `{ }`

A block introduces a lexical scope. Any declaration inside the block — imports, types, patterns, variables, rules, facts, tests — is local to the block. Once the closing `}` is reached, those names are no longer in scope.

```
{
   Aa :: Object
   Aa ::= XXX
   Aa x
   x = XXX ? [(x=XXX)][..]
}

{
   Bb :: Object
   Bb ::= XXX
   Bb x
   x = XXX ? [(x=XXX)][..]
}
```

The two blocks above each define a type, a literal, a variable, and a test. None of them collide, even though both use the name `XXX` and both use `x`. The scopes are independent.

### What scopes are good for

- **Isolating a DSL example** in a file that already imports common modules
- **Wrapping a pattern transformation expansion** (see [`../guides/language-transformations.md`](../guides/language-transformations.md) — the `::> { ... }` block is a scope)
- **Making two versions of the same construct coexist** in a single file for comparison

### `{Namespace}`

A scope may prefix a pattern declaration with `{Namespace}` to mark it as belonging to the current scope's namespace:

```
{
   Aa :: Object
   {Namespace} Aa ::= XXX
   Aa x
   x = XXX ? [(x=XXX)][..]
}
```

This is used when a transformation or tool needs to tell declarations in different scopes apart even when they share a name. For most hand-written code, ordinary scoping is enough.

---

## Visibility modifiers

Three modifiers control how widely a pattern is visible:

| Modifier | Scope of visibility |
|---|---|
| *(none)* — the default | Exported from the module; imports see it |
| `private` | Visible only within the module (or scope) where it is declared |
| `hidden` | Visible for parsing but not for use at call sites — see below |

The `visible` keyword is the complement of `hidden`; see the section on hidden declarations below.

### `private` — module-internal

A `private` declaration is not exported. It is a detail of the module's implementation.

```
private Boolean ::= eq(<Object>, <Object>)  @org.modelingvalue.nelumbo.logic.Equal
```

From `logic.nl`. The `eq` predicate is the native equality primitive; it is used by the `=` rule inside `logic.nl` but is not something callers should reach for directly — they should use `=`.

Other `private` patterns in the stdlib include `add`, `mult`, `string_concat`, `string_length`, `integer_string`. All of them are native-backed primitives that the module wraps in friendlier operators.

### `hidden` — callable but not at call sites

`hidden` restricts visibility in a more nuanced way: a hidden hole is present for parsing but cannot be supplied at the call site; it has to come from another rule or declaration.

The [`hidden.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/hidden.nl) example illustrates the mechanism:

```
{
  Integer ::= <hidden Integer> && <Integer> #35
  Integer ::= <visible Integer> & <Integer> #35

  hidden Integer x
  Integer y, z

  x & y = z  <=>  x * 100 + y = z
  && y = z   <=>  x & y = z

  && 11 = 2211 ? [(x=22)][..]
  x & 11 = 2211 ? [(x=22)][..]
}
```

Notes on this example:

- `<hidden Integer>` is a hole that accepts an integer-typed value which must be a `hidden` variable (or derivable as one). It is absent at the call site: `&& 11 = 2211` — there is no left operand in the source.
- `<visible Integer>` is the opposite: the hole requires a *non*-hidden value.
- `hidden Integer x` declares `x` as a hidden variable — it will fill hidden holes.

Hidden declarations are how Nelumbo supports operators with implicit operands and similar DSL constructs. They are an advanced feature.

---

## Interaction with imports

Imports honour visibility. A module sees:

- Every non-`private`, non-`hidden` declaration in the imported module
- Nothing else

Transitive imports behave transitively: if module A imports B, and B imports C, then A sees C's exported declarations (via B re-exporting its imports). The standard library relies on this: `nelumbo.integers` imports `nelumbo.logic`, so any module that imports `integers` also has access to the logic primitives without an explicit import.

---

## Interaction with scopes

Visibility modifiers apply relative to the declaration's scope. A `private` pattern inside a `{ }` block is private to that block; outside, it does not exist at all (because of the scope), so the modifier is equivalent to plain scoping in that case.

`private` at module top level is what matters most — that is the module's boundary between implementation and API.

---

## See also

- [`grammar.md`](grammar.md) — where these declarations fit syntactically
- [`../guides/modules-and-imports.md`](../guides/modules-and-imports.md) — the module system end to end (Phase 4)
- [`hidden.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/hidden.nl) — working hidden-operator example
- [`scoping.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/scoping.nl) — working scope example
