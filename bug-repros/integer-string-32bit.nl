// Bug: integer_string (Strings.java:82) parses with Integer.parseInt, so int(...) is
// silently limited to 32-bit although all other integer ops are BigInteger-based.
// Correct: the round trip below succeeds. Actual: int("12345678901") yields falsehood.
import nelumbo.strings

String a
Integer x

str(12345678901) = a ? [(a="12345678901")][..]
int("12345678901") = x ? [(x=12345678901)][..]
