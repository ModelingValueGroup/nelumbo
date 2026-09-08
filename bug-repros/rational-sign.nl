// Bug: Rational.normalize (Rational.java:69) never moves a negative sign from the
// denominator to the numerator, so division by a negative yields e.g. (2,-1) != (-2,1).
// Correct: both queries below pass. Actual: the first reports a mismatch between two
// identically-printing values, the second infers -2 > 0 (via unsigned cross-multiplication).
import nelumbo.rationals

Rational a

20.0 / -10.0 = a ? [(a=-2.0)][..]

E[a] (20.0 / -10.0 = a & a > 0.0) ? [][..]
