// Regression test (fixed 2026-10-06; was bugs/alternation-option-identity.nl).
// Multi-keyword alternation options must keep their identity: different
// options compare unequal. (Was: SequencePattern.args dropped the alt/keep
// flag for its elements, so a multi-keyword option like `aa bb` or `dd ee` was
// stored as a null arg and `wrap aa bb = wrap dd ee` was true; the
// single-keyword option `cc` was unaffected. Fixed in the patterns' args: the
// keep flag is passed through SequencePattern.args to its elements now, so the
// keyword texts of the matched option are kept as its identity.)
// C is declared `:: Struct` - the supertype for plain structural equality, as
// in tests/optional-presence-lost.nl and logicTest.nl "Structs".
import nelumbo.logic

C :: Struct
C ::= wrap <(> aa bb <|> dd ee <|> cc <)>

wrap aa bb = wrap aa bb ? [()][]
wrap cc    = wrap cc    ? [()][]
wrap aa bb = wrap dd ee ? [][()]
wrap cc    = wrap dd ee ? [][()]
