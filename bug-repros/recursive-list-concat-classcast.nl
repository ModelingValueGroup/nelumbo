// Bug: a recursive list-CONCAT builder crashes with
//   java.lang.ClassCastException: org.modelingvalue.nelumbo.lang.Variable
//   cannot be cast to org.modelingvalue.collections.List
// DETERMINISTIC on the CLI: 5/5 runs, with -DPARALLEL_COLLECTIONS=false.
//
// build(n) accumulates [1..n] by concatenation. Concat-FREE recursion (a plain
// counter) does NOT crash - the crash needs the `s = pre+[n]` concat inside the
// recursion. A recursive set-UNION accumulator crashes identically, so it is
// recursive COLLECTION accumulation in general.
//
// This is the rowsBefore/rowsFrom shape of examples/sudoku-*-smart.nl, which now
// ALSO crash on the CLI (REGRESSION vs the 2026-09-10 notes of 20/20 CLI-clean).
// It blocks any functional grid update and every candidate-set builder.
//
// Correct: s = [1,2,3]. Actual: ClassCastException (Variable -> List).
// Found 2026-09-11 while writing examples/sudoku-4x4-csp.nl (Norvig CSP solver).
import nelumbo.collections

Integer       n
List<Integer> s, pre

List<Integer> ::= build(<Integer>)

build(n)=s <=> s=[]                                if n=0,
               E[pre](build(n-1)=pre & s=pre+[n])  if n>0

build(3)=s ? [(s=[1,2,3])][..]
