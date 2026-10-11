// Regression test (fixed 2026-10-11; was bugs/quantifier-definitive-from-unknown.nl).
// A fully bound quantifier must not turn an incomplete body into a definitive answer:
// A is true only when the body's falsehoods are complete and none applies, E is false
// only when the body's facts are complete and none applies.
// (Was: (1) Predicate.infer never enumerated a unary predicate with an unbound argument,
// so q(a) stayed unknown - fixed by looking facts up before that guard; (2)
// ExistentialQuantifier/UniversalQuantifier.resolve turned a body without facts and
// falsehoods into E false / A true, and any body fact into an A fact, without checking
// completeness. Here q(a) is [(a=X)][..]: X is a fact, Y's status is not derived, so A
// stays unknown although it was definitively true.)
import nelumbo.logic

Person :: Object
Male   :: Person

FactType ::= q(<Person>)

Male ::= X, Y

fact q(X)

Person a

q(X)        ? [()][]
q(a)        ? [(a=X)][..]
E[a](q(a))  ? [()][]
!E[a](q(a)) ? [][()]
A[a](q(a))  ? [..][..]
