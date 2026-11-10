import nelumbo.collections
Person      :: Object
Person      ::= Juliana, Beatrix, Irene, Margriet
Integer     ::= age(<Person>)
Set<Person> ::= children(<Person>)
Person      c
fact age(Beatrix)=20, age(Irene)=15, age(Margriet)=30
fact children(Juliana)={Beatrix, Irene, Margriet}
// no navigation: c gets no values
E[c](age(c)>18) ?
A[c](age(c)>10) ?
// navigate from Juliana to her children
E[c](c in children(Juliana) & age(c)>18) ?
E[c](c in children(Juliana) & age(c)>40) ?
A[c]((c in children(Juliana)) -> age(c)>10) ?
A[c]((c in children(Juliana)) -> age(c)>18) ?
