
import nelumbo.strings

// Language

Root ::= attr <Type> <NAME> <Type> #100

Type OT, AT 
NAME AN

attr OT AN AT  ::> { 
    AT       ::= <OT>.AN
    Root     ::= <OT>.AN := <AT>
    private FactType ::= AN(<OT>,<AT>) 
    OT o
    AT a
    o.AN=a  <=>  AN(o,a)
    o.AN := a ::> {
       fact AN(o,a)
    }
} 

// Model

Person :: Object

attr Person name String
attr Person address String
attr Person friend Person

// Example

Person ::= Piet, Jan 

Piet.name    := "Piet"
Piet.address := "Kalverstraat"
Jan.name     := "Jan"
Jan.address  := "Kalverstraat"
Jan.friend   := Piet

// Queries

String s
Person p

p.name="Piet"            ? [(p=Piet)][..]
Piet.address=s           ? [(s="Kalverstraat")][..]
p.address="Kalverstraat" ? [(p=Piet),(p=Jan)][..]
Jan.friend.name=s        ? [(s="Piet")][..]
