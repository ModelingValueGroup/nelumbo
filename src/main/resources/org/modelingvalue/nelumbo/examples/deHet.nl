import nelumbo.strings

Lidwoord :: Object

Lidwoord ::= de, het

Root     ::= "attr" <Type> <Lidwoord> <NAME> <Type> #100

{
    Type     OT, AT
    Lidwoord lw
    NAME     n

    attr OT lw n AT ::> {
        Root             ::= <lw> n van <{OT,Literal}> is <{AT,Literal}> #0
        Root             ::= wat is <lw> n van <OT> ? #0
        private FactType ::= n(<OT>,<AT>)

        OT o
        AT a
        wat is lw n van o ? ::> {
            n(o,a) ?
        }

        {OT,Literal} ol
        {AT,Literal} al
        lw n van ol is al ::> {
            fact n(ol,al)
        }
    }
}

Persoon :: Object
attr Persoon de naam String
attr Persoon het adres String

Persoon ::= Piet, Jan

de naam van Jan is "Jan"
het adres van Piet is "Kalverstraat"

wat is de naam van Jan    ?
wat is het adres van Piet ?
