
<Person>   :: <Node>

<Relation> ::= friends(<Person>,<Person>)
<Person>   ::= friend(<Person>)

<Person> a, b, c

friend(a)=c <=> friends(a,c) |
                friends(c,a)  |
                friend(friend(a))=c

<Person>   ::= Piet, Jan, Klaas, Kees, Bart

friends(Piet, Jan)
friends(Jan,  Klaas)

friend(Piet)=b    ? [(b=Jan),(b=Klaas),(b=Piet)][..]
friend(Jan)=b     ? [(b=Jan),(b=Klaas),(b=Piet)][..]
friend(Klaas)=Jan ? [()][]
friend(Kees)=Kees ? [][()]

