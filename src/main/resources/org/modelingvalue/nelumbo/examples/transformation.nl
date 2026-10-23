import nelumbo.collections
import nelumbo.strings

// Language

Root ::= attr <Type> <NAME> <Type> #100

Type OT, AT
NAME AN

attr OT AN AT  ::> {
    AT   ::= <OT>.AN       #100,
             <hidden OT>AN #100

    Root ::= <{OT,Literal}>.AN  := <{AT,Literal}#0>                                  #100,
             <hidden {OT,Variable}>AN := <{AT,Function}#0> <(> "if" <Boolean#0> <)?> #100

    {OT,Literal}  ol
    {AT,Literal}  al

    (ol.AN := al) ::> {
        fact ol.AN = al
    }

    {AT,Function} af
    {Boolean}     bf
    hidden OT     o

    AN ==  o.AN

    (AN := af) ::> {
        o.AN ==  af
    }

    (AN := af if bf) ::> {
        o.AN ==  af if bf
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

hidden Person p

address := street + "  " + str(number) if number < 100
address := street + " " + str(number) if number >= 100

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

name="Piet"               ? [(p=Piet),..][..]
Piet.street               ? [("Kalverstraat")][..]
street="Kalverstraat"     ? [(p=Piet),(p=Jan),..][..]
Piet.address              ? [("Kalverstraat  88")][..]
Jan.address               ? [("Kalverstraat 122")][..]
Jan.friends map [p](name) ? [(["Piet"])][..]
