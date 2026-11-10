import nelumbo.lang
Person   :: Object
Person   ::= Piet, Jan
Sentence :: Root
Sentence ::= <Person> <(> knows <|> does not know <)> <Person> #0
Jan knows Piet
Jan does not know Piet
