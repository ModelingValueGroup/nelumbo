// Bug: RepetitionPattern.args (RepetitionPattern.java:171) consumes the separator
// unconditionally without backtracking; if the token after it does not match the
// repeated pattern, extraction fails and Functor.args throws on VALID input.
// Correct: both queries evaluate. Actual: IllegalArgumentException crash.
// C is declared generic (`C<T>`) rather than over a bare `<Integer>` so this
// functor keeps plain structural equality (see langTest.nl "6. Generic type
// parameter") instead of being auto-promoted to Function semantics, which
// would need its own `<=>` rule (like fib(n)=f) to resolve `=` at all.
import nelumbo.integers

Type T
Seq<T> :: Object
Seq<T> ::= seq(<(> <T> <,> , <)+> , end)

seq(1, 2, end) = seq(1, 2, end) ? [()][]
seq(1, end) = seq(1, 2, end)    ? [][()]
