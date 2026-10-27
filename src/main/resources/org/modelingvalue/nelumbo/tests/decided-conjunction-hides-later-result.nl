// Regression test (fixed 2026-10-27; was bugs/decided-conjunction-hides-later-result.nl).
// A conjunction already decided false by its other operand evaluates a call with an
// unbound result (2+2=a) only shallowly. That shallow unknown is not remembered: the
// same call queried later still gets its value, and an exception in it (6/0=a) is
// still reported. RegressionTest.decidedConjunctionHidesLaterResult checks that the
// divide by zero is the only error.
import nelumbo.integers

Integer ::= dbl(<Integer>)
Integer a, n, d

dbl(n)=d <=> d=n+n

1 > 2 & 2 + 2 = a  ? [][()]
2 + 2 = a          ? [(a=4)][..]

1 > 2 & dbl(5) = a ? [][()]
dbl(5) = a         ? [(a=10)][..]

1 > 2 & 3 * 4 = 12 ? [][()]
3 * 4 = 12         ? [()][]

1 > 2 & 6 / 0 = a  ? [][()]
6 / 0 = a          ?
