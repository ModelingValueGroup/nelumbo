import nelumbo.logic
Person :: Object
Boolean ::= friends(<Person>,<Person>)
Person ::= Piet, Jan, Klaas
fact friends(Piet, Jan),
     friends(Jan,  Klaas)
Person X
friends(Piet, X) ?
