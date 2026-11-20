// Regression test (fixed 2026-11-19; was bugs/generic-struct-nested-equality-binding.nl).
// Equality between generic struct terms nested two levels deep must bind the
// variable: box(box(c))=box(box(red)) gives c=red. (Was: the literal functor's
// pattern types were converted with the shallow Type.toLiteral, so the outer
// box's argument kept Box<Color> on one side and Box<{Color,Literal}> on the
// other; the functors compared unequal and Equal returned no binding. Fixed
// by converting the pattern types of struct functors with Type.toLiteralDeep.)
import nelumbo.logic

Color :: Object
Color ::= red, green, blue
Type T
Box<T> :: Object
struct Box<T> ::= box(<T>)
Crate<T> :: Object
struct Crate<T> ::= crate(<T>)

Color c
Box<Color> bc

box(c)=box(red)               ? [(c=red)][..]
box(box(c))=box(box(red))     ? [(c=red)][..]
box(bc)=box(box(red))         ? [(bc=box(red))][..]
crate(box(c))=crate(box(red)) ? [(c=red)][..]
box(crate(c))=box(crate(red)) ? [(c=red)][..]
