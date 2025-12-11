
<Root> ::= attr <Type> <NAME> <Type>

<Type> OT, AT 
<NAME> AN

attr <OT> AN <AT>  ::> { 
    <AT>       ::= <OT>.AN
    <Relation> ::= AN(<OT>,<AT>)
    <Root>     ::= <OT>.AN := <AT> 
    <OT> o
    <AT> a
    o.AN=a  <=>  AN(o,a)
    o.AN := a ::> {
       AN(o,a)
    }
} 

<Person> :: <Node>
attr <Person> name <String>
attr <Person> address <String>

<Person> ::= Piet, Jan 
<String> s
<Person> p

Piet.name := "Piet"

Piet.name=s ? [(s="Piet")][..]
