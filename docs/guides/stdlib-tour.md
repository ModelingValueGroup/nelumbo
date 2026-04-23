# Standard library tour

The Nelumbo standard library is 145 lines of Nelumbo across five files. That is remarkably small — and because it is written in Nelumbo, reading it is one of the best ways to learn how the language is actually used.

This guide walks through all five modules in dependency order, showing how each builds on the previous ones, what is native and what is derived, and what idiomatic Nelumbo looks like in production use.

The files:

1. [`logic.nl`](#1-nelumbologic-29-lines) — 29 lines — Boolean, connectives, quantifiers, equality
2. [`integers.nl`](#2-nelumbointegers-37-lines) — 37 lines — arithmetic and comparison
3. [`rationals.nl`](#3-nelumborationals-47-lines) — 47 lines — exact rational arithmetic
4. [`strings.nl`](#4-nelumbostrings-24-lines) — 24 lines — string operations
5. [`collections.nl`](#5-nelumbocollections-8-lines) — 8 lines — generic `Set<E>` and `List<E>`

Each module is small enough to read in full, and the commentary around them illuminates the idioms they establish.

---

## 1. `nelumbo.logic` (29 lines)

The foundation. Every other module imports this, directly or transitively.

```
private Boolean ::= eq(<Object>, <Object>)                  @...Equal

Boolean ::= true                                            @...NBoolean,
            false                                           @...NBoolean,
            unknown                                         @...NBoolean,
            ! <Boolean>                             #25     @...Not,
            <Boolean> & <Boolean>                   #22     @...And,
            <Boolean> | <Boolean>                   #20     @...Or,
            E[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @...ExistentialQuantifier,
            A[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @...UniversalQuantifier,
            <Boolean> -> <Boolean>                  #18,
            <Boolean> "<->" <Boolean>               #16,
            <Object> != <Object>                    #30

Boolean p1, p2

p1->p2  <=> !p1|p2
p1<->p2 <=> (p1->p2)&(p2->p1)

Literal  l1, l2
Function f1, f2
Object   n1, n2

l1=l2  <=> eq(l1, l2)
l1=f1  <=> f1=l1
n1!=n2 <=> !(n1=n2)
```

### What is native here

Six of the things in this file are bound to Java classes — the ones with `@...` annotations: `eq`, `NBoolean`, `Not`, `And`, `Or`, `ExistentialQuantifier`, `UniversalQuantifier`. Everything else is derived in Nelumbo.

### What is derived

- **`->` (implication)** is defined as `!p | q`. No Java code is involved.
- **`<->` (bi-implication)** is defined as `(p -> q) & (q -> p)`.
- **`!=` (inequality)** is defined as `!(n1 = n2)`.
- **`=` for mixed literal/function** is defined as a rewrite: `l1 = f1 <=> f1 = l1`, swapping sides so the function is on the left. This is what makes `5 = fib(n)` work the same way as `fib(n) = 5`.

This is the module's first big lesson: **even at the deepest level of the language, most derivations happen in Nelumbo, not Java.** The Java surface is kept small.

### Idioms to notice

- The `::=` declaration lists many alternatives separated by commas — all productions for `Boolean`. This is the conventional way to declare a type with many forms.
- Precedence annotations follow a ladder: `<->` at 16, `->` at 18, `|` at 20, `&` at 22, `!` at 25, `!=` at 30. Tighter-binding operators get higher numbers.
- `E[...]` and `A[...]` use `<Variable#100>` to require that the binding-site position contains a bare variable (precedence 100 is near the top — almost primary-expression tight). The body uses `<Boolean#0>` to accept any Boolean expression, even low-precedence ones.
- Three distinct internal types (`Literal`, `Function`, `Object`) appear in the equality rules. These are used to classify operands so the right rule fires.

---

## 2. `nelumbo.integers` (37 lines)

Builds arithmetic on top of logic.

```
import nelumbo.logic

Integer :: Object

private Boolean ::= add(<Integer>, <Integer>, <Integer>)   @...Add,
                    mult(<Integer>, <Integer>, <Integer>)  @...Multiply

Boolean ::= <Integer>  >   <Integer>   #30  @...GreaterThan,
            <Integer> "<"  <Integer>   #30,
            <Integer> "<=" <Integer>   #30,
            <Integer>  >=  <Integer>   #30

Integer ::= <NUMBER>                @...NInteger,
            <Integer> - <Integer>   #40,
            <Integer> + <Integer>   #40,
                      - <Integer>   #80,
            <Integer> * <Integer>   #50,
            <Integer> / <Integer>   #50,
                      | <Integer> | #35

Integer a, b, c

a<b    <=>  b>a
a<=b   <=>  a<b | a=b
a>=b   <=>  a>b | a=b

a+b=c  <=>  add(a,b,c)
a-b=c  <=>  add(c,b,a)
a*b=c  <=>  mult(a,b,c)
a/b=c  <=>  mult(c,b,a)

-a=b   <=>  0-a=b

|a|=b  <=>  b=a   if a>=0,
            b=-a  if a<0
```

### What is native

Four classes: `Add`, `Multiply`, `GreaterThan`, `NInteger`. That's it — **all of integer arithmetic** in Java.

### What is derived from those four

Everything else, using rule rewriting. The most characteristic pattern:

```
a + b = c  <=>  add(a, b, c)
a - b = c  <=>  add(c, b, a)
a * b = c  <=>  mult(a, b, c)
a / b = c  <=>  mult(c, b, a)
```

Subtraction is not a separate operation — it's addition with the arguments permuted. Division is multiplication permuted. Because `add` and `mult` are three-argument *relations* (not two-argument functions), any of the three arguments can be the "output" — the native computes whichever one is missing.

Similarly, `<`, `<=`, `>=` all derive from `>`:

```
a < b   <=>  b > a
a <= b  <=>  a < b | a = b
a >= b  <=>  a > b | a = b
```

Unary minus is a one-liner:

```
-a = b  <=>  0 - a = b
```

And absolute value uses the guard pattern with mutually exclusive conditions:

```
|a| = b  <=>  b = a   if a >= 0,
              b = -a  if a <  0
```

### Idioms to notice

- The rewrite-with-permutation idiom makes the native count small. Rather than writing `Subtract` and `Divide` Java classes, the module reuses `Add` and `Multiply` in inverted roles. This is only possible because the natives are relational.
- `private` is used on `add` and `mult` because they are implementation details. Callers should use `+`, `-`, `*`, `/`, not the underlying private relations.
- Two-clause absolute-value rule with mutually exclusive guards — the canonical way to define a piecewise function without risking contradictions.

### `integersTest.nl` as specification

The test file exercises every operator in multiple directions:

```
a+11=21  ? [(a=10)][..]     // solve for left operand
10+a=21  ? [(a=11)][..]     // solve for right operand
10+11=a  ? [(a=21)][..]     // compute the sum

|a|=10   ? [(a=-10),(a=10)][(a=0),..]   // |a|=10 has two solutions
```

Read it when you want to confirm how an operator behaves in a case you are unsure about.

---

## 3. `nelumbo.rationals` (47 lines)

Builds exact rationals on top of integers. Structurally identical to `integers`.

```
import nelumbo.integers

Rational :: Object

private Boolean ::= add(<Rational>, <Rational>, <Rational>)   @...Add,
                    mult(<Rational>, <Rational>, <Rational>)  @...Multiply


Boolean ::= <Rational>  >   <Rational>   #30     @...GreaterThan,
            <Rational> "<"  <Rational>   #30,
            <Rational> "<=" <Rational>   #30,
            <Rational>  >=  <Rational>   #30,
            iir(<Integer>,<Integer>,<Rational>)  @...IntegersRational

Rational ::= <DECIMAL>                            @...Rational,
             <Rational> - <Rational>   #40,
             <Rational> + <Rational>   #40,
                        - <Rational>   #80,
             <Rational> * <Rational>   #50,
             <Rational> / <Rational>   #50,
                        | <Rational> | #35,
             r(<Integer>),
             r(<Integer>/<Integer>)

Rational a, b, c

a<b    <=>  b>a
a<=b   <=>  a<b | a=b
a>=b   <=>  a>b | a=b

a+b=c  <=>  add(a,b,c)
a-b=c  <=>  add(c,b,a)
a*b=c  <=>  mult(a,b,c)
a/b=c  <=>  mult(c,b,a)

-a=b   <=>  0.0-a=b

|a|=b  <=>  b=a   if a>=0.0,
            b=-a  if a<0.0

Integer x, y

r(x)=a    <=> iir(x,1,a)
r(x/y)=a  <=> iir(x,y,a)
```

### What's new relative to integers

**`IntegersRational` (`iir`)** — a three-argument relation that converts between `(Integer, Integer)` and `Rational`. Two Nelumbo rules wrap it:

```
r(x)   = a  <=>  iir(x, 1, a)
r(x/y) = a  <=>  iir(x, y, a)
```

So `r(5)` is promoted to `5.0`, and `r(1/3)` is an exact one-third. No silent conversion between `Integer` and `Rational` — you must go through `r(...)`.

### The key lesson

Notice that the arithmetic, comparison, and absolute-value rules are **textually identical** to the integer versions, differing only in the types and the literal `0.0` vs. `0`. The same structural shape works for both number types; the types and the natives do the work of distinguishing them.

This is a good template: **when adding a new numeric-like type, mirror the integer module structure.** You will end up with the same four private natives (`add`, `mult`, `>` by another name, and a literal constructor) and the same set of derived rules.

---

## 4. `nelumbo.strings` (24 lines)

The smallest non-trivial module.

```
import nelumbo.integers

String :: Object

String ::= <STRING>  @...NString

private Boolean ::= string_concat(<String>, <String>, <String>)  @...Concat,
                    string_length(<String>, <Integer>)            @...Length,
                    integer_string(<Integer>, <String>)           @...ToInteger

String  ::= <String> + <String>  #40,
            str(<Integer>)

Integer ::= len(<String>),
            int(<String>)

String  a, b, c
Integer x

a + b = c     <=>  string_concat(a, b, c)
len(a)  = x   <=>  string_length(a, x)
int(a)  = x   <=>  integer_string(x, a)
str(x)  = a   <=>  integer_string(x, a)
```

### What's native

Four natives: `NString` (the literal), `Concat` (three-way concatenation), `Length` (two-way length), `ToInteger` (bidirectional integer/string conversion).

### What's derived

The surface operations — `+` on strings, `str(i)`, `len(s)`, `int(s)` — are all Nelumbo wrappers around those four natives.

The nicest trick here is that `int(a) = x` and `str(x) = a` share the same native `integer_string`, just wrapped in two directions. Two user-facing operations, one native primitive.

### Bidirectional behaviour

The surface operator `+` is also relational:

```
"foo" + "bar" = a       ? [(a="foobar")][..]   // forward
a + "bar"     = "foobar" ? [(a="foo")][..]     // solve for prefix
"foo" + a     = "foobar" ? [(a="bar")][..]     // solve for suffix
```

All three work from the same rule and the same native. The native `Concat` handles the three combinations internally — see [`native-classes.md`](../reference/native-classes.md#concat) for how.

---

## 5. `nelumbo.collections` (8 lines)

The shortest module.

```
import nelumbo.integers

Type E

Set<E>  ::= { <(> <E> <,> , <)*> }  @...NSet
List<E> ::= [ <(> <E> <,> , <)*> ]  @...NList
```

That's the whole module.

### What it introduces

- **`Type E`** — the declaration that introduces a generic type parameter. This is the only module that demonstrates generics in the stdlib.
- **`Set<E>` and `List<E>`** — parameterised container types with literal syntax.

### How the literal syntax works

The structural markers `<(> <E> <,> , <)*>` decode as:

- `<(>` … `<)*>` — a group with zero-or-more repetition
- `<E>` — the element (typed by the generic parameter)
- `<,>` — the separator marker
- `,` after `<,>` — the actual separator character the user writes

So `Set<E>` accepts `{}`, `{x}`, `{x, y}`, `{x, y, z}`, and so on. Same for `List<E>` with `[` and `]`.

### What is absent

Operations — membership, union, length, map, fold — are not in the module as of this writing. The module provides value types and literal syntax; richer behaviour is either in natives not yet shipped, or left to the user's own modules. Check the latest source and tests when you go to use it.

---

## The takeaway

Reading all five stdlib modules in order, a few observations crystallise:

- **The stdlib is small.** 145 lines of Nelumbo total. Not because the language is underpowered — because the language is expressive enough that a little code covers a lot.
- **Most of it is not native.** Perhaps a quarter of the pattern declarations have `@` annotations. The rest are defined in Nelumbo using rules.
- **Layering is strict.** Each module imports the one below it; no module imports sideways. This is a good model for your own libraries.
- **Three-way relations are the canonical primitive shape.** If you are thinking of adding a new operation, check whether it fits the `op(a, b, c)` shape; if it does, you can probably reuse the integer/rational/string pattern.
- **Rule rewriting does most of the work.** `a - b = c <=> add(c, b, a)` is one line. Defining subtraction without rewriting would mean another native, more code, and more bugs.

When you are writing your own module, you are writing in the same style the stdlib uses. When you are writing a native, you are writing the same kind of Java the stdlib uses.

---

## See also

- [`../reference/stdlib/`](../reference/stdlib/) — per-module reference with exports summary
- [`../reference/native-classes.md`](../reference/native-classes.md) — catalogue of every shipped native
- [`writing-your-own-module.md`](writing-your-own-module.md) — build a library in the same style
- [`native-cookbook.md`](native-cookbook.md) — write natives in the same style
- [`../explanation/architecture.md`](../explanation/architecture.md) — why the stdlib is a library, not part of the language
