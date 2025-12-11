
// Language

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

// Model

<Person> :: <Node>

attr <Person> name <String>
attr <Person> address <String>

// Example

<Person> ::= Piet, Jan 

Piet.name := "Piet"
Piet.address := "Kalverstraat"

// Queries

<String> s
<Person> p

p.name="Piet"  ? [(p=Piet)][..]
Piet.address=s ? [(s="Kalverstraat")][..]
