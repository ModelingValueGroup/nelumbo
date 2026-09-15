// Bug: the STRING regex lets an unclosed quote span lines; TokenizerResult.checkToken
// (Tokenizer.java:153) then indexes past the line end. Run with java -ea:
// StringIndexOutOfBoundsException instead of a clean parse error on the unclosed quote.
// This is a common transient state while typing in the editor/LSP.
import nelumbo.strings

String a

a = "abc
a = "x" ?
