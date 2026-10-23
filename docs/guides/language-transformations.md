# Language transformations — `::>`

> **Status: under construction.** The `::>` operator and the transformation mechanism it introduces are actively evolving. The design and syntax described on this page reflect the current state of the working examples (`transformation.nl`, `deHet.nl`), but **details are likely to change between releases**. Treat this guide as a working overview, not a stable specification.

The `::>` operator is what makes Nelumbo a **language workbench** rather than just a logic engine. It lets you define new keywords and constructs that **expand into ordinary Nelumbo declarations** when the user writes them. The transformation is applied at parse/load time, so by the time the reasoner runs there are no macros left — just regular types, patterns, facts, and rules.

If you have read the reference pages, you already know all the ingredients `::>` produces. This guide explains how to compose them.

---

## The essential shape

```nelumbo
pattern-with-variables  ::>  {
    ...declarations that use those variables...
}
```

Both sides of `::>` share the same variables, declared in the surrounding scope. When the user writes source matching the left-hand side, the engine **binds those variables to what the user wrote** — using the same binding machinery it uses during query evaluation — and then emits the declarations in the block with those bindings applied. The result is added to the program as if it had been written directly.

A concrete example, from [`transformation.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/transformation.nl):

```nelumbo
Root ::= attr <Type> <NAME> <Type> #100

Type OT, AT
NAME AN

attr OT AN AT  ::> {
    AT   ::= <OT>.AN       #100,
             <hidden OT>AN #100

    Root ::= <{OT,Literal}>.AN  := <{AT,Literal}#0>                                  #100,
             <hidden {OT,Variable}>AN := <{AT,Function}#0> <(> "if" <Boolean#0> <)?> #100

    {OT,Literal}  ol
    {AT,Literal}  al

    (ol.AN := al) ::> {
        fact ol.AN = al
    }

    {AT,Function} af
    {Boolean}     bf
    hidden OT     o

    AN ==  o.AN

    (AN := af) ::> {
        o.AN ==  af
    }

    (AN := af if bf) ::> {
        o.AN ==  af if bf
    }
}
```

Reading this top to bottom:

1. The first line, `Root ::= attr <Type> <NAME> <Type> #100`, **introduces the new keyword** `attr` into the top-level grammar. `Root` is the special type Nelumbo uses for statement-level productions; extending `Root` means "the user can now write this at the top level of their program." The keyword `attr` is followed by a type, an identifier (the attribute's name, a [`<NAME>`](../reference/lang/built-in-tokens.md#name) hole) and another type.
2. The next block declares **variables for the transformation**: `OT` and `AT` of type `Type`, `AN` of type `NAME`. These are ordinary variable declarations; they just happen to be used by a transformation rather than by a rule.
3. The `attr OT AN AT ::> { ... }` line is the transformation itself. When the user writes something like `attr Person name String`, the engine binds `OT = Person`, `AN = name`, `AT = String` (via the same machinery it uses to bind logical variables in queries) and then emits the declarations in the block with those bindings applied.

The block gives every attribute four things:

- **Two ways to read it.** `<OT>.AN` is the attribute of a written object (`Piet.name`). `<hidden OT>AN` is the attribute of an *implicit* object (`name` alone), filled in by the [hidden variable](../reference/lang/visibility.md#hidden--implicit-operands) `o`. The rule `AN == o.AN` says both mean the same.
- **An assignment for known values.** `Piet.name := "Piet"` matches `(ol.AN := al)` and becomes `fact Piet.name = "Piet"`. The intersection types `{OT,Literal}` and `{AT,Literal}` restrict this form to a literal object and a literal value.
- **An assignment that derives a value.** `address := street + " " + str(number)` matches `(AN := af)`: the left operand is the implicit object, the value an expression (`{AT,Function}`). It becomes the rule `o.address == street + " " + str(number)`, so every person's `address` is computed from that same person's `street` and `number`.
- **The same with a guard.** `address := ... if number >= 100` matches `(AN := af if bf)` and becomes `o.address == ... if number >= 100`.

The attribute itself is a function, `Piet.name`, with the closed, single result every function has (see [Open facts and `one`](../reference/logic/statements.md#open-facts-and-one)), so it can be queried directly:

```nelumbo
attr Person name String
attr Person street String
attr Person number Integer
attr Person address String

hidden Person p

address := street + "  " + str(number) if number < 100
address := street + " " + str(number) if number >= 100

Person ::= Piet, Jan

Piet.name    := "Piet"
Piet.street  := "Kalverstraat"
Piet.number  := 88

Piet.address          ? [("Kalverstraat  88")][..]
name="Piet"           ? [(p=Piet),..][..]
```

`hidden Person p` is what makes the derived assignments and the last query work: `address := ...` and `name="Piet"` leave the person out, and the parser fills in `p` (see [Visibility](../reference/lang/visibility.md#implicit-objects)).

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
| `{OT,Literal} ol` | a literal of the type bound to `OT` (`Piet`) | transformation expansion |
| `{AT,Function} af` | an expression of the type bound to `AT` (`street + " " + str(number)`) | transformation expansion |
| `Lidwoord lw` | a `Lidwoord` literal (`de` or `het`) the user wrote | transformation expansion |

In ordinary rules, variables are bound by navigating relations and reasoning three-valuedly over the results. In transformations, the same variables are bound by matching the user's source syntax against the transformation's left-hand side — **same binding, no fact/falsehood reasoning layered on top**.

A type in a declaration may itself be a transformation variable: `{OT,Literal} ol` gets its type from what `OT` is bound to, intersected with `Literal`. That is how one transformation can say "a literal object" or "a computed value" for every attribute it expands.

Some of these variable types — `Type`, `NAME` — exist primarily for transformations. Others, like `Lidwoord`, are ordinary user-defined types that happen to work in transformation positions because the engine binds them the same way. You can use any type in this role; what matters is that the engine can bind variables of that type by matching against what the user wrote.

### Why this matters for reading `.nl` files

When you see `Type OT` at the top of a block containing `::>`, you are looking at a variable declaration — exactly the same kind you would see in a rule-bearing block. The difference is not in the variable itself, but in the context: in a rule, the engine binds the variable during reasoning; in a `::>` transformation, the engine binds it during expansion.

This is also why a transformation's variables can be wrapped in a `{ ... }` scope — scopes work the same way for both uses.

---

## Extending `Root`

The `Root` type is Nelumbo's hook for **top-level statement syntax**. To introduce a new keyword, you declare a `Root` pattern:

```nelumbo
Root ::= attr <Type> <NAME> <Type> #100
```

Reading this: "at the top level, the user may write `attr` followed by a type, an identifier, and another type." The `#100` precedence keeps the statement from colliding with other parsing rules.

Once declared, `attr ...` is a valid top-level construct. But it doesn't *do* anything yet — you still have to pair it with a `::>` transformation that gives it meaning:

```nelumbo
attr OT AN AT  ::>  {
    ...
}
```

The pattern on the left of `::>` mirrors the `Root` declaration, with metavariables (`OT`, `AN`, `AT`) where the `<...>` holes were.

> This "declare, then transform" pattern is characteristic of `::>` usage. The declaration tells the parser what to accept; the transformation tells the engine what it means.

---

## Nesting transformations

A transformation block can itself contain a `::>`. In `transformation.nl` the outer transformation declares the `Root` patterns for assignment, and three nested transformations give them meaning:

```nelumbo
attr OT AN AT  ::> {
    ...
    Root ::= <{OT,Literal}>.AN  := <{AT,Literal}#0>                                  #100,
             <hidden {OT,Variable}>AN := <{AT,Function}#0> <(> "if" <Boolean#0> <)?> #100
    ...
    (ol.AN := al) ::> {
        fact ol.AN = al
    }
    ...
    (AN := af if bf) ::> {
        o.AN ==  af if bf
    }
}
```

The outer transformation expands `attr Person address String` into many things, among them the `Root` patterns `Person.address := String` and `address := String if Boolean`. Those newly introduced patterns get transformations too: every use of the assignment syntax turns into a `fact` or a rule.

So the chain is:

```text
User writes:           Transformation expands to:          Further expands to:

attr Person address String
                   ->  String ::= <Person>.address,
                                  <hidden Person>address
                       Root   ::= <{Person,Literal}>.address := ...,
                                  <hidden {Person,Variable}>address := ... if ...
                       address == o.address
                       (ol.address := al) ::> { fact ol.address = al }
                       (address := af if bf) ::> { o.address == af if bf }
                       ...

Piet.address := "Kalverstraat"
                                                       ->  fact Piet.address = "Kalverstraat"

address := street + " " + str(number) if number >= 100
                                                       ->  o.address == street + " " + str(number)
                                                                        if number >= 100
```

A nested transformation has **the same semantics as the outer one**: it binds the variables on its left-hand side against user syntax and emits the declarations on its right-hand side. There is no inner-vs-outer distinction in mechanism, and no special "action" mode: `fact ol.AN = al` is just another declaration, no different from a `::=` or a `==` rule. Both transformations run at compile time, so nesting them carries no runtime cost.

The nested left-hand sides are in parentheses, `(AN := af) ::> {`. Without them the value hole (`<{AT,Function}#0>`, the lowest precedence) runs on into `::>`, and `AN := af ::> {` fails with `Unexpected token '>'`.

---

## A second worked example — natural-language DSL

The [`deHet.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/deHet.nl) example shows a transformation used to build a **Dutch-language DSL** for declaring and querying attributes. The user ends up writing:

```nelumbo
attr Persoon de naam String
attr Persoon het adres String

de naam van Jan is "Jan"
het adres van Piet is "Kalverstraat"

wat is de naam van Jan ?
wat is het adres van Piet ?
```

The transformation is structured the same way as the first example, but richer:

```nelumbo
Lidwoord :: Object

Lidwoord ::= de, het

Root     ::= "attr" <Type> <Lidwoord> <NAME> <Type> #100

{
    Type     OT, AT
    Lidwoord lw
    NAME     n

    attr OT lw n AT ::> {
        Root            ::= <lw> n van <{OT,Literal}> is <{AT,Literal}> #0
        Root            ::= wat is <lw> n van <OT> ? #0
        private Boolean ::= n(<OT>,<AT>)

        OT o
        AT a
        wat is lw n van o ? ::> {
            n(o,a) ?
        }

        {OT,Literal} ol
        {AT,Literal} al
        lw n van ol is al ::> {
            fact n(ol,al)
        }
    }
}
```

Two things to notice:

- **`Lidwoord`** (Dutch for *article*, as in "the") is an ordinary user-defined type with two literals `de` and `het`. But in the transformation's metavariable list (`Lidwoord lw`), it is acting as a meta-type, because the transformation uses it syntactically. The same type can serve both roles.
- **A private relation instead of a function.** `deHet.nl` stores the attribute as `n(<OT>,<AT>)`, a `private Boolean` relation that only the generated Dutch sentences use. `transformation.nl` uses a function (`<OT>.AN`) instead, which the user can query and compute with directly.
- **Two inner `::>` blocks** handle the two user-level actions: "say X is Y" (turned into a `fact`) and "ask what X is" (turned into a query). The transformation therefore defines not just syntax, but the full user-facing semantics, in one place.

When the user writes `attr Persoon de naam String`, all of the following happen:

- The `Root` grammar gains two new patterns: `de naam van Persoon is String` and `wat is de naam van Persoon ?`
- A private relation `naam(Persoon, String)` is declared
- Two transformations are registered that turn user uses of those new Root patterns into facts and queries respectively

The user then gets to write natural Dutch and have it work.

---

## What you can put inside `::>`

A transformation block accepts **any declaration or statement** that is legal at the point where the block expands. In practice this means the body of a `::>` block can contain:

- Type declarations (`T :: S`)
- Pattern declarations (`T ::= P`, with or without `private`/`hidden`)
- `Boolean` declarations
- Variable declarations, also `hidden` ones and ones whose type is a transformation variable (`{OT,Literal} ol`)
- Rules (`L <=> R`, `L == e`)
- Fact assertions (`fact E`)
- Further `::>` transformations (nested)
- Queries and tests (`E ?`, `E ? [F][N]`)

It is the same vocabulary you would use at the top level of a `.nl` file. The transformation is not a second, smaller language — it is a templated fragment of Nelumbo itself.

---

## Scoping

Transformations interact with scope blocks. The `deHet.nl` example puts the whole metavariable list and transformation inside a `{ ... }` block:

```nelumbo
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

Inside the transformation body, declarations added by the expansion live in the scope where the user wrote the invocation — not inside a hidden inner scope. So `attr Person name String` adds its patterns, its variables (`ol`, `al`, `af`, `bf`, `o`) and the rule `name == o.name` to the surrounding scope. This is usually what you want; it is why the transformed code can be referenced from ordinary user code later in the file.

---

## When to reach for a transformation

Transformations are powerful, which means they are also easy to overuse. A good rule of thumb:

- **Use a rule (`<=>` or `==`)** when the same abstraction can be expressed as an equivalence over values.
- **Use multiple `::=` patterns** when you want several surface syntaxes for the same underlying relation.
- **Use a transformation (`::>`)** only when the abstraction you want introduces *structural* declarations — new types, new relations, new patterns — that depend on the arguments.

The `attr` example is a good test case: `attr Person name String` introduces a new function `Person.name`, its implicit form `name` and its assignment syntax, all named after the argument. No rule can do that, because rules cannot declare patterns. Transformation is the right tool.

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
- **Interaction with `private`/`hidden` inside transformations is evolving.** The worked examples use `private Boolean ::= ...`, `<hidden OT>` holes and a `hidden OT o` variable inside transformations and they work, but finer-grained visibility controls may be added.
- **An expansion cannot always be written out by hand.** Read the expanded declarations as a substitution of the bound values. Typed out literally for one attribute, `(ol.street := al) ::> { ... }` fails to parse (`Unexpected token ':='`), while the same line works inside the transformation.
- **Namespace handling.** Any type that specialises `Namespace` introduces its own scope; declaring a functor `private` inside such a type keeps it confined to that scope. See [`../reference/visibility.md`](../reference/lang/visibility.md). This is the piece that keeps declarations from different transformation expansions from colliding.
- **Syntax of the `::>` operator itself** may tighten over releases. The current form supports both the patterns shown here and the nested form; a consolidated spec is a work in progress.
- **Performance** is not a concern. Transformations — including nested ones — are expanded at compile/load time only, so they contribute nothing to query execution. Heavy transformation use affects load time, not the reasoner's runtime.

Check the [latest examples](../../src/main/resources/org/modelingvalue/nelumbo/examples/) and the release notes for up-to-date behaviour. When this guide lags behind the code, trust the code.

---

## See also

- [`../reference/lang/grammar.md`](../reference/lang/grammar.md) — how `::>` fits in the overall grammar
- [`../reference/built-in-tokens.md`](../reference/lang/built-in-tokens.md) — the `<NAME>` identifier token and type holes used in transformation LHS
- [`../reference/visibility.md`](../reference/lang/visibility.md#hidden--implicit-operands) — hidden variables and implicit objects (`<hidden OT>AN`)
- [`../reference/visibility.md`](../reference/lang/visibility.md) — scopes, `private`, and how `Namespace`-derived types introduce their own scopes
- [`../reference/writing-rules.md`](../reference/logic/writing-rules.md) — the simpler alternative for value-level abstraction
- [`transformation.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/transformation.nl) — the canonical worked example
- [`deHet.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/deHet.nl) — natural-language DSL variant
- [`transformationAssignment.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/transformationAssignment.nl) — exercise: write the rule and the transformation for a simpler, relation-based `attr` (`private Boolean ::= AN(<OT>,<AT>)`)
