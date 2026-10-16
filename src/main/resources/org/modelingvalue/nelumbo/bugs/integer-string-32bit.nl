// Bug: int(...) is limited to 32 bits (all other integer operations are unbounded), so
// int("12345678901") yields no result.
import nelumbo.strings

String a
Integer x

str(12345678901) = a ? [(a="12345678901")][..]
int("12345678901") = x ? [(x=12345678901)][..]
