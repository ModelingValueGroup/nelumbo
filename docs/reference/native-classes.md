# Native classes — the catalogue

Every pattern in the standard library that needs Java backing is bound to a class under `src/main/java/org/modelingvalue/nelumbo/`. This page catalogues the shipped native classes, grouped by structural role, with a note on what each one does and how it returns its results.

Read this alongside [`native-api.md`](native-api.md) (which describes the API surface) and [`../guides/native-cookbook.md`](../guides/native-cookbook.md) (which walks through implementing new ones). This page is the "what's already there" reference.

---

## Package layout

```
org.modelingvalue.nelumbo.*           base classes and engine
    Node                              base class for AST nodes (values)
    Predicate           (in .logic)   base class for Boolean-producing natives
    CompoundPredicate   (in .logic)   predicate holding sub-predicates
    BinaryPredicate     (in .logic)   two-argument predicate with reduction logic
    Quantifier          (in .logic)   base for E[] and A[]
    InferContext, InferResult         context and result types for infer()

org.modelingvalue.nelumbo.lang        top-level statement forms declared by lang.nl
    Import, Type, Variable, Functor, Transform, Namespace, Parenthesized

org.modelingvalue.nelumbo.patterns    pattern meta-grammar declared by lang.nl
    TokenTextPattern, AlternationPattern, RepetitionPattern,
    OptionalPattern, SequencePattern, NodeTypePattern

org.modelingvalue.nelumbo.logic       Boolean, connectives, quantifiers, equality,
                                       plus the fact/<=>/? statement forms
    NBoolean, Not, And, Or, NIs, Equal, ExistentialQuantifier,
    UniversalQuantifier, Fact, Rule, Query

org.modelingvalue.nelumbo.integers    integer arithmetic
    NInteger, Add, Multiply, GreaterThan

org.modelingvalue.nelumbo.rationals   exact rational arithmetic
    Rational, Add, Multiply, GreaterThan, IntegersRational

org.modelingvalue.nelumbo.strings     string operations
    NString, Concat, Length, ToInteger

org.modelingvalue.nelumbo.collections container literals + set-builder
    NSet, NList, SetBuilder, BuildSet

org.modelingvalue.nelumbo.datetime    ISO 8601 dates, times, date-times, durations
    NDate, NTime, NDateTime, NPeriod, Add, AddPeriod, MultiplyPeriod,
    GreaterThan, IsoDuration
```

The `lang` and `patterns` packages are the natives that back the patterns declared in `lang.nl`. They implement the bootstrap layer — without them, the hand-coded parser could read `lang.nl` but no `@`-bound class would exist to handle the patterns it declares.

---

## Roles

Shipped natives fall into five structural roles. Reading this classification first makes the per-class notes below easier to follow.

| Role | Base class | What it does | Example |
|---|---|---|---|
| **Constant / literal** | `Node` (or `Predicate` for `NBoolean`) | Parses a source literal into a value object | `NInteger`, `NString`, `Rational`, `NBoolean` |
| **Three-arg functional relation** | `Predicate` | Relation with one output and one or more inputs; binds the missing one | `Add`, `Multiply`, `Concat`, `IntegersRational`, `ToInteger` |
| **Comparison predicate** | `Predicate` | Two-arg relation that decides true/false when both sides are known | `GreaterThan` (integers and rationals), `Equal`, `Length` |
| **Logical connective** | `BinaryPredicate` / `CompoundPredicate` | Combines sub-predicate results according to a truth table | `And`, `Or`, `Not` |
| **Quantifier** | `Quantifier` (extends `CompoundPredicate`) | Evaluates a sub-predicate under many bindings and aggregates | `ExistentialQuantifier`, `UniversalQuantifier`, `BuildSet` |
| **Container** | `Node` | Literal collection of elements | `NSet`, `NList` |

---

## `org.modelingvalue.nelumbo.logic`

### `NBoolean`

