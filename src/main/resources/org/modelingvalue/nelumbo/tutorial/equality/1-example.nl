import nelumbo.integers
Integer ::= double(<Integer>)
Integer n
double(n) == n*2
double(2)=4 ?
double(2)=5 ?
Point   :: Object
Point   ::= point(<Integer>,<Integer>)
point(1,2)=point(1,2) ?
Pos     :: Literal
Pos     ::= pos(<Integer>,<Integer>)
Pos     q
pos(1,2)=pos(1,2) ?
pos(1,2)=pos(2,1) ?
q=pos(1,2) ?
