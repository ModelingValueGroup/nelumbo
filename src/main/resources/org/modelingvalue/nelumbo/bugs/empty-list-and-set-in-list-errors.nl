// Bug: an empty list or set ([] or {}) as an element of a collection breaks equality:
// binding a variable to such a collection finds nothing ([[],[2]] = ll is [][..]) and
// a computed collection holding [] is not equal to the same literal (false).
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
