// Bug: an empty set or list literal ({} or []) as a value of a function rule (==)
// does not parse: "Node {} of unexpected type {Literal,Set<...>}, expected R".
// The same rule written with <=> (f(i)=s <=> s={} if i=0) works.
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