- Backs: `Boolean ::= true, false, unknown`
- Role: constant
- Value: a `Boolean` field (`true`, `false`, or `null` for `unknown`)
- Notes: singletons `TRUE`, `FALSE`, `UNKNOWN` are maintained as static fields and reused by connectives to produce compact results. The `infer` method returns `factCC()` / `falsehoodCC()` / `unknown()` based on the stored value.

### `Not`

- Backs: `! <Boolean>`
- Role: logical connective (unary)
- Base: `CompoundPredicate`
- Strategy: inspects the sub-predicate's result. If the sub is `isTrueCC()`, returns `NBoolean.FALSE.result()`. If `isFalseCC()`, returns `NBoolean.TRUE.result()`. Otherwise returns `predResult.flipComplete()` — swap the facts and falsehoods, and swap their closure flags. This is the formal three-valued negation.

### `And`, `Or`

- Back: `<Boolean> & <Boolean>` and `<Boolean> | <Boolean>`
- Role: logical connective (binary)
- Base: `BinaryPredicate`
- Strategy: each overrides four short-circuit predicates: `isTrue(one, i)`, `isFalse(one, i)`, `isTrue(pair)`, `isFalse(pair)`, plus `isLeft(pair)` and `isRight(pair)` for reduction.
  - `And`: returns `false` (reduced) if one operand is `isFalseCC()`; returns `true` only when both operands are `isTrueCC()`; reduces to the other operand when one is `isTrueCC()`.
  - `Or`: mirror image. Returns `true` if one operand is `isTrueCC()`; returns `false` only when both are `isFalseCC()`; reduces to the other operand when one is `isFalseCC()`.
- This is where the non-pessimistic three-valued behaviour of `unknown & false = false` and `unknown | true = true` is implemented.

### `NIs`

- Backs: `Boolean ::= <Object> = <Object> #30`
- Role: comparison predicate (public)
- Strategy: the general-purpose equality native. Handles equality between any two `Object` values, including variables and partially-bound expressions. Returns `factCC()` / `falsehoodCC()` when both sides are fully ground, and partial-information results otherwise.

### `Equal`

- Backs: `private Boolean ::= eq(<Literal>, <Literal>)`
- Role: comparison predicate (private — used only via the rules `l1 = l2 <=> eq(l1, l2)` inside `logic.nl`)
- Strategy: structural equality over literal `Node`s. Returns `factCC()` if both sides unify exactly, `falsehoodCC()` if they provably disagree.
- The `eq` predicate is the leaf primitive for comparing two `Literal` values; `NIs` builds on it for the general `Object`-equality case.

### `ExistentialQuantifier`, `UniversalQuantifier`

- Back: `E[...](<Boolean>)` and `A[...](<Boolean>)`
- Role: quantifier
- Base: `Quantifier` extends `CompoundPredicate`
- Strategy: evaluate the body under the current binding, then **strip the local variables** from each resulting binding and aggregate:
  - `ExistentialQuantifier` adds each body-fact to the facts side (with locals removed); body-falsehoods go to the falsehoods side unless they are already on the facts side (an existential witness overrides a matching falsehood).
  - `UniversalQuantifier` is the mirror: body-falsehoods go to the falsehoods side; body-facts go to the facts side unless they match a falsehood (a universal counterexample overrides a matching fact).
- Completeness is conservative: if the body's corresponding side was incomplete, the quantifier's side is marked incomplete.

---

## `org.modelingvalue.nelumbo.integers`

### `NInteger`

- Backs: `Integer ::= <NUMBER>`
- Role: constant
- Value: `BigInteger`
- Notes: parses decimal (`123`) and base-N (`36#xyz`) literals via the `#` separator. Values large enough to exceed `Long` range print in base 36.

### `Add`

- Backs: `private Boolean ::= add(<Integer>, <Integer>, <Integer>)`
- Role: three-arg functional relation
- Strategy: the canonical three-way relation pattern. If all three are bound, verify and return `factCC`/`falsehoodCC`. If two are bound, compute and return `set(i, computed).factCI()`. If more than one is unbound, return `unresolvable()`.
- The stdlib uses this single class for both addition and subtraction by the rule rewrite `a - b = c <=> add(c, b, a)`.

