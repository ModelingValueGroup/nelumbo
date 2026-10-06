// Regression test (fixed 2026-10-06; was bugs/optional-presence-lost.nl).
// A matched optional must be recorded as PRESENT even when its body extracts no
// argument (a multi-keyword body like `big bang`): wrap big bang != wrap.
// (Was: OptionalPattern.args recorded such a match as Optional.empty, i.e. as
// absent, so `wrap big bang = wrap` was true. Fixed in OptionalPattern.args: a
// matched body without an argument is kept as a non-empty Optional now.)
// C is declared `:: Struct` - the supertype for plain structural equality, as
// in tests/repetition-separator-greedy.nl and logicTest.nl "Structs".
import nelumbo.logic

C :: Struct
C ::= wrap <(> big bang <)?>

wrap big bang = wrap          ? [][()]
wrap big bang = wrap big bang ? [()][]
