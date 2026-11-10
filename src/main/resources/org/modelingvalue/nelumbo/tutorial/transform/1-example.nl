import nelumbo.strings
Root ::= attr <Type> <NAME> <Type> #100
Type OT, AT
NAME AN
attr OT AN AT ::> {
    AT   ::= <OT>.AN                             #30
    Root ::= <{OT,Literal}>.AN := <{AT,Literal}> #30
    {OT,Literal} ol
    {AT,Literal} al
    ol.AN := al ::> {
        fact ol.AN = al
    }
}
Person :: Object
attr Person name String
Person ::= Piet
Piet.name := "Piet"
String n
Piet.name=n ?
