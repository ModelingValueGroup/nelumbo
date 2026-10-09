// Bug: `e in f(...)` stays undecided ([..][..]) when the collection argument
// is a call of a user function. It does not matter whether f returns a Set or
// a List, whether e is in it or not, or whether the membership is negated.
// The same membership via a variable (E[s](f(...)=s & e in s)) or on a
// literal decides, and other predicates take the same call as argument fine:
// `ss(0) < {1,2,3}`, `ss(0) = {1,2}`, `|ss(0)| = i`, `ss(0) - {1} = s` and
// `ff(0) in {1,2}` all decide (control queries at the end).
// Correct: the four `in` queries below decide like their variable forms.
// Actual: all four are [..][..].
// Code location: not investigated. Possibly related: `in` is declared twice in
// collections.nl (for Set<E> via elementOf, for List<E> via pos), the other
// predicates above once - unverified.
// Found 2026-10-09 in examples/sudoku-4x4-csp.nl (plan Task 6): the guards
// `if d in cell(g,r,c)` / `if !(d in cell(g,r,c))` of elim are undecided, so
// elim never decides.
import nelumbo.collections

Set<Integer>  ::= ss(<Integer>)
List<Integer> ::= ll(<Integer>)
Integer       ::= ff(<Integer>)
Integer       i, j
Set<Integer>  s
List<Integer> l

ss(i)=s <=> s={1,2}
ll(i)=l <=> l=[1,2]
ff(i)=j <=> j=2

1 in ss(0)    ? [()][]
3 in ss(0)    ? [][()]
!(1 in ss(0)) ? [][()]
1 in ll(0)    ? [()][]

// controls (decide today)
E[s](ss(0)=s & 1 in s) ? [()][]
E[l](ll(0)=l & 1 in l) ? [()][]
ss(0) < {1,2,3}        ? [()][]
ff(0) in {1,2}         ? [()][]
