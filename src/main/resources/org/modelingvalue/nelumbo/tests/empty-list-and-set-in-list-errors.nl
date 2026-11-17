// Regression test (fixed 2026-11-17; was bugs/empty-list-and-set-in-list-errors.nl).
// An empty list or set ([] or {}) as an element of a collection gets the element type of
// its neighbours: such a collection binds to a variable and equals a computed collection.
// (Was: [[],[2]] = ll gave [][..] and a computed collection holding [] was not equal.)
import nelumbo.collections

List<Integer>       l
List<List<Integer>> ll
Set<Integer>        s
Set<Set<Integer>>   ss
List<Set<Integer>>  ls
Set<List<Integer>>  sl

[[],[2]] = ll                       ? [(ll=[[],[2]])][..]
{{},{2}} = ss                       ? [(ss={{},{2}})][..]
{[],[2]} = sl                       ? [(sl={[],[2]})][..]
[{},{2}] = ls                       ? [(ls=[{},{2}])][..]

{{1},{2}} map [s]({}) = [{},{}]     ? [()][]
[{1},{2}] map [s]({}) = [{},{}]     ? [()][]
{[1],[2]} map [l]([]) = [[],[]]     ? [()][]
[[1],[2]] map [l]([]) = [[],[]]     ? [()][]

set [] at 0 in [[1],[2]]            ? [([[],[2]])][..]
set {} at 0 in [{1},{2}]            ? [([{},{2}])][..]

set [] at 0 in [[1],[2]] = [[],[2]] ? [()][]
set [] at 0 in [[1],[2]] = ll       ? [(ll=[[],[2]])][..]
set {} at 0 in [{1},{2}] = [{},{2}] ? [()][]
set {} at 0 in [{1},{2}] = ls       ? [(ls=[{},{2}])][..]
