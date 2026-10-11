// Bug: ExistentialQuantifier.resolve (ExistentialQuantifier.java:70-79) turns every
// falsehood of the body into a falsehood of the quantifier once the local variables are
// removed from its binding. One false candidate (m=1) then makes E[m,s](...) DEFINITIVELY
// false, although E is only false when every candidate is false.
// Here the body salary(Piet,m,s) & m>1 is honestly [..][(m=1),..] (incomplete because of
// multi-unbound-fact-lookup-unknown.nl), and m=7 is a real witness.
// Not the same code path as quantifier-definitive-from-unknown.nl (line 89, a body with
// no facts AND no falsehoods): with "& true" instead of "& m>1" the body has no
// falsehoods and E correctly stays unknown.
// Correct: true (m=7, s=4000 is a witness).
// Actual: [][()] - and therefore !E[...] is definitively true.
// Found 2026-10-09 while writing examples/taxYear.nl, whose first draft guarded with
// !E[c2,s2](salary(p,c2,s2) & c<c2 & c2<=d) - exactly this shape.
import nelumbo.integers

Person   :: Object

FactType ::= salary(<Person>,<Integer>,<Integer>)

Person   ::= Piet

Integer m, s

fact salary(Piet, 1, 3000),
     salary(Piet, 7, 4000)

salary(Piet,m,s) & m>1          ?
E[m,s](salary(Piet,m,s) & true) ?
E[m,s](salary(Piet,m,s) & m>1)  ? [()][..]
!E[m,s](salary(Piet,m,s) & m>1) ? [][()]
