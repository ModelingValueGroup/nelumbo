import nelumbo.logic
Person  :: Object
Person  ::= Piet, Jan, Klaas, Kees
Boolean ::= <Person> is a friend of <Person> #30
Person  me, f, ff, Who
ff is a friend of me <=> me is a friend of ff | E[f](ff is a friend of f & f is a friend of me)
fact Jan is a friend of Piet
fact Klaas is a friend of Jan
fact Kees is a friend of Klaas
Piet is a friend of Kees ? [()][]
Piet is a friend of Piet ? [()][]
Who is a friend of Piet  ? [(Who=Jan),(Who=Kees),(Who=Klaas),(Who=Piet),..][..]
