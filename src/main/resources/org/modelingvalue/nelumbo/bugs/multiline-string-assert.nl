// Bug: an unclosed string quote makes the tokenizer index past the line end:
// StringIndexOutOfBoundsException (with java -ea) instead of a parse error.
import nelumbo.strings

String a

a = "abc
a = "x" ?
