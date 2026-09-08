// Bug: RepetitionPattern.args (RepetitionPattern.java:161) passes alt=false, so a
// keyword-only repetition contributes nothing per iteration and the iteration count
// is lost in the semantic value.
// Correct: rep aa != rep aa aa. Actual: they compare equal.
import nelumbo.logic

C :: Object
C ::= rep <(> aa <)+>

rep aa = rep aa aa    ? [][()]
rep aa = rep aa       ? [()][]
