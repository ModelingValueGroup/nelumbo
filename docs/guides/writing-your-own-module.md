# Writing your own module

A Nelumbo **module** is a `.nl` file that other `.nl` files can `import`. Nothing more, nothing less. The stdlib modules (`nelumbo.lang`, `nelumbo.logic`, `nelumbo.integers`, …) are ordinary modules that happen to ship with Nelumbo — even `lang.nl`, which declares the language's syntax, is structurally just another `.nl` file. A module you write sits at the same layer; it is a first-class library in the same sense.

This guide shows how to structure a module, what to export and what to keep private, and how to make it findable from other files.

---

## The shortest possible module

Any `.nl` file is a module. A trivial one:

```
// even.nl
import nelumbo.integers

Boolean ::= even(<Integer>)

Integer x, y

even(x) <=> E[y](y = x/2)
```

Another file can use it:

```
// mycode.nl
import org.modelingvalue.nelumbo.examples.even

even(10) ? [()][]
even(11) ? [][()]
```

That's it. No declaration of "module" keyword, no export list. Files are modules.

---

## Import paths

The path in an `import` statement is the **dotted form of the file path**, relative to the classpath roots. In the shipped project, the classpath root for stdlib and example modules is `src/main/resources/`. So:

| File | Import path |
|---|---|
| `src/main/resources/org/modelingvalue/nelumbo/logic/logic.nl` | `org.modelingvalue.nelumbo.logic` |
| `src/main/resources/org/modelingvalue/nelumbo/integers/integers.nl` | `org.modelingvalue.nelumbo.integers` — or the short alias `nelumbo.integers` |
| `src/main/resources/org/modelingvalue/nelumbo/examples/friends.nl` | `org.modelingvalue.nelumbo.examples.friends` |

The `nelumbo.X` short aliases are reserved for the six shipped stdlib modules (`lang`, `logic`, `integers`, `rationals`, `strings`, `collections`). Your own modules use their full dotted path.

### Imports in your own project

When you use Nelumbo as a library in a Java/Kotlin project, your classpath resources are however you have your project laid out. A common choice is to put `.nl` files under `src/main/resources/<your package>/<module>.nl`, and import them via their full package path.

---

## What a module exports

By default, **every non-`private` declaration is exported** to any module that imports this one. There is no separate export list.

To hide a declaration, mark it `private`:

```
private Boolean ::= helper(<T>, <T>, <T>)  @com.example.Helper
```

`private` is most useful for native-backed primitives that you wrap with more ergonomic user-facing patterns — the same technique the stdlib uses for `add`, `mult`, `string_concat`, and so on.

See [`../reference/visibility.md`](../reference/visibility.md) for the full visibility rules, including `hidden` and scope blocks.

---

## Module structure — a recommended layout

A well-structured module looks, in order:

1. **`import` statements** — always at the top
2. **Type declarations** — `T :: S`
3. **Private natives** (if any) — `private ... @...`
4. **Public pattern declarations** — `T ::= pattern` for the operators and functions you want users to write
5. **Variable declarations** — the `T a, b, c` lines that the rules below use
6. **Rules** — the `<=>` bi-implications that give meaning to the patterns
7. **Transformations** (if any) — `::> { ... }` blocks for syntax-extending constructs

`integers.nl` is a clean example of this layout:

```
import nelumbo.logic                          // 1. imports

Integer :: Object                             // 2. types

private Boolean ::= add(...)  @...Add,        // 3. private natives
                    mult(...) @...Multiply

Boolean ::= <Integer> > <Integer>  @...GT,    // 4. public patterns
            ...

Integer ::= <NUMBER> @...NInteger,
            <Integer> + <Integer>   #40,
            ...

Integer a, b, c                               // 5. variables

a < b  <=>  b > a                             // 6. rules
a + b = c  <=>  add(a, b, c)
...
```

This order makes the module read top to bottom in a natural way: *here is what I need, here are the types I introduce, here are the primitives that need Java help, here are the user-facing operations, here are the variables, here are the rules that connect everything.* Anyone reading your file later will thank you for following it.

---

## What goes in a module

A module can contain any top-level construct: types, patterns, variables, facts, rules, transformations, queries, and tests. See [`../reference/grammar.md`](../reference/grammar.md) for the full list.

