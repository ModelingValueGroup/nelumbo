// Regression test (fixed 2026-10-18; was bugs/diagonal-fact-lookup.nl,
// bugs/quantifier-definitive-from-unknown.nl and bugs/unbound-fact-lookup-complete-empty.nl).
// A predicate with all arguments unbound (q(a), r(a,a), s(a,b)) is not resolved, because a
// variable is never bound by enumerating a type: it is unknown, never definitively false,
// and quantifiers over it stay unknown. A bound argument or = makes the lookup work.
import nelumbo.logic

Person  :: Object
Male    :: Person

Boolean ::= q(<Person>), r(<Person>,<Person>), s(<Person>,<Person>)

Male    ::= X, Y

Person a, b

fact q(X)
fact r(X,X)
fact s(X,Y)

q(X)               ? [()][]
r(X,X)             ? [()][]
s(X,Y)             ? [()][]

q(a)               ? [..][..]
r(a,a)             ? [..][..]
s(a,b)             ? [..][..]
E[a](q(a))         ? [..][..]
E[a](r(a,a))       ? [..][..]
E[a](E[b](s(a,b))) ? [..][..]
!E[a](q(a))        ? [..][..]
A[a](q(a))         ? [..][..]

s(X,b)             ? [(b=Y),..][..]
E[b](s(X,b))       ? [()][]
E[a](a=X & q(a))   ? [()][]
