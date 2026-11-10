import nelumbo.lang
Person   :: Object
Person   ::= Piet, Jan, Marie, Klaas
Sentence :: Root
Sentence ::= <Person> invites <(> <Person> <,> , <)+> <(> and <Person> <)?> #0
Piet invites Jan
Piet invites Jan and Marie
Piet invites Jan, Marie and Klaas
