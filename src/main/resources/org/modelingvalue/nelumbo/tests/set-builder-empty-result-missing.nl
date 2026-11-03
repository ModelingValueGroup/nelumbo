// Regression test (fixed 2026-11-03; was bugs/set-builder-empty-result-missing.nl).
// A set-builder whose condition has no solutions yields the empty set, so comparing
// it with {} is true. (Was: its result had no binding.)
import nelumbo.collections

Integer i

{[i](i in {1,2,3} & i>5)}={} ? [()][]
{[i](i in {})}={}            ? [()][]
