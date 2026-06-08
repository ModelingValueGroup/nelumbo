# Nelumbo documentation

<img src="nelumbo.svg" alt="Nelumbo" width="60" height="60" align="right" />

Nelumbo is a declarative logic meta-language. You define the syntax and semantics of your language in a `.nl` file, and the same file runs and tests it. Because Nelumbo reasons about **facts and falsehoods as peers**, it has genuine logical negation — not Prolog's negation-as-failure — and produces results as a two-sided `[facts][falsehoods]` structure.

> **Status:** Nelumbo is at an early stage of development. Some features are still evolving; where that matters, individual pages note it.

This folder contains the user documentation. For the project overview, build instructions, and release notes, see the [repository README](../README.md). For the presentation-deck version of this material, see [`NELUMBO.md`](NELUMBO.md).

---

## Start here

1. **[Reading a query and test](getting-started/reading-a-test.md)** — the first thing to read. Without this, every `.nl` file in the repo is unreadable because every example ends with tests in the `? [facts][falsehoods]` notation.
2. **[Your first program](getting-started/first-program.md)** — line-by-line walkthrough of `fibonacci.nl`, exercising imports, patterns, rules with guards, and tests.

Thirty minutes with those two pages is enough to open any `.nl` file in the repo and follow what's going on.

---

## Reading paths

Different readers want different things. Pick the path that matches your goal.

### "I want to understand what Nelumbo is and why."

- [Reading a query and test](getting-started/reading-a-test.md) — the language's headline feature, three-valued logic
- [Architecture — how Nelumbo is layered](explanation/architecture.md) — Java core → `lang.nl` (syntactic bootstrap) → `logic.nl` (three-valued logic + `<=>`/`fact`/`?`) → numeric/data stdlib → user code
- [Three-valued logic](reference/three-valued-logic.md) — the semantic foundation, with truth tables
- [`nelumbo.lang`](reference/stdlib/lang.md) — the `.nl` file that declares the whole syntax, including `::`, `::=`, `::>`, and the pattern meta-grammar
- [`nelumbo.logic`](reference/stdlib/logic.md) — the `.nl` file that declares `Boolean`, the connectives, equality, and the `fact` / `<=>` / `?` statement forms

### "I want to write my own DSL using Nelumbo."

- The starting pair above, then
- [Grammar](reference/grammar.md) and [Operators](reference/operators.md) — reference
- [Writing rules](reference/writing-rules.md) — `<=>`, guards, merging, contradictions
- [Language transformations](guides/language-transformations.md) — the `::>` mechanism for defining new keywords (status: under construction)
- [Writing your own module](guides/writing-your-own-module.md) — packaging a reusable library

### "I want to extend Nelumbo with a new primitive in Java."

- [Architecture](explanation/architecture.md) — understand the layering first
- [Native API](reference/native-api.md) — the `Predicate`, `InferResult`, `infer()` surface
- [Native classes catalogue](reference/native-classes.md) — what's already shipped
- [Native cookbook](guides/native-cookbook.md) — five recipes with complete skeletons

### "I want to read the stdlib to learn idiom."

- [Standard library tour](guides/stdlib-tour.md) — all seven modules in dependency order
- [`stdlib/lang.md`](reference/stdlib/lang.md), [`logic.md`](reference/stdlib/logic.md), [`integers.md`](reference/stdlib/integers.md), [`rationals.md`](reference/stdlib/rationals.md), [`strings.md`](reference/stdlib/strings.md), [`collections.md`](reference/stdlib/collections.md), [`datetime.md`](reference/stdlib/datetime.md) — per-module reference

### "I need to look something up."

