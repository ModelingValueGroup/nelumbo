// Bug: a list of lists holding an empty list `[]` next to a list with an expression
// (`[1+2]`) does not parse: "Node [](1+2) of unexpected type ..." (in either order).
// Correct: it evaluates like [[1],[1+2]] does.
import nelumbo.collections

[[],[1+2]]  ? [([[],[3]])][..]
[[1+2],[]]  ? [([[3],[]])][..]
