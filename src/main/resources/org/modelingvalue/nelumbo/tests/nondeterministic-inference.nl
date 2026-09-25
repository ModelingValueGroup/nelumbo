// Regression test (fixed 2026-09-25; was bugs/nondeterministic-inference.nl).
// Inference results must be the SAME on every run of the same file. (Was:
// nondeterministic run-to-run even with -DPARALLEL_COLLECTIONS=false - that
// flag serializes the collections library, but KnowledgeBase runs inference
// on its own ContextPool with Collection.PARALLELISM workers, hard floor 2.
// Observed 2026-09-10: at default parallelism the last query below failed in
// 3 of 4 consecutive runs; with -DPARALLELISM=2 it passed 4 of 4. Suspected
// the rank-based interning inside equals() of immutable-collections
// corrupting structurally-equal predicates across inference threads.)
// RegressionTest runs this file 10x with RANDOM_NELUMBO=true as the
// determinism check; sudoku-4x4-smart.nl's 1-in-10 undecided result was the
// same flake (SudokuExamplesTest).
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
meAll(0)=sl ? [(sl=[{1},{1},{2}])][..]
sat(meAll(0),0)=cs ? [(cs={1})][..]
