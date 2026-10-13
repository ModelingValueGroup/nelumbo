// Bug: a functor whose whole pattern is ONE type hole (`<Integer>` below: a
// coercion, "every Integer is also a Cell") is accepted at its declaration, but
// from then on the parser crashes on every expression of the hole's type,
// wherever it occurs - here on every line after the declarations, the
// unrelated `1+1=v` too:
//   NullPointerException: "this.leftPrecedence" is null
//   at syntax.PatternResult.leftPrecedence (PatternResult.java:106)
//   via syntax.Parser.parseNode (Parser.java:129)
// It is uncaught: the CLI dies with a stack trace, no query is evaluated.
// Correct: the coercion works (the docs only say a pattern may start with a
// hole; nothing forbids a lone hole), all three queries decide as stated. If a
// lone hole is meant to be illegal, correct = a diagnostic at the declaration
// (then convert this repro to `// expect-error-per-query`); an NPE on unrelated
// lines is wrong either way.
// Actual: NullPointerException.
// Control: with `Cell ::= blank` alone (no `<Integer>` alternative, no num(k)
// rule, no num(3) query) the other two queries pass.
// Code location: not investigated beyond the stack trace.
// Found 2026-10-13 in examples/sudoku-4x4-csp.nl (plan Task 17): the grid DSL
// cell `Cell ::= ".", <Integer>` (a blank or a clue) crashes the whole file.
import nelumbo.integers

Cell    :: Object
Cell    ::= blank, <Integer>
Integer ::= num(<Cell>)
Integer k, v

num(blank)=v <=> v=0
num(k)=v     <=> v=k

1+1=v        ? [(v=2)][..]
num(blank)=v ? [(v=0)][..]
num(3)=v     ? [(v=3)][..]
