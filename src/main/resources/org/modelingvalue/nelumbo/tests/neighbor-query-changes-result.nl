// Regression test (fixed 2026-09-25; was bugs/neighbor-query-changes-result.nl).
// A query's result depends only on the rules and its own inputs, not on which
// OTHER queries the file contains. (Was: extracting an element of a mapped
// result via pos (sat(meAll(0),0)) was deterministically UNDECIDED as the file
// stands - at any parallelism - and became decided by adding the unrelated
// whole-result query `meAll(0)=sl ? [(sl=[{1},{1},{2}])][..]` BEFORE the
// extractions. The same neighbor-sensitivity showed at larger scale as result
// FORMS flipping between closed [(x)] and open [(x),..] depending on the
// surrounding queries, see examples/sudoku-9x9-smart.nl's test-section
// comment - so this file deliberately does NOT contain the whole-result query.)
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
