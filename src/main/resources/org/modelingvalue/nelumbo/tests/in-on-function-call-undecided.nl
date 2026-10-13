// Regression test (fixed 2026-10-13; was bugs/in-on-function-call-undecided.nl).
// `e in f(...)` must decide when the collection argument is a call of a user
// function, whether f returns a Set or a List, whether e is in it or not, and
// whether the membership is negated - just like the variable form
// E[s](f(...)=s & e in s). (Was: all four `in` queries UNDECIDED [..][..],
// while the variable form, a literal collection and other predicates on the
// same call - `ss(0) < {1,2,3}`, `ff(0) in {1,2}` - decided; those are kept as
// controls at the end.) Found 2026-10-09 in examples/sudoku-4x4-csp.nl (plan
// Task 6): the guards `if d in cell(g,r,c)` / `if !(d in cell(g,r,c))` of elim
// were undecided, so elim never decided.
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

// controls (decided before the fix too)
E[s](ss(0)=s & 1 in s) ? [()][]
E[l](ll(0)=l & 1 in l) ? [()][]
ss(0) < {1,2,3}        ? [()][]
ff(0) in {1,2}         ? [()][]
