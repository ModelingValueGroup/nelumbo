// Regression test (fixed 2026-11-26; was bugs/comment-after-operator.nl).
// A // starts a comment also directly after an operator; a single / remains an operator.
import nelumbo.integers
import nelumbo.strings

1 + 1 = 2 ?// no space before this comment: the query must still parse
1 + 1 = 2 ? // with a space it works fine
6 / 3 = 2 ? [()][]
1 +// this comment ends the operator and the rest is evaluated on the next line
 1 = 2 ? [()][]
String s
s = "https://example.org/a//b" ? [(s="https://example.org/a//b")][..]
