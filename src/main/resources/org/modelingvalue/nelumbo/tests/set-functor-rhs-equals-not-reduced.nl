// Regression test (fixed 2026-09-24; was bugs/set-functor-rhs-equals-not-reduced.nl).
// A collection-returning functor CALL on the RHS of `=` in a rule body must be
// evaluated, exactly like on the LHS: pick(i)=l <=> l = mk(i) gives l=[1,2].
// (Was: the result carried the unreduced term mk(0) plus a leaking anonymous
// binding, while `mk(i) = l` on the LHS worked - `=` was non-commutative for
// collection-returning functors. Same reduction/typing family as
// recursive-list-concat-classcast.)
// The bare fact form `mk(i)=[1,2]` the original repro used is rejected since
// 2026-09-24 ("unexpected type Boolean"); the rule form below is the
// documented one.
import nelumbo.collections

Integer       i
List<Integer> l

List<Integer> ::= mk(<Integer>), pick(<Integer>)

mk(i)=l   <=> l=[1,2]
pick(i)=l <=> l = mk(i)     // collection-returning functor on RHS

pick(0)=l ? [(l=[1,2])][..]
