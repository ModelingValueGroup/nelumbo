import nelumbo.logic
Person  :: Object
Person  ::= Piet, Jan, Klaas, Kees, Bob
Boolean ::= friends(<Person>,<Person>), quarrels(<Person>,<Person>), invites(<Person>,<Person>)
Person  p, q
fact friends(Piet, Jan), friends(Piet, Klaas)
fact quarrels(Piet, Kees), quarrels(Piet, Bob)
invites(p,q) <=> friends(p,q)
invites(p,q) <=> false if quarrels(p,q)
invites(Piet,q) ? [(q=Jan),(q=Klaas),..][(q=Bob),(q=Kees),..]
