# Formatting

The Nelumbo language server can format a `.nl` document (LSP `textDocument/formatting`
and `textDocument/rangeFormatting`). Every IDE plugin exposes this through its usual
"Format Document" / "Format Selection" command. Formatting only ever changes
**whitespace** — it never reorders, inserts, or rewrites tokens — and it is **idempotent**:
formatting an already-formatted file is a no-op.

This page is the canonical description of what the formatter does, so you can write files
by hand in the same style the tool produces.

---

## Worked example

```
Integer :: Object

Boolean ::= <Integer> ">" <Integer> #30 @nelumbo.integers.GreaterThan,
            <Integer> "<" <Integer> #30 @nelumbo.integers.LessThan

Integer a, b
Object  n

fib(n)=f <=>  f=n                 if n>=0 & n<=1,
              f=fib(n-1)+fib(n-2) if n>1
```

Everything below is a rule the formatter applies to reach a layout like this.

---

## Indentation

- **Top-level statements start at column 0.** Any leading indentation is removed.
- **Continuation lines hang under the first item** of the statement they continue. A line
  is a continuation when the previous line ends with something the parser carries onto the
  next line — a comma, an opening bracket, or a trailing operator such as `|`. The first
  item is the token after the declaration operator (`::`, `::=`, `<=>`) or, when there is
  no operator, after the leading keyword (e.g. `fact`).

```
Person ::= p(<Person>),
           c(<Person>),
           d(<Person>)

fact pc(Hendrik, Juliana),
     pc(Juliana, Beatrix)
```

## Spacing after operators

Exactly one space follows `::`, `::=`, and the query `?`. The rule operator `<=>` is
followed by **two** spaces — the convention used throughout the standard library.

```
Person     :: Object
fib(n)=f   <=>  f=n
fib(5)=f    ? [(f=5)][..]
```

## Column alignment

Within a run of lines, the formatter pads whitespace so related markers line up into a
column. A run continues across a **single** blank line, so adjacent groups separated by one
blank line share the same column — for example, every `<=>` in a section of related rules
lines up even when blank lines group the rules visually. A run is broken by **two or more**
blank lines, or by an intervening line that is not part of the run (a statement of a
different kind, or a comment line). Use a double blank line to keep two groups aligned
independently.

| What aligns | Where | Notes |
|---|---|---|
| `::`, `::=`, `<=>` | adjacent declaration lines | all share **one** column |
| `?` | adjacent query lines | its own column |
| variable names | `Type name, …` blocks | padded under the longest type |
| `#N` precedence | alternatives of one `::=`/`<=>` body | |
| `@`-annotations | alternatives of one `::=` body | sits in the column right of `#N` |
| `if` guards | clauses of one `<=>` rule body | |
| trailing `//` comments | one statement | two spaces past the longest line |

```
Person     :: Object        Literal  l1, l2        Person ::= p(<Person>),  // parent
LongAnimal ::= legs(...)    Function f1, f2                   c(<Person>),  // child
fib(n)=f   <=>  f=n         Object   n1, n2                   d(<Person>)   // descendant
```

A `#N` written **inside** a pattern hole (`<Variable#100>`, `<Boolean#0>`) is part of the
pattern, not a statement-level precedence marker, and is left untouched.

## Blank lines and trailing whitespace

- Blank lines at the **start** and **end** of the file are removed.
- The file always ends with exactly one newline (added if missing).
- Blank lines **between** statements are left as written — a single blank keeps the
  surrounding statements in one alignment run, a double blank separates them (see above).
- Trailing whitespace is stripped from every line.

## Range formatting

When you format a selection, only the selected lines are edited, but the alignment column
is still computed from the whole surrounding block — so a reformatted line lines up with its
unselected block-mates.

---

## See also

- [`grammar.md`](grammar.md) — the syntax these statements follow
- [`precedence-and-associativity.md`](precedence-and-associativity.md) — the `#N` system the precedence column aligns
- [`writing-rules.md`](writing-rules.md) — `<=>` rules and `if` guards
