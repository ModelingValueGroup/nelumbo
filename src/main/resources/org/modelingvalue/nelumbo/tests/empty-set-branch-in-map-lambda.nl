// Regression test (fixed 2026-09-25; was bugs/empty-set-branch-in-map-lambda.nl).
// A guarded Set-valued rule with an empty-set branch (ds={}), called from a
// map lambda, must map like any other rule: sat(mtAll(0),j) yields the mapped
// values. (Was: the map evaluated to undecided [..][..] as soon as the {}-rule
// existed in the file - the rule worked standalone, and the same shape with a
// non-empty branch value mapped fine. Found 2026-09-10 while writing
// examples/sudoku-9x9-smart.nl, whose candidate elimination needed exactly
// this shape and had to be abandoned.)
// Note: {} inside a NESTED list literal does not type-check ("Node {} of
// unexpected type ..., expected E$..."), which is why the map result is
// extracted with sat instead of compared as a whole list.
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
// mapped (was undecided):
sat(mtAll(0),0)=cs ? [(cs={})][..]
sat(mtAll(0),1)=cs ? [(cs={1})][..]
