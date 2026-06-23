# Standard library tour

The Nelumbo standard library is around 300 lines of Nelumbo across seven files. That is remarkably small — and because it is written in Nelumbo, reading it is one of the best ways to learn how the language is actually used.

This guide walks through all seven modules in dependency order, showing how each builds on the previous ones, what is native and what is derived, and what idiomatic Nelumbo looks like in production use.

The files:

1. [`lang.nl`](#1-nelumbolang-51-lines) — 51 lines — syntactic bootstrap: tokens, object hierarchy, the pattern meta-grammar, top-level forms
2. [`logic.nl`](#2-nelumbologic-44-lines) — 44 lines — Boolean, connectives, quantifiers, equality, and the `fact`/`<=>`/`?` statement forms
3. [`integers.nl`](#3-nelumbointegers-36-lines) — 36 lines — arithmetic and comparison
4. [`rationals.nl`](#4-nelumborationals-46-lines) — 46 lines — exact rational arithmetic
5. [`strings.nl`](#5-nelumbostrings-24-lines) — 24 lines — string operations
6. [`collections.nl`](#6-nelumbocollections-21-lines) — 21 lines — generic `Set<E>` and `List<E>`, plus set-builder notation
7. [`datetime.nl`](#7-nelumbodatetime-96-lines) — 96 lines — ISO 8601 dates, times, date-times, and durations

Each module is small enough to read in full, and the commentary around them illuminates the idioms they establish.

---

## 1. `nelumbo.lang` (51 lines)

The syntactic bootstrap. Every other `.nl` file — including `logic.nl` — is parsed using the `::=` declarations in this file. The Java core knows just enough to load `lang.nl`; from there on, the same machinery the user writes parses everything.

```
import nelumbo.lang   // not literally — this is itself nelumbo.lang

// Token types — produced by the tokenizer
SINGLEQUOTE :: NATIVE   SEMICOLON :: NATIVE   COMMA  :: NATIVE
LEFT        :: NATIVE   RIGHT     :: NATIVE   STRING :: NATIVE
NUMBER      :: NATIVE   NAME      :: NATIVE   OPERATOR :: NATIVE
NEWLINE     :: NATIVE
BEGINOFFILE :: NATIVE   ENDOFFILE :: NATIVE

// Object hierarchy
Object        :: NATIVE
Type          :: Object
Variable      :: Object
Root          :: Object             // a top-level statement
Functor       :: Root                // a `::=`-declared pattern
Pattern       :: Object #PATTERN     // a pattern fragment
Namespace     :: Object              // a `{ ... }` scope
RootNamespace :: Root, Namespace

// File / scope grammar
Namespace     ::= <BEGINOFFILE> ... <ENDOFFILE>   @...Namespace
RootNamespace ::= { ... }                          @...Namespace

// Pattern meta-grammar — declares the syntax of `::=` patterns themselves
Pattern ::= <NAME>                                       @nelumbo.patterns.TokenTextPattern,
            <STRING>                                     @nelumbo.patterns.TokenTextPattern,
            ...
            <LEFT> ... <RIGHT>                           @nelumbo.patterns.SequencePattern,
            "<" (visible|hidden)? <Type#100> (#NUMBER)? ">"  @nelumbo.patterns.NodeTypePattern

// Top-level statement forms
Root ::= "import" ...                                                @nelumbo.lang.Import,
         <Root#0> ::> <RootNamespace>                                @nelumbo.lang.Transform,
         (hidden)? <Type#100> <NAME>, ...                             @nelumbo.lang.Variable,
         <NAME> (< <Type#100> >)? :: <Type#100>, ... (# <NAME>)?     @nelumbo.lang.Type,
         (private)? <Type#100> ::= <Pattern#100>+ (#NUMBER)? (@...)?  @nelumbo.lang.Functor

// Generic parenthesisation — one rule, applies to every type
Type P
P ::= (<P>)   @nelumbo.lang.Parenthesized
```

### What is unique about this file

- **No `<=>` rules, no `fact`, no `?`.** All those forms are declared in `logic.nl` and are not yet available when `lang.nl` is being loaded. `lang.nl` contains only `::`, `::=`, and `::>` declarations.
- **Self-bootstrap.** The declarations describe the very syntax used to write them. The Java parser used to first read `lang.nl` is a minimal hand-coded equivalent of these rules; once the file is loaded, the patterns it installed take over.
- **Every other stdlib module starts with `import nelumbo.lang` transitively** — `logic.nl` imports it directly; everything else gets it via `logic`.

### Idioms to notice

- The `Pattern` block is the densest part of the file: it uses quoted operator characters (`"<"`, `"("`, `"|"`, `","`, `")"`, …) to talk about the very `<...>` syntax those characters have meaning in. This is the meta-syntax describing itself.
- The `Type P` / `P ::= (<P>)` pair at the end is the canonical demonstration of generics. `collections.nl` uses the same mechanism for `Set<E>` and `List<E>`.

See [`../reference/stdlib/lang.md`](../reference/stdlib/lang.md) for the full annotated walk-through.

---

## 2. `nelumbo.logic` (44 lines)

The three-valued logic layer. This is where Boolean, the connectives, and — crucially — the `fact`, `<=>`, and `?` statement forms are declared.

```
import nelumbo.lang

Boolean   :: Object
FactType  :: Boolean
Function  :: Object
Literal   :: Object

private Boolean ::= eq(<Literal>,<Literal>)                 @nelumbo.logic.Equal

Boolean ::= true                                            @nelumbo.logic.NBoolean,
            false                                           @nelumbo.logic.NBoolean,
            unknown                                         @nelumbo.logic.NBoolean,
            ! <Boolean>                             #25     @nelumbo.logic.Not,
            <Boolean> & <Boolean>                   #22     @nelumbo.logic.And,
            <Boolean> | <Boolean>                   #20     @nelumbo.logic.Or,
            E[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @nelumbo.logic.ExistentialQuantifier,
            A[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @nelumbo.logic.UniversalQuantifier,
            <Object> =  <Object>                    #30     @nelumbo.logic.NIs,
            <Object> != <Object>                    #30,
            <Boolean> -> <Boolean>                  #18,
            <Boolean> "<->" <Boolean>               #16

pattern BINDING ::= [ ... ]

// Top-level statement forms — declared here, not in the Java core
Root ::= "fact" <Boolean#0>, ...                                       @nelumbo.logic.Fact,
         <Boolean#0> "<=>" (<Boolean#0> ("if" <Boolean#0>)?), ...      @nelumbo.logic.Rule,
         <Boolean#0> ? (<BINDING> <BINDING>)?                          @nelumbo.logic.Query

Boolean p1, p2
p1 -> p2  <=> !p1 | p2
p1 <-> p2 <=> (p1 -> p2) & (p2 -> p1)

Literal  l1, l2
Function f1, f2
Object   n1, n2

l1 = l2  <=> eq(l1, l2)
l1 = f1  <=> f1 = l1
n1 != n2 <=> !(n1 = n2)
```

### What is native here

Nine native bindings: `Equal` (for the private literal-equality `eq`), `NBoolean`, `Not`, `And`, `Or`, `ExistentialQuantifier`, `UniversalQuantifier`, `NIs` (the public `<Object> = <Object>` operator), and three statement-form natives `Fact`, `Rule`, `Query`. Everything else is derived in Nelumbo.

### What is derived

- **`->` (implication)** is defined as `!p | q`. No Java code involved.
- **`<->` (bi-implication)** is defined as `(p -> q) & (q -> p)`.
- **`!=` (inequality)** is defined as `!(n1 = n2)`.
- **`=` for mixed literal/function** is defined as a rewrite: `l1 = f1 <=> f1 = l1`, swapping sides so the function is on the left. This is what makes `5 = fib(n)` work the same way as `fib(n) = 5`.

This is the module's first big lesson: **even at the deepest level of the language, most derivations happen in Nelumbo, not Java.** The Java surface is kept small.

### The key statement forms

`fact`, `<=>`, and `?` are themselves `Root ::=` patterns declared in this file. A `.nl` file that imports only `nelumbo.lang` (and not `nelumbo.logic`) can declare types and patterns but has no way to assert facts, write rules, or run queries.

### Idioms to notice

- The `::=` declaration for `Boolean` lists many alternatives separated by commas — all productions for `Boolean`. This is the conventional way to declare a type with many forms.
- Precedence annotations follow a ladder: `<->` at 16, `->` at 18, `|` at 20, `&` at 22, `!` at 25, `=`/`!=` at 30. Tighter-binding operators get higher numbers.
- `E[...]` and `A[...]` use `<Variable#100>` to require that the binding-site position contains a bare variable (precedence 100 is near the top — almost primary-expression tight). The body uses `<Boolean#0>` to accept any Boolean expression, even low-precedence ones.
- Four internal types (`Boolean`, `Literal`, `Function`, `Object`) appear in the equality rules. The `Literal`/`Function`/`Object` split is what makes the three equality rules sufficient.

---

## 3. `nelumbo.integers` (36 lines)

Builds arithmetic on top of logic.

```
import nelumbo.logic

Integer :: Object

private Boolean ::= add(<Integer>, <Integer>, <Integer>)   @...Add,
                    mult(<Integer>, <Integer>, <Integer>)  @...Multiply

Boolean ::= <Integer> ">"  <Integer>   #30  @...GreaterThan,
            <Integer> "<"  <Integer>   #30,
            <Integer> "<=" <Integer>   #30,
            <Integer> ">=" <Integer>   #30

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

## 4. `nelumbo.rationals` (46 lines)

Builds exact rationals on top of integers. Structurally identical to `integers`.

```
import nelumbo.integers

Rational :: Object

private Boolean ::= add(<Rational>, <Rational>, <Rational>)   @...Add,
                    mult(<Rational>, <Rational>, <Rational>)  @...Multiply


Boolean ::= <Rational> ">"  <Rational>   #30     @...GreaterThan,
            <Rational> "<"  <Rational>   #30,
            <Rational> "<=" <Rational>   #30,
            <Rational>  >=  <Rational>   #30,
            iir(<Integer>,<Integer>,<Rational>)  @...IntegersRational

Rational ::= <(> - <)?> <[> <NUMBER> . <NUMBER> <]>  @...Rational,
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

## 5. `nelumbo.strings` (24 lines)

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

## 6. `nelumbo.collections` (60 lines)

The only module that uses generic-type parameters.

```
import nelumbo.integers

Type E

Collection<E> :: Object
Set<E>        :: Collection<E>
List<E>       :: Collection<E>

private Boolean ::= build(<E>, <Boolean#0>, <Set<E>>)  @...BuildSet,
                    size(...), indexOf(...), elementOf(...), subset(...),
                    intersection(...), union(...), diff(...), concat(...)  @...Collections

Set<E>  ::= { <(> <E> <,> , <)*> }       @...NSet,
            { [ <E> ] ( <Boolean#0> ) }  @...SetBuilder,
            <Set<E>> && <Set<E>>,        ▸ intersection
            <Set<E>> || <Set<E>>,        ▸ union
            <Set<E>> -  <Set<E>>         ▸ difference
List<E> ::= [ <(> <E> <,> , <)*> ]       @...NList,
            <List<E>> + <List<E>>        ▸ concatenation
Integer ::= | <Collection<E>> |,         ▸ cardinality
            <E> "pos" <List<E>>          ▸ 0-based index
Boolean ::= <Set<E>> "<" <Set<E>>, ...,  ▸ subset / superset
            <E> "in" <Collection<E>>     ▸ membership

E e   Boolean c   Set<E> s

{[e](c)} = s  <=>  build(e, c, s)
... operator rules wiring each operator to its predicate ...
```

### What it introduces

- **`Type E`** — the declaration that introduces a generic type parameter. `lang.nl` uses the same mechanism for parenthesisation (`Type P; P ::= (<P>)`); this is its first use to define container types.
- **`Collection<E>`, `Set<E>`, and `List<E>`** — parameterised container types with literal syntax. `Collection<E>` is the common supertype.
- **Set-builder notation** — `{[e](c)}`, the comprehension form of `Set<E>`.
- **Algebraic operations** — cardinality `|c|`, membership `e in c`, subset/superset `< > <= >=`, set intersection/union/difference `&& || -`, list concatenation `+`, and list indexing `e pos l`. Each is a relation backed by the `Collections` native class and runs in both directions. See [`reference/stdlib/collections.md`](../reference/stdlib/collections.md#operations) for the full table.

### How the literal syntax works

The structural markers `<(> <E> <,> , <)*>` decode as:

- `<(>` … `<)*>` — a group with zero-or-more repetition
- `<E>` — the element (typed by the generic parameter)
- `<,>` — the separator marker
- `,` after `<,>` — the actual separator character the user writes

So `Set<E>` accepts `{}`, `{x}`, `{x, y}`, `{x, y, z}`, and so on. Same for `List<E>` with `[` and `]`.

### Set-builder notation

`Set<E>` has a second form — the comprehension `{[e](c)}`, "the set of all `e` such that `c`". The `[e]` slot must be a bare variable, and `(c)` is any Boolean condition over it. It reduces to one native rule:

```
{[e](c)} = s  <=>  build(e, c, s)
```

`build` is backed by `BuildSet`, which — like `E[...]` and `A[...]` — is a **quantifier**: it evaluates the condition under each binding of the local element variable, strips that variable, and gathers the witnessing values into a set. So set construction reuses the same three-valued quantifier machinery as the logic layer:

```
Integer i
{[i](|i|=10)} = s  ?  [(s={-10,10})][(s={0}),..]
```

The two solutions of `|i| = 10` become the fact `s = {-10, 10}`; `i = 0` is a proven non-member, so `{0}` lands on the falsehoods side with `..` for the open remainder.

### What is still absent

Algebraic operations — membership, union, intersection, length, map, fold — are not in the module as of this writing. The module provides value types, literal syntax, and the comprehension constructor; richer behaviour is either in natives not yet shipped, or left to the user's own modules. Check the latest source and tests when you go to use it.

---

## 7. `nelumbo.datetime` (96 lines)

The largest stdlib module, and a good demonstration that the integer idioms scale to a much richer value domain. It imports `nelumbo.integers` (for the `Period * Integer` scaling operator) and adds four independent value types.

```
import nelumbo.integers

DateTime :: Object    Date :: Object    Time :: Object    Period :: Object

DateTime ::= <[> <Date> T <Time#50> <]>                          @...NDateTime, <DateTime> + <Period> #40, ...
Date     ::= <[> <NUMBER> - <NUMBER> - <NUMBER> <]>              @...NDate,     <Date> + <Period> #40, ...
Time     ::= <[> <NUMBER> : <NUMBER> ... <]>                    @...NTime,     <Time> + <Period> #40, ...
Period   ::= <[> P ... <]>                                       @...NPeriod,   <Period> + <Period> #40, <Period> * <Integer> #50, ...

private Boolean ::= datetime_add(<DateTime>,<Period>,<DateTime>)  @...Add,
                    date_add(<Date>,<Period>,<Date>)              @...Add,
                    time_add(<Time>,<Period>,<Time>)              @...Add,
                    period_add(<Period>,<Period>,<Period>)        @...AddPeriod,
                    period_multiply(<Period>,<Integer>,<Period>)  @...MultiplyPeriod

DateTime a, b    Period x, y, z    Integer n

a+x=b  <=>  datetime_add(a,x,b)
a-x=b  <=>  datetime_add(b,x,a)
a-b=x  <=>  datetime_add(b,x,a)
x*n=y  <=>  period_multiply(x,n,y)
```

### What is native

Nine classes: four literal constructors (`NDate`, `NTime`, `NDateTime`, `NPeriod`), three relations (`Add` — shared by all three instant types, `AddPeriod`, `MultiplyPeriod`), one comparison (`GreaterThan`), and the value record `IsoDuration` that backs `Period`. Every value is a `java.time` type under the hood.

### What is derived

The same rewrites as the numeric modules. Subtraction is `datetime_add` permuted, so "instant + duration", "instant − duration", and "instant − instant" all flow through one native; `<`, `<=`, `>=` derive from `>` and `=` for each of the four types.

### Idioms to notice

- **The literals are connected-token groups.** Each is wrapped in `<[> … <]>`, which forbids whitespace between the inner tokens — that is what makes `2024-01-15` tokenize tightly rather than as three numbers and two minus signs.
- **Periods carry two semantics at once.** They use *field-based* equality (`P1M != P30D`) but a *nominal* magnitude for ordering (months = 30 days, years = 365). The module is a compact case study in modelling domain semantics through the native's `equals`/`compare`, not the grammar.
- **Validation happens at parse time.** Invalid dates and malformed periods reject with `file:line:col` during parsing, so they never reach the query engine as falsehoods.

See [`../reference/stdlib/datetime.md`](../reference/stdlib/datetime.md) for the full per-operator reference and [`datetimeTest.nl`](../../src/main/resources/org/modelingvalue/nelumbo/tests/datetimeTest.nl) for the executable specification.

---

## The takeaway

Reading all seven stdlib modules in order, a few observations crystallise:

- **The stdlib is small.** Around 300 lines of Nelumbo total. Not because the language is underpowered — because the language is expressive enough that a little code covers a lot.
- **The syntax itself is in `.nl` files.** `lang.nl` declares the pattern meta-grammar and the `::`, `::=`, `::>`, `import`, variable, type, and functor statement forms. `logic.nl` declares `fact`, `<=>`, and `?`. The Java core only knows enough to load `lang.nl`.
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
