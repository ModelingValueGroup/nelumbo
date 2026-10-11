// Regression test (fixed 2026-10-11; was bugs/exists-false-from-one-false-witness.nl).
// An existential is false only when EVERY candidate is false: one false candidate of a
// body whose facts are incomplete must leave E unknown.
// (Was: ExistentialQuantifier.resolve turned every falsehood of the body into a
// falsehood of the quantifier once the local variables were removed from its binding,
// without checking that the body's facts were complete, so E[n](n>3) - body
// [..][(n=3),..] - was definitively false and !E[n](n>3) definitively true. Fixed: E
// concludes false only when the body's facts are complete.)
// salary(Piet,m,s) & m>1 is complete since the multi-open fact lookup
// (multi-unbound-fact-lookup.nl), so its E is decided true; n>3 keeps the original
// shape of an incomplete body with one known false candidate.
// Found 2026-10-09 while writing examples/taxYear.nl, whose first draft guarded with
// !E[c2,s2](salary(p,c2,s2) & c<c2 & c2<=d) - exactly this shape.
import nelumbo.integers

Person   :: Object

FactType ::= salary(<Person>,<Integer>,<Integer>)

Person   ::= Piet

Integer m, s, n

fact salary(Piet, 1, 3000),
     salary(Piet, 7, 4000)

E[m,s](salary(Piet,m,s) & m>1)  ? [()][]
!E[m,s](salary(Piet,m,s) & m>1) ? [][()]
E[m,s](salary(Piet,m,s) & m>9)  ? [][()]

// incomplete body, one known false candidate (n=3): undecided, not false
n>3        ? [..][(n=3),..]
E[n](n>3)  ? [..][..]
!E[n](n>3) ? [..][..]
