// Bug: RepetitionPattern.args (RepetitionPattern.java:171) consumes the separator
// unconditionally without backtracking; if the token after it does not match the
// repeated pattern, extraction fails and Functor.args throws on VALID input.
// Correct: both queries evaluate. Actual: IllegalArgumentException crash.
import nelumbo.integers

C :: Object
C ::= seq(<(> <Integer> <,> , <)+> , end)

seq(1, 2, end) = seq(1, 2, end) ? [()][]
seq(1, end) = seq(1, 2, end)    ? [][()]
