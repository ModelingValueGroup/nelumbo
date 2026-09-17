// Regression test (fixed 2026-09-11; was bug-repros/string-concat-prefix.nl).
// Solving a+suffix=whole must give the correct prefix for asymmetric splits.
// (Was: string_concat used s.substring(0, a2.length()) instead of
// s.substring(0, s.length()-a2.length()); only symmetric splits were correct.)
import nelumbo.strings

String a

a+"r"="bar" ? [(a="ba")][..]
a+"obar"="foobar" ? [(a="fo")][..]
