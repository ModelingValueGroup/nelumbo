
import nelumbo.strings

Lidwoord :: Object

Lidwoord ::= de, het

Root ::=  "attr" <Type> <Lidwoord> <NAME> <Type> #100

{
  Type OT, AT
  Lidwoord lw
  NAME n

  attr OT lw n AT ::> {
     Root ::= <lw> n van <OT> is <AT> #0
     Root ::= wat is <lw> n van <OT> ? #0
     private FactType ::= n(<OT>,<AT>)
     OT o
     AT a
     lw n van o is a ::> {
        fact n(o,a)
     }
     wat is lw n van o ? ::> {
        n(o,a) ?
     }
  }
}

Persoon  :: Object
attr Persoon de naam String
attr Persoon het adres String 

Persoon ::= Piet, Jan

de naam van Jan is "Jan"
het adres van Piet is "Kalverstraat"

String a

wat is de naam van Jan ?
wat is het adres van Piet ?
