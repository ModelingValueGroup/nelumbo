// Regression test (fixed 2026-11-22; was bugs/type-variable-name-prefix-split.nl).
// In a hole of a type parameter, a constant whose name starts with that parameter's name is
// one constant: with `Type T` and the hole `<T>`, `box Tom` is not split into the variable
// declaration `box [T om]`.
import nelumbo.logic

Animal  :: Object
Animal  ::= Rex, Tom
Type T
Box<T>  :: Object
Box<T>  ::= box <T>
Boolean ::= <Box<Animal>> is full #30

box Rex is full ? [..][..]
box Tom is full ? [..][..]
