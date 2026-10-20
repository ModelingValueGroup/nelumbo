// Bug: an empty collection literal is rejected as the argument of a typed hole:
// sz({}) gives "Node {} of unexpected type {Literal,Set<E$..$$>,Struct}, expected
// Set<Integer>" (ln([]) the same for List<Integer>), and the query is dropped.
// Bound through a variable (ns={}) it is accepted.
import nelumbo.collections

Integer       ::= sz(<Set<Integer>>), ln(<List<Integer>>)

Integer       k
Set<Integer>  ns
List<Integer> il

sz(ns)=k <=> k=|ns|
ln(il)=k <=> k=|il|

E[ns](ns={} & sz(ns)=k) ? [(k=0)][..]
sz({})=k                ? [(k=0)][..]
ln([])=k                ? [(k=0)][..]
