import nelumbo.strings

// Language

Root ::= attr <Type> <NAME> <Type> #100

Type OT, AT
NAME AN

attr OT AN AT  ::> {
    AT               ::= <OT>.AN                                 #100
    Root             ::= <{OT,Literal}>.AN  := <{AT,Literal}#0>  #100,
                         <{OT,Variable}>.AN := <{AT,Function}#0> #100

    private FactType ::= AN(<OT>,<AT>)

    OT o
    AT a

    {OT,Literal}  ol
    {AT,Literal}  al
    {AT,Function} af

    (ol.AN := al) ::> {
        fact AN(ol,al)
        o.AN=a <=>  AN(o,a)
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
attr Person address String
attr Person friend Person

// Rules

Person p

p.address := p.street + " " + str(p.number)

// Facts

Person ::= Piet, Jan

Piet.name    := "Piet"
Piet.street  := "Kalverstraat"
Piet.number  := 11
Jan.name     := "Jan"
Jan.street   := "Kalverstraat"
Jan.number   := 22
Jan.friend   := Piet

// Queries 

String s
Person p

p.name="Piet"           ? [(p=Piet)][..]
Piet.street=s           ? [(s="Kalverstraat")][..]
p.street="Kalverstraat" ? [(p=Piet),(p=Jan)][..]
Jan.friend.name=s       ? [(s="Piet")][..]

Piet.address=s          ? [(s="Kalverstraat 11")][..]
