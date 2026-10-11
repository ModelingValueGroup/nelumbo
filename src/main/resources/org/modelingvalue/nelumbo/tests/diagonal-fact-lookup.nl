// Regression test (fixed 2026-10-11; was bugs/diagonal-fact-lookup.nl).
// The diagonal shape r(a,a) must find the fact r(X,X), so E[a](r(a,a)) is true.
// (Was: KnowledgeBase.addFact indexed only one open argument at a time, so r(a,a)
// had no index entry and getFacts answered "complete, no facts". Fixed: lookups
// with two or more open positions filter the per-functor fact set, and a repeated
// variable requires equal arguments.)
// The last query has no expectation on purpose (it illustrates; its exact
// completeness is debatable).
// E[a](r(a,a)) is decided true with no falsehoods, hence [()][] (the repro expected [()][..]).
import nelumbo.logic

Person   :: Object

FactType ::= r(<Person>,<Person>)

Person   ::= X, Y

Person a, b

fact r(X,X)

r(X,X)             ? [()][]
E[a](r(a,a))       ? [()][]
E[a](E[b](r(a,b))) ?
