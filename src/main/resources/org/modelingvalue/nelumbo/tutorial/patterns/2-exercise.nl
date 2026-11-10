import nelumbo.lang
Person   :: Object
Person   ::= Piet, Jan, Marie, Klaas
Sentence :: Root
// change the pattern so all three sentences parse
Sentence ::= <Person> invites <Person> #0
Piet invites Jan
Piet invites Jan and Marie
Piet invites Jan, Marie and Klaas
