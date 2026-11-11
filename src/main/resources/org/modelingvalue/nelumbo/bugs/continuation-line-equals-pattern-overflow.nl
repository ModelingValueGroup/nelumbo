// expect-error-per-query: Unexpected token '<'
// Bug: with nelumbo.logic loaded, an unquoted = or != in a functor alternative after a line
// break in the alternation overflows the parser's stack (StackOverflowError). On one line,
// `Boolean ::= x, y, <Object> != <Object> #30`, it is a parse error.
// Correct: that same parse error, "Unexpected token '<'", at the declaration.
import nelumbo.logic

Boolean ::= x,
  y, <Object> != <Object> #30
