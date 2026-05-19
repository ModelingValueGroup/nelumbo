# Your first Nelumbo program

This page walks through `fibonacci.nl` one line at a time. By the end you will have seen every ingredient of a working Nelumbo program: an `import`, a syntax declaration, a variable declaration, a rule with guards, and a block of tests.

Before reading on, make sure you have read [`reading-a-test.md`](reading-a-test.md). If `[(f=5)][..]` does not mean anything to you yet, start there.

The file we are working with is [`src/main/resources/org/modelingvalue/nelumbo/examples/fibonacci.nl`](../../src/main/resources/org/modelingvalue/nelumbo/examples/fibonacci.nl). It is 19 lines long.

---

## The whole program

```
import  nelumbo.integers

Integer ::= fib(<Integer>)

Integer n, f

fib(n)=f <=> f=n                 if n>=0 & n<=1,
             f=fib(n-1)+fib(n-2) if n>1

fib(0)=f       ? [(f=0)][..]
fib(1)=f       ? [(f=1)][..]
fib(2)=f       ? [(f=1)][..]
fib(3)=f       ? [(f=2)][..]
fib(5)=f       ? [(f=5)][..]
fib(10)=f      ? [(f=55)][..]
fib(100)=f     ? [(f=36#22r8fozas3n8w3)][..]
fib(1000)=f    ? [(f=36#18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf)][..]
```

That is the whole thing: a working, tested, arbitrary-precision Fibonacci implementation in a logic language. The naive recursive form is fast even at `fib(1000)` because Nelumbo's reasoner memoises inferences automatically — the same code written directly in Python would be exponential and never finish, since Python has no implied memoisation. Now let us read it.

---

## Line 1 — `import nelumbo.integers`

```
import  nelumbo.integers
```

Nelumbo ships with a small standard library. Each module is itself a `.nl` file, written in Nelumbo. `nelumbo.integers` provides the `Integer` type, arithmetic syntax (`+`, `-`, `*`, `/`, unary minus, absolute value), and comparison operators (`<`, `<=`, `>`, `>=`). It is what gives us the ability to write `n-1` and `n>=0` below.

Internally, `nelumbo.integers` itself imports `nelumbo.logic`, which defines `Boolean`, `&`, `|`, `!`, the quantifiers `E[]` and `A[]`, and equality. So by importing `integers` you transitively get the full base of the language.

**Takeaway:** most Nelumbo files start with one or two imports. `logic` is the foundation; `integers`, `rationals`, `strings`, and `collections` build on it.

---

## Line 3 — declare a new function `fib`

```
Integer ::= fib(<Integer>)
```

This is a **pattern declaration**. The operator `::=` reads as *"has the pattern"*. The line says: *an `Integer` can be written as `fib(<Integer>)`* — in other words, we are adding `fib(...)` to the syntax of the language as a new way to produce an `Integer`.

Key things to notice:

- `Integer ::= ...` means *the thing being defined produces a value of type `Integer`*.
- `<Integer>` inside the pattern is a **hole** — a placeholder that must itself be an `Integer` expression.
- Nelumbo does not distinguish "functions" from "expressions" at the grammar level. `fib(5)`, `5+3`, and `fib(n-1)+fib(n-2)` are all just `Integer` expressions that happen to be built using different patterns.

**Takeaway:** `::=` adds syntax. You are literally extending the language's grammar every time you write it.

---

## Line 5 — declare variables

```
Integer n, f
```

This declares two variables of type `Integer`, named `n` and `f`. They are available in the rules and tests below.

Variables in Nelumbo are **logical variables**, not storage cells. They do not have a single value; they stand for "whatever value makes the rule hold." The engine finds bindings for them during query evaluation.

**Takeaway:** think of `n` and `f` the way you would in algebra, not the way you would in Java.

---

## Lines 7–8 — the rule

```
fib(n)=f <=> f=n                 if n>=0 & n<=1,
             f=fib(n-1)+fib(n-2) if n>1
```

This is the heart of the program. Line by line:

- `<=>` is **logical equivalence** and it is how rules are written in Nelumbo. The rule says: *the statement on the left holds exactly when the statement on the right holds.*
- The left-hand side, `fib(n)=f`, reads *"`fib(n)` equals `f`"*. This is the shape of every query you will write against `fib`: you ask the engine to find the `f` such that `fib(n)=f`.
- The right-hand side has **two alternatives separated by a comma**. The comma here is **purely shorthand** for repeating the left-hand side. What you see compressed onto two lines is really two separate rules:

  ```
  fib(n)=f <=> f=n                 if n>=0 & n<=1
  fib(n)=f <=> f=fib(n-1)+fib(n-2) if n>1
  ```

  Both rules bi-imply the left-hand side; neither supersedes the other. The `if`-guards ensure that for any given `n` only one of the two rules contributes, so there is no overlap. (When rules *do* overlap and both produce closed results on the same side, that is a contradiction — see the note below.)
