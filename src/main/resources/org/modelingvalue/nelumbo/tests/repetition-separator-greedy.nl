// Regression test (fixed 2026-10-06; was bugs/repetition-separator-greedy.nl).
// A repetition's separator that is NOT followed by another repeated element
// belongs to the enclosing pattern: in seq(1, 2, end) the last `,` is the
// literal comma before `end`, not a separator. (Was: RepetitionPattern.args
// consumed the separator unconditionally without backtracking; when the token
// after it did not match the repeated pattern, extraction failed and
// Functor.args threw IllegalArgumentException on VALID input. The backtracking
// itself (`i = beforeSeparator` in RepetitionPattern.args) is in the engine
// since eeb06747 (2026-09-08); the repro stayed red for another reason: Seq<T>
// was declared `:: Object`, and since bba88fc8 (2026-09-09) every Object
// functor with arguments - generic or not - is promoted to Function semantics,
// where `=` between two terms is undecided ([..][..]) without a defining `<=>`
// rule. Declaring the type `:: Struct` gives it plain structural equality (see
// langTest.nl "8. Generic type parameter" and logicTest.nl "Structs"), and
// with that both queries evaluate.)
import nelumbo.integers

Type T
Seq<T> :: Struct
Seq<T> ::= seq(<(> <T> <,> , <)+> , end)

seq(1, 2, end) = seq(1, 2, end) ? [()][]
seq(1, end) = seq(1, 2, end)    ? [][()]
