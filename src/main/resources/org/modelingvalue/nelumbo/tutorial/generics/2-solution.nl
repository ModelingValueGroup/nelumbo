import nelumbo.lang
Person   :: Object
Person   ::= Piet, Jan, Marie
Animal   :: Object
Animal   ::= Rex, Tom, Bello
Type E
Team<E>  :: Object
Team<E>  ::= team of <(> <E> <,> , <)+>
Sentence :: Root
Sentence ::= <Team<E>> plays against <Team<E>> #0
team of Piet, Jan plays against team of Marie
team of Rex plays against team of Tom, Bello
