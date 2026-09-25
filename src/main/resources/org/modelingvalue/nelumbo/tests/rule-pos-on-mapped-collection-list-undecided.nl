// Regression test (fixed 2026-09-25; was bugs/rule-pos-on-mapped-collection-list-undecided.nl).
// A user rule that wraps the native `pos` (at(l,c)=s <=> s pos l = c) must
// decide when the list comes out of a `map` AND its elements are collections
// (Set<Integer> here; List<Integer> elements failed the same way). (Was:
// UNDECIDED [..][..], deterministically, on exactly that combination - the
// same rule on a literal list of sets, the native pos on the mapped list, the
// same rule shape on a mapped list of Integers, and the same rule on a
// concat-built list of sets all worked. Found 2026-09-24 in
// examples/sudoku-4x4-csp.nl: every cell(g,r,c) read on a putRow-built row
// was undecided, which blocked Task 4 of the CSP plan.)
import nelumbo.collections

Integer            c, j
Set<Integer>       s
List<Set<Integer>> l

Set<Integer> ::= at(<List<Set<Integer>>>,<Integer>)

at(l,c)=s <=> s pos l = c

at([{1},{2}],1)=s                        ? [(s={2})][..]   // literal list: works
E[l](l=[0,1] map [j]({1}) & s pos l = 0) ? [(s={1})][..]   // native pos on mapped list: works
E[l](l=[0,1] map [j]({1}) & at(l,0)=s)   ? [(s={1})][..]   // rule on mapped list: was UNDECIDED
