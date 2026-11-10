// Regression test (fixed 2026-11-10; was bugs/literal-type-hole-pattern-merge.nl).
// A functor whose first hole has a user-declared `:: Literal` type (`Boolean ::= near(<Pos>)`)
// was reported as clashing with itself: "Non deterministic pattern merge
// Boolean::=near(<Pos>) <> Boolean::=near(<Pos>)", and the fact `near(here)` was not found.
import nelumbo.logic

Pos     :: Literal
Pos     ::= here, there
Boolean ::= near(<Pos>)

fact near(here)

near(here)  ? [()][]
near(there) ? [..][..]
