import nelumbo.collections
import nelumbo.strings

// Language

Root ::= attr <Type> <NAME> <Type> #100

Type OT, AT
NAME AN

attr OT AN AT  ::> {
    AT   ::= <OT>.AN                                 #100

    Root ::= <{OT,Literal}>.AN  := <{AT,Literal}#0>  #100,
             <{OT,Variable}>.AN := <{AT,Function}#0> #100

    OT o
    AT a

    {OT,Literal}  ol
    {AT,Literal}  al
    {AT,Function} af

    (ol.AN := al) ::> {
        fact ol.AN = al
    }

    (o.AN := af) ::> {
        o.AN=a <=>  af=a
    }
}


// Model

Person :: Object

attr Person name String
attr Person street String
attr Person number Integer
attr Person friends Set<Person>

attr Person address String

// Rules

Person p, f

p.address := p.street + " " + str(p.number)

// Facts

Person ::= Piet, Jan

Piet.name    := "Piet"
Piet.street  := "Kalverstraat"
Piet.number  := 11
Jan.name     := "Jan"
Jan.street   := "Kalverstraat"
Jan.number   := 22
Jan.friends  := {Piet}

// Queries 

String       s
Person       p
List<String> l

p.name="Piet"                 ? [(p=Piet),..][..]
Piet.street=s                 ? [(s="Kalverstraat")][..]
p.street="Kalverstraat"       ? [(p=Piet),(p=Jan),..][..]
Piet.address=s                ? [(s="Kalverstraat 11")][..]
Jan.friends map [f](f.name)=l ? [(l=["Piet"])][..]
