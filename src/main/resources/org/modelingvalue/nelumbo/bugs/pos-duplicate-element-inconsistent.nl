// Bug: `pos` is declared as a function, so a list element occurring twice has two
// indexes and is reported as an inconsistent result; `in` on such a list fails too.
import nelumbo.collections

Integer i

2 pos [1,2,2] = i ? [(i=1),(i=2)][..]
2 in [1,2,2]      ? [()][]
