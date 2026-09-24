// Bug: arithmetic inside a SET literal is not evaluated. In a rule body
// mk(j)=s <=> s={j+1} the query mk(1)=s is UNDECIDED ([..][..]); the same
// arithmetic outside a literal (two(j)=x <=> x=j+1) works. Inside a `map`
// lambda ([0,1] map [j]({j+1})) the element node is a ListImpl carried as a
// Set: printing the result crashes with
//   ClassCastException: ListImpl cannot be cast to Set
//   at collections/NSet.collection (NSet.java:53) <- NSet.toString
// DETERMINISTIC on the CLI (2/2 runs, each query also alone).
// Note: the LIST-literal twin `[j+1]` does not even parse
// ("Unexpected token '+', expected ',',']'"), so it cannot be probed here.
//
// Correct: s = {2}, l = [{1},{2}]. Actual: undecided, then ClassCast.
// Found 2026-09-24 while resuming examples/sudoku-4x4-csp.nl (Norvig CSP solver).
import nelumbo.collections

Integer            j, x
Set<Integer>       s
List<Set<Integer>> l

Integer            ::= two(<Integer>)
Set<Integer>       ::= mk(<Integer>)
List<Set<Integer>> ::= mkAll(<Integer>)

two(j)=x   <=> x=j+1
mk(j)=s    <=> s={j+1}
mkAll(j)=l <=> l=[0,1] map [j]({j+1})

two(1)=x   ? [(x=2)][..]          // control: works
mk(1)=s    ? [(s={2})][..]        // UNDECIDED
mkAll(0)=l ? [(l=[{1},{2}])][..]  // ClassCastException ListImpl -> Set
