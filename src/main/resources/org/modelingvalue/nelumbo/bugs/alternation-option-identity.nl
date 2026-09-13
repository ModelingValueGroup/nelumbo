// Bug: SequencePattern.args (SequencePattern.java:162) drops the alt flag, so
// multi-keyword alternation options are stored as a null arg and lose their identity.
// Correct: different options compare unequal. Actual: wrap aa bb = wrap dd ee is true.
import nelumbo.logic

C :: Object
C ::= wrap <(> aa bb <|> dd ee <|> cc <)>

wrap aa bb = wrap aa bb ? [()][]
wrap cc    = wrap cc    ? [()][]
wrap aa bb = wrap dd ee ? [][()]
wrap cc    = wrap dd ee ? [][()]
