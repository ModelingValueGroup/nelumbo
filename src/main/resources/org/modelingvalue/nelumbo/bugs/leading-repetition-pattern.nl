// Bug: a pattern that starts with a repetition of holes accepts only one element:
// with `<(> <Person> <,> , <)+> are friends`, `Piet, Jan are friends` does not parse
// ("Unexpected token ','"); a Root pattern fails the same way. A pattern that starts
// with a word or a single hole parses any number of elements.
import nelumbo.logic

Person   :: Object
Person   ::= Piet, Jan, Marie
Boolean  ::= <(> <Person> <,> , <)+> are friends #30

Piet are friends             ? [..][..]
Piet, Jan are friends        ? [..][..]
Piet, Jan, Marie are friends ? [..][..]
