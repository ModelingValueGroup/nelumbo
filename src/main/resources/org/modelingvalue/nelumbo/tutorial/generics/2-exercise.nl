import nelumbo.lang
Person   :: Object
Person   ::= Piet, Jan, Marie
Animal   :: Object
Animal   ::= Rex, Tom, Bello
// make Team generic, so that animals can form teams too
Team     :: Object
Team     ::= team of <(> <Person> <,> , <)+>
Sentence :: Root
Sentence ::= <Team> plays against <Team> #0
team of Piet, Jan plays against team of Marie
team of Rex plays against team of Tom, Bello
