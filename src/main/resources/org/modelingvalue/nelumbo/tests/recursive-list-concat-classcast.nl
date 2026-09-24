// Regression test (fixed 2026-09-24; was bugs/recursive-list-concat-classcast.nl).
// A recursive list-CONCAT builder must reduce: build(n) accumulates [1..n] by
// concatenating the recursive result with [n].
// (Was: ClassCastException Variable -> List at NList.collection on the CLI, and
// an unreduced-term expectation mismatch in the test JVM; introduced by
// bba88fc8 (2026-09-09), bisected 2026-09-13. Recursive set-UNION accumulation
// crashed identically - recursive COLLECTION accumulation in general.)
import nelumbo.collections

Integer       n
List<Integer> s, pre

List<Integer> ::= build(<Integer>)

build(n)=s    <=>  s=[]             if n=0,
                   s=build(n-1)+[n] if n>0

build(3)=s ? [(s=[1,2,3])][..]
