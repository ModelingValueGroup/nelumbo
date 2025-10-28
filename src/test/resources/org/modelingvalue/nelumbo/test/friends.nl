
<Person>   :: <Node>

<Relation> ::= friends(<Person>,<Person>)
<Person>   ::= friend(<Person>)

<Person> a, b, c

friend(a)=c <=> friends(a,c) |
                friends(c,a) |
                friend(friend(a))=c

<Person>   ::= Piet, Jan, Klaas, Wim, Tom

friends(Piet, Jan)
friends(Jan,  Klaas)

friend(Piet)=b ? [friend(Piet)=Jan,friend(Piet)=Klaas,friend(Piet)=Piet][..]
friend(Jan)=b  ? [friend(Jan)=Jan,friend(Jan)=Klaas,friend(Jan)=Piet][..]

