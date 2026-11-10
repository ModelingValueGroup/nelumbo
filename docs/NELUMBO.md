# Nelumbo

<img src="nelumbo.svg" alt="Nelumbo" width="50" height="50" />

## Hands-on workshop

A logic meta-language for specifying DSLs

**nelumbo.nl/tutorial.html**

---

## Today

We follow the tutorial **together, in its order**: four rounds,
each with a short explanation, exercises and a **checkpoint**.

| Round                      | Tutorial sections                     |
|----------------------------|---------------------------------------|
| 1. Syntax                  | Types & functors to Scoping           |
| 2. Facts and rules         | Propositional logic to Cyclic rules   |
| 3. Data and languages      | Data types to Controlled English      |
| 4. Projects                | Family tree, Club fees, ...           |

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

```nelumbo
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

## The tutorial and its editor

**nelumbo.nl/tutorial.html**: 31 sections in the sidebar, each with an example and exercises.
Nothing to install: a browser is all you need.

* Edit the code: results appear at the end of each line
* Hover over a result, a name or an error for details
* Red underline: a parse error or a wrong result; a green check mark: done
* **Show solution** when you are stuck, **Reset** to start over
* Your edits are saved in your browser: come back later and continue

---

## How we work

* Everybody follows the **same order**: the order of the tutorial
* Work in **pairs**: one types, both think; switch every section
* At a **checkpoint** everybody stops, finished or not, and we compare
* **Behind?** Use **Show solution** to catch up: the next round starts together
* **Ahead?** Help a neighbour, or vary the exercise: try to break the logic
* Note what surprised you: we collect it at every checkpoint

---

# Round 1: Syntax

Define the phrases of your own language

---

## Types and functors

```nelumbo
import nelumbo.lang
Person   :: Object                  // a type
Male     :: Person                  // subtypes
Female   :: Person
Male     ::= Piet, Jan              // constants
Female   ::= Marie
Person   ::= father(<Person>)       // a functor: <Person> is a hole
Sentence :: Root                    // Root: may stand on its own line
Sentence ::= <Person> knows <Person> #0

Piet knows Marie
father(Marie) knows Jan
```

| Operator | Meaning                          |
|----------|----------------------------------|
| `::`     | type, subtype                    |
| `::=`    | functor (pattern) or constants   |
| `#N`     | precedence: higher binds tighter |

---

## Patterns

<!-- nelumbo-context
Person   :: Object
Person   ::= Piet, Jan, Marie, Klaas
Sentence :: Root
-->

```nelumbo
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

## Go: round 1

In this order, in the **Syntax** group:

1. Types & functors
2. Patterns
3. Precedence
4. Parentheses
5. Generics
6. Connected tokens
7. Named patterns
8. Scoping

There are no queries yet: an exercise is done when every sentence parses.

---

## Checkpoint 1: Syntax

* Which sentence was hardest to make parse? What was the error message?
* Where did you need a precedence `#N`, and how did you pick the number?
* When would you use `private` in a language of your own?
* What surprised you?

**Take away**: a language is types plus phrases; precedence decides how phrases group.

---

# Round 2: Facts and rules

Give the phrases a meaning

---

## Queries and results

```nelumbo
import nelumbo.logic
true & false ?       // [][()]
unknown & true ?     // [..][..]
unknown | true ?     // [()][]
```

A result is `[facts][falsehoods]`: the answers for which the query is true, and those for which it is false.

| Result          | Means                                        |
|-----------------|----------------------------------------------|
| `[()][]`        | true                                         |
| `[][()]`        | false                                        |
| `[..][..]`      | unknown                                      |
| `[(X=Jan),..]`  | X=Jan is an answer; there may be more (`..`) |

---

## Expected results are tests

<!-- nelumbo-context
Person  :: Object
Person  ::= Piet, Jan, Klaas
Boolean ::= friends(<Person>,<Person>)
Person  X
fact friends(Piet, Jan), friends(Jan, Klaas)
-->

```nelumbo
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

<!-- nelumbo-context
Person  :: Object
Person  ::= Piet, Jan, Klaas
Person  X
Integer a
-->

```nelumbo
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

```nelumbo
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

<!-- nelumbo-context
Person  :: Object
Person  ::= Piet
Food    :: Object
Food    ::= Apple, Pear, Nut, Fish, Bread
Boolean ::= likes(<Person>,<Food>), allergic(<Person>,<Food>), eats(<Person>,<Food>)
Person  p
Food    f
fact likes(Piet, Apple), likes(Piet, Pear)
fact allergic(Piet, Nut), allergic(Piet, Fish)
-->

```nelumbo
eats(p,f) <=> likes(p,f)
eats(Piet,f) ?      // [(f=Apple),(f=Pear),..][..]

