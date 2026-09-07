// Bug: string_concat (Strings.java:55) returns s.substring(0, a2.length()) where the
// prefix is s.substring(0, s.length() - a2.length()). Only symmetric splits are correct,
// which is exactly what the existing stringsTest.nl case covers, masking the bug.
// Correct: a="ba" resp. a="fo". Actual: a="b" resp. a="foob".
import nelumbo.strings

String a

a+"r"="bar" ? [(a="ba")][..]
a+"obar"="foobar" ? [(a="fo")][..]
