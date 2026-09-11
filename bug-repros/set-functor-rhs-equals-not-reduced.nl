// Bug: a collection-returning functor CALL on the RHS of `=` in a rule body is
// NOT evaluated - the result carries the unreduced term (mk(0)) plus a leaking
// anonymous binding. The SAME call on the LHS (`mk(i)=l`) evaluates correctly,
// and an Integer-returning functor works on EITHER side, so `=` is inconsistently
// non-commutative for collection-returning functors. A `map` expression on the
// RHS of `=` is left unreduced the same way.
//
//   pick(i)=l <=> l = mk(i)   -> l = mk(0)   (WRONG)
//   pick(i)=l <=> mk(i) = l   -> l = [1,2]   (correct)
//
// Found 2026-09-11 while writing examples/sudoku-4x4-csp.nl (Norvig CSP solver).
//
// Correct: l = [1,2]. Actual: l = mk(0).
import nelumbo.collections

Integer       i
List<Integer> l

List<Integer> ::= mk(<Integer>), pick(<Integer>)

mk(i)=[1,2]
pick(i)=l <=> l = mk(i)     // collection-returning functor on RHS -> not reduced

pick(0)=l ? [(l=[1,2])][..]
