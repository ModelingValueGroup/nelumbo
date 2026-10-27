// Regression test (fixed 2026-10-26; was bugs/integers-div-zero.nl,
// bugs/collections-index-out-of-range.nl and bugs/datetime-multiply-overflow.nl).
// An exception in a built-in operation (division by zero, an index outside the list,
// a period too long for its day count) leaves that query unknown, so it is reported as
// an error with the exception's message and the query has no result. It no longer
// crashes the whole evaluation: the other queries still evaluate.
// RegressionTest.nativeExceptionWithoutResult checks the errors.
import nelumbo.integers, nelumbo.collections, nelumbo.datetime

Integer x, i
Period  y

6 / 0 = x            ?
6 / 2 = x            ? [(x=3)][..]

0 * x = 6            ?
2 * x = 6            ? [(x=3)][..]

at 7 in [1,2,3] = i  ?
at 2 in [1,2,3] = i  ? [(i=3)][..]

P1D * 9999999999 = y ?
P1D * 3 = y          ? [(y=P3D)][..]
