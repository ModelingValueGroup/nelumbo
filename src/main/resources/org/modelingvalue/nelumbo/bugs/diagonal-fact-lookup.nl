// Bug: the lookup of the diagonal shape r(a,a) answers "complete, no facts" although
// r(X,X) is a fact, so E[a](r(a,a)) is definitively false instead of true.
// The last query has no expectation (it illustrates).
import nelumbo.logic

Person   :: Object

Boolean ::= r(<Person>,<Person>)

Person   ::= X, Y

Person a, b

fact r(X,X)

r(X,X)             ? [()][]
E[a](r(a,a))       ? [()][]
E[a](E[b](r(a,b))) ?