### `Multiply`

- Backs: `private Boolean ::= mult(<Integer>, <Integer>, <Integer>)`
- Role: three-arg functional relation
- Strategy: as `Add`, with one extra subtlety for division. When the product is bound and a factor is bound, use `divideAndRemainder`: if the remainder is zero, return `set(other, quotient).factCI()`; otherwise return `falsehoodCI()` (no integer completes the equation). This is how `21 / 10 = a` yields `[]` on the facts side.

### `GreaterThan`

- Backs: `Boolean ::= <Integer> > <Integer>` (only `>`; `<`, `<=`, `>=` are in `integers.nl`)
- Role: comparison predicate
- Strategy: when both sides are bound, compare and return `factCC()` or `falsehoodCC()`. When only one side is bound, use `set(i, get(1-i)).falsehoodsII()` — which records the specific falsehood "`x > x`" with both sides open, contributing partial information without overclaiming.

---

## `org.modelingvalue.nelumbo.rationals`

### `Rational`

- Backs: `Rational ::= <(> - <)?> <[> <NUMBER> . <NUMBER> <]>`
- Role: constant
- Value: two `BigInteger`s — numerator and denominator, stored in reduced form via `gcd`.
- Notes: `Rational.of(num, den)` normalises. The `toString` method prints a decimal form with two fractional digits.

### `Add`, `Multiply`

- Back: `private Boolean ::= add(...)` and `mult(...)` over `Rational`
- Role: three-arg functional relation
- Strategy: same three-way pattern as the integer versions, but the arithmetic goes via numerator-denominator crossed-multiplication. Both operators construct results with `Rational.of(num, den)` (which normalises).

### `GreaterThan`

- Backs: `Boolean ::= <Rational> > <Rational>`
- Role: comparison predicate
- Strategy: cross-multiply before comparing: `ln*rd > rn*ld` iff `l > r` when both are positive-denominator rationals. Same `set(i, ...).falsehoodsII()` partial-falsehood strategy as the integer comparison.

### `IntegersRational`

- Backs: `private Boolean ::= iir(<Integer>, <Integer>, <Rational>)`
- Role: three-arg functional relation (with an unusual signature: takes two integers and a rational)
- Strategy: when both integers are bound, construct the rational and either verify or bind the output. When the rational is bound and both integers are unbound, split the rational into numerator and denominator — this is how `r(x/y) = 0.5` could bind `x, y`. When exactly one integer is bound, declare `unresolvable()`.
- This is the conversion bridge between integers and rationals, used by `r(x)` and `r(x/y)` at the language level.

---

## `org.modelingvalue.nelumbo.strings`

### `NString`

- Backs: `String ::= <STRING>`
- Role: constant
- Value: Java `String`, with surrounding double quotes stripped on parse and re-added on print.

### `Concat`

- Backs: `private Boolean ::= string_concat(<String>, <String>, <String>)`
- Role: three-arg functional relation
- Strategy: the three-way pattern applied to strings. All three bound: verify. Two bound, sum unbound: concatenate and return `set(2, ...).factCI()`. The clever cases are when the **sum and one addend** are bound: use `startsWith` / `endsWith` to compute the missing addend, or return `falsehoodCI()` if no addend completes the equation.
- This is what makes `a + "bar" = "foobar"` produce `(a="foo")` — the native splits "foobar" at the known suffix.

### `Length`

- Backs: `private Boolean ::= string_length(<String>, <Integer>)`
- Role: comparison / functional relation (two-arg; output is the integer length)
- Strategy: when the string is bound, compute its length and either verify the supplied integer or bind it. When only the integer is bound, return `unknown()` — the relation cannot enumerate all strings of a given length.

### `ToInteger`

- Backs: `private Boolean ::= integer_string(<Integer>, <String>)`
- Role: three-arg functional relation (two-arg in practice: the conversion is `Integer <-> String`)
- Strategy: parses the string as a `BigInteger` via `Integer.parseInt` (note: the current implementation uses the fixed-width `int` parser, which limits the range on the parse side). Handles `NumberFormatException` by returning `falsehoodCC()` (if the integer was also supplied) or `falsehoodCI()` (if not). The reverse direction formats an integer with `BigInteger.toString()` into the canonical decimal form.

