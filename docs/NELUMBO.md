# Nelumbo

<img src="nelumbo.svg" alt="Nelumbo" width="50" height="50" />

## Hands-on workshop

A logic meta-language for specifying DSLs

**nelumbo.nl/tutorial.html**

---

## Today

* **Introduction**: what Nelumbo is, and the ideas you need for the exercises
* **Hands-on**: the tutorial on nelumbo.nl, at your own pace
* **Wrap-up**: what surprised you, questions

Nothing to install: a browser is all you need.

---

## What is Nelumbo?

A **logic meta-language**. In one `.nl` file you define:

* the **syntax** of your own language (a DSL)
* its **meaning**: facts and rules
* its **tests**: questions with the answers you expect

You get an editor for your language right away: highlighting, hover,
and results while you type, in the browser and in any IDE (via LSP).

Made for knowledge that has to be exact, such as complex tax laws and clinical knowledge.

---

## Design choices

* **Declarative**: no order of execution, no over-specification
* **Three-valued**: true, false *and* unknown: reasoning over incomplete facts
* **Monotonic**: new knowledge never takes back an answer; contradictions are reported
* **Strongly typed**, with generics
* **Navigation only**: a variable gets values from known relations, never by trying everything
* **Extensible**: built-in types and operators are Nelumbo modules backed by Java

---

## A first taste

```text
import nelumbo.logic
Person  :: Object                                       // syntax
Person  ::= Piet, Jan, Klaas, Kees, Bob
Boolean ::= <Person> is a friend of <Person> #30
Person  me, f, ff, Who

// a friend of a friend of mine is a friend of mine     // semantics
ff is a friend of me <=> E[f](ff is a friend of f & f is a friend of me)

fact Jan is a friend of Piet                            // facts
fact Klaas is a friend of Jan
fact Kees is a friend of Klaas

Who is a friend of Piet ?    // [(Who=Jan),(Who=Kees),(Who=Klaas),..][..]
Bob is a friend of Piet ?    // [..][..]   unknown, not false
```

---

## Syntax: types and functors

```text
import nelumbo.lang
Person   :: Object                  // a type
Male     :: Person                  // a subtype
Male     ::= Piet, Jan              // constants
Female   ::= Marie
Person   ::= father(<Person>)       // a functor: <Person> is a hole
Sentence :: Root                    // Root: may stand on its own line
Sentence ::= <Person> knows <Person> #0

Piet knows Marie
father(Marie) knows Jan
```

| Operator | Meaning                         |
|----------|---------------------------------|
| `::`     | type, subtype                   |
| `::=`    | functor (pattern) or constants  |
| `#N`     | precedence: higher binds tighter |

---

## Syntax: patterns

```text
Sentence ::= <Person> <(> really <)?> <(> likes <|> loves <)> <Person> #0,
             <Person> is <NUMBER> years old                            #0,
             <Person> says <STRING>                                    #0,
             party with <(> <Person> <,> , <)+>                        #0

Piet really loves Marie
Klaas is 42 years old
party with Piet, Jan, Marie
```

* `<(> ... <)?>` optional, `<(> a <|> b <)>` choice
* `<(> <Person> <,> , <)+>` one or more, separated by commas (`<)*>`: zero or more)
* `<NUMBER>`, `<STRING>`, `<NAME>`: tokens
* A pattern that starts with a hole is an **operator**: it needs a precedence

---

## Queries and results

```text
import nelumbo.logic
true & false ?       // [][()]
unknown & true ?     // [..][..]
unknown | true ?     // [()][]
```

A result is `[facts][falsehoods]`: the answers for which the query is true, and those for which it is false.

| Result          | Means                                      |
|-----------------|--------------------------------------------|
| `[()][]`        | true                                       |
| `[][()]`        | false                                      |
| `[..][..]`      | unknown                                    |
| `[(X=Jan),..]`  | X=Jan is an answer; there may be more (`..`) |

---

## Expected results are tests

```text
friends(Piet, X) ? [(X=Jan),..][..]
```

* Write the result you expect after the `?`
* Match: a **green check mark** at the end of the line
* Mismatch: the expectation is **underlined in red**, followed by the actual result
* No expectation: the result in a **purple label**; hover to see all of it

There is no Run button: everything is evaluated while you type.
The exercises use expected results to show when you are done.

---

## Facts: open and closed

```text
Boolean ::= friends(<Person>,<Person>)
fact friends(Piet, Jan), friends(Jan, Klaas)
friends(Piet, X) ?      // [(X=Jan),..][..]   open: more friends may exist

Integer ::= age(<Person>)
fact age(Piet)=40
age(Piet)=a ?           // [(a=40)][..]       closed: one value
age(Piet)=41 ?          // [][()]
```

* What is not asserted is **unknown**, not false
* A function has one value; `<one City>` makes any hole unique
* **Monotonic**: a later `fact age(Piet)=22` is an inconsistency

---

## Rules

```text
Integer ::= fib(<Integer>)
Integer n
fib(n) == n if n<=1, fib(n-1)+fib(n-2) if n>1

fib(7) ?                // [(13)][..]
```

* `<=>` defines a relation, `==` the value of a function
* `if` guards each alternative; a rule may call itself
* Where a guard fails, the alternative is **silent**: it says nothing, it does not say false

---

## Combining rules: both sides

