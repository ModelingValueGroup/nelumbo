// Regression test (fixed 2026-10-27; was bugs/pos-duplicate-element-inconsistent.nl).
// A list element occurring twice has two indexes. The removed `pos` was declared as a
// function, so this was reported as an inconsistent result and `in` failed too. The
// index relation `at i in l = e` may have several answers; the functions `first index`,
// `last index` and `indexes` have one.
import nelumbo.collections

Integer i

at i in [1,2,2] = 2   ? [(i=1),(i=2)][..]
2 in [1,2,2]          ? [()][]
2 first index [1,2,2] ? [(1)][..]
2 last index [1,2,2]  ? [(2)][..]
2 indexes [1,2,2]     ? [({1,2})][..]