---

## `org.modelingvalue.nelumbo.collections`

### `NSet`

- Backs: `Set<E> ::= { <(> <E> <,> , <)*> }`
- Role: container constant
- Value: backed by the internal immutable `Set<T>` collection type.

### `NList`

- Backs: `List<E> ::= [ <(> <E> <,> , <)*> ]`
- Role: container constant
- Value: backed by the internal immutable `List<T>` collection type.
- Notes: `NList` supports an `elementsFlattened()` helper that recursively unwraps nested `NList` values. This is useful when a parse produces a tree of concatenated list fragments that you want to flatten.

### `SetBuilder`

- Backs: `Set<E> ::= { [ <E> ] ( <Boolean#0> ) }` — set-builder (comprehension) notation
- Role: container constant (parse-time AST node)
- Strategy: holds the bound element variable and the membership condition. At parse time it enforces that the `[ … ]` slot is a bare `Variable` (otherwise a `ParseException` "… must be a variable"), and declares that variable as a local via `localVars()`. The actual set construction is delegated to the `build` predicate (`BuildSet`) through the rule `{[e](c)} = s <=> build(e, c, s)`.

### `BuildSet`

- Backs: `private Boolean ::= build(<E>, <Boolean#0>, <Set<E>>)`
- Role: quantifier (extends `Quantifier`, like `ExistentialQuantifier`/`UniversalQuantifier`)
- Strategy: evaluates the condition under every binding of the local element variable, then **strips** that variable and aggregates. Each group of facts sharing the rest of the binding produces one fact whose third slot is an `NSet` of the witnessing values; each falsehood produces a singleton `NSet` of its non-member value on the falsehoods side. Completeness flags are inherited from the condition's result, so `{[i](|i|=10)}` yields `[(s={-10,10})][(s={0}),..]` — the two solutions as a fact, `{0}` as a proven non-member, `..` for the open remainder.

---

## `org.modelingvalue.nelumbo.datetime`

### `NDate`, `NTime`, `NDateTime`, `NPeriod`

- Back: the `Date`, `Time`, `DateTime`, and `Period` literals
- Role: constant
- Values: `LocalDate`, `LocalTime`, `LocalDateTime`, and `IsoDuration` respectively
- Notes: each parses its connected-token (`<[> … <]>`) literal at construction time. Invalid input (`2024-13-08`, the period `P1D1Y`) is rejected with a `ParseException` carrying `file:line:col`, so it surfaces during parsing rather than as a query falsehood. `NDateTime` builds a zone-less `LocalDateTime`. `NPeriod` normalizes the time part on build (`P1YT90M` → `P1YT1H30M`).

### `IsoDuration`

- Backs: the value behind every `Period` (a `record(Period, Duration)`); not itself `@`-bound
- Role: value record
- Notes: pairs a calendar `java.time.Period` (Y/M/W/D) with an exact `Duration` (H/M/S). Equality is **field-based** (`P1M != P30D`, the correct ISO 8601 semantics); `plus`/`minus`/`multipliedBy` operate componentwise; `toString` renders the canonical `P…T…` form.

### `Add`

- Backs: `datetime_add` / `date_add` / `time_add` *(private)* — one class for all three
- Role: three-arg functional relation
- Strategy: the canonical relational shape applied to instants. With the two inputs bound it computes the third; with the sum and the duration bound it subtracts; with both instants bound it returns the duration *between* them. `date_add` only accepts a duration whose time part is zero and `time_add` one whose calendar part is zero (otherwise `unresolvable()`); `DateTime` accepts both.

### `AddPeriod`

- Backs: `period_add` *(private)*
- Role: three-arg functional relation
- Strategy: `IsoDuration` addition, reversible — `a + b = c` binds whichever of the three is open via `plus`/`minus`. Deterministic.

### `MultiplyPeriod`

