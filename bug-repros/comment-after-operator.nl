// Bug: TokenType.OPERATOR (TokenType.java:37) only guards '//' at the FIRST character,
// so a comment directly after an operator char is swallowed into the operator token.
// Correct: both lines below are the same query plus a comment.
// Actual: the first line tokenizes '?//' as one operator and fails to parse.
import nelumbo.integers

1 + 1 = 2 ?// no space before this comment: parse error while the bug exists
1 + 1 = 2 ? // with a space it works fine
