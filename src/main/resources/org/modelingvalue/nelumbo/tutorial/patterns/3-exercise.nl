import nelumbo.lang
Person   :: Object
Person   ::= Piet, Jan
Sentence :: Root
// change the pattern so both sentences parse
Sentence ::= <Person> knows <Person> #0
Jan knows Piet
Jan does not know Piet
