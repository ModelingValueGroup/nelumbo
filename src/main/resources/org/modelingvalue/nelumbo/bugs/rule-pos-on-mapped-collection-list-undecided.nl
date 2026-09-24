// Bug: a user rule that wraps the native `pos` (at(l,c)=s <=> s pos l = c) is
// UNDECIDED ([..][..]) when the list comes out of a `map` AND its elements are
// collections (Set<Integer> here; List<Integer> elements fail the same way).
// DETERMINISTIC on the CLI (2/2 runs, same in isolation and next to other queries).
//
// Everything around it works, so the combination is the trigger:
//   - the same rule on a literal list of sets:      at([{1},{2}],1)=s   -> {2}
//   - the NATIVE pos on the mapped list:            s pos l = 0         -> {1}
//   - the same rule shape on a mapped list of Integers (ati(il,c)=x <=> x pos il = c
//     with il=[0,1] map [j](j+1))                                        -> works
//   - the same rule on a concat-built list of sets (pre+[rw]+suf)       -> works
//
// Hit in examples/sudoku-4x4-csp.nl: putRow builds a row with `map`, and every
// later cell(g,r,c) read (catRow(rw,c)=sc <=> sc pos rw = c) on that row is
// undecided, which makes put(...) unusable -> Task 4 of the CSP plan BLOCKED.
// Possibly related: empty-set-branch-in-map-lambda.nl (map + Set-valued rules).
//
// Correct: s = {1}. Actual: undecided.
// Found 2026-09-24 while resuming examples/sudoku-4x4-csp.nl (Norvig CSP solver).
import nelumbo.collections

Integer            c, j
Set<Integer>       s
List<Set<Integer>> l

Set<Integer> ::= at(<List<Set<Integer>>>,<Integer>)

at(l,c)=s <=> s pos l = c

at([{1},{2}],1)=s                        ? [(s={2})][..]   // literal list: works
E[l](l=[0,1] map [j]({1}) & s pos l = 0) ? [(s={1})][..]   // native pos on mapped list: works
E[l](l=[0,1] map [j]({1}) & at(l,0)=s)   ? [(s={1})][..]   // rule on mapped list: UNDECIDED
