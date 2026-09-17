// Bug: a guarded Set-valued rule with an empty-set branch (ds={}), called
// from a map lambda, makes the map evaluate to undecided ([..][..]). The
// same rule works standalone, and in a file WITHOUT this rule the identical
// shape with a non-empty branch value ({9} instead of {}) maps fine - it
// cannot serve as an in-file control because merely having the {}-rule in
// the file makes the control map undecided too (see
// sibling-rule-contamination.nl).
// Probable area: lambda lifting / InferResult completeness for empty
// collection literals.
// Correct: sat(mtAll(0),j) yields the mapped values. Actual: undecided.
// Related: {} inside a NESTED list literal does not even type-check
// ("Node {} of unexpected type ..., expected E$..."), which is why the map
// result is extracted with sat instead of compared as a whole list.
// Found 2026-09-10 while writing examples/sudoku-9x9-smart.nl (its candidate
// elimination needed exactly this shape and had to be abandoned).
import nelumbo.collections

R :: Object
Set<Integer>       ::= mt(<Integer>), sat(<List<Set<Integer>>>,<Integer>)
List<Set<Integer>> ::= mtAll(<Integer>)

Set<Integer>       ds, cs
List<Set<Integer>> sl, crw
Integer            n, j

sat(crw,j)=cs <=>  cs pos crw = j
mt(j)=ds      <=>  ds={}  if j=0,  ds={1} if j!=0
mtAll(n)=sl   <=>  sl = [0,1,2] map [j](mt(j))

// the rule itself works standalone, empty branch included:
mt(0)=ds ? [(ds={})][..]
mt(1)=ds ? [(ds={1})][..]
// mapped, it is undecided:
sat(mtAll(0),0)=cs ? [(cs={})][..]
sat(mtAll(0),1)=cs ? [(cs={1})][..]
