import nelumbo.lang
Person   :: Object
Person   ::= Piet, Jan, Marie, Klaas
pattern GUESTS ::= <(> <Person> <,> , <)+> <(> and <Person> <)?>
Sentence :: Root
Sentence ::= <Person> invites <GUESTS> #0,
             party with <GUESTS>       #0
Piet invites Jan, Marie and Klaas
party with Jan and Marie
