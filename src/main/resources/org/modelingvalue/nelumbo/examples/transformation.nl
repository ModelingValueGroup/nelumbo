import nelumbo.collections
import nelumbo.strings

// Language

Root ::= <(> in <|> out <)> <Type> <NAME> <Type> #100

Type OT, AT
NAME AN

in OT AN AT  ::> {
    AT               ::= <OT>.AN                                 #100
    Root             ::= <{OT,Literal}>.AN  := <{AT,Literal}#0>  #100
    private FactType ::= AN(<OT>,<AT>)

    OT o
    AT a

    o.AN=a <=>  AN(o,a)

    {OT,Literal}  ol
    {AT,Literal}  al

    (ol.AN := al) ::> {
        fact AN(ol,al)
    }
}

out OT AN AT  ::> {
    AT   ::= <OT>.AN                                 #100
    Root ::= <{OT,Variable}>.AN := <{AT,Function}#0> #100

    OT o
    AT a

    {OT,Literal}  ol
    {AT,Function} af

    (o.AN := af) ::> {
        o.AN=a <=>  af=a
    }
}

// Model

Person :: Object

in Person name String
in Person street String
in Person number Integer
in Person friends Set<Person>

out Person address String

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

p.name="Piet"                 ? [(p=Piet)][..]
Piet.street=s                 ? [(s="Kalverstraat")][..]
p.street="Kalverstraat"       ? [(p=Piet),(p=Jan)][..]
Piet.address=s                ? [(s="Kalverstraat 11")][..]
Jan.friends map [f](f.name)=l ? [(l=["Piet"])][..]
