import     nelumbo.collections

Person  :: Object

Boolean ::= friends(<Person>,<Person>)

Person      a, b, c
Set<Person> s

friends(a,c) <=>  friends(a,c) | friends(c,a) |
                  E[b](friends(a,b) & friends(b,c))

Person       ::= Piet, Jan, Klaas, Kees

fact friends(Piet, Jan)
fact friends(Jan, Klaas)
fact friends(Jan, Kees)

friends(Piet, a)    ? [(a=Jan),(a=Kees),(a=Klaas),(a=Piet),..][..]
friends(Jan, a)     ? [(a=Jan),(a=Kees),(a=Klaas),(a=Piet),..][..]
friends(Klaas, Jan) ? [()][]
