// Regression test (fixed 2026-11-17; was bugs/empty-collection-function-rule-value.nl).
// An empty set or list literal ({} or []) is a value of a function rule (==). (Was:
// "Node {} of unexpected type {Literal,Set<...>}, expected R" and the rule dropped.)
import nelumbo.collections

Integer       i
Set<Integer>  ::= f(<Integer>), h(<Integer>)
List<Integer> ::= k(<Integer>)

f(i) == {} if i=0, {i} if i>0
h(i) == {}
k(i) == [] if i=0, [i] if i>0

f(0) ? [({})][..]
f(2) ? [({2})][..]
h(0) ? [({})][..]
k(0) ? [([])][..]
k(2) ? [([2])][..]
