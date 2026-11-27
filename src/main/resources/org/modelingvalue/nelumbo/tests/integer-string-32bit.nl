// Regression test (fixed 2026-11-26; was bugs/integer-string-32bit.nl).
// int(...) must accept arbitrary-precision integers, not only Java's signed 32-bit range.
import nelumbo.strings

String a
Integer x

str(12345678901) = a ? [(a="12345678901")][..]
int("12345678901") = x ? [(x=12345678901)][..]
int("2147483647") = x ? [(x=2147483647)][..]
int("2147483648") = x ? [(x=2147483648)][..]
int("000012345678901") = x ? [(x=12345678901)][..]
int("12345678901    ") = x ? [][..]
int("NaN") = x ? [][..]