- `if` attaches a **guard** to a rule. `f=n if n>=0 & n<=1` reads *"`f` equals `n`, provided `n` is 0 or 1."* `f=fib(n-1)+fib(n-2) if n>1` is the recursive case.

Read the whole rule in English: *"`fib(n)` equals `f` if, for `n` in 0..1, `f` equals `n`; and `fib(n)=f` if, for `n` greater than 1, `f` equals `fib(n-1) + fib(n-2)`."* That is the definition of Fibonacci.

### A note on multiple rules for the same relation

You can write many rules for the same left-hand side, either using the comma shorthand or as separate `<=>` declarations. Nelumbo **merges** the results of all applicable rules: facts from every rule go into the facts side, falsehoods from every rule go into the falsehoods side. No rule wins over another; every rule is a bi-implication and contributes equally.

This merging is sound as long as the rules **agree**. When they don't — specifically, when two rules both produce **closed** (non-`..`) claims on the same side that conflict — the program has a contradiction. A common way to avoid contradictions is to keep at most one rule closed on each side and let the others end with an incompleteness marker on that side. In practice, mutually exclusive `if`-guards (like the ones in `fib`) are the cleanest pattern.

**Takeaway:** a rule is a declarative equivalence, not an assignment or a function body. Multiple rules for the same relation all bi-imply it simultaneously; the engine uses them in both directions — it can answer "what is `fib(5)`?" and, in principle, "for which `n` is `fib(n)=55`?"

---

## Lines 10–18 — the tests

```
fib(0)=f  ? [(f=0)][..]
fib(1)=f  ? [(f=1)][..]
fib(5)=f  ? [(f=5)][..]
fib(10)=f ? [(f=55)][..]
```

Each line is a **test** in the sense defined in [`reading-a-test.md`](reading-a-test.md): a query followed by an expected result.

- The query `fib(5)=f ?` asks *"find bindings for `f` such that `fib(5)=f`."*
- The expected facts side is `[(f=5)]` — the test asserts the engine will find exactly the binding `f=5`.
- The expected falsehoods side is `[..]` — the test makes no claim about falsehoods.

The test passes if the engine's actual result matches. If you change `[(f=5)]` to `[(f=6)]` and re-run, the test will fail, and Nelumbo will tell you so.

### The big numbers

```
fib(100)=f ? [(f=36#22r8fozas3n8w3)][..]
```

`36#22r8fozas3n8w3` is an integer literal in base 36. Nelumbo integers are arbitrary precision, and for very large values the decimal representation becomes unwieldy, so `fib(100)` is printed in base 36. You can read it as "base 36, digits `22r8fozas3n8w3`." The `fib(1000)` test uses the same notation for the same reason.

**Takeaway:** tests live next to the code they test, in the same file, in the same language. There is no separate test runner to configure.

---

## Try breaking it

The fastest way to internalise what each piece does is to break one thing at a time and read the error.

**Break a test.** Change `fib(5)=f ? [(f=5)][..]` to `fib(5)=f ? [(f=6)][..]` and re-run. The engine will report a test failure, showing the actual result (`(f=5)`) alongside the expected one (`(f=6)`).

**Break a guard.** Change `if n>1` to `if n>2` on the recursive case. Now `fib(2)` has no matching guard and the engine cannot derive a fact for it. The test `fib(2)=f ? [(f=1)][..]` will fail, and you will see an "unknown" result instead of `(f=1)`.

**Remove the import.** Delete `import nelumbo.integers`. Parsing will fail at the first use of `+` or `>=`, because those operators are not part of core Nelumbo — they come from the integers module. This is a good demonstration that **the standard library is genuinely a library**: without it, you have a minimal logic language.

---

## What to read next

You have now seen all four of Nelumbo's core building blocks:

1. `import` — pull in definitions from another module
2. `::=` — add syntax
3. variable declarations and `<=>` rules — add semantics
4. `?` — queries, and `? [..][..]` — tests

Good next files to open, in order:

- **`family.nl`** — introduces types (`Male :: Person`), fact types (`FactType ::= pc(...)`), literal enumerations (`Male ::= Hendrik, Bernhard, ...`), and the `fact` block for ground truth.
- **`friends.nl`** — a shorter recursive example with a different flavour (symmetric + transitive closure).
- **`logicTest.nl`** — a complete specification of three-valued logic in test form. Useful to skim whenever you are unsure how `&`, `|`, `!`, `->`, `<->`, `E[]`, or `A[]` behave in edge cases.
- **`belasting.nl`** — the same mechanisms used to write Dutch tax rules in near-natural-language syntax. A good preview of what Nelumbo is actually for.

And when you want to see *how the language is built out of itself*, open `src/main/resources/org/modelingvalue/nelumbo/integers/integers.nl`. It is 37 lines long and defines everything you used on this page.
