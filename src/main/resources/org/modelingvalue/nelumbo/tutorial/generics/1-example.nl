import nelumbo.lang
Person   :: Object
Person   ::= Piet, Jan, Marie
Animal   :: Object
Animal   ::= Rex, Tom, Bello
Type E
Team<E>  :: Object
Team<E>  ::= team of <(> <E> <,> , <)+>
Sentence :: Root
Sentence ::= <Team<Person>> plays against <Team<Person>> #0,
             <Team<Animal>> chases <Team<Animal>>        #0
team of Piet, Jan plays against team of Marie
team of Rex chases team of Tom, Bello