A useful discipline: **a library module should be idempotent and side-effect-free at import time.** That means:

- **Do** declare types, patterns, rules, and transformations freely.
- **Usually do** include `fact` assertions if they are part of the module's data (like `nelumbo.examples.friends` asserting who is friends with whom).
- **Usually avoid** top-level queries and tests in a library module. Queries print output; tests pass or fail. A user importing your module doesn't want your tests to run in their program.

Put tests for a library in a **separate** `.nl` file that imports the library and contains the tests:

```
// mymodule.nl          ← the library
import nelumbo.integers
...rules...

// mymoduleTest.nl      ← the tests
import mymodule
...tests...
```

The stdlib follows this convention. `integers.nl` contains no tests; `examples/integersTest.nl` contains them, importing `nelumbo.integers`.

---

## Layering your own libraries

If your project grows, structure it the same way the stdlib does: a small number of layered modules, each importing the ones below it.

A plausible project layout:

```
src/main/resources/com/example/
  core.nl              // fundamental types and rules
  pricing.nl           // imports core
  shipping.nl          // imports core
  orders.nl            // imports pricing and shipping
```

Each module declares what it needs; none imports anything it doesn't use. User programs import `com.example.orders` and transitively get the full stack.

This works because `import` is transitive. A module that imports `orders` sees everything exported by `orders`, plus everything exported by `pricing` (because `orders` imports `pricing`), and so on. You don't need to list the whole transitive closure in every file.

---

## Transformations as reusable DSL infrastructure

If your module defines a DSL using `::>`, the transformation belongs in the module. Users who import your module automatically gain the DSL syntax.

A practical example: the `attr` transformation in `transformation.nl` is a DSL for declaring attributes. If you extracted it into a separate module, any program could gain the `attr X y Z` syntax by importing that one module.

Packaging a transformation as a module is a common structure for a reusable DSL:

```
// myattrs.nl
import nelumbo.strings

Root ::= attr <Type> <n> <Type>  #100

Type OT, AT
NAME AN

attr OT AN AT ::> {
    ...
}
```

Any consumer of `myattrs` then writes `import myattrs` and has the `attr` keyword available.

See [`language-transformations.md`](language-transformations.md) for the full mechanics of `::>`.

---

## Natives and modules

If your module needs a Java-backed primitive, you write the Java class separately (following [`native-cookbook.md`](native-cookbook.md)) and reference it with `@`:

```
// mymodule.nl
private Boolean ::= my_primitive(<T>, <T>)  @com.example.MyPrimitive

T a, b

user_op(a) = b  <=>  my_primitive(a, b)
```

Two things must be true at runtime:

1. The Java class `com.example.MyPrimitive` must be on the classpath of the Nelumbo program that parses your `.nl`.
2. The `.nl` file itself must be discoverable from the program's `import` statement — typically by being on the classpath under the matching resource path.

For a Java project using Nelumbo as a library, both requirements are met automatically if you ship the `.java` and `.nl` files in the same jar.

---

## A short checklist for a new module

- [ ] The file is named sensibly and located at the path matching its import path.
- [ ] It starts with all necessary imports, one per line.
- [ ] Types are declared before patterns that use them.
- [ ] Natives, if any, are marked `private`.
- [ ] User-facing operators and functions have appropriate precedence annotations.
- [ ] Variables referenced in rules are declared with appropriate types.
- [ ] Rules come after the patterns they rewrite.
- [ ] Tests live in a separate `somethingTest.nl` file that imports this module.
- [ ] If the module defines a DSL via `::>`, the transformation is in the module itself, not duplicated at each call site.

Follow those and your module will look and behave like the stdlib ones — which is the pattern you want.

---

## See also

- [`../reference/grammar.md`](../reference/grammar.md) — the full set of things that can appear at the top level of a module
- [`../reference/visibility.md`](../reference/visibility.md) — `private`, `hidden`, scopes
- [`../reference/stdlib/`](../reference/stdlib/) — per-module reference for the stdlib modules to model yours on
- [`stdlib-tour.md`](stdlib-tour.md) — reading the stdlib modules end to end
- [`language-transformations.md`](language-transformations.md) — when your module needs to introduce new syntax
- [`native-cookbook.md`](native-cookbook.md) — when your module needs new primitives
