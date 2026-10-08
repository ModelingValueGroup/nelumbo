// Regression test (fixed 2026-10-06; was bugs/set-literal-arithmetic-unevaluated.nl).
// Arithmetic inside a collection literal must be evaluated: in a rule body
// mk(j)=s <=> s={j+1} the query mk(1)=s must give s={2}, and inside a `map`
// lambda ([0,1,2] map [j]({j+1})) the elements must be real sets. (Was: the
// set-literal rule was UNDECIDED ([..][..]) while the same arithmetic outside
// a literal (two(j)=x <=> x=j+1) worked, and in the map lambda the element
// node was a ListImpl carried as a Set, so printing the result crashed with
// ClassCastException: ListImpl cannot be cast to Set at NSet.collection
// (NSet.java:53) <- NSet.toString. Deterministic on the CLI. The LIST-literal
// twin `[j+1]` did not even parse back then - "Unexpected token '+', expected
// ',',']'" - so it is covered here as well now. Found 2026-09-24 while resuming
// examples/sudoku-4x4-csp.nl (Norvig CSP solver); fixed by c0e42831
// "expressions/functions in singletons" + 629285e2 "more then one
// expressions/functions as collection elements".)
import nelumbo.collections

Integer             j, x
Set<Integer>        s
List<Integer>       li
List<Set<Integer>>  l
List<List<Integer>> ll

Integer             ::= two(<Integer>)
Set<Integer>        ::= mk(<Integer>)
List<Integer>       ::= mkl(<Integer>)
List<Set<Integer>>  ::= mkAll
List<List<Integer>> ::= mkAllL

two(j)=x           <=>  x=j+1
mk(j)=s            <=>  s={j+1}
mkl(j)=li          <=>  li=[j+1]
mkAll=l            <=>  l=[0,1,2] map [j]({j+1})
mkAllL=ll          <=>  ll=[0,1,2] map [j]([j+1])

two(1)=x   ? [(x=2)][..]                // control: always worked
mk(1)=s    ? [(s={2})][..]              // was undecided
mkl(1)=li  ? [(li=[2])][..]             // list twin: did not parse
mkAll=l    ? [(l=[{1},{2},{3}])][..]    // was ClassCastException ListImpl -> Set
mkAllL=ll  ? [(ll=[[1],[2],[3]])][..]   // list twin in a map lambda
