// Bug: a functor with a hole of a user-declared `:: Literal` type (`Boolean ::= near(<Pos>)`)
// is reported as clashing with itself: "Non deterministic pattern merge
// Boolean::=near(<Pos>) <> Boolean::=near(<Pos>)", and the fact `near(here)` is not found
// (`[..][..]`). With `Pos :: Object` there is no error and the fact holds.
import nelumbo.logic

Pos     :: Literal
Pos     ::= here, there
Boolean ::= near(<Pos>)

fact near(here)

near(here)  ? [()][]
near(there) ? [..][..]
