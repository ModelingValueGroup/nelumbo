# Reading a Nelumbo query and test

If you open any `.nl` file in Nelumbo and scroll to the bottom, you will see lines that look like this:

```
fib(5)=f         ? [(f=5)][..]
m(Amalia)=Willem ? [][()]
unknown          ? [..][..]
```

Until you understand this notation you cannot read Nelumbo code, because almost every example file ends with a block of it. This page is the key. Read it first.

---

## Why there are two sides to every result

Most logic languages only tell you whether something is true. Prolog, for example, will answer *"yes, `p` is provable"* or *"no, I could not prove `p`."* It does not distinguish between **"`p` is false"** and **"I do not know whether `p` is true."** Both come back as failure. That conflation is called *negation-as-failure*, and it is the reason `\+` in Prolog is not a real logical negation.

Nelumbo is different. Nelumbo reasons about **facts and falsehoods as peers**. For any query, the engine first **navigates relations** to produce candidate bindings for the free variables, and then, for each candidate, **reasons about whether the expression is true or false** under that binding. The two activities overlap — navigation is itself driven by the results of other queries — but they are distinguishable, and they produce the two sides of the result together. This makes `!` (logical NOT) a first-class operator that behaves the way a mathematician expects: `!p` is provable when `p` has actually been shown to be false, not merely when `p` could not be proved.

Because Nelumbo computes two things, every query result has two parts:

```
? [ facts ][ falsehoods ]
      ^         ^
      |         `-- bindings that make the expression false
      `------------ bindings that make the expression true
```

This is not a stylistic choice. It is the syntactic expression of Nelumbo's three-valued reasoning: a binding can be in the facts, in the falsehoods, in neither (genuinely unknown), and — by construction — never in both.

---

## Query vs. test

The `?` character by itself turns any expression into a **query**. When a query appears on its own, the engine runs it and prints the result:

```
fib(5)=f ?
```

When a query is followed by two bracketed lists, it becomes a **test**. The brackets are the *expected* result; the engine runs the query and compares:

```
fib(5)=f ? [(f=5)][..]
          \_________/
          expected result
```

If the actual result matches the expected result, the test passes. If not, it fails. Tests are how Nelumbo files verify themselves — you can run any `.nl` file and it will tell you which tests passed and which failed.

---

## The incompleteness marker `..`

Inside either bracket, `..` is the **incompleteness marker**. It means:

> "I am not claiming this side of the result is complete. There may be other bindings on this side that I have not listed."

`..` is not a wildcard and it does not match values. It is a statement about the *author's claim*, not about the values themselves. A bracket without `..` is a closed claim ("these are exactly the bindings on this side"); a bracket with `..` is an open claim ("at least these bindings, possibly more").

This matters because Nelumbo's reasoner cannot always enumerate every solution. When it cannot, it is honest about that, and `..` is how tests acknowledge it.

---

## The four shapes you need to recognise

Once you know the two sides and the incompleteness marker, almost every test in the codebase is one of four shapes.

### 1. Proven true, nothing else

```
true         ? [()][]
m(Amalia)=Maxima  ? [()][]
```

Reading: the facts side contains exactly one binding, `()`, which is the **empty binding** — a solution that binds no variables because the expression has no free variables to bind. The falsehoods side is closed and empty. Translation: *the expression is unconditionally true.*

### 2. Proven false, nothing else

```
false            ? [][()]
m(Amalia)=Willem ? [][()]
```

Reading: facts side is closed and empty, falsehoods side contains the empty binding. Translation: *the expression is unconditionally false.*

### 3. Genuinely unknown

```
unknown ? [..][..]
```

Reading: neither side makes any claim of completeness, and neither side lists any concrete bindings. Translation: *the engine cannot determine whether this is true or false.* This is the only way an unknown result is expressed — **`..` on both sides and nothing else**.

### 4. A concrete binding with open tail

```
fib(5)=f    ? [(f=5)][..]
m(Amalia)=a ? [(a=Maxima)][..]
```

Reading: the facts side asserts that `f=5` (resp. `a=Maxima`) is a proven fact. The `..` on the falsehoods side says "I am not making any claim about what is false." This is the most common shape in the example files: *"here is the answer I care about; I am not bothering to enumerate falsehoods."*

---

## Reading three real examples

### From `logicTest.nl`

```
true              ? [()][]
false             ? [][()]
unknown           ? [..][..]

true & true       ? [()][]
true & false      ? [][()]
unknown & true    ? [..][..]
unknown & false   ? [][()]
```

Notice the last line. `unknown & false` is **proven false**, not unknown — because no matter what the unknown turns out to be, conjoining it with `false` yields `false`. Nelumbo knows this. A system based on negation-as-failure could not distinguish this case from "I don't know."

### From `family.nl`

```
m(Amalia)=Maxima  ? [()][]
m(Amalia)=Willem  ? [][()]
m(Amalia)=a       ? [(a=Maxima)][..]
```

The first two are closed: the engine proves Maxima is Amalia's mother and proves Willem is not. The third finds one binding for the free variable `a` and declines to claim the falsehoods side is complete.

### From `fibonacci.nl`

```
fib(0)=f  ? [(f=0)][..]
fib(1)=f  ? [(f=1)][..]
fib(5)=f  ? [(f=5)][..]
```

Each test asserts one fact and leaves the falsehoods side open. You could tighten the first to `[(f=0)][(f=1),(f=2),..]` if you wanted to assert some specific falsehoods, but in practice that is rarely useful.

---

## The empty binding `()` — a note

`()` is easy to misread as "nothing" or "no answer." It is the opposite: `()` is a **successful solution that happened to bind zero variables**. An expression with no free variables (`true`, `1+1=2`, `m(Amalia)=Maxima`) can only ever produce `()` as a binding, so `[()][]` is how you say *"proven true,"* and `[][()]` is how you say *"proven false."*

An **empty bracket** `[]`, by contrast, is a *closed claim of no solutions on this side*: "I assert there are no bindings here, and this claim is complete."

---

## Glossary

| Term | Meaning |
|---|---|
| **Query** | An expression followed by `?`. Runs the engine and returns a result. |
| **Test** | A query followed by `[expected facts][expected falsehoods]`. Passes iff the engine's result matches. |
| **Facts** | Left bracket. Bindings proven to make the expression true. |
| **Falsehoods** | Right bracket. Bindings proven to make the expression false. |
| **Incompleteness marker** `..` | "This side may have more bindings than I have listed." A claim about the author's knowledge, not a value. |
| **Empty binding** `()` | A successful solution that binds zero variables. |
| **Empty bracket** `[]` | A closed claim that this side has no bindings. |
| **Unknown result** | `[..][..]` — neither side is complete and neither asserts any bindings. |

---

## Next

Now that you can read results, go read `first-program.md` — a line-by-line walkthrough of `fibonacci.nl` — and then open any file under `src/main/resources/org/modelingvalue/nelumbo/examples/` and follow along.
