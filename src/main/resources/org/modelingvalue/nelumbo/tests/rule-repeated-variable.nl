// Regression test (fixed 2026-10-11).
// A query that repeats a variable, like rel(p,p), may only get answers where both
// arguments are equal, also when it is answered through a rule.
// (Was: Rule.biimply binds only values from the query into the rule head, so rel(p,p)
// evaluates the body as r(p,q); with two open arguments r(p,q) enumerates r(X,Y) too,
// which cannot be bound back to rel(p,p) - a NullPointerException while printing the
// result. Fixed: biimply drops condition results that do not match the query.)
import nelumbo.logic

Person   :: Object
FactType ::= r(<Person>,<Person>)
Boolean  ::= rel(<Person>,<Person>)
Person   ::= X, Y
Person   p, q

rel(p,q) <=> r(p,q)

fact r(X,Y), r(X,X)

rel(p,p)       ? [(p=X)][..]
E[p](rel(p,p)) ? [()][]
rel(Y,Y)       ? [][()]
