// Bug: a fact lookup with a bound argument does not bind a variable nested in another
// argument: owns(Piet,box(n)) is unknown, while E[b](owns(Piet,b) & b=box(n)) finds n=1.
import nelumbo.integers

Person  :: Object
Box     :: Struct
Boolean ::= owns(<Person>,<Box>)
Box     ::= box(<Integer>)
Person  ::= Piet

Integer n
Box b

fact owns(Piet, box(1))

owns(Piet,box(n)) ? [(n=1),..][..]
E[b](owns(Piet,b) & b=box(n)) ? [(n=1),..][..]
