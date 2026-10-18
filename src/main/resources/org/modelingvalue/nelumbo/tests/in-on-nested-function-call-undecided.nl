// Regression test (fixed 2026-10-16; was bugs/in-on-nested-function-call-undecided.nl).
// `e in f(a)` must decide when the argument a is itself a call, user (`g(0)`) or
// built-in (`0+1`), also inside a rule body (o2). (Was: UNDECIDED [..][..], while
// `1 in f(0)` and `f(g(0))=s` decided.)
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
