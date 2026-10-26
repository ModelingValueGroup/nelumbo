// Bug: when a conjunction is already decided false by its other operand, a call with an
// unbound result in it (2+2=a) gets only a shallow evaluation, whose unknown result is
// remembered: the same call queried later is unknown ([..][..]) instead of its value.
// Also hides an exception: `6/0=a` alone reports "BigInteger divide by zero", after
// `1>2 & 6/0=a` it is silently unknown. Fully bound calls (3*4=12) are not affected.
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
