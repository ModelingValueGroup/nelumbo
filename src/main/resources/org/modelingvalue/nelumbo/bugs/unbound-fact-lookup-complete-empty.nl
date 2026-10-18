// Bug: a fact predicate with all arguments unbound (q(a), s(a,b)) answers "complete,
// no facts" although q(X) and s(X,Y) are facts, so E over it is definitively false
// (and !E true) instead of true.
// The last four queries have no expectation (they illustrate).
import nelumbo.logic

Person  :: Object
Male    :: Person

Boolean ::= q(<Person>), s(<Person>,<Person>)

Male    ::= X, Y

Person a, b

fact q(X)
fact s(X,Y)

q(X)               ? [()][]
s(X,Y)             ? [()][]
E[a](q(a))         ? [()][]
E[a](E[b](s(a,b))) ? [()][]
q(a)               ?
s(a,b)             ?
!E[a](q(a))        ?
A[a](q(a))         ?
