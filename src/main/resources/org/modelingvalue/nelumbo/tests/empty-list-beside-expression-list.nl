// Regression test (fixed 2026-11-17; was bugs/empty-list-beside-expression-list.nl).
// A list of lists holding an expression (`[1+2]`), also next to an empty list, parses and
// its value equals the same literal. (Was: "Node [](1+2) of unexpected type ...", and the
// value [[1],[3]] of [[1],[1+2]] did not match the literal [[1],[3]].)
import nelumbo.collections

[[1],[1+2]] ? [([[1],[3]])][..]
[[],[1+2]]  ? [([[],[3]])][..]
[[1+2],[]]  ? [([[3],[]])][..]
