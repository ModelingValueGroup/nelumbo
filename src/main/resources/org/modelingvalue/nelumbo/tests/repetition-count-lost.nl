// Regression test (fixed 2026-09-11; was bug-repros/repetition-count-lost.nl).
// A keyword-only repetition must contribute per iteration so the iteration count
// is kept in the semantic value: `rep aa` != `rep aa aa`.
// (Was: RepetitionPattern.args passed alt=false, losing the count - the two
// compared equal.)
import nelumbo.logic

C :: Object
C ::= rep <(> aa <)+>

rep aa = rep aa aa    ? [][()]
rep aa = rep aa       ? [()][]
