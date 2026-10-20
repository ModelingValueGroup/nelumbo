import nelumbo.collections
import nelumbo.strings

// Language

Root ::= attr <Type> <NAME> <Type> #100

Type OT, AT
NAME AN

attr OT AN AT  ::> {
    AT   ::= <OT>.AN                                 #100

    Root ::= <{OT,Literal}>.AN  := <{AT,Literal}#0>                            #100,
             <{OT,Variable}>.AN := <{AT,Function}#0> <(> "if" <Boolean#0> <)?> #100

    OT o
    AT a

    {OT,Literal}  ol
    {AT,Literal}  al
    {AT,Function} af
    {Boolean}     bf

    (ol.AN := al) ::> {
        fact ol.AN = al
    }

    (o.AN := af) ::> {
        o.AN=a <=>  af=a
    }

    (o.AN := af if bf) ::> {
        o.AN=a <=>  af=a if bf
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

p.address := p.street + "  " + str(p.number) if p.number < 100
p.address := p.street + " " + str(p.number) if p.number >= 100

// Facts

Person ::= Piet, Jan

Piet.name    := "Piet"
Piet.street  := "Kalverstraat"
Piet.number  := 88
Jan.name     := "Jan"
Jan.street   := "Kalverstraat"
Jan.number   := 122
Jan.friends  := {Piet}

// Queries 

String       s
Person       p
List<String> l

p.name="Piet"                 ? [(p=Piet),..][..]
Piet.street=s                 ? [(s="Kalverstraat")][..]
p.street="Kalverstraat"       ? [(p=Piet),(p=Jan),..][..]
Piet.address=s                ? [(s="Kalverstraat  88")][..]
Jan.address=s                 ? [(s="Kalverstraat 122")][..]
Jan.friends map [f](f.name)=l ? [(l=["Piet"])][..]