Jump straight to the [reference](#reference) section below. Everything in `reference/` is organised as a lookup, not a read-through.

---

## Full table of contents

### Getting started

- [Reading a query and test](getting-started/reading-a-test.md)
- [Your first program](getting-started/first-program.md)

### Reference

The irreducible facts about the language.

- [Grammar](reference/grammar.md) — the core syntax of `.nl` files
- [Operators](reference/operators.md) — complete operator catalogue
- [Built-in tokens and pattern holes](reference/built-in-tokens.md) — `<NUMBER>`, `<STRING>`, `<Variable>`, etc.
- [Precedence and associativity](reference/precedence-and-associativity.md) — the `#N` system
- [Visibility](reference/visibility.md) — `private`, `hidden`, `visible`, and scope blocks
- [Three-valued logic](reference/three-valued-logic.md) — truth tables and identities
- [Test expression semantics](reference/test-expression-semantics.md) — formal definition of when a test passes
- [Writing rules](reference/writing-rules.md) — `<=>`, guards, merging, contradictions
- [Native API](reference/native-api.md) — implementing predicates in Java
- [Native classes catalogue](reference/native-classes.md) — every shipped `@`-bound class

### Standard library reference

- [`nelumbo.lang`](reference/stdlib/lang.md) — the bootstrap layer: token types, object hierarchy, pattern meta-grammar, top-level statements
- [`nelumbo.logic`](reference/stdlib/logic.md) — Boolean, connectives, quantifiers, equality
- [`nelumbo.integers`](reference/stdlib/integers.md) — arbitrary-precision integer arithmetic
- [`nelumbo.rationals`](reference/stdlib/rationals.md) — exact rationals
- [`nelumbo.strings`](reference/stdlib/strings.md) — strings, concatenation, conversion
- [`nelumbo.collections`](reference/stdlib/collections.md) — generic `Set<E>` and `List<E>`, plus set-builder notation
- [`nelumbo.datetime`](reference/stdlib/datetime.md) — ISO 8601 dates, times, date-times, and durations

### Guides

Task-oriented how-tos.

- [Standard library tour](guides/stdlib-tour.md) — reading all seven stdlib modules in order
- [Writing your own module](guides/writing-your-own-module.md) — packaging a reusable library
- [Language transformations](guides/language-transformations.md) — the `::>` meta-feature *(under construction)*
- [Native cookbook](guides/native-cookbook.md) — recipes for writing Java natives

### Explanation

Background and design.

- [Architecture — how Nelumbo is layered](explanation/architecture.md)

---

## Map of the example files

The `.nl` files under [`src/main/resources/org/modelingvalue/nelumbo/examples/`](../src/main/resources/org/modelingvalue/nelumbo/examples/) are the canonical worked examples. A short guide to what's in each:

| File | What it demonstrates |
|---|---|
| `fibonacci.nl` | Integer arithmetic, recursive rule with guards, basic tests |
| `family.nl` | Types, subtypes, `FactType`, literal enumerations, ancestor/descendant recursion |
| `friends.nl` | Mutually-recursive relations, symmetric + transitive closure |
| `even.nl` | Simplest useful predicate using an existential quantifier |
| `max.nl`, `ternary.nl` | Generic types (`Type T`), conditional expressions, user-defined operators |
| `power.nl`, `maxFib.nl` | Composed recursive definitions |
| `logicTest.nl` | Executable specification of the three-valued logic truth tables |
| `integersTest.nl`, `rationalsTest.nl`, `stringsTest.nl`, `collectionsTest.nl` | Per-stdlib-module test suites, useful to skim for corner cases |
| `belasting.nl` | Natural-language DSL (Dutch tax rules) using multi-word pattern syntax |
| `transformation.nl`, `deHet.nl` | Language transformations with `::>` — defining new top-level keywords |
| `scoping.nl`, `hidden.nl` | Scope blocks and hidden-variable patterns |
| `whoIs.nl` | Importing a user-written module (not just stdlib) |
| `queryOnly.nl` | Bare queries without `[..][..]` — useful during development |
| `*Assignment.nl` | "Fill in the rules" exercise versions of the corresponding examples |

---

## A note on conventions used in the docs

- **`..`** is the *incompleteness marker* — it means "this side of the result may have more bindings than are listed." It is never called a wildcard because it doesn't match values.
- **Facts** (left bracket) and **falsehoods** (right bracket) are the two sides of every query result.
- **In-language** extensions (rules, transformations, modules) are distinguished from **host-language** extensions (Java natives bound with `@`). The docs use these terms consistently.
- Cross-references in every page point to other pages when a topic is covered in more depth elsewhere. If you follow the links, you end up with a coherent tour of the language.

---

## Feedback and contributing

The documentation is new and will have errors. Corrections and suggestions are welcome via the [GitHub issue tracker](https://github.com/ModelingValueGroup/nelumbo/issues). The source files for the docs are under `docs/` in the repository.
