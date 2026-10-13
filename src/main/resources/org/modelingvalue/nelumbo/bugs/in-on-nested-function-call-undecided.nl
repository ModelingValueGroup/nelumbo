// Bug: `e in f(a)` is undecided when the argument a is itself a call, user
// (`g(0)`) or built-in (`0+1`). Membership in a call with a plain argument, and
// the same call compared with `=`, decide. Follow-up of
// tests/in-on-function-call-undecided.nl (plain-argument case, fixed).
// Correct: 1 in f(g(0)) is true, 3 in f(g(0)) is false, 1 in f(0+1) is true,
// and the same inside a rule body (o2).
// Actual: all UNDECIDED [..][..] (f(g(0))=s itself decides, s={1,2}).
// Code location: not investigated.
// Found 2026-10-13 in examples/sudoku-4x4-csp.nl (plan Task 6): `d in cell(g,r,c)`
// with `cell(g,r,c)=sc <=> sc = catRow(crow(g,r),c)` keeps all three elim probes undecided.
import nelumbo.collections

Set<Integer>  ::= f(<Integer>), inner(<List<Integer>>), o2(<Integer>), o3(<Integer>)
Integer       ::= g(<Integer>)
List<Integer> ::= mk(<Integer>)
Integer       i, j
Set<Integer>  s
List<Integer> l

f(i)=s     <=> s={1,2}
g(i)=j     <=> j=i
inner(l)=s <=> s={1,2}
mk(i)=l    <=> l=[i]
o2(i)=s    <=> s=inner(mk(i))
o3(i)=s    <=> E[l](mk(i)=l & s=inner(l))

1 in f(g(0))  ? [()][]
3 in f(g(0))  ? [][()]
1 in f(0+1)   ? [()][]
// the CSP-shaped case: the nested call sits inside a rule body
1 in o2(0)    ? [()][]
3 in o2(0)    ? [][()]
// controls (decide)
1 in f(0)     ? [()][]
f(g(0))=s     ? [(s={1,2})][..]
o2(0)=s       ? [(s={1,2})][..]
1 in o3(0)    ? [()][]