- Backs: `period_multiply` *(private)*
- Role: three-arg functional relation
- Strategy: scales an `IsoDuration` by an `Integer` (`P1D * 3 = P3D`).

### `GreaterThan`

- Backs: `>` on `DateTime`, `Date`, `Time`, and `Period`
- Role: comparison predicate
- Strategy: when both sides are bound, compares and returns `factCC()`/`falsehoodCC()`; with one side open it records the open falsehood via `set(i, …).falsehoodsII()`, mirroring the integer comparison. `Period` is compared by a **nominal** magnitude (months = 30 days, years = 365), an explicit convention because `P1M` vs `P30D` has no exact ordering.

---

## Cross-reference — by `.nl` binding site

This table lets you go from a line in an `.nl` file to the Java class that implements it.

| `.nl` file | Pattern | `@` binding |
|---|---|---|
| `lang.nl` | `Namespace ::= ...` and `RootNamespace ::= { ... }` | `nelumbo.lang.Namespace` |
| `lang.nl` | `Pattern ::= <NAME> \| <STRING> \| <OPERATOR> \| ...` (literal tokens) | `nelumbo.patterns.TokenTextPattern` |
| `lang.nl` | `Pattern ::= <(> ... <\|> ... <)>` | `nelumbo.patterns.AlternationPattern` |
| `lang.nl` | `Pattern ::= <(> ... <,> ... <)+/*>` | `nelumbo.patterns.RepetitionPattern` |
| `lang.nl` | `Pattern ::= <(> ... <)?>` | `nelumbo.patterns.OptionalPattern` |
| `lang.nl` | `Pattern ::= <LEFT> ... <RIGHT>` | `nelumbo.patterns.SequencePattern` |
| `lang.nl` | `Pattern ::= "<" (vis/hid)? <Type> (#N)? ">"` | `nelumbo.patterns.NodeTypePattern` |
| `lang.nl` | `Root ::= "import" ...` | `nelumbo.lang.Import` |
| `lang.nl` | `Root ::= <Root> ::> <RootNamespace>` | `nelumbo.lang.Transform` |
| `lang.nl` | `Root ::= (hidden)? <Type> <NAME>, ...` | `nelumbo.lang.Variable` |
| `lang.nl` | `Root ::= <NAME> ... :: <Type>, ...` | `nelumbo.lang.Type` |
| `lang.nl` | `Root ::= (private)? <Type> ::= <Pattern>+, ...` | `nelumbo.lang.Functor` |
| `lang.nl` | `P ::= (<P>)` (generic parenthesisation) | `nelumbo.lang.Parenthesized` |
| `logic.nl` | `true`, `false`, `unknown` | `NBoolean` |
| `logic.nl` | `!<Boolean>` | `Not` |
| `logic.nl` | `<Boolean> & <Boolean>` | `And` |
| `logic.nl` | `<Boolean> \| <Boolean>` | `Or` |
| `logic.nl` | `E[...](...)` | `ExistentialQuantifier` |
| `logic.nl` | `A[...](...)` | `UniversalQuantifier` |
| `logic.nl` | `<Object> = <Object>` | `NIs` |
| `logic.nl` | `eq(<Literal>,<Literal>)` *(private)* | `Equal` |
| `logic.nl` | `Root ::= "fact" ...` | `nelumbo.logic.Fact` |
| `logic.nl` | `Root ::= <Boolean> "<=>" ...` | `nelumbo.logic.Rule` |
| `logic.nl` | `Root ::= <Boolean> ? (Binding Binding)?` | `nelumbo.logic.Query` |
| `integers.nl` | `<(> - <)?> <[> <NUMBER> <(> "#" ... <)?> <]>` | `NInteger` |
| `integers.nl` | `add(<Integer>,<Integer>,<Integer>)` *(private)* | `integers.Add` |
| `integers.nl` | `mult(<Integer>,<Integer>,<Integer>)` *(private)* | `integers.Multiply` |
| `integers.nl` | `<Integer> > <Integer>` | `integers.GreaterThan` |
| `rationals.nl` | `<(> - <)?> <[> <NUMBER> . <NUMBER> <]>` | `rationals.Rational` |
| `rationals.nl` | `add(<Rational>,<Rational>,<Rational>)` *(private)* | `rationals.Add` |
| `rationals.nl` | `mult(<Rational>,<Rational>,<Rational>)` *(private)* | `rationals.Multiply` |
| `rationals.nl` | `<Rational> > <Rational>` | `rationals.GreaterThan` |
| `rationals.nl` | `iir(<Integer>,<Integer>,<Rational>)` | `IntegersRational` |
| `strings.nl` | `<STRING>` | `NString` |
| `strings.nl` | `string_concat(<String>,<String>,<String>)` *(private)* | `Concat` |
| `strings.nl` | `string_length(<String>,<Integer>)` *(private)* | `Length` |
| `strings.nl` | `integer_string(<Integer>,<String>)` *(private)* | `ToInteger` |
| `collections.nl` | `Set<E> ::= { ... }` | `NSet` |
| `collections.nl` | `Set<E> ::= { [ <E> ] ( <Boolean> ) }` | `SetBuilder` |
| `collections.nl` | `build(<E>,<Boolean>,<Set<E>>)` *(private)* | `BuildSet` |
| `collections.nl` | `List<E> ::= [ ... ]` | `NList` |
| `datetime.nl` | `Date ::= <[> <NUMBER> - <NUMBER> - <NUMBER> <]>` | `datetime.NDate` |
| `datetime.nl` | `Time ::= <[> <NUMBER> : <NUMBER> ... <]>` | `datetime.NTime` |
| `datetime.nl` | `DateTime ::= <[> <Date> T <Time#50> ... <]>` | `datetime.NDateTime` |
| `datetime.nl` | `Period ::= <[> P ... <]>` | `datetime.NPeriod` |
| `datetime.nl` | `datetime_add/date_add/time_add(...)` *(private)* | `datetime.Add` |
| `datetime.nl` | `period_add(<Period>,<Period>,<Period>)` *(private)* | `datetime.AddPeriod` |
| `datetime.nl` | `period_multiply(<Period>,<Integer>,<Period>)` *(private)* | `datetime.MultiplyPeriod` |
| `datetime.nl` | `<DateTime\|Date\|Time\|Period> > <...>` | `datetime.GreaterThan` |

