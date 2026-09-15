// Bug: inference results are NONDETERMINISTIC across runs of the same file,
// even with -DPARALLEL_COLLECTIONS=false (that flag serializes the
// collections library, but KnowledgeBase runs inference on its own
// ContextPool with Collection.PARALLELISM workers - hard floor of 2, see
// immutable-collections ContextThread.createPool/Collection.PARALLELISM).
// Observed 2026-09-10: at default parallelism the last query below failed in
// 3 of 4 consecutive runs and passed in the other; with -DPARALLELISM=2 it
// passed 4 of 4. Probable area: the rank-based interning inside equals() of
// immutable-collections corrupting structurally-equal predicates across
// inference threads (the known parallel-collections interning bug, NOT
// prevented by PARALLEL_COLLECTIONS=false).
// Correct: same file, same result, every run. Actual: run it a few times.
// NOTE for run-all.sh: this file may show PASS on a lucky run.
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
