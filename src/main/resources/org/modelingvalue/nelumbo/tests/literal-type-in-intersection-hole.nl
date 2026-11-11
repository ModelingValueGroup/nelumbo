// Regression test (fixed 2026-11-11; was bugs/literal-type-in-intersection-hole.nl).
// An intersection hole of a type and `Literal` (`<{Point,Literal}>`) accepts a literal of
// that type, here a struct application `p(1,2)`. (Was: with `Point :: Literal` the hole
// rejected every Point: "Node p(1,2) of unexpected type Point, expected {Literal,Point}".)
import nelumbo.logic
import nelumbo.integers

Point        :: Object
struct Point ::= p(<Integer>,<Integer>)
Boolean      ::= at <Integer> lies <{Point,Literal}> #30

fact at 1 lies p(1,2)

at 1 lies p(1,2) ? [()][]
at 1 lies p(2,1) ? [..][..]
