// Bug: an intersection hole whose member types include a type declared `:: Literal` together
// with `Literal` itself (`<{Point,Literal}>` with `Point :: Literal`) rejects every value of
// that type: "Node p(1,2) of unexpected type Point, expected {Literal,Point}". A `Point` is a
// `Literal`, so the hole should accept it as `<Point>` does. With `Thing :: Object`,
// `<{Thing,Literal}>` accepts a constant `Thing`.
import nelumbo.logic
import nelumbo.integers

Point   :: Literal
Point   ::= p(<Integer>,<Integer>)
Boolean ::= at <Integer> lies <{Point,Literal}> #30

fact at 1 lies p(1,2)

at 1 lies p(1,2) ? [()][]
at 1 lies p(2,1) ? [..][..]