eats(p,f) <=> false if allergic(p,f)
eats(Piet,f) ?      // [(f=Apple),(f=Pear),..][(f=Fish),(f=Nut),..]
eats(Piet,Bread) ?  // [..][..]
```

* Facts and rules of one relation are combined; a rule can add only falsehoods
* They must agree: a conflict is reported as an inconsistency
* A **cyclic** rule is solved by iteration until nothing new is found: the *fixpoint*

---

## Go: round 2

In this order, in the **Semantics** group:

1. Propositional logic
2. Facts & queries
3. Unique values
4. Rules & recursion
5. Combining rules
6. Cyclic rules

From here on an exercise is done when every query shows a green check mark.

---

## Checkpoint 2: Facts and rules

* Which result surprised you? Was it unknown where you expected false?
* When does an answer have no `..`? What closed it?
* Did you run into an inconsistency? What caused it?
* How did the fixpoint of the cyclic rule find its answers?

**Take away**: unknown is not false; facts and rules only add knowledge, never take it back.

---

# Round 3: Data and languages

Functions, literals, quantifiers and transformations

---

## Functions and literals

* Built-in types come from modules: `import nelumbo.integers`, `strings`, `collections`, ...
* A functor of an `Object` type is a **function**: `double(2)` stands for the value a rule or fact gives it
* Without a rule or fact that value is unknown: even `point(1,2)=point(1,2)` is unknown
* A type declared `:: Literal` builds **values**: `pos(1,2)` is equal to another one exactly when it is built the same way

---

## Quantifiers navigate

* `E[x](...)` there exists, `A[x](...)` for all
* A quantifier never tries every declared member of a type: its variable gets values from a known relation

<!-- nelumbo-context
Person      :: Object
Person      ::= Juliana, Beatrix, Irene, Margriet
Integer     ::= age(<Person>)
Set<Person> ::= children(<Person>)
Person      c
fact age(Beatrix)=20, age(Irene)=15, age(Margriet)=30
fact children(Juliana)={Beatrix, Irene, Margriet}
-->

```nelumbo
E[c](age(c)>18) ?                               // [..][..]
E[c](c in children(Juliana) & age(c)>18) ?      // [()][]
A[c]((c in children(Juliana)) -> age(c)>18) ?   // [][()]
```

---

## Your own language

A transformation `::>` turns one statement into others, for example a dictionary into sentences:

<!-- nelumbo-context
Article :: Object
Article ::= a, an
Root    ::= noun <Article> <NAME> #100,
            verb <NAME>           #100
Thing   :: Object
{
    Article ar
    NAME    n, v
    noun ar n ::> {
        Root            ::= <{Thing,Literal}> is <ar> n #0
        Root            ::= is <Thing> <ar> n ?         #0
        private Boolean ::= n(<Thing>)
        Thing x
        is x ar n ? ::> {
            n(x) ?
        }
        {Thing,Literal} t
        t is ar n ::> {
            fact n(t)
        }
    }
    verb v ::> {
        Root            ::= <{Thing,Literal}> v <{Thing,Literal}> #0
        Root            ::= who v <Thing> ?                       #0
        private Boolean ::= v(<Thing>,<Thing>)
        Thing x, y
        who v y ? ::> {
            v(x,y) ?
        }
        {Thing,Literal} s, o
        s v o ::> {
            fact v(s,o)
        }
    }
}

Thing ::= Rex, Hoot, Tom
-->

```nelumbo
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

## Go: round 3

In this order:

1. Data types
2. Equality
3. Relational KB
4. Quantifiers
5. Custom DSL
6. Transformations
7. Controlled English

---

## Checkpoint 3: Data and languages

* Why does a quantifier not try every declared person? What would go wrong?
* When is `=` decided, and when does it stay unknown?
* Which words did you add to the controlled English? What would you add next?
* What surprised you?

**Take away**: navigate from what is known; a transformation turns your sentences into facts and queries.

---

# Round 4: Projects

A whole language from given facts and questions

---

## Go: round 4

* Only the end of the file is given: facts and questions with expected answers
* You write the language above it: types, phrases, variables and rules
* Hints are folded away under each project
* In this order, as far as you get:
  1. Family tree
  2. Club fees
  3. Train network
  4. Number toolkit, Set algebra, List statistics, Vectors, Expression interpreter, Room booking, Access policy

---

## When you are stuck

* A sentence (`:: Root`) needs `#0`; a phrase starting with a hole needs a precedence
* `[..][..]` is not false: facts are open. Close them with `one`, or a rule giving `false`
* A failing guard is silent: write the `false if ...` branch yourself
* A quantifier gives no answer? Let it navigate from a known relation
* Everything unknown at once? Check the types of your variables
* Still stuck: **Show solution**, or ask us

---

## Final round: show and compare

* Show your **Family tree**: how did you define ancestor, sister, aunt? Several solutions are right
* How far did you get? Which project was hardest, and why?
* Which knowledge from your own work would you write in Nelumbo? What is missing for it?
* What should change in the tutorial?

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

* Reactive update execution semantics
* Deprecation and migration support
* Removal of functors, private import and external DSLs
* Full Unicode in names, operators and patterns

---

## Help us

* Tell us what surprised or confused you today
* Try to falsify the logic
* Write tests and libraries
* Help write scientific publications
* GitHub: https://github.com/ModelingValueGroup/nelumbo
* Email: wim.bast@gmail.com

---

# Q&A

---

<!-- .slide: class="dense" -->

## Backup: native semantics

The integer operators are rules over a native relation:

<!-- nelumbo-context
Integer a, b, c
-->

```nelumbo
private Boolean ::= add(<Integer>,<Integer>,<Integer>) @nelumbo.integers.Integers
a+b=c <=> add(a,b,c)
a-b=c <=> add(c,b,a)
```

---

<!-- .slide: class="dense" -->

## Backup: the Java side

A `@NelumboMethod` whose name and parameter count match the functor; an unbound argument is `null`:

```java
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