The natives back roughly twice as many language-level patterns once you count the Nelumbo-defined derivations (`<`, `<=`, `>=`, `-` unary, `-` binary, `/`, `|x|`, `->`, `<->`, `!=`, `str`, `int`, `len`, `r`).

---

## What is *not* native

A few constructs that might look like they should be native are actually pure Nelumbo definitions:

- `->` and `<->` — defined in `logic.nl` as rewrites using `!`, `|`, and `&`.
- `!=` — defined in `logic.nl` as `!(a = b)`.
- `<`, `<=`, `>=` on integers and rationals — defined in the respective modules as rewrites using `>` and `=`.
- `-` (binary subtraction) — defined as `a - b = c <=> add(c, b, a)`.
- `-` (unary) — defined as `-a = b <=> 0 - a = b`.
- `/` — defined as `a / b = c <=> mult(c, b, a)`.
- `|x|` — defined via a two-clause rule with mutually exclusive guards.
- `str`, `int`, `len`, `r(...)` — surface wrappers around the underlying string and rational primitives.

Looking at the stdlib with this lens — **what is native and what is not** — is one of the best ways to understand Nelumbo's design philosophy. The Java surface is deliberately minimal; the rest is the meta-language at work.

---

## See also

- [`native-api.md`](native-api.md) — the API surface: `infer`, `InferResult`, the helper methods, the completeness-flag convention
- [`../guides/native-cookbook.md`](../guides/native-cookbook.md) — hands-on recipes for writing new natives
- [`../explanation/architecture.md`](../explanation/architecture.md) — why the Java/Nelumbo split is drawn where it is
- [`stdlib/`](stdlib/) — per-module reference for what each stdlib module exports
