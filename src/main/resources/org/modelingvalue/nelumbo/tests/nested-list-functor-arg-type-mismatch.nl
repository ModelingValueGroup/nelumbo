// Regression test (fixed 2026-09-24; was bugs/nested-list-functor-arg-type-mismatch.nl).
// A collection-returning functor CALL used directly as the list operand of the
// native `pos` must be reduced first: x pos lst(0) = 1 gives x=11.
// (Was: java.lang.IllegalArgumentException: argument type mismatch at
// logic/Predicate.callMethod; E-binding the call first was the workaround. The
// sudoku accessor cell(g,r,c)=x <=> x=at(row(g,r),c) hit this via the nested
// row(g,r) call. Same reduction/typing family as recursive-list-concat-classcast.)
// The bare fact form `lst(i)=[10,11,12,13]` the original repro used is rejected
// since 2026-09-24 ("unexpected type Boolean"); the rule form below is the
// documented one.
import nelumbo.collections

Integer       x, i
List<Integer> l
List<Integer> ::= lst(<Integer>)

lst(i)=l <=> l=[10,11,12,13]     // constant list (arg ignored; nullary functors are rejected)

x pos lst(0) = 1 ? [(x=11)][..]
