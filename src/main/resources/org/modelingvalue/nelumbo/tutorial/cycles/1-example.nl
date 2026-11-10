import nelumbo.logic
Person  :: Object
Person  ::= Piet, Jan, Klaas, Kees, Bob
Boolean ::= <Person> is a friend of <Person> #30
Person  me, f, ff, Who
// a friend of a friend of mine is a friend of mine
ff is a friend of me <=> E[f](ff is a friend of f & f is a friend of me)
fact Jan is a friend of Piet
fact Klaas is a friend of Jan
fact Kees is a friend of Klaas
Who is a friend of Piet ?
Kees is a friend of Piet ?
Bob is a friend of Piet ?
