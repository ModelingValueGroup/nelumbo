// Bug: dividing by a negative rational leaves the sign in the denominator: the result
// prints as -2 but does not equal -2.0, and a > 0.0 holds for it.
import nelumbo.rationals

Rational a

20.0 / -10.0 = a ? [(a=-2.0)][..]

E[a] (20.0 / -10.0 = a & a > 0.0) ? [][..]
