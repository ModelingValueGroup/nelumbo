// Bugs: (1) Predicate.infer (Predicate.java:335) never enumerates unary predicates with
// an unbound argument, so q(a) stays unknown even though q(X) is an indexed fact.
// (2) Fully-bound quantifiers (ExistentialQuantifier.java:90 / UniversalQuantifier.java:70)
// turn that unknown body into a DEFINITIVE answer: E[a](q(a)) false, A[a](q(a)) true.
// Correct: q(X) is a fact, so X witnesses E[a](q(a)) - it must be true.
// Actual: E[a](q(a)) is definitively false AND A[a](q(a)) definitively true.
// The A and ! queries have no expectation (their exact correct completeness is
// debatable) - they are here to show the contradiction.
import nelumbo.logic

Person :: Object
Male   :: Person

FactType ::= q(<Person>)

Male ::= X, Y

fact q(X)

Person a

q(X)        ? [()][]
E[a](q(a))  ? [()][..]
A[a](q(a))  ?
!E[a](q(a)) ?
