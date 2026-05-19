# Language transformations — `::>`

> **Status: under construction.** The `::>` operator and the transformation mechanism it introduces are actively evolving. The design and syntax described on this page reflect the current state of the working examples (`transformation.nl`, `deHet.nl`), but **details are likely to change between releases**. Treat this guide as a working overview, not a stable specification.

The `::>` operator is what makes Nelumbo a **language workbench** rather than just a logic engine. It lets you define new keywords and constructs that **expand into ordinary Nelumbo declarations** when the user writes them. The transformation is applied at parse/load time, so by the time the reasoner runs there are no macros left — just regular types, patterns, facts, and rules.

If you have read the reference pages, you already know all the ingredients `::>` produces. This guide explains how to compose them.

---

## The essential shape

```
pattern-with-variables  ::>  {
    ...declarations that use those variables...
}
```

Both sides of `::>` share the same variables, declared in the surrounding scope. When the user writes source matching the left-hand side, the engine **binds those variables to what the user wrote** — using the same binding machinery it uses during query evaluation — and then emits the declarations in the block with those bindings applied. The result is added to the program as if it had been written directly.

A concrete example, from [`transformation.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/transformation.nl):

```
Root ::= attr <Type> <n> <Type>  #100

Type OT, AT
NAME AN

attr OT AN AT  ::>  {
    AT       ::= <OT>.AN
    Root     ::= <OT>.AN := <AT>
    private FactType ::= AN(<OT>,<AT>)
    OT o
    AT a
    o.AN = a  <=>  AN(o,a)
    o.AN := a ::> {
        fact AN(o,a)
    }
}
```

Reading this top to bottom:

1. The first line, `Root ::= attr <Type> <n> <Type> #100`, **introduces the new keyword** `attr` into the top-level grammar. `Root` is the special type Nelumbo uses for statement-level productions; extending `Root` means "the user can now write this at the top level of their program." The keyword `attr` is followed by two type names and an identifier (the attribute's name).
2. The next block declares **variables for the transformation** — `OT` and `AT` of type `Type`, `AN` of type `NAME`. These are ordinary variable declarations; they just happen to be used by a transformation rather than by a rule.
3. The `attr OT AN AT ::> { ... }` line is the transformation itself. When the user writes something like `attr Person name String`, the engine binds `OT = Person`, `AN = name`, `AT = String` — via the same machinery it uses to bind logical variables in queries — and then emits the declarations in the block with those bindings applied.

The result: when the user writes

```
attr Person name String
```

the effect is **exactly the same** as if they had written

```
String ::= <Person>.name
Root   ::= <Person>.name := <String>
private FactType ::= name(<Person>, <String>)
Person o
String a
o.name = a  <=>  name(o, a)
o.name := a ::> {
    fact name(o, a)
}
```

Eight declarations, none of them surprising. The transformation is just an abbreviation.

---

## Variables in transformations

The variables that appear in a transformation — `Type OT`, `NAME AN`, `Lidwoord lw` — are declared and bound by **the same mechanism Nelumbo uses to bind logical variables during query evaluation**. There is no separate template-matching subsystem; the engine's binding machinery is simply being run in a different mode.

This matters because variables in `Nelumbo` come in one underlying flavour — *something the engine finds a value for* — and the declaration syntax is the same regardless of what that value represents:

| Declaration | What the engine binds it to | Used during |
|---|---|---|
| `Integer n`   | an integer value              | query evaluation |
| `Person a`    | a `Person` literal or result  | query evaluation |
| `Type OT`     | a type name supplied by the user | transformation expansion |
| `NAME AN`     | an identifier supplied by the user | transformation expansion |
| `Lidwoord lw` | a `Lidwoord` literal (`de` or `het`) the user wrote | transformation expansion |

In ordinary rules, variables are bound by navigating relations and reasoning three-valuedly over the results. In transformations, the same variables are bound by matching the user's source syntax against the transformation's left-hand side — **same binding, no fact/falsehood reasoning layered on top**.

Some of these variable types — `Type`, `NAME` — exist primarily for transformations. Others, like `Lidwoord`, are ordinary user-defined types that happen to work in transformation positions because the engine binds them the same way. You can use any type in this role; what matters is that the engine can bind variables of that type by matching against what the user wrote.

### Why this matters for reading `.nl` files

When you see `Type OT` at the top of a block containing `::>`, you are looking at a variable declaration — exactly the same kind you would see in a rule-bearing block. The difference is not in the variable itself, but in the context: in a `<=>` rule, the engine binds the variable during reasoning; in a `::>` transformation, the engine binds it during expansion.

This is also why a transformation's variables can be wrapped in a `{ ... }` scope — scopes work the same way for both uses.

---

## Extending `Root`

The `Root` type is Nelumbo's hook for **top-level statement syntax**. To introduce a new keyword, you declare a `Root` pattern:

```
Root ::= attr <Type> <n> <Type>  #100
```

Reading this: "at the top level, the user may write `attr` followed by a type, an identifier, and another type." The `#100` precedence keeps the statement from colliding with other parsing rules.

Once declared, `attr ...` is a valid top-level construct. But it doesn't *do* anything yet — you still have to pair it with a `::>` transformation that gives it meaning:

```
attr OT AN AT  ::>  {
    ...
}
```

The pattern on the left of `::>` mirrors the `Root` declaration, with metavariables (`OT`, `AN`, `AT`) where the `<...>` holes were.

> This "declare, then transform" pattern is characteristic of `::>` usage. The declaration tells the parser what to accept; the transformation tells the engine what it means.

---

## Nesting transformations

A transformation block can itself contain a `::>`. Look again at the inner clause from `transformation.nl`:

```
attr OT AN AT  ::>  {
    ...
    Root     ::= <OT>.AN := <AT>
    ...
    o.AN := a ::> {
        fact AN(o,a)
    }
}
```

The outer transformation expands `attr Person name String` into many things, one of which is another `Root` pattern: `Person.name := String`. That newly introduced pattern is **also** given a transformation — a nested `::>` that turns every use of the assignment syntax (e.g. `Piet.name := "Piet"`) into a `fact` declaration.

So the chain is:

```
User writes:           Transformation expands to:        Further expands to:

attr Person name String
                   ->  String ::= <Person>.name
                       Root ::= <Person>.name := <String>
                       private FactType ::= name(<Person>,<String>)
                       Person o
                       String a
                       o.name = a <=> name(o,a)
                       o.name := a ::> { fact name(o,a) }

Piet.name := "Piet"
                                                     ->  fact name(Piet, "Piet")
```

A nested transformation has **the same semantics as the outer one**: it binds the variables on its left-hand side against user syntax and emits the declarations on its right-hand side. There is no inner-vs-outer distinction in mechanism, and no special "action" mode — `fact name(o, a)` is just another declaration, no different from a `::=` or a `<=>`. Both transformations run at compile time, so nesting them carries no runtime cost.

---

## A second worked example — natural-language DSL

The [`deHet.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/deHet.nl) example shows a transformation used to build a **Dutch-language DSL** for declaring and querying attributes. The user ends up writing:

```
attr Persoon de naam String
attr Persoon het adres String

de naam van Jan is "Jan"
het adres van Piet is "Kalverstraat"

wat is de naam van Jan ?
wat is het adres van Piet ?
```

The transformation is structured the same way as the first example, but richer:

```
Lidwoord :: Object
Lidwoord ::= de, het

Root ::= "attr" <Type> <Lidwoord> <n> <Type>  #100

{
  Type OT, AT
  Lidwoord lw
  NAME n

  attr OT lw n AT ::> {
     Root ::= <lw> n van <OT> is <AT>              #0
     Root ::= wat is <lw> n van <OT> ?             #0
     private FactType ::= n(<OT>, <AT>)
     OT o
     AT a
     lw n van o is a ::> {
        fact n(o, a)
     }
     wat is lw n van o ? ::> {
        n(o, a) ?
     }
  }
}
```

Two things to notice:

- **`Lidwoord`** (Dutch for *article*, as in "the") is an ordinary user-defined type with two literals `de` and `het`. But in the transformation's metavariable list (`Lidwoord lw`), it is acting as a meta-type, because the transformation uses it syntactically. The same type can serve both roles.
- **Two inner `::>` blocks** handle the two user-level actions: "say X is Y" (turned into a `fact`) and "ask what X is" (turned into a query). The transformation therefore defines not just syntax, but the full user-facing semantics, in one place.

When the user writes `attr Persoon de naam String`, all of the following happen:

- The `Root` grammar gains two new patterns: `de naam van Persoon is String` and `wat is de naam van Persoon ?`
- A private fact type `naam(Persoon, String)` is declared
- Two transformations are registered that turn user uses of those new Root patterns into facts and queries respectively

The user then gets to write natural Dutch and have it work.

---

## What you can put inside `::>`

A transformation block accepts **any declaration or statement** that is legal at the point where the block expands. In practice this means the body of a `::>` block can contain:

- Type declarations (`T :: S`)
- Pattern declarations (`T ::= P`, with or without `private`/`hidden`)
- `FactType` declarations
- Variable declarations
- Rules (`L <=> R`)
- Fact assertions (`fact E`)
- Further `::>` transformations (nested)
- Queries and tests (`E ?`, `E ? [F][N]`)

It is the same vocabulary you would use at the top level of a `.nl` file. The transformation is not a second, smaller language — it is a templated fragment of Nelumbo itself.

---

## Scoping

Transformations interact with scope blocks. The `deHet.nl` example puts the whole metavariable list and transformation inside a `{ ... }` block:

```
{
  Type OT, AT
  Lidwoord lw
  NAME n

  attr OT lw n AT ::> {
     ...
  }
}
```

This keeps the metavariables `OT`, `AT`, `lw`, `n` local to that block. Without the scope, they would linger in the file's namespace and could collide with other names. **Wrapping a transformation's metavariables in a `{ ... }` is a useful hygiene practice.**

Inside the transformation body, declarations added by the expansion live in the scope where the user wrote the invocation — not inside a hidden inner scope. So `attr Person name String` adds `Person o`, `String a`, and the rules to the surrounding scope. This is usually what you want; it is why the transformed code can be referenced from ordinary user code later in the file.

---

## When to reach for a transformation

Transformations are powerful, which means they are also easy to overuse. A good rule of thumb:

- **Use a rule (`<=>`)** when the same abstraction can be expressed as an equivalence over values.
- **Use multiple `::=` patterns** when you want several surface syntaxes for the same underlying relation.
- **Use a transformation (`::>`)** only when the abstraction you want introduces *structural* declarations — new types, new fact types, new patterns — that depend on the arguments.

The `attr` example is a good test case: `attr Person name String` introduces a new fact type `name(Person, String)` whose *name* depends on the argument. No rule can do that, because rules cannot declare fact types. Transformation is the right tool.

If you find yourself writing a transformation whose body is one rule with substitutions, consider whether the same effect can be achieved with generic types (`Type E`) instead. Transformations are a last resort when declarative mechanisms cannot express the shape you need.

---

## How this works under the hood

A useful mental model for `::>`: **it uses Nelumbo's own variable-binding machinery, running without the logical reasoning on top.**

When the reasoner evaluates a query, it does two things for each candidate: it binds free variables by navigating relations, and it reasons three-valuedly over the results to classify each binding as a fact, a falsehood, or unknown. The binding step and the reasoning step are distinct.

A transformation uses **only the binding step**. When a `::>` left-hand side matches user source, the engine binds the transformation's variables the same way it would bind the free variables of any other Nelumbo expression — by matching the pattern against the input and finding values that make the pattern fit. Once the variables are bound, the transformation's right-hand side is emitted as declarations. No fact/falsehood reasoning is performed on the transformation; it is pure binding.

This is why metavariable declarations look like logical-variable declarations (they are the same thing), why the same scopes and the same visibility modifiers work on them, and why adding a new variable type for transformations is no different from adding any other type. The `::>` operator does not require a second language or a second engine — it is the same engine being asked to do half its usual job.

---

## Status, caveats, open questions

Since `::>` is under active development, here are some specific points to be aware of when using it today:

- **Error messages from transformations are still rough.** A mistake in a transformation body may surface as a confusing diagnostic about the expanded code rather than the source you wrote.
- **Interaction with `private`/`hidden` inside transformations is evolving.** The worked examples use `private FactType ::= ...` inside transformations and it works, but finer-grained visibility controls may be added.
- **Namespace handling** (see the `{Namespace}` prefix in [`../reference/visibility.md`](../reference/visibility.md)) is the area most likely to see refinement — it is the piece that keeps multiple transformations from colliding.
- **Syntax of the `::>` operator itself** may tighten over releases. The current form supports both the patterns shown here and the nested form; a consolidated spec is a work in progress.
- **Performance** is not a concern. Transformations — including nested ones — are expanded at compile/load time only, so they contribute nothing to query execution. Heavy transformation use affects load time, not the reasoner's runtime.

Check the [latest examples](../../src/main/resources/org/modelingvalue/nelumbo/examples/) and the release notes for up-to-date behaviour. When this guide lags behind the code, trust the code.

---

## See also

- [`../reference/grammar.md`](../reference/grammar.md) — how `::>` fits in the overall grammar
- [`../reference/built-in-tokens.md`](../reference/built-in-tokens.md) — the `<n>` identifier token and type holes used in transformation LHS
- [`../reference/visibility.md`](../reference/visibility.md) — scopes and the `{Namespace}` mechanism
- [`../reference/writing-rules.md`](../reference/writing-rules.md) — the simpler alternative for value-level abstraction
- [`transformation.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/transformation.nl) — the canonical worked example
- [`deHet.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/deHet.nl) — natural-language DSL variant
- [`transformationAssignment.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/transformationAssignment.nl) — the "rules-and-transformation-stubbed-out" exercise version of the same example