```text
eats(p,f) <=> likes(p,f)
eats(Piet,f) ?      // [(f=Apple),(f=Pear),..][..]

eats(p,f) <=> false if allergic(p,f)
eats(Piet,f) ?      // [(f=Apple),(f=Pear),..][(f=Fish),(f=Nut),..]
eats(Piet,Bread) ?  // [..][..]
```

* Facts and rules of one relation are combined
* A rule can add only falsehoods
* They must agree: a conflict is reported as an inconsistency

---

## Cycles and quantifiers

* A **cyclic** rule is solved by iteration until nothing new is found: the *fixpoint*
* `E[x](...)` there exists, `A[x](...)` for all
* Quantifiers **navigate**: they never try every declared member of a type

```text
E[c](age(c)>18) ?                               // [..][..]
E[c](c in children(Juliana) & age(c)>18) ?      // [()][]
A[c]((c in children(Juliana)) -> age(c)>18) ?   // [][()]
```

---

## Your own language

A transformation `::>` turns one statement into others, for example a dictionary into sentences:

```text
noun a dog
noun an owl
verb chases

Rex is a dog            // a fact
Hoot is an owl
Rex chases Hoot

is Rex a dog ?          // a query
who chases Hoot ?
```

`Hoot is a owl` does not parse: the dictionary says *an* owl.

---

## The tutorial

**nelumbo.nl/tutorial.html**: 31 sections, each with an example and exercises.

| Group           | Sections | You learn                                                    |
|-----------------|----------|--------------------------------------------------------------|
| Syntax          | 8        | types, functors, patterns, precedence, generics, scoping      |
| Semantics       | 11       | logic, facts, rules, cycles, data types, quantifiers, DSLs    |
| Transformations | 2        | rewriting statements, controlled English                     |
| Projects        | 10       | a whole language from given facts and questions              |

---

## The editor

* Edit the code: results appear at the end of each line
* Hover over a result, a name or an error for details
* Red underline: a parse error or a wrong result
* **Show solution** when you are stuck, **Reset** to start over
* Your edits are saved in your browser: come back later and continue

---

## Suggested route

1. **Syntax**: Types & functors, Patterns, Precedence
2. **Semantics**: from Propositional logic to Cyclic rules
3. Then choose:
   * more depth: Quantifiers, Custom DSL, Transformations
   * a **project**: Family tree, Club fees and Train network first; the later ones are harder

Projects give only facts and questions with expected answers; you write the language above them.
Hints are folded away under each project.

Working in pairs works well.

---

## When you are stuck

* A sentence (`:: Root`) needs `#0`; a phrase starting with a hole needs a precedence
* `[..][..]` is not false: facts are open. Close them with `one`, or a rule giving `false`
* A failing guard is silent: write the `false if ...` branch yourself
* A quantifier gives no answer? Let it navigate from a known relation
* Everything unknown at once? Check the types of your variables
* Still stuck: **Show solution**, or ask us

---

## Beyond the browser

* **nelumbo.nl**: Sandbox (examples), Library (standard modules), Docs (reference)
* **Command line**: `java -jar nelumbo-cli-<version>.jar file.nl` checks every expected result, for CI too; `-i` for a REPL
* **IDE plugins**: VS Code, IntelliJ, Eclipse (Language Server Protocol)
* **MCP server**: let an AI assistant write and check Nelumbo
* Downloads: GitHub releases

---

## Where Nelumbo stands

Early stage: incompatible changes are likely.

Recently added: generics, lists and sets, transformations, the editor on the web.

On the list:

* Full Unicode in names, operators and patterns
* A better word for "falsehood"
* Reactive update execution semantics
* Deprecation and migration support

---

## Help us

* Tell us what surprised or confused you today
* Try to falsify the logic
* Write tests and libraries
* Help write scientific publications
* GitHub: https://github.com/ModelingValueGroup/nelumbo
* Email: wim.bast@gmail.com

---

# Let's start

<img src="nelumbo.svg" alt="Nelumbo" width="50" height="50" />

**nelumbo.nl/tutorial.html**

---

# Q&A

---

## Backup: native semantics

The integer operators are rules over a native relation:

```text
private Boolean ::= add(<Integer>,<Integer>,<Integer>)
                    @nelumbo.integers.Integers
a+b=c <=> add(a,b,c)
a-b=c <=> add(c,b,a)
```

---

<!-- .slide: class="dense" -->

## Backup: the Java side

A `@NelumboMethod` whose name and parameter count match the functor; an unbound argument is `null`:

```text
@NelumboMethod
protected InferResult add(NInteger addend1, NInteger addend2, NInteger sum) {
    if (nrOfUnbound() > 1) {
        return unknown();
    }
    BigInteger a1 = addend1 == null ? null : addend1.value();
    BigInteger a2 = addend2 == null ? null : addend2.value();
    BigInteger s = sum == null ? null : sum.value();
    if (a1 != null && a2 != null) {
        BigInteger r = a1.add(a2);
        if (s != null) {
            return r.equals(s) ? factCC() : falsehoodCC();
        }
        return set(2, NInteger.of(r)).factCI();
    } else if (a1 != null && s != null) {
        return set(1, NInteger.of(s.subtract(a1))).factCI();
    } else if (a2 != null && s != null) {
        return set(0, NInteger.of(s.subtract(a2))).factCI();
    }
    return unknown();
}
```
