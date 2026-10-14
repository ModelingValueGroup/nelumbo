// Bug: rational multiplication with a 0 factor builds n/0 rationals and crashes the
// whole evaluation (ArithmeticException). Correct: 0.0*a=0.0 holds for any a, 0.0*a=1.0 never.
import nelumbo.rationals

Rational a

0.0 * a = 0.0 ? [..][..]
0.0 * a = 1.0 ? [][..]
