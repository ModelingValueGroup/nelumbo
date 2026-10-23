# Visibility — scopes, `private`, `hidden`

> **Level:** `nelumbo.lang`. Available after `import nelumbo.lang`, or transitively through `nelumbo.logic` or any package.

Nelumbo has two mechanisms for controlling what names are visible where: **scope blocks** using `{ }` and **visibility modifiers** (`private`, `hidden`, `visible`). This page describes both.

---

## Scope blocks — `{ }`

A block introduces a lexical scope. Any declaration inside the block — imports, types, patterns, variables, rules, facts, tests — is local to the block. Once the closing `}` is reached, those names are no longer in scope.

```nelumbo
{
   Aa :: Object
   private Aa ::= XXX
   Aa x
   x = XXX ? [(x=XXX)][..]
}

{
   Bb :: Object
   private Bb ::= XXX
   Bb x
   x = XXX ? [(x=XXX)][..]
}
```

The two blocks above (from [`scoping.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/scoping.nl)) each define a type, a literal, a variable, and a test. None of them collide, even though both use the name `XXX` and both use `x`. The scopes are independent; the `private` modifier on each `::=` keeps the `XXX` literal local to its block.

### What scopes are good for

- **Isolating a DSL example** in a file that already imports common modules
- **Wrapping a pattern transformation expansion** (see [`../guides/language-transformations.md`](../../guides/language-transformations.md) — the `::> { ... }` block is a scope)
- **Making two versions of the same construct coexist** in a single file for comparison

### Custom scope types

Any type that specialises `Namespace` introduces a scope of its own — no explicit `{Namespace}` prefix is needed (and none is accepted). Declaring a functor `private` within such a type confines it to that namespace, exactly as `private` does inside a `{ ... }` block.

```nelumbo
{
   Aa :: Object
   private Aa ::= XXX
   Aa x
   x = XXX ? [(x=XXX)][..]
}
```

From [`scoping.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/scoping.nl). The `private` keeps `XXX` local to the block; outside the block the literal does not exist.

---

## Visibility modifiers

Two modifiers change what a declaration does beyond its scope:

| Modifier | Effect |
|---|---|
| *(none)* — the default | Exported from the module; imports see it |
| `private` | Visible only within the module (or scope) where it is declared |
| `hidden` | On a variable: filled in where the source leaves an operand out — see below |

`<hidden T>` and `<visible T>` holes in a pattern say whether that operand may be left out; see the section on `hidden` below.

### `private` — module-internal

A `private` declaration is not exported. It is a detail of the module's implementation.

```nelumbo
private Boolean ::= eq(<Literal>, <Literal>)  @org.modelingvalue.nelumbo.logic.Equal
```

From `logic.nl`. The `eq` predicate is the native literal-equality primitive; it is used by the rule `l1 = l2 <=> eq(l1, l2)` inside `logic.nl` but is not something callers should reach for directly — they should use `=`. (The general-case `=` operator on `Object` is bound to a separate, public native `NIs`.)

Other `private` patterns in the stdlib include `add`, `mult`, `string_concat`, `string_length`, `integer_string`. All of them are native-backed primitives that the module wraps in friendlier operators.

### `hidden` — implicit operands

A `hidden` variable is filled in by the parser where the source leaves an operand out. When an expression starts with an operator that needs a left operand, the parser uses the hidden variable in scope as that operand. The [`hidden.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/hidden.nl) example starts with the simplest case:

```nelumbo
hidden Integer a

>10 & <20 & =15 ? [(a=15)][(a=10),(a=20),..]
```

`>10 & <20 & =15` reads as `a>10 & a<20 & a=15`. This works for any operator with a left operand, also inside a rule body, where `*100+y` reads as `x*100+y` (see below).

- A hidden variable is declared like any variable, with `hidden` in front: `hidden Integer a`.
- One hidden variable per scope. A second one in the same scope, even of another type, is an error. A hidden variable of an outer scope is also used inside nested `{ }` blocks, so give each use its own block, as `hidden.nl` does.
- The result names the hidden variable's value without its name: the test above prints `a>10&a<20&a=15 ? [(15)][(10),(20),..]`. In an expected result, still write the name (`(a=15)`). A bare value is only accepted for a [function query](../logic/statements.md#function-queries), whose result variable is a hidden variable the query introduces itself.
- A printed node shows the variable the parser filled in (`a>10`).

#### `<hidden T>` and `<visible T>` holes

A pattern can say whether its left operand may be implicit:

| Hole | Accepts |
|---|---|
| `<T>` | a written operand, or the hidden variable when the operand is left out |
| `<hidden T>` | only the hidden variable, filled in because the operand is left out |
| `<visible T>` | only a written operand |

`<visible T>` still accepts the hidden variable when you write its name. That lets one operator have a shorthand with a meaning of its own:

```nelumbo
Integer ::= <hidden Integer> $$ <Integer> #35
Integer ::= <visible Integer> $ <Integer> #35

hidden Integer x
Integer y

x$y ==  *100+y
$$y ==  x$y

$$11=2211 ? [(x=22)][..]
x$11=2211 ? [(x=22)][..]
```

`x$y` is `x*100+y`, with `*100+y` using the implicit `x`. `$$y` exists only in the shorthand form and means `x$y`. The other two combinations are syntax errors: `x$$11` (`$$` takes no written operand) and `$11` (`$` needs one).

#### Implicit objects

A hidden hole in front of a name gives a name that reads as a property of an implicit object:

```nelumbo
Test   :: Object

String ::= <Test>.name       #35,
           <hidden Test>name #35

Test   ::= T1, T2

fact T1.name = "T1", T2.name = "T2"

hidden Test t

name ==  t.name

name="T1" ? [(t=T1),..][..]
```

`name` on its own is the name of the hidden `t`, so `name="T1"` asks which `Test` is called `"T1"`. [`transformation.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/transformation.nl) uses this for derived attributes. Its `attr` transformation adds `<hidden OT>AN` next to `<OT>.AN`, so a rule about one object can name that object's other attributes without naming the object:

```nelumbo
hidden Person p

address := street + " " + str(number) if number >= 100
```

Here `address`, `street` and `number` are all attributes of the same implicit `p`.

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
- [`hidden.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/hidden.nl) — implicit operands, hidden/visible holes, implicit objects
- [`transformation.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/transformation.nl) — implicit objects in derived attributes
- [`scoping.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/scoping.nl) — working scope example
