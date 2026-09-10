// Bug: whether a query is decided depends on which OTHER queries the file
// contains. Extracting an element of a mapped result via pos
// (sat(meAll(0),0)) is deterministically UNDECIDED as the file stands - at
// any parallelism. Add the whole-result query
//     meAll(0)=sl ? [(sl=[{1},{1},{2}])][..]
// BEFORE the extractions and everything passes (verified with
// -DPARALLELISM=2 for determinism; at default parallelism results
// additionally race, see nondeterministic-inference.nl).
// The same neighbor-sensitivity shows at larger scale as result FORMS
// flipping between closed [(x)] and open [(x),..] depending on surrounding
// queries (see examples/sudoku2.nl's test-section comment).
// Correct: a query's result depends only on the rules and its own inputs.
// Actual: the two extractions below are undecided until an unrelated query
// is added.
import nelumbo.collections

R :: Object
Set<Integer>       ::= me(<Integer>), sat(<List<Set<Integer>>>,<Integer>)
List<Set<Integer>> ::= meAll(<Integer>)

Set<Integer>       ds, cs
List<Set<Integer>> sl, crw
Integer            n, j, i

sat(crw,j)=cs <=>  cs pos crw = j
me(j)=ds      <=>  ds={1} if E[i](i in {0,1,2} & i=j),  ds={2} if !E[i](i in {0,1,2} & i=j)
meAll(n)=sl   <=>  sl = [0,1,5] map [j](me(j))

me(1)=ds ? [(ds={1})][..]
me(5)=ds ? [(ds={2})][..]
sat(meAll(0),0)=cs ? [(cs={1})][..]
sat(meAll(0),2)=cs ? [(cs={2})][..]
