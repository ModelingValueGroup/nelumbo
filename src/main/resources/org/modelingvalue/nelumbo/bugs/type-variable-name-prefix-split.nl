// Bug: in a hole of a type parameter, a constant whose name starts with that parameter's
// name is split into a variable declaration: with `Type T` and the hole `<T>`, `box Tom`
// parses as `box [T om]` ("Node box[{T,Variable}::=om] of unexpected type ..."). `Tim` fails the
// same way, `Rex` parses; with `Type E` instead, `Tom` parses.
import nelumbo.logic

Animal  :: Object
Animal  ::= Rex, Tom
Type T
Box<T>  :: Object
Box<T>  ::= box <T>
Boolean ::= <Box<Animal>> is full #30

box Rex is full ? [..][..]
box Tom is full ? [..][..]
