import nelumbo.collections
Person      :: Object
Person      ::= Juliana, Beatrix, Irene, Margriet
Integer     ::= age(<Person>)
Set<Person> ::= children(<Person>)
Person      c
fact age(Beatrix)=20, age(Irene)=15, age(Margriet)=30
fact children(Juliana)={Beatrix, Irene, Margriet}
// is every child of Juliana older than 12?
A[c]((c in children(Juliana)) -> age(c)>12) ? [()][]
// does Juliana have a child younger than 18?
E[c](c in children(Juliana) & age(c)<18) ? [()][]
