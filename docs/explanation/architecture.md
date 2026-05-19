# Architecture — how Nelumbo is layered

Nelumbo is built in layers. Reading a single `.nl` file can make it feel like one monolithic language, but the reality is more interesting: most of what looks like "Nelumbo" is actually the standard library, which is itself written in Nelumbo on top of a small Java core. Understanding the layers makes it clearer where your code fits, where the stdlib fits, and where to extend the system.

---

## The layers

```
 ┌──────────────────────────────────────────────────┐
 │              User programs and tests             │    .nl files
 │  family.nl, fibonacci.nl, your DSL, your rules   │
 ├──────────────────────────────────────────────────┤
 │              User-written libraries              │    .nl files
 │        your reusable modules and transformations │
 ├──────────────────────────────────────────────────┤
 │           Numeric / data stdlib modules          │    .nl files
 │     integers, rationals, strings, collections    │    in src/main/resources/
 ├──────────────────────────────────────────────────┤
 │           nelumbo.logic — three-valued logic     │    .nl file
 │   Boolean, !, &, |, ->, <->, =, !=, E[], A[],    │
 │   plus the top-level forms `fact`, `<=>`, `?`    │
 ├──────────────────────────────────────────────────┤
 │           nelumbo.lang — syntactic bootstrap     │    .nl file
 │  token types, Object hierarchy, the pattern      │
 │  meta-grammar, `import`, `::`, `::=`, `::>`,     │
 │  variable / type / functor declarations          │
 ├──────────────────────────────────────────────────┤
 │                Nelumbo Java core                 │    .java files
 │  tokenizer, hardcoded bootstrap parser for       │
 │  lang.nl, reasoner, binder, @-bound natives      │
 └──────────────────────────────────────────────────┘
```

Each layer is built out of the layer below, and each layer is accessible to layers above. The diagram is not aspirational; it is literally how the shipped code is organised.

The split between `nelumbo.lang` and `nelumbo.logic` is important. **The entire syntax of Nelumbo is defined in `.nl` files** — `lang.nl` declares the grammar of patterns, types, variables, and top-level statements; `logic.nl` declares the three-valued Boolean layer plus the `fact`, `<=>`, and `?` statements that drive execution. The Java core contains just enough hardcoded parsing to load `lang.nl`; from that point on, every file (including `lang.nl` itself, on a second pass) is parsed using the `::=` patterns the loaded files have installed.

---

## Layer by layer

### The Java core

At the bottom is a Java codebase in `src/main/java/org/modelingvalue/nelumbo/`. It does four jobs:

1. **Tokenize and bootstrap-parse `lang.nl`.** The tokenizer is in Java. The parser used to read `lang.nl` is a hand-coded bootstrap — it knows just enough about `::`, `::=`, `<NAME>`, and the token types to load `lang.nl`. Every subsequent file (including a re-read of `lang.nl` itself) is parsed using the `::=` declarations installed by the loaded files.
2. **Resolve names**, apply visibility and scope rules, and load imported modules.
3. **Run the reasoner** — the navigator that produces candidate bindings, together with the three-valued logic that classifies them as facts, falsehoods, or unknown.
4. **Host native primitives** — Java classes referenced by `@...` annotations from `.nl` source, providing operations the language cannot derive from itself.

The Java core is small and focused. It deliberately does **not** know about integers, rationals, strings, or collections, and not even about `Boolean`, `<=>`, `?`, or `fact`. Those are all introduced from `.nl` files.

### The standard library (`nelumbo.*` modules)

Six `.nl` files under `src/main/resources/org/modelingvalue/nelumbo/` collectively form the stdlib:

- `lang/lang.nl` — the syntactic bootstrap: tokens, the `Object` / `Type` / `Variable` / `Root` / `Pattern` / `Namespace` / `Functor` hierarchy, the pattern meta-grammar (`<T>`, `<(>...<)+>`, `<(>...<)?>` , …), and the top-level forms `import`, `::`, `::=`, `::>`.
- `logic/logic.nl` — Boolean values, connectives (`!`, `&`, `|`, `->`, `<->`), quantifiers (`E[]`, `A[]`), equality (`=`, `!=`), and the three execution-driving statement forms `fact`, `<=>`, `?`.
- `integers/integers.nl` — arbitrary-precision integers, arithmetic, comparison
- `rationals/rationals.nl` — exact rationals built on integers
- `strings/strings.nl` — strings and integer-string conversion
- `collections/collections.nl` — generic `Set<E>` and `List<E>`

These files are ordinary Nelumbo. They use `import`, `::`, `::=`, `<=>`, `::>`, and `private` exactly the way your code does. What sets them apart is that they bind certain patterns to Java classes using `@`:

```
private Boolean ::= add(<Integer>, <Integer>, <Integer>)   @org.modelingvalue.nelumbo.integers.Add
```

What this means in practice:

- **The language's syntax lives in `lang.nl`.** The hand-coded Java bootstrap exists only to load that file — once `lang.nl` is in place, the same `::=` declarations the user writes are what parse the rest.
- **The three-valued logic lives in `logic.nl`.** That includes `<=>` itself: rules are a statement form declared in `logic.nl`, not a Java keyword. Without `nelumbo.logic`, a `.nl` file can declare types and patterns but cannot write rules, assert facts, or run queries.
- Most of what looks like language features — `->`, `<->`, `-` as unary, `|x|`, `<=`, `int(s)`, `str(i)` — is **defined in Nelumbo**, not native. Only a handful of genuinely irreducible primitives (the tokenizer, `add`, `mult`, `>` between integers, string concat, and so on) are implemented in Java.
- You can study the stdlib to learn idiom. It is around 200 lines of Nelumbo across six files, and it demonstrates almost every feature of the language.

