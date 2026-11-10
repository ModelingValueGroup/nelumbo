import nelumbo.lang
Person   :: Object
Male     :: Person
Female   :: Person
Male     ::= Piet, Jan
Female   ::= Marie
Person   ::= father(<Person>)
Sentence :: Root
Sentence ::= <Person> knows <Person> #0
Piet knows Marie
father(Marie) knows Jan
