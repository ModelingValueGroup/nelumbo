
<Predicate> ::= <Person> is <Person>   #30
<Person>    ::= a friend of <Person>   #35

<Person> A, B, Who

A is B             <=> A=B
a friend of A = B  <=> friend(A)=B

Who is a friend of Piet   ?   [(Who=Jan),(Who=Klaas),(Who=Piet)][..]

