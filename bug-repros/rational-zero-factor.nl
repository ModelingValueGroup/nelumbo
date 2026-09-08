// Bug: Rationals.mult (Rationals.java:81/85) builds n/0 rationals when a factor is 0.
// Correct: 0.0*a=0.0 is true for any a (unknown/any), 0.0*a=1.0 is falsehood.
// Actual: gcd(0,0) -> ArithmeticException, or a fabricated 1/0 "fact" that crashes on toString.
import nelumbo.rationals

Rational a

0.0 * a = 0.0 ? [..][..]
0.0 * a = 1.0 ? [][..]
