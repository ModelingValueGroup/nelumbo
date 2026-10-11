// Regression test (fixed 2026-10-11).
// A recursive rule whose evaluation runs back into a predicate that is still on the
// stack must close the cycle: a fully bound query that nothing derives is false.
// (Was: two defects in closing a cycle. CompoundPredicate.resolve dropped the cycle
// tag of an undecided reduce-pass result when it re-evaluated the conjunct deep, so an
// intermediate unknown was memoized as final; and Predicate.fixpoint closed a stable
// cycle as "complete, no facts, no falsehoods", which a fully bound predicate does not
// read as false. A cyclic graph showed it on develop; enumerating facts with two open
// arguments made acyclic graphs hit it as well, see multi-unbound-fact-lookup.nl.)
// Two graphs with their own functors, so the queries do not share memoized results.
import nelumbo.logic

City     :: Object

FactType ::= track(<City>,<City>),
             loop(<City>,<City>)

Boolean  ::= <City> reaches <City> #30,
             <City> loops <City> #30

City     ::= A1, U, M
City     x, y, z

// acyclic: A1 -> U -> M
x reaches y <=> track(x,y) | E[z](track(x,z) & z reaches y)

fact track(A1,U), track(U,M)

A1 reaches M ? [()][]
M reaches A1 ? [][()]
U reaches A1 ? [][()]

// cyclic: A1 -> U -> A1
x loops y <=> loop(x,y) | E[z](loop(x,z) & z loops y)

fact loop(A1,U), loop(U,A1)

A1 loops U ? [()][]
A1 loops A1 ? [()][]
A1 loops M ? [][()]
U loops M  ? [][()]
