
import     nelumbo.logic

<Person>   :: <Object>

<Relation> ::= friends(<Person>,<Person>)
<Person>   ::= friend(<Person>)

<Person> A, B, C

friend(A)=C <=> friends(A,C) |
                friends(C,A)  |
                friend(friend(A))=C

<Person>   ::= Piet, Jan, Klaas, Kees, Bart

friends(Piet, Jan)
friends(Jan,  Klaas)

friend(Piet)=B    ? [(B=Jan),(B=Klaas),(B=Piet)][..]
friend(Jan)=B     ? [(B=Jan),(B=Klaas),(B=Piet)][..]
friend(Klaas)=Jan ? [()][]
friend(Kees)=Kees ? [][()]

