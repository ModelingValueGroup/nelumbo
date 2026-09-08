// Bug: OptionalPattern.args (OptionalPattern.java:118) records a matched optional whose
// body extracted no argument (multi-keyword body) as Optional.empty, i.e. as absent.
// Correct: wrap big bang != wrap. Actual: they compare equal.
import nelumbo.logic

C :: Object
C ::= wrap <(> big bang <)?>

wrap big bang = wrap          ? [][()]
wrap big bang = wrap big bang ? [()][]
