// Regression test (fixed 2026-11-11; was bugs/literal-binding-expectation-unchecked.nl).
// An expected result that binds a variable of a user type is checked: both expectations
// below are WRONG on purpose, and each query must report "Expected result". (Was: with
// `:: Literal` types such a binding was ignored, here = p ? [(p=there)][..] passed, or
// it did not parse: "Unexpected token '='".)
import nelumbo.integers

Pos          :: Object
Pos          ::= here, there
Range        :: Object
struct Range ::= range(<Integer>, <Integer>)

Pos   p
Range r

here = p        ? [(p=there)][..]
range(1, 5) = r ? [(r=range(1,6))][..]
