// Regression test (fixed 2026-11-03; was bugs/empty-collection-literal-rejected-as-argument.nl).
// An empty collection literal is accepted as the argument of a typed hole: sz({}) and
// ln([]) evaluate like the variable-bound E[ns](ns={} & sz(ns)=k). (Was: "Node {} of
// unexpected type {Literal,Set<E$..$$>}, expected Set<Integer>" and the query dropped.)
import nelumbo.collections

Integer ::= sz(<Set<Integer>>), ln(<List<Integer>>)

Integer       k
Set<Integer>  ns
List<Integer> il

sz(ns)=k <=>  k=|ns|
              ln(il)=k <=>  k=|il|

E[ns](ns={} & sz(ns)=k) ? [(k=0)][..]
sz({})=k                ? [(k=0)][..]
ln([])=k                ? [(k=0)][..]
