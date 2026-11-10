import nelumbo.logic
Person   :: Object
Boolean ::= friends(<Person>,<Person>)
Person   ::= Piet, Jan, Klaas
Person a, b, c, Who
friends(a,c) <=> friends(a,c) | friends(c,a) | E[b](friends(a,b) & friends(b,c))
Boolean ::= <Person> knows <Person> #30
Person X, Y
X knows Y <=> friends(Y,X)
fact friends(Piet, Jan), friends(Jan, Klaas)
Who knows Piet ? [(Who=Jan),(Who=Klaas),(Who=Piet),..][..]