### User-written libraries

Nothing prevents your own code from living at the library layer. Any `.nl` file you write can be `import`-ed by other `.nl` files — see the `whoIs.nl` example, which imports `org.modelingvalue.nelumbo.examples.friends`. Modules of your own are a full-fledged way to organise reusable rules, types, and DSL constructs.

There is no formal distinction between "standard library" and "user library" apart from location. A stdlib module is one that ships in Nelumbo's own source tree under `nelumbo.*`; a user library is one you write and distribute separately.

### User programs and tests

At the top are the `.nl` files in your own project — the ones that import the modules you need, define your own types and rules, and run queries or tests against them. The Fibonacci, family, friends, tax (`belasting.nl`), and attribute examples all live here.

A user program is not structurally different from a library; the difference is intent. A library is meant to be imported; a program is meant to be run. Both are the same kind of file.

---

## Two extension paths

Given the layering, there are two distinct ways to extend Nelumbo:

### In-language extension — another `.nl` file

You write a new `.nl` file, declare types, patterns, rules, and optionally transformations. Import it from your program (or have the program import a module that imports yours, transitively). The new code lives at the library or program layer; no Java is involved.

This is the right choice for:

- Domain rules (tax law, family relations, custom DSLs)
- New operators defined in terms of existing operators (`->` and `<->` in `logic.nl` are built this way)
- Language transformations (`::>` expansions that introduce new keywords)
- Generic abstractions using `Type E`

### Host-language extension — a new Java native

You write a Java class extending `Predicate`, `Function`, or `Node` (depending on what you're defining), implement its `infer` method, and bind it from an `.nl` file with `@`. The new code lives at the Java core layer.

This is the right choice for:

- Primitive operations the language genuinely cannot derive from itself (arithmetic, string manipulation, I/O)
- Performance-critical paths where a rule-based implementation is too slow
- Integration with external Java libraries

The stdlib itself follows a clear discipline on this: the **minimum possible** is native, and everything else is in Nelumbo. `add` and `mult` are native; subtraction, division, unary minus, and absolute value are in Nelumbo. `>` is native; `<`, `<=`, `>=` are in Nelumbo. `NBoolean`, `And`, `Or`, `Not`, the equality natives `NIs` and `Equal`, `E[]`, and `A[]` are native; `->`, `<->`, and `!=` are in Nelumbo.

When in doubt, reach for the in-language path first. Drop to Java only when the in-language path cannot express what you need.

---

## A concrete walk through one line

Consider the familiar Fibonacci test:

```
fib(5) = f  ?  [(f=5)][..]
```

What actually happens when this runs:

1. **Parser (Java + loaded `.nl` declarations)** tokenises the line. The tokenizer is Java; the actual grammar (`?`, `[..][..]`, `=`, `fib(<Integer>)`, the `<NUMBER>` `5`) is recognised using pattern declarations loaded from `lang.nl`, `logic.nl`, `integers.nl`, and your own file.
2. **Name resolver (Java)** ties `f` to its variable declaration (`Integer n, f`), `fib` to your pattern declaration, `=` to the native `NIs` predicate declared in `logic.nl`.
3. **Reasoner (Java)** starts with the query `fib(5) = f`. Because `=` is a relation, the reasoner treats it as an equation to solve.
4. **Your rule in Nelumbo** fires: `fib(n) = f <=> f = fib(n-1) + fib(n-2) if n > 1`. The reasoner recursively evaluates `fib(4)` and `fib(3)`.
5. **Stdlib rule in Nelumbo** (`integers.nl`): `a + b = c <=> add(a, b, c)`. The reasoner turns each addition into a call to the private `add` relation.
6. **Native `Add` (Java)**: with two arguments bound and one unbound (the sum), it returns `set(2, NInteger.of(...)).factCI()` — a fact, facts side closed, falsehoods side open.
7. **Result bubbles back up** through all the rule invocations, unifying `f = 5` at the top.
8. **Test comparison (Java)** runs the expected-result parser on `[(f=5)][..]`, compares it to the actual result, and emits pass or fail.

Four layers touched: your file, stdlib integers, stdlib logic, Java native. Every layer does its own job; the `@` annotations are the only bridge between Nelumbo and Java.

---

## Why this matters for documentation

The layered architecture shapes the rest of the docs:

- **Reference pages** document the Java-core concepts and the syntax of the language itself.
- **Stdlib pages** document each module as a library: what it exports, how it is built, which pieces are native vs. Nelumbo-defined.
- **Guides** cover the two extension paths as separate topics: in-language (rules, transformations, modules) and host-language (the native cookbook).
- **Tutorials** teach reading a `.nl` file, working outward from the user-program layer.

When you are looking for how to do something, ask: **which layer is the right one?** A rule lives in your program; a custom operator usually lives in a module; a genuinely new primitive lives in Java. The layers are the map.

---

## See also

- [`../reference/grammar.md`](../reference/grammar.md) — what the core grammar is
- [`../reference/stdlib/logic.md`](../reference/stdlib/logic.md) — the foundation stdlib module
- [`../guides/stdlib-tour.md`](../guides/stdlib-tour.md) — guided read-through of all six stdlib modules
- [`../guides/writing-your-own-module.md`](../guides/writing-your-own-module.md) — the in-language extension path
- [`../guides/native-cookbook.md`](../guides/native-cookbook.md) — the host-language extension path
- [`../reference/native-classes.md`](../reference/native-classes.md) — catalogue of every shipped native
