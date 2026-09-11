// Bug: a collection-returning functor CALL used as an argument crashes with
//   java.lang.IllegalArgumentException: argument type mismatch
//   at logic/Predicate.callMethod (reflective native invoke)
// DETERMINISTIC on the CLI: 9/9 runs, independent of PARALLELISM /
// PARALLEL_COLLECTIONS.
//
// lst(0) returns a list; used directly as the list operand of the native `pos`
// it crashes. Binding it via E first WORKS:  E[l](lst(0)=l & x pos l = 1).
// Simple arithmetic nesting (inc(inc(i))) does NOT crash - specific to a
// collection-returning functor call reaching a native consumer unresolved.
//
// The sudoku accessor cell(g,r,c)=x <=> x=at(row(g,r),c) hits this via the
// nested row(g,r) call; worked around in examples/sudoku-4x4-csp.nl by E-binding.
//
// Correct: x = 11 (element at index 1). Actual: argument type mismatch.
// Found 2026-09-11 while writing examples/sudoku-4x4-csp.nl (Norvig CSP solver).
import nelumbo.collections

Integer       x, i
List<Integer> ::= lst(<Integer>)

lst(i)=[10,11,12,13]     // constant list (arg ignored; nullary functors are rejected)

x pos lst(0) = 1 ? [(x=11)][..]
